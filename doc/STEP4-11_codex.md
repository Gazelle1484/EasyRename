# STEP 4-11: SAF走査回数削減によるリネーム高速化 Codex回答

## 作業ブランチ

`feature/step4-11-saf-scan-optimization`

作業開始時は `feature/step4-10-rename-performance-log...origin/feature/step4-10-rename-performance-log` で、未コミット差分として以下があった。

- `doc/STEP4-10_codex.md`: STEP4-10実機ログ分析結果の追記
- `doc/STEP4-11.md`: STEP4-11指示ファイルの追加

`main` はSTEP4-10までの変更を含んでいないため、指示の代替案どおり `feature/step4-10-rename-performance-log` から分岐した。

## 実装内容

- `ExecuteRenameUseCase` 側の事前同名チェックを削除した。
- 同名チェックは `SafDocumentDataSource.renameFile()` 内に一本化した。
- `directory.listFiles()` の結果を、対象ファイル探索と同名チェックの両方に使うようにした。
- `FileAlreadyExists` の失敗結果は維持した。
- `EasyRenamePerf` の主要ログは維持した。
- UseCase側に `useCase duplicate precheck skipped` ログを追加した。
- 成功ケースで毎回呼んでいた `exists()` / `canWrite()` / `isFile` の詳細確認ログを削減した。
- Prefix / Suffix / Replace の仕様、CSV仕様、成功後1件更新方式は変更していない。

## 実装コード

### ExecuteRenameUseCase.kt

削除した処理。

```kotlin
storageRepository.existsInSameDirectory(renamePair.directoryUri, renamePair.resolvedNewName)
```

新しい流れ。

```text
1. ValidateRenameUseCaseでファイル名検証
2. UseCase側の同名事前チェックはスキップ
3. storageRepository.renameFile(...) を呼ぶ
4. DataSource側の結果を返す
```

確認用に以下のログを追加した。

```text
EasyRenamePerf: useCase duplicate precheck skipped
```

### SafDocumentDataSource.kt

`directory.listFiles()` の結果を1回取得し、対象探索と同名チェックの両方に使うようにした。

```kotlin
val directoryFiles = directory.listFiles().filter { it.isFile }
val targetFile = directoryFiles.firstOrNull { it.uri == fileUri }
    ?: directoryFiles.firstOrNull { it.name == beforeName }
val duplicateFile = directoryFiles.firstOrNull { file ->
    file.name == newName
}
```

同名ファイルが対象ファイル自身でない場合は、従来どおり失敗にする。

```kotlin
if (duplicateFile != null && duplicateFile.uri != targetFile.uri) {
    return RenameResult(
        beforeName = targetFile.name ?: beforeName,
        afterName = newName,
        success = false,
        errorMessage = "A file with the same name already exists.",
        errorType = RenameErrorType.FileAlreadyExists,
    )
}
```

成功ケースでは、以下の重い可能性がある詳細確認を毎回ログ出力しないようにした。

```kotlin
targetFile.exists()
targetFile.canWrite()
targetFile.isFile
```

`EasyRenamePerf` の対象探索・`renameTo`・SAF全体時間ログは維持している。

### StorageRepository.kt

変更なし。

`existsInSameDirectory()` は残しているが、今回のリネームフローでは呼ばれなくなった。将来の別用途や手動検証に備え、削除はしていない。

### StorageRepositoryImpl.kt

変更なし。

Repository層の性能ログはSTEP4-10のまま維持している。

### RenameMatchingViewModel.kt

変更なし。

成功後1件更新方式、`FileAlreadyExists` 失敗時に対象ファイルや候補を更新しない挙動は既存のまま維持した。

### RenameMatchingFragment.kt

変更なし。

UI仕様は変更していない。

### ErrorDialog.kt

変更なし。

`FileAlreadyExists` は既存どおり「同じ名前のファイルが既に存在します。」相当で表示される。

### その他変更ファイル

- `doc/STEP4-10_codex.md`
- `doc/STEP4-11.md`
- `doc/STEP4-11_codex.md`

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `doc/STEP4-10_codex.md`
- `doc/STEP4-11.md`
- `doc/STEP4-11_codex.md`

## 変更理由

## 全体アーキテクチャ

既存のMVVM + Repository構成を維持した。

```text
UI層
→ ViewModel層
→ UseCase層
→ Repository層
→ SAF DataSource層
```

今回の問題は、1件リネームの中でSAFディレクトリ走査が重複していたことである。UseCase側で事前同名チェックを行い、DataSource側でも対象探索と同名チェックを行っていたため、SAF Providerへのアクセス回数が増えていた。

## 概要

UseCaseはファイル名検証とRepository呼び出しに集中させ、同名チェックはDataSource側へ寄せた。DataSourceは1回取得した `listFiles()` 結果を使い、対象探索と同名チェックをまとめて行う。

## 設計詳細

- `ExecuteRenameUseCase`
  - 責務: ファイル名検証、Repositoryへのリネーム依頼、`sourceFileId` 付与。
  - 同名チェックはDataSourceへ移譲。

- `SafDocumentDataSource`
  - 責務: SAF上の対象ファイル解決、同名チェック、`renameTo` 実行。
  - Android SAF / DocumentFile依存はData層に閉じ込める。

## 採用理由・根拠

同名チェックをDataSource側に一本化した理由は、同名チェックも対象ファイル探索も同じディレクトリ直下の `DocumentFile` 一覧を必要とするためである。UseCase側で事前チェックすると、DataSource側の対象探索と合わせて少なくとも2回のSAF走査が発生する。これはSTEP4-10ログで約0.75〜0.80秒の余分な待ち時間として見えていた。

この設計はSRPにも合っている。UseCaseは「リネーム実行の業務手順」を持ち、DataSourceは「SAF Provider上で安全にリネームできるか」を判断する。Provider依存の同名判断をData層に寄せることで、UI / Domain層にSAFの都合を漏らさない。

## 代替案

- UseCase側の同名チェックを残す
  - 有効な条件: DataSource以外にも複数のStorage実装があり、Domain側で共通チェックしたい場合。
  - 採用しない理由: 現状のStorage実装はSAFであり、実機ログ上の遅さの主因になっているため。

- ディレクトリ一覧をキャッシュする
  - 有効な条件: 大量ファイルを連続リネームし、一覧の鮮度管理を設計できる場合。
  - 採用しない理由: 今回は1機能単位の改善であり、キャッシュ無効化や外部変更検知まで含めると範囲が広がるため。

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
12. 同名ファイルが既にある名前でリネームし、FileAlreadyExistsとして失敗表示されることを確認する
13. 失敗時に元ファイルがリネーム済みにならないことを確認する
14. 失敗時に候補が使用済みにならないことを確認する
15. EasyRenamePerfログでSTEP4-10よりUseCase全体時間が短くなっていることを確認する
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

- 作業開始時のgit status: `feature/step4-10-rename-performance-log...origin/feature/step4-10-rename-performance-log`、`doc/STEP4-10_codex.md` 変更、`doc/STEP4-11.md` 追加あり
- 作成したブランチ名: `feature/step4-11-saf-scan-optimization`
- 分岐元ブランチ: `feature/step4-10-rename-performance-log`
- commit message: `Reduce SAF scans during rename`
- push先ブランチ: `origin/feature/step4-11-saf-scan-optimization`
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
EasyRenamePerf: useCase duplicate precheck skipped
EasyRenamePerf: useCase end elapsedMs=...
EasyRenamePerf: repository rename end elapsedMs=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=...
```

### 6. 比較観点

```text
STEP4-10では総時間が約2186〜2213msだった
STEP4-10ではUseCase全体が約2122〜2164msだった
STEP4-10ではRepository + SAFが約1352〜1405msだった
STEP4-10ではSAF対象探索が約557〜603msだった
STEP4-10ではrenameToが約343〜391msだった

STEP4-11後:
- UseCase全体時間が短くなっているか
- repository rename start前の待ち時間が減っているか
- saf resolve targetが大きく増えていないか
- renameToは従来と同程度か
- FileAlreadyExists時も失敗表示されるか
```

## 後に回す機能メモ

- 自動連番機能
- 自動連番ON/OFFボタン
- 候補ごとの連番カウンタ管理
- 同じ候補を複数ファイルに使える自動連番モード
- SAF処理のDispatchers.IO対応
- リネーム中のローディング表示
- リネームボタンの二重押下防止強化
- 手動更新ボタン
- リネーム成功後の明示的な再読み込み
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- Edge-to-Edge / WindowInsetsの正式対応
- 画面回転時の完全な状態復元
- CSVプレビュー
- リネーム前後の差分プレビュー
- 同名時の自動連番
- 履歴・取り消し機能
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー

## 未解決事項・リスク

- `existsInSameDirectory()` はRepositoryに残っているが、今回のリネームフローでは呼ばれない。将来的に未使用が確定したら削除してよい。
- SAF対象探索自体はまだ `directory.listFiles()` に依存しているため、ファイル数が多い場合は約0.5〜0.6秒程度残る可能性がある。
- `DocumentFile.renameTo()` 自体の約0.3〜0.4秒はProvider依存であり、今回の修正では短縮対象にしていない。
- ViewModel内I/Oの `Dispatchers.IO` 対応はまだ行っていない。

## 次に進めるべきSTEP

実機でSTEP4-11後の `EasyRenamePerf` ログを取得し、STEP4-10と比較する。

期待する改善は以下。

```text
UseCase全体時間:
STEP4-10 約2122〜2164ms
→ STEP4-11で repository rename start前の約0.75〜0.80秒が減る見込み
```

確認後、まだ体感が重い場合は次に以下を検討する。

1. SAF対象探索の削減
2. `Dispatchers.IO` 対応
3. リネーム中ローディング表示
4. 対象ファイル情報のキャッシュ設計
