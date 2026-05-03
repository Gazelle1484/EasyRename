# STEP 4-12: singleUri fast pathによるリネーム高速化 Codex回答

## 作業ブランチ

`feature/step4-12-single-uri-fast-path`

`main` / `develop` にはSTEP4-11の高速化変更が入っていなかったため、STEP4-12の指示どおり、前提実装を含む `feature/step4-11-saf-scan-optimization` から分岐した。

作業開始時点で未コミット差分があった。

- `doc/STEP4-11_codex.md`: STEP4-11後の実機ログ分析とSTEP4-12方針追記
- `doc/STEP4-12.md`: STEP4-12指示ファイル追加

## 実装内容

- `SafDocumentDataSource.renameFile()` の先頭で `DocumentFile.fromSingleUri(context, fileUri)` による直接リネームを試す fast path を追加した。
- fast path が成功した場合は `directory.listFiles()` を実行せず、そのまま成功結果を返すようにした。
- fast path が `false` / 例外 / `DocumentFile` 解決失敗で失敗した場合は、STEP4-11までの `treeUri` 探索方式へfallbackするようにした。
- fallback側では、従来どおり1回の `directory.listFiles()` 結果で対象ファイル探索と `FileAlreadyExists` 検出を行う。
- `RenameResult` に `renamePath` を追加し、`SingleUri` / `TreeUriFallback` のどちらを通ったかログで確認できるようにした。
- `EasyRenamePerf` ログに fast path 成功 / fast path 失敗 / fallback開始 / fallback成功 / fallback失敗 / 最終経路を追加した。
- Repository / UseCase / ViewModel の性能ログにも `path=...` を追加した。
- Prefix / Suffix / Replace の名前解決、CSV仕様、成功後1件更新方式は変更していない。

## 実装コード

### SafDocumentDataSource.kt

リネーム開始直後に fast path を試す。

```kotlin
val fastPathResult = trySingleUriFastPath(fileUri, newName)
if (fastPathResult.success) {
    Log.d(
        TAG_PERF,
        "saf renameFile end elapsedMs=${SystemClock.elapsedRealtime() - start} path=${fastPathResult.renamePath} success=true ...",
    )
    return fastPathResult
}
```

fast path が失敗した場合のみ、既存の `treeUri` 探索方式へ進む。

```kotlin
Log.d(
    TAG_PERF,
    "treeUri fallback start reason=${fastPathResult.errorType ?: fastPathResult.errorMessage} ...",
)
```

`singleUri` 直接リネームは専用メソッドに分けた。

```kotlin
private fun trySingleUriFastPath(fileUri: Uri, newName: String): RenameResult
```

`renameTo` は既存の安全処理を共通利用し、`UnsupportedOperationException` / `SecurityException` / `IllegalArgumentException` / `Exception` を握りつぶさず `RenameResult` に変換する。

```kotlin
private fun renameToSafely(
    targetFile: DocumentFile,
    newName: String,
    beforeName: String,
    renamePath: RenamePath = RenamePath.TreeUriFallback,
): RenameResult
```

fallback側の同名チェックは維持している。

```kotlin
val directoryFiles = directory.listFiles().filter { it.isFile }
val targetFile = directoryFiles.firstOrNull { it.uri == fileUri }
    ?: directoryFiles.firstOrNull { it.name == beforeName }
val duplicateFile = directoryFiles.firstOrNull { file ->
    file.name == newName
}
```

### RenameResult.kt

リネーム経路をログ・調査用に保持するため、`renamePath` を追加した。

```kotlin
val renamePath: RenamePath? = null,
```

追加した enum。

```kotlin
enum class RenamePath {
    SingleUri,
    TreeUriFallback,
}
```

### RenameErrorType.kt

独立ファイルはなく、既存どおり `RenameResult.kt` 内の `RenameErrorType` を利用している。

今回、新しいエラー種別は追加していない。既存の以下を維持した。

- `FileAlreadyExists`
- `PermissionDenied`
- `UnsupportedOperation`
- `RenameFailed`
- `FileNotFound`
- `Unknown`

### その他変更ファイル

- `StorageRepositoryImpl.kt`
  - `repository rename end` ログに `path=${result.renamePath}` を追加。
- `ExecuteRenameUseCase.kt`
  - `useCase end` ログに `path=${resultWithSource.renamePath}` を追加。
- `RenameMatchingViewModel.kt`
  - `rename total`、`renameResult`、状態更新ログに `path=...` を追加。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/model/RenameResult.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `doc/STEP4-12_codex.md`

作業開始前から存在した関連差分。

- `doc/STEP4-11_codex.md`
- `doc/STEP4-12.md`

## 変更理由

## 全体アーキテクチャ

既存の MVVM + UseCase + Repository + SAF DataSource 構成を維持した。

```text
UI層
→ ViewModel層
→ UseCase層
→ Repository層
→ SAF DataSource層
→ Android SAF / DocumentProvider
```

UI層はユーザー操作と状態表示、ViewModel層は画面状態の更新、UseCase層はリネーム操作の業務手順、Repository層はデータ操作の抽象化、DataSource層はAndroid SAF固有処理を担当する。

今回の高速化対象は `DocumentFile` / SAF Provider 依存が強いため、変更の中心を `SafDocumentDataSource` に置いた。これにより、Domain層やUI層に `singleUri` / `treeUri` の判断を漏らさずに済む。

## 概要

STEP4-11後の実機ログでは、`saf resolve target` が約812msで、次の主なボトルネックになっていた。これは `treeUri` から `directory.listFiles()` を実行して対象ファイルを探す時間である。

そこで、まず `fileUri` から直接 `DocumentFile` を作ってリネームする `singleUri` fast path を試し、成功するProviderでは `listFiles()` を省略する設計にした。失敗するProviderでは、これまで成功実績のある `treeUri` 探索方式へfallbackする。

## 設計詳細

- `SafDocumentDataSource`
  - 責務: SAF上の対象ファイル解決、同名チェック、`renameTo` 実行、Provider例外の安全な変換。
  - `trySingleUriFastPath()`: `fromSingleUri` による直接リネームを試す。
  - `renameToSafely()`: `renameTo` の戻り値と例外を `RenameResult` へ変換する。
  - fallback処理: `fromTreeUri` + `listFiles()` で対象探索と同名チェックを行う。

- `RenameResult`
  - 責務: リネーム結果、失敗理由、更新後URI、元ファイルID、リネーム経路を運ぶ。
  - `renamePath`: 実機ログで `SingleUri` / `TreeUriFallback` を判別するための調査用情報。

- `StorageRepositoryImpl`
  - 責務: Domain層からDataSource層への橋渡し。
  - リネーム経路をRepositoryログにも出す。

- `ExecuteRenameUseCase`
  - 責務: ファイル名検証、Repository呼び出し、`sourceFileId` 付与。
  - リネーム方式の選択はDataSourceに任せる。

- `RenameMatchingViewModel`
  - 責務: 選択状態、実行状態、成功後1件更新、エラー表示用状態の更新。
  - 結果ログに `renamePath` を出すが、UI仕様は変更しない。

## 採用理由・根拠

`singleUri` fast path + `treeUri` fallback にした理由は、速度と安定性の両方を取るためである。

`singleUri` 方式は `directory.listFiles()` が不要なため、成功すればSTEP4-11で残っていた約812msの対象探索を省略できる可能性がある。一方、STEP4-6では `fromSingleUri(...).renameTo(...)` で `UnsupportedOperationException` が発生していたため、単独採用は危険である。

そこで、まず高速な経路を試し、失敗時だけ安定実績のある `treeUri` 探索へ戻す構成にした。この設計はKISSとYAGNIにも合っている。Provider別設定やキャッシュ設計を今すぐ持ち込まず、現在のボトルネックに対して最小変更で効果を確認できる。

責務分離の観点では、SAF Providerの違いによる分岐はDataSource層に閉じ込めるべきである。UseCaseやViewModelが `singleUri` / `treeUri` を知ると、UIや業務ロジックがAndroidストレージ実装に引っ張られる。今回の設計では依存方向を維持し、UI層から見れば「1件リネームを実行して `RenameResult` を受け取る」だけで済む。

## 代替案

- `singleUri` 方式だけに戻す
  - 有効な条件: 対象Providerが必ず `fromSingleUri(...).renameTo(...)` をサポートする場合。
  - 採用しない理由: STEP4-6で `UnsupportedOperationException` が実機発生しており、Providerや保存場所によってクラッシュ・失敗リスクがあるため。

- 常に `treeUri` 探索方式を使い続ける
  - 有効な条件: 安定性だけを優先し、1件あたり1秒前後の待ち時間を許容できる場合。
  - 採用しない理由: STEP4-11後のログで `saf resolve target` 約812msが明確なボトルネックとして見えており、改善余地が大きいため。

- ディレクトリ一覧をキャッシュする
  - 有効な条件: 大量ファイルを連続リネームし、外部変更や成功後更新の整合性を設計できる場合。
  - 採用しない理由: キャッシュ無効化、ファイルマネージャー等からの外部変更、同名判定の鮮度管理が必要になり、単機能修正の範囲を超えるため。

## 動作確認方法

```text
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. Prefixモードでマッチング画面へ進む
5. ファイルを1件選択する
6. 候補を1件選択する
7. リネーム実行する
8. リネーム成功することを確認する
9. Suffixモードで1件リネームする
10. Replaceモードで1件リネームする
11. Prefix / Suffix / Replaceの連続リネームでFileNotFoundが再発しないことを確認する
12. UnsupportedOperationException が発生してもアプリが落ちないことを確認する
13. singleUri fast pathが成功した場合、treeUri fallbackへ進んでいないことをログで確認する
14. singleUri fast pathが失敗した場合、treeUri fallbackで成功することをログで確認する
15. 同名ファイルが既にある名前でリネームし、FileAlreadyExistsとして失敗表示されることを確認する
```

## ビルド確認結果

以下を実行した。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果。

```text
BUILD SUCCESSFUL
```

以下も実行した。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果。

```text
BUILD SUCCESSFUL
```

## Git操作結果

- 作業開始時のgit status: `feature/step4-12-single-uri-fast-path`、`doc/STEP4-11_codex.md` 変更、`doc/STEP4-12.md` 追加あり
- 作成したブランチ名: `feature/step4-12-single-uri-fast-path`
- 分岐元ブランチ: `feature/step4-11-saf-scan-optimization`
- commit message: `Add single URI rename fast path`
- push先ブランチ: `origin/feature/step4-12-single-uri-fast-path`
- commit hash: コミット作成後に最終応答で報告
- 未コミット差分の有無: コミット後に確認

## 実機ログ確認手順

### 1. 端末確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

### 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

### 3. 性能ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

### 4. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 5. 最低3回分控えるログ

```text
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=... path=...
EasyRenamePerf: repository rename end elapsedMs=... path=...
EasyRenamePerf: singleUri fast path start
EasyRenamePerf: singleUri fast path success elapsedMs=...
EasyRenamePerf: singleUri fast path failed reason=... elapsedMs=...
EasyRenamePerf: treeUri fallback start
EasyRenamePerf: treeUri fallback success elapsedMs=...
EasyRenamePerf: treeUri fallback failed reason=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=... path=...
```

### 6. 比較観点

```text
STEP4-11:
- rename total: 約1310ms
- useCase: 約1246ms
- repository + SAF: 約1246ms
- saf resolve target: 約812ms
- renameTo: 約404ms

STEP4-12後:
- singleUri fast path success が出ているか
- fast path成功時に treeUri fallback が出ていないか
- saf resolve target が省略されているか
- rename total がSTEP4-11より短くなっているか
- fallback時でもSTEP4-11相当の安定性が維持されているか
- FileAlreadyExists が維持されているか
```

## 後に回す機能メモ

- 自動連番機能
- 自動連番ON/OFFボタン
- 候補ごとの連番カウンタ管理
- 同じ候補を複数ファイルに使える自動連番モード
- singleUri fast pathのON/OFF設定
- fast path前の軽量同名チェック
- Provider別のrename方式選択
- SAF処理のDispatchers.IO対応
- リネーム中のローディング表示
- リネームボタンの二重押下防止強化
- 手動更新ボタン
- リネーム成功後の明示的な再読み込み
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- Edge-to-Edge / WindowInsets正式対応
- 画面回転時の完全な状態復元
- CSVプレビュー
- リネーム前後の差分プレビュー
- 同名時の自動連番
- 履歴・取り消し機能
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー

## 未解決事項・リスク

- `singleUri` fast pathでは `directory.listFiles()` を省略するため、fast path成功時は事前同名チェックを行わない。Providerが同名リネームをどう扱うかはProvider依存である。
- Android標準の外部ストレージProviderでは同名時に `renameTo` が失敗する可能性が高いが、上書きに近い挙動をするProviderが見つかった場合は、fast path前の軽量同名チェックまたはfast pathの設定化を検討する。
- fast pathが失敗するProviderでは、従来どおり `treeUri` fallbackに進むため、STEP4-11と同程度の時間がかかる。
- `DocumentFile.renameTo()` 自体の約0.3〜0.4秒はProvider依存であり、今回の修正では短縮対象にしていない。
- ViewModel内I/Oの `Dispatchers.IO` 対応はまだ行っていない。

## 次に進めるべきSTEP

実機でSTEP4-12後の `EasyRenamePerf` ログを取得し、`path=SingleUri` が出るか確認する。

確認結果に応じて、次のSTEPを判断する。

```text
1. fast path成功で大きく短縮できた場合
   - 同名時の安全性確認
   - fast path ON/OFF設定の要否検討

2. fast pathが失敗してfallbackばかりになる場合
   - Provider別のrename方式選択
   - Dispatchers.IO対応
   - ローディング表示

3. 体感の重さが残る場合
   - SAF処理のバックグラウンド化
   - リネーム中の二重押下防止強化
```
