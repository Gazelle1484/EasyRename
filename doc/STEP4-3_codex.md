このSTEPでは、STEPで確定し3た空実装に基づいて、**コンパイル可能な実装コードを生成したいです。codexの回答をもとにフォーマットに従ってSTEP4-の4プロンプトを生成してください。
## フォーマット　STEP 4-4: 実装（機能単位）
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

# STEP 4-3: ViewModel層実装 Codex回答

今回は指示どおり、**ViewModel層とUI Stateのみ**を対象に実装しました。UI、Repository / SAF、UseCaseの追加実装には触れていません。

## 全体アーキテクチャ

採用している構成は、これまでと同じ **MVVM + Repository** です。

```text
UI層
↓
ViewModel層
↓
Domain / UseCase層
↓
Repository層
↓
SAF / ContentResolver
```

今回のSTEP4-3では、UI層とDomain層の間にあるViewModelの状態管理を実装しました。ViewModelはRepositoryやSAFを直接呼ばず、必ずUseCase経由で処理します。

この設計にした理由は、ViewModelを「画面状態の管理」と「UseCase呼び出し」に集中させるためです。ViewModelからRepositoryを直接呼ぶ代替案もありますが、その場合ファイル操作やCSV読み込みの詳細がPresentation層へ漏れ、テストしにくくなります。

## 実装コード

### HomeViewModel.kt

```kotlin
package com.example.easyrename.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.easyrename.domain.usecase.GenerateRenameCandidateUseCase
import com.example.easyrename.domain.usecase.LoadDirectoryFilesUseCase
import com.example.easyrename.domain.usecase.LoadRenameRulesFromCsvUseCase
import com.example.easyrename.model.AppError
import com.example.easyrename.ui.home.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val loadDirectoryFilesUseCase: LoadDirectoryFilesUseCase,
    private val loadRenameRulesFromCsvUseCase: LoadRenameRulesFromCsvUseCase,
    private val generateRenameCandidateUseCase: GenerateRenameCandidateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onDirectorySelected(uri: Uri) {
        _uiState.update { state ->
            state.copy(isLoading = true, error = null)
        }

        runCatching {
            loadDirectoryFilesUseCase(uri)
        }.onSuccess { files ->
            _uiState.update { state ->
                state.copy(
                    selectedDirectoryName = resolveDisplayName(uri),
                    targetFileCount = files.size,
                    isReadyToStartMatching = state.selectedCsvFileName != null,
                    isLoading = false,
                    error = null,
                )
            }
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = AppError.Unknown(
                        detailMessage = throwable.message ?: "Failed to load directory files.",
                        throwable = throwable,
                    ),
                )
            }
        }
    }

    fun onCsvSelected(uri: Uri) {
        _uiState.update { state ->
            state.copy(isLoading = true, error = null)
        }

        runCatching {
            val rules = loadRenameRulesFromCsvUseCase(uri)
            generateRenameCandidateUseCase(rules)
        }.onSuccess { candidates ->
            _uiState.update { state ->
                state.copy(
                    selectedCsvFileName = resolveDisplayName(uri),
                    renameCandidateCount = candidates.size,
                    isReadyToStartMatching = state.selectedDirectoryName != null,
                    isLoading = false,
                    error = null,
                )
            }
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = AppError.Unknown(
                        detailMessage = throwable.message ?: "Failed to load CSV rules.",
                        throwable = throwable,
                    ),
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { state ->
            state.copy(error = null)
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
    }
}
```

### RenameMatchingViewModel.kt

```kotlin
package com.example.easyrename.viewmodel

import androidx.lifecycle.ViewModel
import com.example.easyrename.domain.usecase.ExecuteRenameUseCase
import com.example.easyrename.domain.usecase.ResolveRenameNameUseCase
import com.example.easyrename.domain.usecase.ValidateRenameUseCase
import com.example.easyrename.model.AppError
import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenamePair
import com.example.easyrename.model.RenameTargetFile
import com.example.easyrename.ui.matching.RenameMatchingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RenameMatchingViewModel(
    private val resolveRenameNameUseCase: ResolveRenameNameUseCase,
    private val validateRenameUseCase: ValidateRenameUseCase,
    private val executeRenameUseCase: ExecuteRenameUseCase,
    initialTargetFiles: List<RenameTargetFile> = emptyList(),
    initialRenameCandidates: List<RenameCandidate> = emptyList(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RenameMatchingUiState(
            targetFiles = initialTargetFiles,
            renameCandidates = initialRenameCandidates,
        ),
    )
    val uiState: StateFlow<RenameMatchingUiState> = _uiState.asStateFlow()

    fun selectTargetFile(fileId: String) {
        _uiState.update { state ->
            val updatedFiles = state.targetFiles.map { file ->
                file.copy(isSelected = file.id == fileId)
            }
            val selectedTargetFileId = updatedFiles.firstOrNull { it.isSelected }?.id

            state.copy(
                targetFiles = updatedFiles,
                selectedTargetFileId = selectedTargetFileId,
                canExecuteRename = canExecuteRename(
                    selectedTargetFileId = selectedTargetFileId,
                    selectedCandidateId = state.selectedCandidateId,
                ),
                error = null,
            )
        }
    }

    fun selectRenameCandidate(candidateId: String) {
        _uiState.update { state ->
            val updatedCandidates = state.renameCandidates.map { candidate ->
                candidate.copy(isSelected = candidate.id == candidateId)
            }
            val selectedCandidateId = updatedCandidates.firstOrNull { it.isSelected }?.id

            state.copy(
                renameCandidates = updatedCandidates,
                selectedCandidateId = selectedCandidateId,
                canExecuteRename = canExecuteRename(
                    selectedTargetFileId = state.selectedTargetFileId,
                    selectedCandidateId = selectedCandidateId,
                ),
                error = null,
            )
        }
    }

    fun executeSelectedRename() {
        _uiState.update { state ->
            state.copy(isExecuting = true, error = null)
        }

        val currentState = _uiState.value
        val selectedFile = currentState.targetFiles.firstOrNull { it.id == currentState.selectedTargetFileId }
        val selectedCandidate = currentState.renameCandidates.firstOrNull { it.id == currentState.selectedCandidateId }

        if (selectedFile == null || selectedCandidate == null) {
            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    canExecuteRename = false,
                    error = AppError.Unknown("Target file or rename candidate is not selected."),
                )
            }
            return
        }

        runCatching {
            val resolvedNewName = resolveRenameNameUseCase(selectedFile, selectedCandidate)
            val renamePair = RenamePair(
                sourceFile = selectedFile,
                renameCandidate = selectedCandidate,
                resolvedNewName = resolvedNewName,
            )

            if (!validateRenameUseCase(renamePair)) {
                _uiState.update { state ->
                    state.copy(
                        isExecuting = false,
                        error = AppError.InvalidFileName,
                    )
                }
                return
            }

            executeRenameUseCase(renamePair)
        }.onSuccess { result ->
            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    lastResult = result,
                    error = null,
                )
            }
            refreshAfterRename()
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    error = AppError.Unknown(
                        detailMessage = throwable.message ?: "Failed to execute rename.",
                        throwable = throwable,
                    ),
                )
            }
        }
    }

    fun refreshAfterRename() {
        _uiState.update { state ->
            val result = state.lastResult
            if (result == null) {
                return@update state
            }

            if (!result.success) {
                return@update state.copy(error = AppError.RenameFailed)
            }

            val updatedFiles = state.targetFiles.map { file ->
                if (file.id == state.selectedTargetFileId) {
                    file.copy(isSelected = false, isRenamed = true)
                } else {
                    file.copy(isSelected = false)
                }
            }
            val updatedCandidates = state.renameCandidates.map { candidate ->
                if (candidate.id == state.selectedCandidateId) {
                    candidate.copy(isSelected = false, isUsed = true)
                } else {
                    candidate.copy(isSelected = false)
                }
            }

            state.copy(
                targetFiles = updatedFiles,
                renameCandidates = updatedCandidates,
                selectedTargetFileId = null,
                selectedCandidateId = null,
                canExecuteRename = false,
                error = null,
            )
        }
    }

    private fun canExecuteRename(
        selectedTargetFileId: String?,
        selectedCandidateId: String?,
    ): Boolean {
        return selectedTargetFileId != null && selectedCandidateId != null
    }
}
```

### HomeUiState.kt

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

### RenameMatchingUiState.kt

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

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `doc/STEP4-3_codex.md`

`HomeUiState.kt` と `RenameMatchingUiState.kt` は既存の状態項目で要件を満たしていたため、今回は変更していません。

## 変更理由

`StateFlow` を採用した理由は、画面が状態を購読するMVVM構成と相性がよく、現在値を常に保持できるためです。`LiveData` でも実装できますが、Kotlin中心の実装では `StateFlow` の方が immutable な `copy` 更新と組み合わせやすく、後続のテストもしやすくなります。

状態更新はすべて `copy` で行いました。これは、UI Stateを単一の状態スナップショットとして扱うためです。個別の可変プロパティを直接更新する代替案は簡単ですが、どの操作でどの状態が変わるか追いにくくなります。

`HomeViewModel` は、ディレクトリ選択とCSV選択の成功時に件数・表示名・準備完了状態を更新します。`isReadyToStartMatching` は「ディレクトリ選択済み」と「CSV選択済み」の両方が成立したときだけ `true` になります。

`RenameMatchingViewModel` は、左右リストの選択状態を片方1件だけに制限し、両方が選択されたときのみ `canExecuteRename = true` にします。リネーム成功後は対象ファイルを `isRenamed = true`、候補を `isUsed = true` にし、選択状態を解除します。

## 動作確認方法

1. `HomeViewModel` をUseCase付きで生成する
2. `onDirectorySelected(uri)` を呼び、`isLoading`、`targetFileCount`、`selectedDirectoryName`、`error` の更新を確認する
3. `onCsvSelected(uri)` を呼び、`renameCandidateCount`、`selectedCsvFileName`、`isReadyToStartMatching` の更新を確認する
4. `RenameMatchingViewModel` を初期ファイル一覧・候補一覧付きで生成する
5. `selectTargetFile(fileId)` を呼び、指定ファイルだけが `isSelected = true` になることを確認する
6. `selectRenameCandidate(candidateId)` を呼び、指定候補だけが `isSelected = true` になり、両方選択時に `canExecuteRename = true` になることを確認する
7. `executeSelectedRename()` を呼び、`lastResult`、`isExecuting`、`isRenamed`、`isUsed`、選択解除が反映されることを確認する

## ビルド確認結果

以下を実行し、成功しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

既存ユニットテストも成功しています。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 未解決事項・リスク

- UI未実装のため、実際の画面購読やボタン操作からの状態更新はまだ確認していません。
- `HomeViewModel` のUseCase呼び出しは現時点では同期実行です。SAFのI/OをUIスレッドで実行しないため、UI接続時に `viewModelScope` と `Dispatchers.IO` の導入を検討する余地があります。
- `LoadDirectoryFilesUseCase`、`LoadRenameRulesFromCsvUseCase`、`ExecuteRenameUseCase` にはまだ `TODO("Not yet implemented")` が残っています。今回はViewModel層のみが対象だったため、UseCase本体は変更していません。
- `RenameMatchingViewModel` に渡す初期ファイル一覧・候補一覧を、画面遷移時にどう受け渡すかはUI実装時に設計する必要があります。
- `ViewModelProvider.Factory` は今回未実装です。UI接続時に実際のUseCase生成方法と合わせて追加するのが自然です。
