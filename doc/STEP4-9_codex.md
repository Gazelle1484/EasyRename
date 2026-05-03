# STEP 4-9: リネーム後状態更新の性能改善 Codex回答

## 作業ブランチ

`feature/step4-9-rename-performance`

作業開始時点では `feature/step4-8-rename-mode-sort` 上に以下の未コミット差分があった。

- `doc/STEP4-8_codex.md`: STEP4-8実機確認後の原因・性能課題追記
- `doc/STEP4-9.md`: STEP4-9指示ファイルの追加

また、`main` / `develop` はSTEP4-7までで、STEP4-8の実装コミットを含んでいなかった。STEP4-9はSTEP4-8の性能改善であり、`RenameMode` やリネーム成功後再読み込み処理を前提にするため、今回は `feature/step4-8-rename-mode-sort` から `feature/step4-9-rename-performance` を作成した。

## 実装内容

- リネーム成功後に毎回 `HomeViewModel.refreshSelectedDirectoryFiles()` を呼ぶ処理をやめた。
- `RenameResult` に `afterUri` と `sourceFileId` を追加した。
- SAFリネーム成功後、`DocumentFile.renameTo()` 後の `targetFile.uri` を `afterUri` として返すようにした。
- `ExecuteRenameUseCase` で `RenameResult.sourceFileId` に元ファイルIDを設定するようにした。
- `RenameMatchingViewModel` で、成功した1件だけ `displayName` / `uri` / `id` / `isRenamed` を更新するようにした。
- `HomeViewModel` に `applyRenameResult(result)` を追加し、Home側の `targetFiles` も成功した1件だけ更新するようにした。
- Home側・Matching側とも、更新後に辞書順ソートを維持した。
- `FileNotFound` 再発確認用に、`sourceFileId`、`beforeName`、`afterName`、`afterUri`、更新後URIのログを追加した。
- 自動連番、一括リネーム、RenameMode仕様変更、CSV仕様変更は行っていない。

## 実装コード

### RenameResult.kt

`afterUri` と `sourceFileId` を追加した。

```kotlin
data class RenameResult(
    val beforeName: String,
    val afterName: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val errorType: RenameErrorType? = null,
    val afterUri: Uri? = null,
    val sourceFileId: String? = null,
)
```

既存呼び出しが壊れないよう、どちらもデフォルト値を `null` にしている。

### SafDocumentDataSource.kt

`renameTo(newName)` 成功後、同じ `DocumentFile` から取得できる `uri` を `afterUri` として返すようにした。

```kotlin
afterUri = if (renameSuccess) targetFile.uri else null
```

ProviderによってURIが変わる場合は新URI、変わらない場合は既存URIが入る。`afterUri == null` の場合はViewModel側で元URIを維持する。

`FileNotFound` 発生時には `directoryUri` / `fileUri` / `beforeName` をログ出力するようにした。

### StorageRepository.kt

変更なし。

`RenameResult` のプロパティ追加はデフォルト値付きのため、Repositoryインターフェースのシグネチャは変更していない。

### StorageRepositoryImpl.kt

変更なし。

DataSourceから返る `RenameResult.afterUri` をそのままUseCaseへ返す。

### ExecuteRenameUseCase.kt

Repositoryから返った `RenameResult` に、元ファイルIDを追加して返すようにした。

```kotlin
val result = storageRepository.renameFile(...)
return result.copy(sourceFileId = renamePair.sourceFile.id)
```

これにより、ViewModel側で `beforeName` の文字列一致に頼らず、選択していたファイルIDで該当1件を更新できる。

### HomeViewModel.kt

`applyRenameResult(result)` を追加した。

```kotlin
fun applyRenameResult(result: RenameResult)
```

処理内容:

- `result.success == false` の場合は何もしない。
- `targetFiles` のうち `file.id == result.sourceFileId` の1件だけ更新する。
- `displayName = result.afterName`
- `uri = result.afterUri ?: file.uri`
- `id = updatedUri.toString()`
- `isSelected = false`
- `isRenamed = true`
- 更新後に `displayName.lowercase()` で辞書順ソートする。

`refreshSelectedDirectoryFiles()` は将来の手動更新用に残したが、リネーム成功時に自動では呼ばない。

### RenameMatchingViewModel.kt

リネーム成功時のMatching側更新を、1件だけの更新に変更した。

```kotlin
val updatedUri = result.afterUri ?: file.uri
file.copy(
    id = updatedUri.toString(),
    displayName = result.afterName,
    uri = updatedUri,
    isSelected = false,
    isRenamed = true,
)
```

候補側は既存どおり、使用した候補だけ `isUsed = true` にしている。

### RenameMatchingFragment.kt

リネーム成功時に呼ぶ処理を変更した。

変更前:

```kotlin
homeViewModel.refreshSelectedDirectoryFiles()
```

変更後:

```kotlin
homeViewModel.applyRenameResult(result)
```

これにより、成功後に `DocumentFile.listFiles()` によるディレクトリ全体再走査を毎回行わない。

### その他変更ファイル

- `doc/STEP4-8_codex.md`
- `doc/STEP4-9.md`
- `doc/STEP4-9_codex.md`

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/model/RenameResult.kt`
- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `doc/STEP4-8_codex.md`
- `doc/STEP4-9.md`
- `doc/STEP4-9_codex.md`

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

今回の性能問題は、リネーム成功後にUI状態を最新化するためだけにData層まで戻り、SAFでディレクトリ全体を再走査していたことが原因である。そこで、Data層から成功後の最小情報だけを `RenameResult` で返し、ViewModel側で該当1件だけ状態更新する構成にした。

## 概要

STEP4-8の暫定対策では、正確性を優先して成功後に全件再読み込みしていた。STEP4-9ではこれをやめ、成功したファイル1件の `afterName` / `afterUri` だけを使ってHome側とMatching側の状態を更新する。

## 設計詳細

- `RenameResult`
  - 責務: リネーム実行結果と、成功後にUI状態更新へ必要な最小情報を運ぶ。
  - 追加情報: `afterUri`, `sourceFileId`

- `SafDocumentDataSource`
  - 責務: SAFの `renameTo` 実行と、Providerから取得できる成功後URIの取得。
  - 注意: ProviderによってURIが変わらない場合もあるため、ViewModel側は `afterUri ?: oldUri` で扱う。

- `HomeViewModel`
  - 責務: Home画面で保持している対象ファイル一覧を、成功した1件だけ更新する。
  - Repository再呼び出しはしない。

- `RenameMatchingViewModel`
  - 責務: Matching画面内の対象ファイル一覧と候補一覧を、成功した1件だけ更新する。
  - 既存の `isRenamed` / `isUsed` 挙動を維持する。

## 採用理由・根拠

全件再読み込みをやめた理由は、SAFの `DocumentFile.listFiles()` がProvider依存で重く、リネーム成功ごとに実行するとファイル数に比例して待ち時間が増えるためである。今回必要なのは「成功した1件の名前とURIを更新すること」だけなので、全件再走査は過剰である。

`RenameResult` に `afterUri` を持たせたのは、SAFの `renameTo` 後にURIが変わるProviderがあるためである。URIが変わる場合に古いURIを保持すると、次回リネーム時に `FileNotFound` が再発する。Data層で取得できる成功後URIをUseCase経由でViewModelへ返すことで、依存方向を崩さずにこの問題を避けられる。

`sourceFileId` を `ExecuteRenameUseCase` で付与したのは、Data層はUI上のファイルIDを知らないためである。Data層にUI状態の概念を入れず、UseCaseで `RenamePair.sourceFile.id` を結果へ付与することで、責務分離を保っている。

## 代替案

- リネーム成功後も全件再読み込みを続ける
  - 有効な条件: ファイル数が少なく、常に実ストレージと完全同期したい場合。
  - 採用しない理由: 実機で全モードが遅くなっており、今回の目的である性能改善に反するため。

- Matching画面内ではURIを更新せず、Homeに戻ったときだけ再読み込みする
  - 有効な条件: 連続リネームをしない仕様の場合。
  - 採用しない理由: Prefix / Suffix / Replaceを連続して試す実機確認で `FileNotFound` を避ける必要があるため。

## 動作確認方法

```text
1. アプリを起動する
2. Home画面でリネームモードをPrefixにする
3. リネーム対象ディレクトリを選択する
4. CSVファイルを選択する
5. マッチング画面へ進む
6. ファイルを1件選択する
7. 候補を1件選択する
8. リネーム実行する
9. 成功後、動作がSTEP4-8より軽くなっていることを確認する
10. 成功後、リネーム済み表示になることを確認する
11. Homeへ戻る
12. 再度マッチング画面へ進む
13. 直前にリネームしたファイルが新しい名前・新しいURI相当で扱われることを確認する
14. Prefix / Suffix / Replaceで連続リネームしてFileNotFoundが再発しないことを確認する
15. 既存CSV形式 A1-1_* でも従来どおり動くことを確認する
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

- 作業開始時のgit status: `feature/step4-8-rename-mode-sort...origin/feature/step4-8-rename-mode-sort`、`doc/STEP4-8_codex.md` 変更、`doc/STEP4-9.md` 追加あり
- 作成したブランチ名: `feature/step4-9-rename-performance`
- 分岐元: `feature/step4-8-rename-mode-sort`
- commit message: `Optimize post-rename state update`
- push先ブランチ: `origin/feature/step4-9-rename-performance`
- commit hash: コミット作成後に最終応答で報告
- 未コミット差分の有無: コミット後に確認

## 実機ログ確認手順

### 1. 端末確認

```powershell
adb devices
```

### 2. ログクリア

```powershell
adb logcat -c
```

### 3. EasyRenameログ確認

```powershell
adb logcat | findstr EasyRename
```

### 4. クラッシュ確認

```powershell
adb logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 5. 確認する内容

```text
- beforeName
- afterName
- sourceFile.uri
- result.afterUri
- Home側targetFiles更新後のuri
- Matching側targetFiles更新後のuri
- renameTo result=true が維持されているか
- FileNotFound が出ていないか
- リネーム成功後に loadFilesInDirectory / listFiles が毎回走っていないか
```

## 後に回す機能メモ

- 自動連番機能
- 自動連番ON/OFFボタン
- 候補ごとの連番カウンタ管理
- 同じ候補を複数ファイルに使える自動連番モード
- ViewModel内I/Oの本格的なDispatchers.IO対応
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

- `refreshSelectedDirectoryFiles()` は将来の手動更新用に残しているが、現時点では自動呼び出ししない。
- Providerによっては `renameTo` 後の `DocumentFile.uri` が変わらない場合がある。その場合は同じURIを維持する。
- `afterUri` が取得できない場合は旧URIを維持するため、特殊なProviderでは古いURI問題が完全には解消しない可能性がある。
- ViewModel内のSAFアクセスはまだ本格的に `Dispatchers.IO` へ移していない。今回の主目的は全件再読み込み削除のため、I/Oスレッド対応は後続STEPへ回した。
- Home側とMatching側の状態は成功結果をもとに1件更新するため、外部アプリで同じディレクトリを同時に変更した場合の完全同期は保証しない。

## 次に進めるべきSTEP

次は実機でSTEP4-9の性能改善を確認する。

優先確認は以下。

1. Prefix / Suffix / Replaceの連続リネームで `FileNotFound` が再発しないこと。
2. リネーム成功直後の待ち時間がSTEP4-8より短くなっていること。
3. 成功後、Homeに戻って再度Matchingへ進んでも新しいURI相当で扱われること。
4. `EasyRename` ログで `afterUri` と更新後URIが確認できること。

この確認後に、自動連番またはViewModel内I/Oの `Dispatchers.IO` 対応へ進むのがよい。
