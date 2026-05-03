````md
# STEP 4-3: 実装（機能単位）用 Codex 指示

STEP4-2では、Repository / SAF層の実装が完了しています。  
このSTEPでは、次の段階として **ViewModel層のみを対象に実装**してください。:contentReference[oaicite:0]{index=0}
回答はdoc.STEP4-3_codex.mdに保存してください。
---

## 目的

- 1機能ずつ確実に完成させる
- UIとDomainをつなぐ状態管理ロジックを実装する
- `TODO("Not yet implemented")` をViewModel層から解消する
- UI未実装でも状態遷移が成立するようにする

---

## 対象範囲（今回実装する範囲）

以下のクラスのみ実装してください。

### ViewModel層

- `HomeViewModel`
- `RenameMatchingViewModel`

### UI State

- `HomeUiState`
- `RenameMatchingUiState`

---

## 指示内容

### STEP 4-3-1: UI状態管理の実装

#### 対象
- `HomeViewModel.uiState`
- `RenameMatchingViewModel.uiState`

#### 実装内容

- `StateFlow` または `MutableStateFlow` を使用する
- 初期状態を明確に定義する
- 状態更新はimmutable（copy）で行う

例：

```kotlin
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState
````

---

### STEP 4-3-2: HomeViewModel 実装

#### 対象メソッド

* `onDirectorySelected`
* `onCsvSelected`
* `clearError`

#### 実装内容

##### onDirectorySelected

* isLoading = true
* `LoadDirectoryFilesUseCase` を呼ぶ
* ファイル数を更新
* ディレクトリ名を設定
* isLoading = false

##### onCsvSelected

* isLoading = true
* `LoadRenameRulesFromCsvUseCase` を呼ぶ
* `GenerateRenameCandidateUseCase` を呼ぶ
* 候補数を更新
* CSV名を設定
* isLoading = false

##### 共通

* 成功時:

  * エラーをクリア
* 失敗時:

  * `AppError` を設定
* `isReadyToStartMatching` を以下で更新:

  * ディレクトリ選択済み
  * CSV選択済み

---

### STEP 4-3-3: RenameMatchingViewModel 実装

#### 対象メソッド

* `selectTargetFile`
* `selectRenameCandidate`
* `executeSelectedRename`
* `refreshAfterRename`

---

#### selectTargetFile

* 指定IDのファイルを選択状態にする
* 他のファイルは未選択にする
* selectedTargetFileId を更新

---

#### selectRenameCandidate

* 指定IDの候補を選択状態にする
* 他は未選択にする
* selectedCandidateId を更新

---

#### canExecuteRename の判定

以下を満たす場合 true:

* selectedTargetFileId != null
* selectedCandidateId != null

---

#### executeSelectedRename

処理フロー：

```text
選択されたファイル取得
→ 選択された候補取得
→ ResolveRenameNameUseCase
→ RenamePair生成
→ ValidateRenameUseCase
→ ExecuteRenameUseCase
→ 結果をuiStateに反映
```

---

#### refreshAfterRename

成功時:

* 対象ファイルを isRenamed = true
* 候補を isUsed = true
* 選択解除
* lastResult 更新

失敗時:

* error を設定

---

### STEP 4-3-4: ViewModel依存注入

#### 実装内容

* コンストラクタ引数でUseCaseを受け取る
* DIライブラリは使用しない

#### 必要に応じて

* `ViewModelProvider.Factory` を実装する

---

## 実装ルール（重要）

* RepositoryやSAFの直接呼び出しは禁止
* UseCase経由でのみ処理する
* 状態は必ずUI Stateに集約する
* null安全を確保する
* 仮データは禁止
* ログ出力は不要

---

## 出力させるもの

```md
## 実装コード

（HomeViewModel / RenameMatchingViewModel / UiState）

## 変更ファイル一覧

- HomeViewModel.kt
- RenameMatchingViewModel.kt
- HomeUiState.kt（必要なら）
- RenameMatchingUiState.kt（必要なら）

## 変更理由

（なぜこの状態管理にしたか）

## 動作確認方法

1. ViewModelを生成
2. ディレクトリ選択メソッド呼び出し
3. CSV選択メソッド呼び出し
4. 状態更新確認
5. 選択処理確認
6. リネーム処理確認

## ビルド確認結果

（成功 or エラー内容）

## 未解決事項・リスク

（UI未接続、非同期処理、例外処理など）
```

---

## ポイント

* 今回は「状態管理だけ」に集中する
* UIはまだ触らない
* SAFは既に実装済みなので呼び出すだけ
* Domainロジックも完成している前提
* 「画面がなくても正しく動くViewModel」を作る

---

## 完了条件

* ViewModelの `TODO` が解消されている
* UI Stateが正しく更新される
* 選択・実行フローが成立している
* ビルドが通る

```
```
