````md id="step4-5-prompt"
# STEP 4-5: 実装（機能単位）用 Codex 指示
これまでの状況をdoc/HANDOFF.mdに記載しています。これを読んで状況をキャッチアップしてください。

STEP4-4では、UI層、Home画面、Matching画面、エラー表示、読み込み表示、ViewModel生成Factoryが実装されています。  
また、UI接続のために `LoadDirectoryFilesUseCase`、`LoadRenameRulesFromCsvUseCase`、`ExecuteRenameUseCase` の最小委譲も補われています。:contentReference[oaicite:0]{index=0}

このSTEPでは、次の段階として **リネーム実行統合のみ** を対象に実装してください。回答はdoc/STEP4-5_codex.mdに保存してください。

---

## 目的

- 1機能ずつ確実に完成させる
- 画面上の選択から実ファイルのリネームまでを一連の処理として完成させる
- 実行前検証、同名ファイルチェック、SAFリネーム、UI状態更新を統合する
- STEP4-4で残った未解決問題のうち、本STEPで扱うべきものを解消する
- ビルドが通る状態を維持する

---

## 前提

これまでのSTEPで以下は実装済みです。

- STEP4-1: Domain層のロジック実装
- STEP4-2: Repository / SAF層の実装
- STEP4-3: ViewModel層とUI Stateの実装
- STEP4-4: UI層実装、Factory実装、最小UseCase委譲

今回のSTEP4-5では、**リネーム実行フローの完成**に集中してください。

---

## 対象範囲（今回実装する範囲）

以下のクラスを対象にしてください。

### Domain / UseCase層

- `ExecuteRenameUseCase`
- `ValidateRenameUseCase`
- `ResolveRenameNameUseCase`

### ViewModel層

- `RenameMatchingViewModel`
- 必要に応じて `HomeViewModel`

### Data層

- `StorageRepository`
- `StorageRepositoryImpl`
- `SafDocumentDataSource`

### UI層

- `RenameMatchingFragment`
- 必要に応じて `HomeFragment`

### Model層

- `RenamePair`
- `RenameResult`
- `AppError`

---

## 本STEPで解決すべき未解決問題

STEP4-4の未解決事項のうち、以下は本STEPで解決してください。

### 1. 同名ファイルチェック

- リネーム前に、同一ディレクトリ内に同名ファイルが存在するか確認する
- 同名ファイルがある場合はリネームを実行しない
- UIには「同名ファイルが既に存在する」ことが分かるエラーを表示する

### 2. 実行前検証の統合

以下をリネーム実行前に必ず確認してください。

- 元ファイルが選択されている
- リネーム候補が選択されている
- 解決後の新ファイル名が空でない
- 新ファイル名に不正文字が含まれていない
- 同名ファイルが存在しない

### 3. リネーム後のUI状態更新

成功時は以下を必ず反映してください。

- 対象ファイルを `isRenamed = true` にする
- 使用した候補を `isUsed = true` にする
- 選択状態を解除する
- 実行ボタンを無効化する
- 成功結果を表示する

失敗時は以下を反映してください。

- `isExecuting = false`
- 選択状態は必要に応じて維持する
- エラー理由を表示する
- `RenameResult.success = false` の内容をUIで確認できるようにする

### 4. `DocumentFile.renameTo` 失敗時の扱い

- `renameTo` が `false` を返した場合は失敗扱いにする
- 例外が発生した場合も失敗扱いにする
- `RenameResult.errorMessage` に理由を入れる
- ViewModel側で `AppError.RenameFailed` または適切なエラーとして扱う

---

## 本STEPで解決しなくてよい未解決問題

以下は本STEPでは未対応で構いません。

- 実機での全DocumentProvider差異検証
- UIデザイン調整
- RecyclerView化
- 画面回転時の完全な状態復元
- 一括リネーム
- 履歴・取り消し機能
- 自動連番
- サブディレクトリ対応

---

## 指示内容

## STEP 4-5-1: RenamePairにディレクトリURIを持たせる

### 背景

同名ファイル存在チェックには、対象ファイルがあるディレクトリURIが必要です。  
現在の `RenamePair` が `sourceFile`、`renameCandidate`、`resolvedNewName` のみを持っている場合、同名チェックに必要なディレクトリ情報が不足します。

### 実装内容

以下のいずれかの方法で対応してください。

#### ベスト案

`RenamePair` に `directoryUri: Uri` を追加する。

```kotlin
data class RenamePair(
    val sourceFile: RenameTargetFile,
    val renameCandidate: RenameCandidate,
    val resolvedNewName: String,
    val directoryUri: Uri,
)
````

#### 代替案

`ExecuteRenameUseCase` の引数に `directoryUri` を追加する。

```kotlin
operator fun invoke(renamePair: RenamePair, directoryUri: Uri): RenameResult
```

### 採用方針

* 変更範囲が小さい方を採用する
* ViewModelとUseCaseの責務が分かりやすい実装にする
* UIから直接Repositoryを呼ばない

---

## STEP 4-5-2: HomeUiStateまたは共有状態からdirectoryUriを参照可能にする

### 背景

`RenameMatchingViewModel` がリネーム実行時に同名チェックを行うには、選択済みディレクトリURIが必要です。

### 実装内容

必要であれば `HomeUiState` に以下を追加してください。

```kotlin
val selectedDirectoryUri: Uri? = null
val selectedCsvUri: Uri? = null
```

`HomeViewModel.onDirectorySelected(uri)` 実行時に `selectedDirectoryUri` を保存してください。

### 注意

* URIを文字列ではなく `Uri` として扱う
* Bundleに大量データを詰めない
* Activityスコープの共有ViewModel方針を維持する

---

## STEP 4-5-3: ExecuteRenameUseCaseを統合実装する

### 対象

* `ExecuteRenameUseCase`

### 実装内容

以下の順序で処理してください。

```text
RenamePair受け取り
→ ValidateRenameUseCaseでファイル名検証
→ StorageRepository.existsInSameDirectoryで同名チェック
→ 同名があればRenameResult(success=false)を返す
→ StorageRepository.renameFileでSAFリネーム実行
→ RenameResultを返す
```

### 注意

* 同名チェックはリネーム実行前に行う
* 同名が存在する場合、`renameFile` は呼ばない
* `ValidateRenameUseCase` がfalseの場合も `renameFile` は呼ばない
* エラー内容はUIで表示できる粒度にする

---

## STEP 4-5-4: RenameMatchingViewModelの実行処理を完成させる

### 対象

* `RenameMatchingViewModel.executeSelectedRename`
* `RenameMatchingViewModel.refreshAfterRename`

### 実装内容

* 選択済みファイルを取得する
* 選択済み候補を取得する
* 選択済みディレクトリURIを取得する
* `ResolveRenameNameUseCase` で新ファイル名を解決する
* `RenamePair` を作成する
* `ExecuteRenameUseCase` を呼ぶ
* 結果を `RenameMatchingUiState` に反映する

### 成功時

* 対象ファイルを `isRenamed = true`
* 使用候補を `isUsed = true`
* 選択解除
* `canExecuteRename = false`
* `lastResult` に成功結果を設定
* `error = null`

### 失敗時

* `isExecuting = false`
* `lastResult` に失敗結果を設定
* 適切な `AppError` を設定
* 必要に応じて選択状態を維持する

---

## STEP 4-5-5: UIで結果とエラーを確認しやすくする

### 対象

* `RenameMatchingFragment`
* `ErrorDialog`

### 実装内容

* `lastResult` が更新されたら成功/失敗を画面に表示する
* 失敗時は `errorMessage` も表示する
* `AppError.FileAlreadyExists` をユーザー向けに表示する
* `AppError.InvalidFileName` をユーザー向けに表示する
* `AppError.RenameFailed` をユーザー向けに表示する

### 表示例

```text
成功:
sample.jpg → A1-1_sample.jpg に変更しました。

失敗:
同名ファイルが既に存在します。
```

---

## STEP 4-5-6: URI権限保持の整理

### 背景

STEP4-4時点では、`takePersistableUriPermission` がUIから明示的に呼ばれていません。

### 実装内容

* ディレクトリ選択後に永続権限保持が必要であれば呼び出す
* CSV選択後に読み取り権限保持が必要であれば呼び出す
* 例外が発生してもアプリが落ちないようにする

### 注意

* Activity Result APIから得られるURI権限の扱いを確認する
* 永続権限取得に失敗しても、即時操作が可能なら処理継続してよい
* 失敗時は必要に応じてログまたはAppErrorに変換する

---

## 実装ルール（重要）

* 今回は「リネーム実行統合」だけに集中する
* UIの見た目改善を主目的にしない
* Domain層の既存仕様を大きく変えない
* Repository / SAFの責務分離を維持する
* UIからRepositoryを直接呼ばない
* ViewModelからSAF APIを直接呼ばない
* 同名ファイル存在時は自動連番にしない
* サブディレクトリ対応を追加しない
* 一括リネームを追加しない
* ビルド成功を最優先にする

---

## 出力させるもの

以下の形式で出力してください。

```md
# STEP 4-5: リネーム実行統合 Codex回答

## 実装内容

## 実装コード

### RenamePair.kt

### HomeUiState.kt

### HomeViewModel.kt

### ExecuteRenameUseCase.kt

### RenameMatchingViewModel.kt

### RenameMatchingFragment.kt

### ErrorDialog.kt

### その他変更ファイル

## 変更ファイル一覧

## 変更理由

## 動作確認方法

## ビルド確認結果

## 未解決事項・リスク

## 次に進めるべきSTEP
```

---

## 動作確認方法として含めてほしい内容

以下を確認手順として提示してください。

```text
1. アプリを起動する
2. Home画面でリネーム対象ディレクトリを選択する
3. Home画面でCSVファイルを選択する
4. マッチング画面へ進む
5. 元ファイルを1件選択する
6. リネーム候補を1件選択する
7. 実行ボタンが有効になることを確認する
8. 実行ボタンを押す
9. 成功時、ファイル名が変更されることを確認する
10. 成功時、対象ファイルがリネーム済み表示になることを確認する
11. 成功時、候補が使用済み表示になることを確認する
12. 同名ファイルが存在する候補で実行し、エラー表示になることを確認する
13. 不正なファイル名候補で実行し、エラー表示になることを確認する
```

---

## ビルド確認

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

## 完了条件

* 画面上で選択した元ファイルと候補を使って、1件リネームを実行できる
* 実行前に不正ファイル名を検出できる
* 実行前に同名ファイルを検出できる
* 同名ファイルがある場合はリネームしない
* `DocumentFile.renameTo` 失敗時にアプリが落ちない
* 成功/失敗結果がUIに反映される
* 成功後、対象ファイルと候補の状態が更新される
* ビルドが通る

```
```
