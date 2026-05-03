実機検証結果と、codexの回答から、STEP4-7のプロンプトを生成してください。プロンプトはフォーマットに従ってください。コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。実機で確認するべきログがあれば別途ユーザに指示してください。
プロンプトはフォーマットに従ってください。
## フォーマット　STEP 4-7: 実装（機能単位）
目的：
- 1機能ずつ確実に完成させる

指示方法：
- 「機能単位」で分割して指示する

例：
- ファイルリネーム処理だけ実装
- UI表示だけ実装
- 入力チェックだけ実装

出力させるもの：
- 実装コード
- 変更ファイル一覧
- 動作確認方法

ポイント：
- 一度に全部やらせない
- 小さく分割
# 実機確認結果
1. アプリを起動する：ok
2. Home画面で一番上のボタンがTopAppBarに隠れていないことを確認する：ok
3. リネーム対象ディレクトリを選択する：ok
4. CSVファイルを選択する：ok
5. マッチング画面へ進む：ok
6. 元ファイルを1件選択する：ok
7. リネーム候補を1件選択する：ok
8. 実行ボタンを押す：ok
9. 成功時はファイル名が変更されることを確認する：ok
10. 失敗時はアプリが落ちず、失敗理由が表示されることを確認する：未確認
11. UnsupportedOperationException が発生する場合、ログに詳細が出ていることを確認する：発生しなかった

###### 追加の要望
- 「マッチング画面へ進む」ボタンをandroidのreferenceに従い色をわかりやすく変えてほしい
- 全体敵に文字サイズを2dp程度あげてほしい
- 「リネーム実行」ボタンをandroidのreferenceに従い色をわかりやすく変えてほしい
- マッチング画面の上部の要素がTopAppBarに隠れているので直してほしい
- マッチング画面を左右に二分割し、左側にリネーム前のファイル、右側に
# STEP 4-6: SAFリネーム失敗修正 Codex回答

## 実装内容

STEP4-6では、実機で発生した `UnsupportedOperationException` の原因調査と、SAFリネーム処理の安定化だけを対象に修正した。

主な実装内容は以下。

- `DocumentFile.fromSingleUri(fileUri).renameTo(newName)` 直接実行をやめ、選択済みディレクトリの `treeUri` から `DocumentFile.fromTreeUri` を作成し、直下ファイル一覧から対象ファイルを再探索して `renameTo` するように変更した。
- `StorageRepository.renameFile` / `SafDocumentDataSource.renameFile` に `directoryUri` を渡すようにした。
- `EasyRename` タグで、`fileUri`、`directoryUri`、リネーム前後名、`DocumentFile` の `exists()` / `canWrite()` / `isFile` / `name`、`renameTo` 結果、例外クラス名、例外メッセージをログ出力するようにした。
- `renameTo` を `UnsupportedOperationException` / `SecurityException` / `IllegalArgumentException` / `Exception` で保護し、クラッシュではなく `RenameResult` として失敗を返すようにした。
- `RenameErrorType` を追加し、UI / ViewModelが失敗種別を扱いやすいようにした。
- `UnsupportedOperation` 系の失敗時は、ユーザーに「この保存場所ではファイル名変更がサポートされていません」と表示するようにした。
- Home画面の最上部ボタンがActionBar配下に隠れないよう、Home rootにActionBar高さ相当の上部余白を追加した。

## 実装コード

### SafDocumentDataSource.kt

`renameFile` のシグネチャを以下に変更した。

```kotlin
fun renameFile(directoryUri: Uri, fileUri: Uri, newName: String): RenameResult
```

処理順序は以下。

1. `fromSingleUri` は診断ログ取得用に使う。
2. `fromTreeUri(directoryUri)` で親ディレクトリを解決する。
3. `listFiles()` から、まず `uri` 一致、次にリネーム前ファイル名一致で対象ファイルを探す。
4. リネーム直前に同名ファイルを再確認する。
5. 見つかった `DocumentFile` に対して `renameTo(newName)` を実行する。
6. `renameTo` の戻り値 `false` と例外をすべて失敗結果に変換する。

例外時は以下のような結果を返す。

```kotlin
RenameResult(
    beforeName = beforeName,
    afterName = newName,
    success = false,
    errorMessage = "UnsupportedOperationException: この保存場所ではリネーム操作がサポートされていません。",
    errorType = RenameErrorType.UnsupportedOperation,
)
```

ログタグは `EasyRename` に統一した。

### StorageRepository.kt

`directoryUri` をData層まで渡せるようにした。

```kotlin
fun renameFile(directoryUri: Uri, fileUri: Uri, newName: String): RenameResult
```

### StorageRepositoryImpl.kt

Repository実装も同じシグネチャへ変更し、DataSourceへ委譲する前に実行ログを追加した。

```kotlin
Log.d(LOG_TAG, "StorageRepository.renameFile directoryUri=$directoryUri fileUri=$fileUri newName=$newName")
return safDocumentDataSource.renameFile(directoryUri, fileUri, newName)
```

### ExecuteRenameUseCase.kt

既存の「ファイル名検証 → 同名チェック → リネーム実行」の流れは維持した。

変更点は以下。

- 失敗時に `RenameErrorType.InvalidFileName` / `RenameErrorType.FileAlreadyExists` を設定。
- 実行直前に `directoryUri`、`fileUri`、リネーム前後名をログ出力。
- `storageRepository.renameFile(directoryUri, fileUri, newName)` を呼ぶように変更。

### RenameResult.kt

失敗理由をViewModel / UIで扱いやすくするため、互換性を保って `errorType` を追加した。

```kotlin
data class RenameResult(
    val beforeName: String,
    val afterName: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val errorType: RenameErrorType? = null,
)
```

追加した種別。

```kotlin
enum class RenameErrorType {
    InvalidFileName,
    FileAlreadyExists,
    PermissionDenied,
    UnsupportedOperation,
    RenameFailed,
    FileNotFound,
    Unknown,
}
```

### RenameMatchingViewModel.kt

`executeSelectedRename` に実行前ログと結果ログを追加した。

失敗時は `RenameResult.errorType` を優先して `AppError` に変換する。

- `UnsupportedOperation` → `AppError.UnsupportedOperation`
- `PermissionDenied` → `AppError.PermissionDenied`
- `InvalidFileName` → `AppError.InvalidFileName`
- `FileAlreadyExists` → `AppError.FileAlreadyExists`

これにより、例外が起きてもViewModelでクラッシュせず、UI Stateに失敗結果が反映される。

### RenameMatchingFragment.kt

失敗結果表示で `RenameResult.errorType` を見るようにした。

`UnsupportedOperation` の場合は以下を表示する。

```text
この保存場所ではファイル名変更がサポートされていません。
別のフォルダを選択するか、端末内ストレージのDocuments/Download配下で試してください。
```

その他の失敗では、既存どおり不正ファイル名、同名ファイル、権限不足、詳細メッセージを表示する。

### ErrorDialog.kt

`AppError.UnsupportedOperation` を追加し、ユーザー向けメッセージを表示するようにした。

### HomeFragment.kt

Home画面のroot paddingを以下の考え方に変更した。

```kotlin
setPadding(32, 32 + resolveActionBarHeight(), 32, 32)
```

`resolveActionBarHeight()` でテーマの `android.R.attr.actionBarSize` を取得し、最上部ボタンがActionBarに隠れないようにした。

### その他変更ファイル

### AppError.kt

`UnsupportedOperation` を追加した。

```kotlin
data object UnsupportedOperation : AppError(ErrorType.UnsupportedOperation)
```

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/model/RenameResult.kt`
- `app/src/main/java/com/example/easyrename/model/AppError.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/common/ErrorDialog.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt`
- `doc/STEP4-6_codex.md`

## 変更理由

このアプリは、UI層、ViewModel層、Domain UseCase層、Repository層、SAF DataSource層に分けたMVVM + Repository構成を採用している。

今回もこの依存方向を維持した。

```text
UI
→ ViewModel
→ UseCase
→ StorageRepository
→ SafDocumentDataSource
→ Android SAF / DocumentFile
```

この構成を維持した理由は、SAFや `DocumentFile` のようなAndroid API依存をData層に閉じ込めるためである。UIやDomain層が `DocumentFile` に直接依存すると、Provider依存の例外処理やURI解決ロジックが画面側へ漏れ、テストしにくく保守しにくい構造になる。

今回の失敗原因として最も可能性が高いのは、`fromSingleUri(fileUri)` で得た `DocumentFile` に対して対象Providerが `renameTo` をサポートしていない、またはTree URI経由で取得した子Documentとして扱う必要があるケースである。そのため、選択済みディレクトリの `treeUri` から対象ファイルを再探索して `renameTo` する方式を採用した。

代替として、`fileUri` に直接 `ContentResolver` 操作を行う案もあるが、Providerごとの差異が大きく、今回の「1機能を安全に完成させる」範囲を超えやすい。コピー作成後に元ファイルを削除する擬似リネームも、ファイル破損、重複、削除失敗時の復旧設計が必要になるため、今回のSTEPでは採用していない。

設計原則としては以下を意識した。

- SRP: SAF固有の処理と例外処理は `SafDocumentDataSource` に集中させた。
- DIP: ViewModel / UseCaseはRepositoryインターフェース越しにリネームを依頼する。
- KISS: 一括リネーム、コピー削除、サブディレクトリ探索などは追加しない。
- YAGNI: 現時点で必要な1件リネームの安定化だけに絞った。

## 動作確認方法

```text
1. アプリを起動する
2. Home画面で一番上のボタンがTopAppBarに隠れていないことを確認する
3. リネーム対象ディレクトリを選択する
4. CSVファイルを選択する
5. マッチング画面へ進む
6. 元ファイルを1件選択する
7. リネーム候補を1件選択する
8. 実行ボタンを押す
9. 成功時はファイル名が変更されることを確認する
10. 失敗時はアプリが落ちず、失敗理由が表示されることを確認する
11. UnsupportedOperationException が発生する場合、ログに詳細が出ていることを確認する
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

## 実機ログ確認手順

### 1. 端末接続確認

```powershell
adb devices
```

期待値。

```text
List of devices attached
xxxxxxxx    device
```

### 2. 既存ログをクリア

```powershell
adb logcat -c
```

### 3. アプリを起動してログ取得

```powershell
adb logcat | findstr EasyRename
```

### 4. 確認する操作

```text
1. アプリを起動
2. ディレクトリ選択
3. CSV選択
4. マッチング画面へ進む
5. 元ファイルを選択
6. 候補を選択
7. リネーム実行
8. 失敗時のログを確認
```

### 5. 確認すべきログ内容

```text
- fileUri
- directoryUri
- beforeName
- afterName
- DocumentFile.fromSingleUri の結果
- exists
- canWrite
- isFile
- name
- renameTo result
- exception class
- exception message
```

## 未解決事項・リスク

- `treeUri` 配下から再探索しても `UnsupportedOperationException` が出る場合、そのDocumentProvider自体がリネームをサポートしていない可能性が高い。
- このSTEPでは、コピーして新規作成し元ファイルを削除する擬似リネームは実装していない。失敗時の破損・重複・削除失敗への復旧設計が必要になるためである。
- `uri` 一致で対象を見つけられない場合はファイル名一致へfallbackしている。直下に同名ファイルが存在しない前提では安全だが、ProviderによってURI表現が変わる場合のための現実的なfallbackである。
- 成功後のファイル一覧再読み込みは未対応で、現状はUI上で `isRenamed` / `isUsed` を更新する既存仕様を維持している。
- Home画面の余白修正は最小対応であり、将来的に独自ToolbarやEdge-to-Edge対応を行う場合は、Activity全体のInsets設計を見直す余地がある。

## 次に進めるべきSTEP

次は実機でSTEP4-6のログを確認する。

優先して見るべき分岐は以下。

1. `treeUri` 再探索後の `renameTo result=true` でリネーム成功するか。
2. `renameTo result=false` の場合、`exists` / `canWrite` / `isFile` / `name` が期待どおりか。
3. `UnsupportedOperationException` が継続する場合、保存場所を端末内ストレージの `Documents` / `Download` 配下に変えると成功するか。

実機でもProvider非対応が確定した場合は、次STEPで「非対応Providerとして案内を強化する」か、「コピー作成 + 元ファイル削除の擬似リネームを安全設計する」かを判断する。
