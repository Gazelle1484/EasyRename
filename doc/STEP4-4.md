````md
# STEP 4-4: 実装（機能単位）用 Codex 指示

STEP4-3では、ViewModel層とUI Stateの実装が完了しています。  
このSTEPでは、次の段階として **UI層のみを対象に実装**してください。:contentReference[oaicite:0]{index=0}
回答はdoc/STEP4-4_codex.mdに保存してください。
---

## 目的

- 1機能ずつ確実に完成させる
- ViewModelの状態を画面に表示できるようにする
- SAFのディレクトリ選択・CSV選択をUIから起動できるようにする
- Home画面とMatching画面の最小UIを実装する
- ビルドが通る状態を維持する

---

## 前提

これまでのSTEPで以下は実装済みです。

- STEP4-1: Domain層のロジック実装
- STEP4-2: Repository / SAF層の実装
- STEP4-3: ViewModel層とUI Stateの実装

今回のSTEP4-4では、**UI表示とユーザー操作の接続のみ**を行ってください。

---

## 対象範囲（今回実装する範囲）

以下のクラスのみ実装してください。

### UI層

- `MainActivity`
- `HomeFragment`
- `RenameMatchingFragment`
- `ErrorDialog`
- `LoadingView`

### 必要に応じて追加してよいもの

- Fragment用レイアウトXML
- RecyclerView Adapter
- ViewModelProvider.Factory
- Navigationに必要な最小限の画面遷移処理

---

## 禁止事項

- Domain層の仕様変更
- Repository / SAF層の仕様変更
- ViewModelの大規模変更
- Composeへの移行
- DIライブラリの追加
- 一括リネーム機能の追加
- サブディレクトリ対応の追加
- 自動連番機能の追加
- 仮データのハードコード
- 不要な大規模リファクタリング

---

## 指示内容

## STEP 4-4-1: MainActivity実装

### 対象

- `MainActivity`

### 実装内容

- アプリ起動時に `HomeFragment` を表示する
- 既存の仮TextView表示をやめる
- Fragmentを配置するためのコンテナを用意する
- 画面遷移は最小構成でよい

### 要件

- `HomeFragment` が初期表示されること
- ビルドが通ること
- 既存パッケージ構成を維持すること

---

## STEP 4-4-2: HomeFragment実装

### 対象

- `HomeFragment`
- `HomeUiState`
- Home画面用レイアウト

### 表示するもの

Home画面には以下を表示してください。

- ディレクトリ選択ボタン
- CSV選択ボタン
- 選択中ディレクトリ名
- 選択中CSVファイル名
- 読み込み済みファイル数
- 読み込み済みリネーム候補数
- マッチング画面へ進むボタン
- 読み込み中表示
- エラー表示

### 操作

#### ディレクトリ選択

- Activity Result APIを使う
- `OpenDocumentTree` を使ってディレクトリを選択する
- 選択されたURIを `HomeViewModel.onDirectorySelected(uri)` に渡す

#### CSV選択

- Activity Result APIを使う
- `OpenDocument` を使ってCSVファイルを選択する
- MIME Typeは可能な範囲でCSV / text系を指定する
- 選択されたURIを `HomeViewModel.onCsvSelected(uri)` に渡す

#### マッチング画面へ進む

- `HomeUiState.isReadyToStartMatching == true` の場合のみ有効化する
- 押下時に `RenameMatchingFragment` へ遷移する

---

## STEP 4-4-3: RenameMatchingFragment実装

### 対象

- `RenameMatchingFragment`
- `RenameMatchingUiState`
- Matching画面用レイアウト
- 必要であればRecyclerView Adapter

### 表示するもの

Matching画面には以下を表示してください。

- 左側または上側: リネーム対象ファイル一覧
- 右側または下側: リネーム候補一覧
- 実行ボタン
- リネーム結果表示
- 読み込み中 / 実行中表示
- エラー表示

### リスト表示

#### 元ファイル一覧

表示項目:

- ファイル名
- リネーム済み状態
- 選択中状態

#### リネーム候補一覧

表示項目:

- 候補名
- 使用済み状態
- 選択中状態

### 操作

#### 元ファイル選択

- タップされたファイルIDを `RenameMatchingViewModel.selectTargetFile(fileId)` に渡す
- 選択状態をUIに反映する

#### 候補選択

- タップされた候補IDを `RenameMatchingViewModel.selectRenameCandidate(candidateId)` に渡す
- 選択状態をUIに反映する

#### リネーム実行

- `RenameMatchingUiState.canExecuteRename == true` の場合のみ実行ボタンを有効化する
- 押下時に `RenameMatchingViewModel.executeSelectedRename()` を呼ぶ
- 成功/失敗結果を画面に表示する

---

## STEP 4-4-4: ErrorDialog実装

### 対象

- `ErrorDialog`

### 実装内容

- `AppError` を受け取り、ユーザー向けメッセージを表示する
- DialogまたはSnackbarで表示する
- エラー種別ごとに最低限わかりやすい文言にする

### 対応するエラー

- `CsvReadFailed`
- `DirectoryReadFailed`
- `PermissionDenied`
- `InvalidFileName`
- `FileAlreadyExists`
- `RenameFailed`
- `Unknown`

---

## STEP 4-4-5: LoadingView実装

### 対象

- `LoadingView`

### 実装内容

- 読み込み中または実行中であることが分かる最小UIを実装する
- ProgressBarのみでもよい
- 複雑なデザインは不要

---

## STEP 4-4-6: ViewModelProvider.Factory実装

### 背景

STEP4-3では、ViewModelがUseCaseをコンストラクタ引数として受け取る設計になっています。  
そのため、UIからViewModelを生成するには `ViewModelProvider.Factory` が必要です。

### 実装内容

- 必要であればFactoryを追加する
- DIライブラリは追加しない
- `SafDocumentDataSource`
- `StorageRepositoryImpl`
- 各UseCase
- ViewModel

を手動で組み立てる

### 注意

- Factory実装は最小限にする
- 将来DIを導入しやすいよう、生成処理を1か所に寄せる

---

## STEP 4-4-7: 画面間データ受け渡し

### 課題

`RenameMatchingViewModel` には、初期ファイル一覧と初期候補一覧が必要です。

### 実装方針

以下のいずれかの方法で、最小実装してください。

#### ベスト案

- 共有ViewModelを使う
- `HomeViewModel` または画面共通ViewModelに読み込み済みファイル一覧・候補一覧を保持する
- `RenameMatchingFragment` から同じデータを参照する

#### 代替案

- ActivityスコープのViewModelを追加する
- Home画面で取得したデータをActivityスコープで保持する

### 禁止

- ファイル一覧や候補一覧をBundleに直接大量投入しない
- 仮データでMatching画面を表示しない

---

## 実装ルール（重要）

- UIはViewModelの状態を表示するだけにする
- CSV解析やリネーム名解決をUIに書かない
- SAF操作をUIに直接書かない
- Activity Result APIの結果はViewModelへ渡す
- 状態購読は `StateFlow` を前提にする
- ライフサイクルを考慮してcollectする
- 画面デザインは最小限でよい
- ビルド成功を最優先にする

---

## 出力させるもの

以下の形式で出力してください。

```md
# STEP 4-4: UI層実装 Codex回答

## 実装内容

## 実装コード

### MainActivity.kt

### HomeFragment.kt

### RenameMatchingFragment.kt

### ErrorDialog.kt

### LoadingView.kt

### 追加したレイアウトXML

### 追加したAdapter

### 追加したFactory

## 変更ファイル一覧

## 変更理由

## 動作確認方法

## ビルド確認結果

## 未解決事項・リスク

## 次に進めるべきSTEP
````

---

## 動作確認方法として含めてほしい内容

以下を確認手順として提示してください。

```text
1. アプリを起動する
2. Home画面が表示される
3. ディレクトリ選択ボタンを押す
4. SAFのディレクトリピッカーが起動する
5. ディレクトリ選択後、ファイル数が表示される
6. CSV選択ボタンを押す
7. SAFのファイルピッカーが起動する
8. CSV選択後、候補数が表示される
9. 両方選択後、マッチング画面へ進むボタンが有効になる
10. Matching画面でファイル一覧と候補一覧が表示される
11. 左右から1件ずつ選択できる
12. 両方選択後、実行ボタンが有効になる
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

## 未解決事項として残してよいもの

このSTEPでは以下は未解決で構いません。

* 実機でのSAFプロバイダ差異確認
* 詳細なUIデザイン調整
* 画面回転時の完全な状態復元
* リネーム後のファイル一覧再読み込み
* 一括リネーム
* 履歴・取り消し機能

---

## 完了条件

* UI層の主要な `TODO("Not yet implemented")` が解消されている
* Home画面が表示される
* SAFのディレクトリ選択を起動できる
* SAFのCSV選択を起動できる
* ViewModelのStateFlowを画面に反映できる
* Matching画面にファイル一覧と候補一覧を表示できる
* 選択状態をUIに反映できる
* ビルドが通る

```
```
