````md id="step4-6-prompt"
# STEP 4-6: 実装（機能単位）用 Codex 指示
doc/HANDOFF_v2から情報をキャッチアップしてください。
STEP4-5では、リネーム実行統合として、`RenamePair` への `directoryUri` 追加、同名チェック、実行前検証、`DocumentFile.renameTo` によるリネーム実行、成功/失敗結果のUI反映まで実装されています。:contentReference[oaicite:0]{index=0}

実機検証では、ディレクトリ選択、CSV選択、マッチング画面遷移、元ファイル選択、候補選択までは成功しています。  
一方で、リネーム実行時に `UnsupportedOperationException` が発生し、リネームできませんでした。

このSTEPでは、**リネーム失敗原因の特定と、SAFリネーム処理の安定化のみ**を対象にしてください。
回答はdoc/STEP4-6_codex.mdに保存してください。
---

## 目的

- 1機能ずつ確実に完成させる
- 実機で発生した `UnsupportedOperationException` の原因を特定する
- SAF / DocumentFile のリネーム処理を安全側に修正する
- リネーム失敗時にアプリが落ちないようにする
- 失敗理由をユーザーと開発者が確認できるようにする
- ビルドが通る状態を維持する

---

## 実機検証結果

以下は実機で確認済みです。

```text
成功:
- ホーム画面でリネーム対象ディレクトリを選択できた
- リネーム先名を書いたCSVを選択できた
- リネームマッチング画面に遷移できた
- リネームするファイルを選択できた
- リネーム候補一覧からリネーム名を選択できた

問題:
- リネーム実行時に UnsupportedOperationException が発生した
- リネームは失敗した

UI問題:
- ホーム画面で一番上のボタンがTopAppBarに隠れている
````

---

## 検証に使用したCSV

```text
A1-1_*
A1-2_*
A1-3_*
```

---

## 今回の対象範囲

### 優先対象

* `SafDocumentDataSource`
* `StorageRepositoryImpl`
* `StorageRepository`
* `ExecuteRenameUseCase`
* `RenameResult`
* `RenameMatchingViewModel`
* `RenameMatchingFragment`
* `ErrorDialog`

### 必要に応じて対象

* `HomeFragment`
* `MainActivity`

---

## 禁止事項

* 一括リネーム機能を追加しない
* 自動連番機能を追加しない
* サブディレクトリ対応を追加しない
* RecyclerView化しない
* Composeへ移行しない
* DIライブラリを追加しない
* 大規模なUI刷新をしない
* Domain層の `*` 解釈ルールを変更しない
* CSV仕様を変更しない

---

# STEP 4-6-1: リネーム失敗原因のログ追加

## 目的

`UnsupportedOperationException` がどこで発生しているかを特定する。

## 対象

* `SafDocumentDataSource.renameFile`
* `StorageRepositoryImpl.renameFile`
* `ExecuteRenameUseCase`
* `RenameMatchingViewModel.executeSelectedRename`

## 実装内容

以下の情報を `Log.d` / `Log.e` で確認できるようにしてください。

```text
- リネーム対象 fileUri
- リネーム前ファイル名
- リネーム後ファイル名
- directoryUri
- DocumentFile.fromSingleUri の結果
- DocumentFile.exists()
- DocumentFile.canWrite()
- DocumentFile.isFile()
- DocumentFile.name
- renameTo の戻り値
- 発生した例外クラス名
- 発生した例外メッセージ
```

## 注意

* ログタグは統一すること
* 例: `EasyRename`
* 例外は握りつぶさず、`RenameResult` にも反映すること
* ログ追加は最小限にすること

---

# STEP 4-6-2: `UnsupportedOperationException` を安全に処理する

## 目的

リネーム失敗時にアプリがクラッシュしないようにする。

## 対象

* `SafDocumentDataSource.renameFile`

## 実装内容

`DocumentFile.renameTo(newName)` の呼び出しを `try-catch` で確実に保護してください。

特に以下を明示的に捕捉してください。

```kotlin
UnsupportedOperationException
SecurityException
IllegalArgumentException
Exception
```

## 期待動作

例外発生時はクラッシュさせず、以下のような `RenameResult` を返してください。

```kotlin
RenameResult(
    beforeName = beforeName,
    afterName = newName,
    success = false,
    errorMessage = "UnsupportedOperationException: この保存場所ではリネーム操作がサポートされていません。"
)
```

## 注意

* `renameTo` が `false` を返した場合も失敗扱いにする
* 例外クラス名を `errorMessage` に含める
* UIでユーザーが原因を把握できるようにする

---

# STEP 4-6-3: SAFリネーム方式の代替実装を検討する

## 背景

実機で `UnsupportedOperationException` が発生しているため、現在の `DocumentFile.fromSingleUri(fileUri).renameTo(newName)` が対象URIまたはDocumentProviderでサポートされていない可能性があります。

## 対象

* `SafDocumentDataSource.renameFile`

## 実装方針

以下の順で実装可能性を確認してください。

---

## ベスト案: treeUri配下から対象DocumentFileを再探索してrenameToする

現在の `fileUri` に対して `DocumentFile.fromSingleUri` を使う方式ではなく、選択済みディレクトリの `treeUri` から `DocumentFile.fromTreeUri` を作成し、その直下ファイル一覧から対象ファイルを探して `renameTo` してください。

### 必要な変更

`StorageRepository.renameFile` または `SafDocumentDataSource.renameFile` に `directoryUri` を渡せるようにしてください。

例:

```kotlin
fun renameFile(
    directoryUri: Uri,
    fileUri: Uri,
    newName: String,
): RenameResult
```

### 探索方法

```text
directoryUri から DocumentFile.fromTreeUri を作る
→ listFiles()
→ uri または name が一致する対象ファイルを探す
→ 見つかった DocumentFile に対して renameTo(newName)
```

### 注意

* まず `uri` 一致を優先する
* `uri` 一致で見つからない場合、必要に応じてファイル名一致を使う
* 同名ファイルチェック済みでも、リネーム直前に再確認する

---

## 代替案: リネーム非対応Providerとしてエラー表示する

treeUriから再探索しても `UnsupportedOperationException` が発生する場合、対象のDocumentProviderがリネーム操作をサポートしていない可能性があります。

その場合は無理にコピー＆削除実装に進まず、以下のエラーとして扱ってください。

```text
この保存場所ではファイル名変更がサポートされていません。
別のフォルダを選択するか、端末内ストレージのDocuments/Download配下で試してください。
```

## 禁止

このSTEPでは、コピーして新規作成し、元ファイルを削除する擬似リネームは実装しないでください。
理由は、失敗時にファイル破損・重複・削除失敗が起きる可能性があり、単機能修正の範囲を超えるためです。

---

# STEP 4-6-4: RenameResultのエラー情報を少し強化する

## 目的

失敗理由をViewModel / UIで扱いやすくする。

## 対象

* `RenameResult`
* 必要に応じて `AppError`

## 実装内容

現在 `RenameResult.errorMessage: String?` のみの場合、必要に応じて以下を追加してください。

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

`RenameResult` に以下を追加してもよいです。

```kotlin
val errorType: RenameErrorType? = null
```

## 注意

* 変更範囲が大きくなる場合は、今回は `errorMessage` の改善だけに留める
* 既存UIが壊れないようにする
* ビルド成功を優先する

---

# STEP 4-6-5: UIでリネーム失敗理由を明確に表示する

## 対象

* `RenameMatchingFragment`
* `ErrorDialog`

## 実装内容

`UnsupportedOperationException` または `UnsupportedOperation` 系の失敗時に、以下のような文言を表示してください。

```text
この保存場所ではファイル名変更がサポートされていません。
別のフォルダを選択するか、端末内ストレージのDocuments/Download配下で試してください。
```

その他の失敗では、`RenameResult.errorMessage` を確認できるようにしてください。

---

# STEP 4-6-6: TopAppBarにボタンが隠れる問題を修正する

## 目的

実機検証で確認されたUI問題を、最小変更で修正する。

## 対象

* `HomeFragment`
* 必要に応じて `MainActivity`

## 実装内容

ホーム画面で一番上のボタンがTopAppBarに隠れないようにしてください。

### 対応候補

* ルートViewに上部paddingを追加
* `fitsSystemWindows` 相当の余白を付与
* TopAppBar下にコンテンツが配置されるようにレイアウトを修正
* ScrollViewの上部余白を調整

## 注意

* UIの全面刷新はしない
* 最小修正に留める
* 既存の画面構造を大きく変えない

---

# STEP 4-6-7: ビルド確認と実機確認用ログ手順を出す

## Codexに実施してほしいビルド確認

以下を実行し、結果を報告してください。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

可能であれば以下も実行してください。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

---

## ユーザーが実機で確認すべきログ

Codexは実機そのものを操作できないため、実機確認時に以下をPowerShellで実行できるよう、手順を出してください。

### 1. 端末接続確認

```powershell
adb devices
```

期待値:

```text
List of devices attached
xxxxxxxx    device
```

---

### 2. 既存ログをクリア

```powershell
adb logcat -c
```

---

### 3. アプリを起動してログ取得

```powershell
adb logcat | findstr EasyRename
```

---

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

---

### 5. 確認すべきログ内容

```text
- fileUri
- directoryUri
- beforeName
- afterName
- canWrite
- exists
- isFile
- renameTo result
- exception class
- exception message
```

---

## 出力させるもの

以下の形式で出力してください。

```md
# STEP 4-6: SAFリネーム失敗修正 Codex回答

## 実装内容

## 実装コード

### SafDocumentDataSource.kt

### StorageRepository.kt

### StorageRepositoryImpl.kt

### ExecuteRenameUseCase.kt

### RenameResult.kt

### RenameMatchingViewModel.kt

### RenameMatchingFragment.kt

### ErrorDialog.kt

### HomeFragment.kt

### その他変更ファイル

## 変更ファイル一覧

## 変更理由

## 動作確認方法

## ビルド確認結果

## 実機ログ確認手順

## 未解決事項・リスク

## 次に進めるべきSTEP
```

---

## 動作確認方法として含めてほしい内容

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
11. `UnsupportedOperationException` が発生する場合、ログに詳細が出ていることを確認する
```

---

## 完了条件

* `UnsupportedOperationException` でアプリがクラッシュしない
* リネーム失敗時にユーザー向けエラーが表示される
* 開発者向けログで失敗原因を確認できる
* treeUri配下のDocumentFile再探索方式を試している
* リネーム成功時は従来どおりUI状態が更新される
* ホーム画面の一番上のボタンがTopAppBarに隠れない
* ビルドが通る

```
```
