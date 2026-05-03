codexの回答を読み、実装のためのプロンプトをフォーマットに従って生成してください。

# ユーザからの提案
## リスク・今後の検討点
- `*` に元ファイル名全体を入れるのか、拡張子を除いた名前を入れてはどうか。（後続ステップで統一的に扱うため。）
- CSVの文字コードをUTF-8固定にするか、Shift_JIS対応も行うか検討が必要です。→対応負荷が小さければ対応したい。
- ViewModelのコンストラクタ引数を実際に注入するには、後続で `ViewModelProvider.Factory` などが必要です。→必要であればたいおうしてください。
- 現在は空実装なので、実行時に呼び出すと `TODO("Not yet implemented")` で停止します。→これは本ステップで解消見込みですね。

# フォーマット
## STEP 4: 実装（機能単位）
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

# codeXの回答
# STEP 3: スケルトン生成（空実装） Codex回答

STEP2で確定した **MVVM + Repository** 構成に基づき、後続STEPで安全に実装を進めるためのコンパイル可能なスケルトンを定義します。

このSTEPの目的は、実処理を完成させることではなく、責務・依存関係・ファイル配置を固定することです。そのため、メソッド本体は `TODO("Not yet implemented")` またはコメントのみとし、CSV解析、SAF操作、リネーム処理などのロジックは書きません。

## 全体アーキテクチャ

採用する構成は **MVVM + Repository** です。

```text
UI層
↓
ViewModel層
↓
Domain / UseCase層
↓
Repository層
↓
DataSource層
↓
Android Storage Access Framework
```

UI層は画面表示とユーザー操作の受け渡しだけを担当します。ViewModel層は画面状態を保持し、UseCaseを呼び出します。Domain層はリネーム候補生成、名前解決、検証、実行指示などのアプリ固有ルールを担当します。Data層はRepositoryを通じてSAFやCSV読み込みの詳細を隠蔽します。

この分離を採用する理由は、Android 33以降のストレージ操作がURIベースで複雑になりやすく、UIやViewModelに `ContentResolver` や `DocumentFile` の詳細を混ぜると保守性とテスト容易性が下がるためです。Repositoryを挟むことで、後続STEPでSAF実装を追加しても、UIやUseCaseへの影響を限定できます。

## 概要

EasyRenameは、ユーザーが選択したディレクトリ内のファイルを、CSVで読み込んだ候補名と手動マッチングし、1件ずつリネームするAndroidアプリです。

STEP3では、画面、状態、UseCase、Repository、DataSource、Model、Utilの骨組みを作成します。実装は後続STEPに回し、ここでは「どのクラスが何に依存するか」を固定します。

## ディレクトリ構成

```text
com.example.easyrename
├── ui
│   ├── MainActivity.kt
│   ├── common
│   │   ├── ErrorDialog.kt
│   │   └── LoadingView.kt
│   ├── home
│   │   ├── HomeFragment.kt
│   │   └── HomeUiState.kt
│   └── matching
│       ├── RenameMatchingFragment.kt
│       └── RenameMatchingUiState.kt
├── viewmodel
│   ├── HomeViewModel.kt
│   └── RenameMatchingViewModel.kt
├── domain
│   ├── usecase
│   │   ├── ExecuteRenameUseCase.kt
│   │   ├── GenerateRenameCandidateUseCase.kt
│   │   ├── LoadDirectoryFilesUseCase.kt
│   │   ├── LoadRenameRulesFromCsvUseCase.kt
│   │   ├── ResolveRenameNameUseCase.kt
│   │   └── ValidateRenameUseCase.kt
│   └── validator
│       └── FileNameValidator.kt
├── data
│   ├── repository
│   │   ├── StorageRepository.kt
│   │   └── StorageRepositoryImpl.kt
│   ├── saf
│   │   └── SafDocumentDataSource.kt
│   └── csv
│       └── CsvRuleParser.kt
├── model
│   ├── AppError.kt
│   ├── RenameCandidate.kt
│   ├── RenamePair.kt
│   ├── RenameResult.kt
│   ├── RenameRule.kt
│   └── RenameTargetFile.kt
└── util
    ├── DateFormatUtil.kt
    └── UriDisplayNameUtil.kt
```

## 設計詳細

### UI層

UI層はFragmentベースの初期構成です。ここでは画面のライフサイクルメソッドとViewModel接続予定箇所だけを置き、実際のレイアウト生成やイベント処理は後続STEPで実装します。

Fragmentを採用する理由は、既存のAndroid Viewベース構成と相性がよく、Compose導入より依存追加が少ないためです。将来的に画面が増えても、Activityを肥大化させず画面単位で責務を分けられます。

### ViewModel層

ViewModelはUseCaseをコンストラクタ引数として受け取ります。DIフレームワークはまだ導入せず、依存方向だけを明確にします。

この形にする理由は、ViewModelがRepositoryやSAFの詳細に直接依存しないようにするためです。後続でViewModelFactoryやDIを導入しても、UseCase境界はそのまま使えます。

### Domain層

UseCaseはRepositoryまたは他UseCaseに依存し、リネーム対象の読み込み、CSVルール読み込み、候補生成、名前解決、検証、実行を分けます。

UseCaseを分ける理由は、`*` の名前解決や同名チェックなどのルール変更がUIに波及しないようにするためです。たとえば将来「拡張子を除いた名前だけを `*` に入れる」仕様に変わっても、主に `ResolveRenameNameUseCase` を変更すれば済みます。

### Data層

RepositoryはUseCaseに対する抽象境界です。`StorageRepositoryImpl` は `SafDocumentDataSource` に処理を委譲する予定ですが、このSTEPでは空実装にします。

SAF操作をDataSourceに分ける理由は、Android API呼び出しが複雑化しやすいためです。Repositoryにすべて書く代替案もありますが、CSV読み込み、URI権限、DocumentFile操作が増えると責務が混ざるため、初期から分離します。

### Model層

Modelは画面状態とドメインデータの受け渡しを安定させるために定義します。`AppError` はsealed classにして、UI側がエラー種別で分岐しやすい形にします。

文字列だけでエラーを扱う代替案は簡単ですが、`PermissionDenied` と `FileAlreadyExists` のようにUI表示や復旧手順が異なるエラーを区別しづらくなるため採用しません。

## 採用理由・根拠

MVVM + Repositoryを採用した理由は、今回のアプリが「画面状態」と「ファイル操作」と「リネームルール」を同時に扱うためです。ActivityやFragmentにすべて書くと、ディレクトリ選択、CSV読み込み、候補選択、検証、実行、エラー表示が混在します。

UseCaseを置くことで、ViewModelは「いつ処理を呼ぶか」に集中し、リネーム仕様そのものはDomain層に閉じ込められます。これはSRP（単一責任の原則）に沿っています。

Repositoryを置くことで、UseCaseはSAFの具体APIに依存しません。これはDIP（依存関係逆転の原則）を意識した構成です。特にAndroidのストレージAPIはOSバージョンやプロバイダによる差が出やすいため、変更の影響をData層に閉じ込める価値があります。

一方で、厳密なClean Architectureほど細かくEntityやMapperを分けていません。現段階ではアプリ規模が小さいため、YAGNIの観点から過剰な層分離は避け、MVVM + Repository + UseCaseに留めます。

## 代替案

### 代替案1: Activity / Fragmentに処理を集約する

小さな試作では最も早く作れます。ただし、このアプリではSAF、CSV、検証、左右リストの選択状態が絡むため、画面クラスが肥大化します。短期プロトタイプなら有効ですが、後続STEPで仕様を追加する前提では保守性が下がります。

### 代替案2: 厳密なClean Architectureにする

Entity、Repository Interface、DataSource、Mapper、UseCase、Presentation Modelを厳密に分ける案です。複数人開発や履歴管理、一括リネーム、複数CSV形式などが入る場合は有効です。ただし現段階ではクラス数が増え、初心者が構造を追いにくくなるため採用しません。

## リスク・今後の検討点

- `*` に元ファイル名全体を入れるのか、拡張子を除いた名前を入れるのかを後続STEPで確定する必要があります。
- SAFの `renameTo` は端末やDocumentProviderによって挙動差が出る可能性があります。
- CSVの文字コードをUTF-8固定にするか、Shift_JIS対応も行うか検討が必要です。
- ViewModelのコンストラクタ引数を実際に注入するには、後続で `ViewModelProvider.Factory` などが必要です。
- 現在は空実装なので、実行時に呼び出すと `TODO("Not yet implemented")` で停止します。

## 各ファイルのコード

### app/src/main/java/com/example/easyrename/ui/MainActivity.kt

```kotlin
package com.example.easyrename.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Replace this placeholder with HomeFragment or HomeScreen in the UI implementation step.
        setContentView(TextView(this).apply { text = "EasyRename" })
    }
}
```

### app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt

```kotlin
package com.example.easyrename.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // TODO: Inflate the home screen layout.
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Register directory and CSV pickers with Activity Result API.
        // TODO: Bind HomeViewModel state to the home screen UI.
    }
}
```

### app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt

```kotlin
package com.example.easyrename.ui.home

import com.example.easyrename.model.AppError

data class HomeUiState(
    val selectedDirectoryName: String? = null,
    val selectedCsvFileName: String? = null,
    val targetFileCount: Int = 0,
    val renameCandidateCount: Int = 0,
    val isReadyToStartMatching: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null,
)
```

### app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt

```kotlin
package com.example.easyrename.ui.matching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class RenameMatchingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // TODO: Inflate the rename matching screen layout.
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Display source files on the left and rename candidates on the right.
        // TODO: Bind selection events to RenameMatchingViewModel.
    }
}
```

### app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt

```kotlin
package com.example.easyrename.ui.matching

import com.example.easyrename.model.AppError
import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

data class RenameMatchingUiState(
    val targetFiles: List<RenameTargetFile> = emptyList(),
    val renameCandidates: List<RenameCandidate> = emptyList(),
    val selectedTargetFileId: String? = null,
    val selectedCandidateId: String? = null,
    val canExecuteRename: Boolean = false,
    val isExecuting: Boolean = false,
    val lastResult: RenameResult? = null,
    val error: AppError? = null,
)
```

### app/src/main/java/com/example/easyrename/ui/common/ErrorDialog.kt

```kotlin
package com.example.easyrename.ui.common

import android.content.Context
import com.example.easyrename.model.AppError

object ErrorDialog {

    fun show(context: Context, error: AppError) {
        // TODO: Show AppError with Material dialog or Snackbar.
    }
}
```

### app/src/main/java/com/example/easyrename/ui/common/LoadingView.kt

```kotlin
package com.example.easyrename.ui.common

import android.content.Context
import android.view.View

class LoadingView(context: Context) : View(context) {
    // TODO: Replace with actual loading UI if needed.
}
```

### app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt

```kotlin
package com.example.easyrename.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.easyrename.domain.usecase.GenerateRenameCandidateUseCase
import com.example.easyrename.domain.usecase.LoadDirectoryFilesUseCase
import com.example.easyrename.domain.usecase.LoadRenameRulesFromCsvUseCase
import com.example.easyrename.ui.home.HomeUiState

class HomeViewModel(
    private val loadDirectoryFilesUseCase: LoadDirectoryFilesUseCase,
    private val loadRenameRulesFromCsvUseCase: LoadRenameRulesFromCsvUseCase,
    private val generateRenameCandidateUseCase: GenerateRenameCandidateUseCase,
) : ViewModel() {

    val uiState: HomeUiState = HomeUiState()

    fun onDirectorySelected(uri: Uri) {
        // TODO: Load files from selected directory through LoadDirectoryFilesUseCase.
    }

    fun onCsvSelected(uri: Uri) {
        // TODO: Load rename rules from selected CSV and generate rename candidates.
    }

    fun clearError() {
        // TODO: Clear current AppError from UI state.
    }
}
```

### app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt

```kotlin
package com.example.easyrename.viewmodel

import androidx.lifecycle.ViewModel
import com.example.easyrename.domain.usecase.ExecuteRenameUseCase
import com.example.easyrename.domain.usecase.ResolveRenameNameUseCase
import com.example.easyrename.domain.usecase.ValidateRenameUseCase
import com.example.easyrename.ui.matching.RenameMatchingUiState

class RenameMatchingViewModel(
    private val resolveRenameNameUseCase: ResolveRenameNameUseCase,
    private val validateRenameUseCase: ValidateRenameUseCase,
    private val executeRenameUseCase: ExecuteRenameUseCase,
) : ViewModel() {

    val uiState: RenameMatchingUiState = RenameMatchingUiState()

    fun selectTargetFile(fileId: String) {
        // TODO: Mark the source file as selected and update canExecuteRename.
    }

    fun selectRenameCandidate(candidateId: String) {
        // TODO: Mark the rename candidate as selected and update canExecuteRename.
    }

    fun executeSelectedRename() {
        // TODO: Execute selected rename through ExecuteRenameUseCase.
    }

    fun refreshAfterRename() {
        // TODO: Mark source file and candidate as completed after successful rename.
    }
}
```

### app/src/main/java/com/example/easyrename/domain/usecase/LoadDirectoryFilesUseCase.kt

```kotlin
package com.example.easyrename.domain.usecase

import android.net.Uri
import com.example.easyrename.data.repository.StorageRepository
import com.example.easyrename.model.RenameTargetFile

class LoadDirectoryFilesUseCase(
    private val storageRepository: StorageRepository,
) {

    operator fun invoke(directoryUri: Uri): List<RenameTargetFile> {
        // TODO: Delegate directory loading to StorageRepository.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/domain/usecase/LoadRenameRulesFromCsvUseCase.kt

```kotlin
package com.example.easyrename.domain.usecase

import android.net.Uri
import com.example.easyrename.data.csv.CsvRuleParser
import com.example.easyrename.data.repository.StorageRepository
import com.example.easyrename.model.RenameRule

class LoadRenameRulesFromCsvUseCase(
    private val storageRepository: StorageRepository,
    private val csvRuleParser: CsvRuleParser,
) {

    operator fun invoke(csvUri: Uri): List<RenameRule> {
        // TODO: Read CSV text from StorageRepository and parse it with CsvRuleParser.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/domain/usecase/GenerateRenameCandidateUseCase.kt

```kotlin
package com.example.easyrename.domain.usecase

import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameRule

class GenerateRenameCandidateUseCase {

    operator fun invoke(rules: List<RenameRule>): List<RenameCandidate> {
        // TODO: Convert rename rules to UI candidates.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt

```kotlin
package com.example.easyrename.domain.usecase

import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameTargetFile

class ResolveRenameNameUseCase {

    operator fun invoke(sourceFile: RenameTargetFile, candidate: RenameCandidate): String {
        // TODO: Resolve wildcard pattern into actual destination file name.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/domain/usecase/ValidateRenameUseCase.kt

```kotlin
package com.example.easyrename.domain.usecase

import com.example.easyrename.model.RenamePair

class ValidateRenameUseCase {

    operator fun invoke(renamePair: RenamePair): Boolean {
        // TODO: Validate empty name, invalid characters, and duplicate file name.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt

```kotlin
package com.example.easyrename.domain.usecase

import com.example.easyrename.data.repository.StorageRepository
import com.example.easyrename.model.RenamePair
import com.example.easyrename.model.RenameResult

class ExecuteRenameUseCase(
    private val storageRepository: StorageRepository,
    private val validateRenameUseCase: ValidateRenameUseCase,
) {

    operator fun invoke(renamePair: RenamePair): RenameResult {
        // TODO: Validate the pair and execute rename through StorageRepository.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/domain/validator/FileNameValidator.kt

```kotlin
package com.example.easyrename.domain.validator

class FileNameValidator {

    fun isValid(fileName: String): Boolean {
        // TODO: Implement Android-compatible file name validation.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt

```kotlin
package com.example.easyrename.data.repository

import android.net.Uri
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

interface StorageRepository {

    fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile>

    fun readTextFromUri(uri: Uri): String

    fun renameFile(fileUri: Uri, newName: String): RenameResult

    fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean

    fun takePersistablePermission(uri: Uri)
}
```

### app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt

```kotlin
package com.example.easyrename.data.repository

import android.net.Uri
import com.example.easyrename.data.saf.SafDocumentDataSource
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

class StorageRepositoryImpl(
    private val safDocumentDataSource: SafDocumentDataSource,
) : StorageRepository {

    override fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile> {
        // TODO: Delegate to SafDocumentDataSource.
        TODO("Not yet implemented")
    }

    override fun readTextFromUri(uri: Uri): String {
        // TODO: Delegate to SafDocumentDataSource.
        TODO("Not yet implemented")
    }

    override fun renameFile(fileUri: Uri, newName: String): RenameResult {
        // TODO: Delegate to SafDocumentDataSource.
        TODO("Not yet implemented")
    }

    override fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean {
        // TODO: Delegate to SafDocumentDataSource.
        TODO("Not yet implemented")
    }

    override fun takePersistablePermission(uri: Uri) {
        // TODO: Delegate to SafDocumentDataSource.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt

```kotlin
package com.example.easyrename.data.saf

import android.content.Context
import android.net.Uri
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

class SafDocumentDataSource(
    private val context: Context,
) {

    fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile> {
        // TODO: Use DocumentFile.fromTreeUri and return direct child files only.
        TODO("Not yet implemented")
    }

    fun readTextFromUri(uri: Uri): String {
        // TODO: Use ContentResolver to read CSV text.
        TODO("Not yet implemented")
    }

    fun renameFile(fileUri: Uri, newName: String): RenameResult {
        // TODO: Use DocumentFile.renameTo or SAF-compatible rename operation.
        TODO("Not yet implemented")
    }

    fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean {
        // TODO: Check direct child file names in selected directory.
        TODO("Not yet implemented")
    }

    fun takePersistablePermission(uri: Uri) {
        // TODO: Persist read/write URI permission for user-selected SAF Uri.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/data/csv/CsvRuleParser.kt

```kotlin
package com.example.easyrename.data.csv

import com.example.easyrename.model.RenameRule

class CsvRuleParser {

    fun parse(csvText: String): List<RenameRule> {
        // TODO: Parse CSV as one rule per line.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/model/RenameTargetFile.kt

```kotlin
package com.example.easyrename.model

import android.net.Uri

data class RenameTargetFile(
    val id: String,
    val displayName: String,
    val uri: Uri,
    val size: Long?,
    val lastModified: Long?,
    val isSelected: Boolean = false,
    val isRenamed: Boolean = false,
)
```

### app/src/main/java/com/example/easyrename/model/RenameRule.kt

```kotlin
package com.example.easyrename.model

data class RenameRule(
    val id: String,
    val rawPattern: String,
    val prefix: String,
    val suffix: String,
    val hasWildcard: Boolean,
    val isUsed: Boolean = false,
)
```

### app/src/main/java/com/example/easyrename/model/RenameCandidate.kt

```kotlin
package com.example.easyrename.model

data class RenameCandidate(
    val id: String,
    val ruleId: String,
    val displayName: String,
    val rawPattern: String,
    val isSelected: Boolean = false,
    val isUsed: Boolean = false,
)
```

### app/src/main/java/com/example/easyrename/model/RenamePair.kt

```kotlin
package com.example.easyrename.model

data class RenamePair(
    val sourceFile: RenameTargetFile,
    val renameCandidate: RenameCandidate,
    val resolvedNewName: String,
)
```

### app/src/main/java/com/example/easyrename/model/RenameResult.kt

```kotlin
package com.example.easyrename.model

data class RenameResult(
    val beforeName: String,
    val afterName: String,
    val success: Boolean,
    val errorMessage: String? = null,
)
```

### app/src/main/java/com/example/easyrename/model/AppError.kt

```kotlin
package com.example.easyrename.model

sealed class AppError(
    val type: ErrorType,
    val message: String? = null,
    val cause: Throwable? = null,
) {
    data object CsvReadFailed : AppError(ErrorType.CsvReadFailed)
    data object DirectoryReadFailed : AppError(ErrorType.DirectoryReadFailed)
    data object PermissionDenied : AppError(ErrorType.PermissionDenied)
    data object InvalidFileName : AppError(ErrorType.InvalidFileName)
    data object FileAlreadyExists : AppError(ErrorType.FileAlreadyExists)
    data object RenameFailed : AppError(ErrorType.RenameFailed)
    data class Unknown(
        val detailMessage: String,
        val throwable: Throwable? = null,
    ) : AppError(ErrorType.Unknown, detailMessage, throwable)
}

enum class ErrorType {
    CsvReadFailed,
    DirectoryReadFailed,
    PermissionDenied,
    InvalidFileName,
    FileAlreadyExists,
    RenameFailed,
    Unknown,
}
```

### app/src/main/java/com/example/easyrename/util/DateFormatUtil.kt

```kotlin
package com.example.easyrename.util

object DateFormatUtil {

    fun format(timestamp: Long?): String {
        // TODO: Format lastModified value for UI display.
        TODO("Not yet implemented")
    }
}
```

### app/src/main/java/com/example/easyrename/util/UriDisplayNameUtil.kt

```kotlin
package com.example.easyrename.util

import android.content.Context
import android.net.Uri

object UriDisplayNameUtil {

    fun getDisplayName(context: Context, uri: Uri): String {
        // TODO: Resolve display name from ContentResolver metadata.
        TODO("Not yet implemented")
    }
}
```

## ビルド要件

現時点で確認できる依存関係は以下です。

- Kotlin Android plugin: `libs.plugins.kotlin.android` が `app/build.gradle.kts` で指定済み
- lifecycle-viewmodel: `libs.androidx.lifecycle.viewmodel.ktx` が指定済み
- fragment: 直接依存は未指定。`androidx.fragment:fragment-ktx` または `androidx.fragment:fragment` の追加検討が必要
- activity-ktx: 直接依存は未指定。Activity Result APIを使う後続STEPでは `androidx.activity:activity-ktx` の追加検討が必要

`assembleDebug` は確認を試みましたが、実行環境で `JAVA_HOME` が未設定のためGradleが起動できませんでした。

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

したがって、このSTEPではコード構造と依存関係の静的確認までを完了とし、実ビルド確認はJDK設定後に再実行する必要があります。
