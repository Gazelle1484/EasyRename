回答はSTEP4_codex.mdに保存すること。

````md
# STEP 4: 実装（機能単位）用 Codex 指示

CodexのSTEP3回答では、MVVM + Repository構成のスケルトンが作成済みで、現状は多くの処理が `TODO("Not yet implemented")` の空実装です。次のSTEPでは、すべてを一度に実装せず、**機能単位で小さく実装**してください。:contentReference[oaicite:0]{index=0}

---

## 目的

- 1機能ずつ確実に完成させる
- `TODO("Not yet implemented")` を段階的に解消する
- ビルドが通る状態を維持する
- Android 33 / SAF前提で安全に実装する

---

## STEP 4-1: ドメインロジック実装

### 対象

- `CsvRuleParser`
- `GenerateRenameCandidateUseCase`
- `ResolveRenameNameUseCase`
- `FileNameValidator`
- `ValidateRenameUseCase`

### 実装内容

- CSVは1行1ルールとして読み込む
- 空行は無視する
- 前後空白は除去する
- `*` を含む場合はワイルドカードとして扱う
- `*` には **元ファイル名から拡張子を除いた名前** を差し込む
- 拡張子は元ファイルの拡張子を維持する
- `*` を含まない場合はCSVの文字列をそのまま候補名として扱う
- ファイル名が空、不正文字を含む場合はエラー扱いにする

### 文字コード方針

- まずUTF-8対応を基本とする
- 対応負荷が小さい場合はShift_JIS読み込みにも対応する
- Shift_JIS対応が大きくなる場合は、実装せず理由を報告する

---

## STEP 4-2: Repository / SAF実装

### 対象

- `StorageRepositoryImpl`
- `SafDocumentDataSource`
- `UriDisplayNameUtil`
- `DateFormatUtil`

### 実装内容

- `DocumentFile.fromTreeUri` を使ってディレクトリ直下のファイル一覧を取得する
- サブディレクトリは対象外
- `ContentResolver` でCSVテキストを読み込む
- URI権限を必要に応じて保持する
- `DocumentFile.renameTo` でリネームする
- 同一ディレクトリ内の同名ファイル存在チェックを行う
- 失敗時は `RenameResult` または `AppError` で返す

---

## STEP 4-3: ViewModel実装

### 対象

- `HomeViewModel`
- `RenameMatchingViewModel`
- `HomeUiState`
- `RenameMatchingUiState`

### 実装内容

- `uiState` を `StateFlow` または `LiveData` で管理する
- ディレクトリ選択時にファイル一覧を読み込む
- CSV選択時にリネーム候補を読み込む
- 左右リストの選択状態を管理する
- 元ファイルと候補が両方選択された場合のみ実行可能にする
- リネーム成功後は以下を更新する
  - 元ファイルをリネーム済みにする
  - 使用済み候補を使用済みにする
  - 選択状態を解除する
  - 結果をUI状態に反映する

### ViewModel生成

- コンストラクタ引数が必要なため、必要であれば `ViewModelProvider.Factory` を実装する
- DIライブラリは追加しない

---

## STEP 4-4: UI実装

### 対象

- `MainActivity`
- `HomeFragment`
- `RenameMatchingFragment`
- `ErrorDialog`
- `LoadingView`

### 実装内容

- Home画面で以下を表示する
  - ディレクトリ選択ボタン
  - CSV選択ボタン
  - 選択中ディレクトリ名
  - 選択中CSV名
  - 読み込み済みファイル数
  - 読み込み済み候補数
  - マッチング画面へ進むボタン
- SAFのディレクトリピッカーを起動する
- SAFのCSVファイルピッカーを起動する
- Matching画面で以下を表示する
  - 左側: 元ファイル一覧
  - 右側: リネーム候補一覧
  - 実行ボタン
  - 結果表示
- エラーはDialogまたはSnackbarで表示する

---

## STEP 4-5: リネーム実行統合

### 対象

- `ExecuteRenameUseCase`
- `RenameMatchingViewModel`
- `StorageRepositoryImpl`
- `SafDocumentDataSource`

### 実装内容

以下の順序でリネームを実行する。

```text
元ファイル選択
→ リネーム候補選択
→ 新ファイル名解決
→ ファイル名検証
→ 同名ファイル確認
→ SAFでリネーム
→ 結果をUIに反映
````

---

## 禁止事項

* 一度に全機能を実装しない
* 不要な大規模リファクタリングをしない
* Composeへ移行しない
* DIライブラリを追加しない
* サブディレクトリ対応を勝手に追加しない
* 一括リネーム機能を追加しない
* 自動連番機能を追加しない
* 仕様未確定の機能を勝手に実装しない

---

## 出力してほしいもの

各STEPごとに以下を出力してください。

```md
## 実装内容

## 変更ファイル一覧

## 変更理由

## 動作確認方法

## ビルド確認結果

## 未解決事項・リスク
```

---

## 完了条件

* `TODO("Not yet implemented")` が対象機能から解消されている
* ビルドが通る
* 最低限、以下の流れが動作する

  * ディレクトリ選択
  * CSV選択
  * ファイル一覧表示
  * 候補一覧表示
  * 1件リネーム
  * 成功/失敗表示

```
```
