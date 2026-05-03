# STEP 3: スケルトン生成（空実装）用 Codex 指示

このSTEPでは、STEP2で確定した設計に基づいて、**コンパイル可能なスケルトンコード（空実装）を生成してください。回答はC:\Users\gazel\AndroidStudioProjects\EasyRename\doc\STEP3_codex.mdに保存すること。**

---

## 目的

- プロジェクトの土台を作る
- 後続STEPで安全に実装を進められる状態を作る
- クラス・構造・依存関係を固定する

---

## 前提

- アーキテクチャ: MVVM + Repository
- 言語: Kotlin
- パッケージ構成: STEP2の設計に従う
- ストレージ: Android Storage Access Framework（URIベース）
- UI: Fragmentベース（初期構成）

---

## 指示内容

以下のルールに従ってスケルトンを生成してください。

### 1. クラス生成

STEP2で定義された以下のクラスをすべて作成してください。

#### UI層
- MainActivity
- HomeFragment（またはHomeScreen）
- RenameMatchingFragment（またはRenameMatchingScreen）
- ErrorDialog
- LoadingView

#### ViewModel層
- HomeViewModel
- RenameMatchingViewModel

#### Domain層（UseCase / Validator）
- LoadDirectoryFilesUseCase
- LoadRenameRulesFromCsvUseCase
- GenerateRenameCandidateUseCase
- ResolveRenameNameUseCase
- ValidateRenameUseCase
- ExecuteRenameUseCase
- FileNameValidator

#### Data層
- StorageRepository（interface）
- StorageRepositoryImpl
- SafDocumentDataSource
- CsvRuleParser

#### Model層
- RenameTargetFile
- RenameRule
- RenameCandidate
- RenamePair
- RenameResult
- AppError（ErrorType含む）

#### Util層
- DateFormatUtil
- UriDisplayNameUtil

---

### 2. メソッド定義

各クラスに対して、STEP2で定義された責務に基づき、**必要なメソッドのシグネチャのみ定義してください。**

例：

```kotlin
fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile>
fun renameFile(fileUri: Uri, newName: String): RenameResult
````

---

### 3. 実装ルール（重要）

以下を厳守してください。

* メソッドの中身はすべて `TODO()` または空実装にする
* ロジックは一切書かない
* Android API呼び出しは書かない（雛形のみ）
* 仮実装（ダミーデータ）も禁止
* コメントで「何をするか」だけ書く

例：

```kotlin
fun renameFile(fileUri: Uri, newName: String): RenameResult {
    // TODO: SAFを使ってファイル名を変更する
    TODO("Not yet implemented")
}
```

---

### 4. UIクラスの扱い

* Fragmentは最低限の構造のみ
* onCreateView / onViewCreated のみ定義
* レイアウト参照は仮でもよい（未定義OK）
* ViewModel取得は雛形だけ書く

---

### 5. ViewModelの状態

以下のStateクラスを作成してください。

#### HomeUiState

```text
selectedDirectoryName
selectedCsvFileName
targetFileCount
renameCandidateCount
isReadyToStartMatching
isLoading
error
```

#### RenameMatchingUiState

```text
targetFiles
renameCandidates
selectedTargetFileId
selectedCandidateId
canExecuteRename
isExecuting
lastResult
error
```

* data class または StateFlow 前提で定義
* 初期値も定義する（null or empty）

---

### 6. エラー設計

* AppErrorをsealed classで定義
* ErrorTypeもenumまたはsealed classで定義

例：

```kotlin
sealed class AppError {
    object CsvReadFailed : AppError()
    object PermissionDenied : AppError()
    data class Unknown(val message: String) : AppError()
}
```

---

### 7. 依存関係

* UseCaseはRepositoryに依存
* ViewModelはUseCaseに依存
* UIはViewModelに依存
* 依存注入は「コンストラクタ引数」で仮定義（DIフレームワーク不要）

---

### 8. パッケージ構成

STEP2の以下構成に従って配置してください。

```text
com.example.easyrename
├── ui
├── viewmodel
├── domain
├── data
├── model
├── util
```

---

## 出力させるもの

以下をすべて出力してください。

### 1. ディレクトリ構成

```text
com.example.easyrename
├── ...
```

---

### 2. 各ファイルのコード

* すべてのクラス
* Kotlinコードとして出力
* ファイル単位で区切る

---

### 3. ビルド要件（不足している場合）

以下が不足している場合のみ指摘してください。

* Kotlin Android plugin
* lifecycle-viewmodel
* fragment
* activity-ktx

※追加はしない、指摘のみ

---

## 禁止事項

* 実装を書くこと
* Android APIを実際に呼び出すこと
* 仮ロジックを書くこと
* データをハードコードすること
* UIを完成させること
* リファクタリング
* 設計変更

---

## 最重要ポイント

* **「動くこと」ではなく「構造を固めること」が目的**
* **TODOだらけでよい**
* **ビルドが通ることだけを保証する**
* **責務に従ったクラス配置を優先する**

```
```
