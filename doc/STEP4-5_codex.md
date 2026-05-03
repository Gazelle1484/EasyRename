実機検証結果と、codexの回答から、STEP4-6のプロンプトを生成してください。プロンプトはフォーマットに従ってください。コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。実機で確認するべきログがあれば別途ユーザに指示してください。
## フォーマット　STEP 4-6: 実装（機能単位）
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
# 実機検証結果
- ホーム画面で一番上のボタンがTopAppBarで隠れている
- リネーム対象のディレクトリを選択できた
- リネーム先の名前が書いてあるcsvを選択できた
- リネームマッチング画面に遷移できた
- リネームするファイルを選択できた
- リネーム候補一覧からリネーム名を選択できた
- リネーム実行すると、UnsuppoortedOperationExpentionが発生し、Renameできなかった
- リネーム用csvの中身は以下の通り
###### リネーム用csvの中身  
A1-1_*\n  
A1-2_*\n  
A1-3_*\n  


# STEP 4-5: リネーム実行統合 Codex回答

## 実装内容

- `RenamePair` に `directoryUri` を追加し、同名ファイルチェックに必要なディレクトリ情報をDomain層へ渡せるようにしました。
- `HomeUiState` に `selectedDirectoryUri` / `selectedCsvUri` を追加し、Home画面で選択したURIをActivityスコープの共有ViewModel経由でMatching画面へ引き継げるようにしました。
- `ExecuteRenameUseCase` に「ファイル名検証 → 同名チェック → SAFリネーム」の順序を統合しました。
- `RenameMatchingViewModel` で、選択ファイル・選択候補・ディレクトリURIから `RenamePair` を作成し、成功/失敗結果を `RenameMatchingUiState` に反映するようにしました。
- `RenameMatchingFragment` で `lastResult` の成功/失敗メッセージを表示し、失敗時の理由をユーザー向け文言に変換するようにしました。
- URI永続権限保持用の `TakePersistablePermissionUseCase` を追加し、HomeViewModelからRepository経由で呼び出すようにしました。権限保持に失敗しても即時処理は継続します。

## 実装コード

### RenamePair.kt

```kotlin
data class RenamePair(
    val sourceFile: RenameTargetFile,
    val renameCandidate: RenameCandidate,
    val resolvedNewName: String,
    val directoryUri: Uri,
)
```

### HomeUiState.kt

```kotlin
data class HomeUiState(
    val selectedDirectoryUri: Uri? = null,
    val selectedCsvUri: Uri? = null,
    val selectedDirectoryName: String? = null,
    val selectedCsvFileName: String? = null,
    ...
)
```

### HomeViewModel.kt

```kotlin
fun onDirectorySelected(uri: Uri) {
    runCatching {
        takePersistablePermissionUseCase.forDirectory(uri)
        loadDirectoryFilesUseCase(uri)
    }.onSuccess { files ->
        _uiState.update { state ->
            state.copy(
                selectedDirectoryUri = uri,
                selectedDirectoryName = resolveDisplayName(uri),
                targetFiles = files,
                targetFileCount = files.size,
                isReadyToStartMatching = state.selectedCsvFileName != null,
                isLoading = false,
                error = null,
            )
        }
    }
}
```

CSV選択時も同様に `selectedCsvUri` を保持し、読み取り権限の永続化を試行します。

### ExecuteRenameUseCase.kt

```kotlin
operator fun invoke(renamePair: RenamePair): RenameResult {
    if (!validateRenameUseCase(renamePair)) {
        return RenameResult(
            beforeName = renamePair.sourceFile.displayName,
            afterName = renamePair.resolvedNewName,
            success = false,
            errorMessage = "Invalid file name.",
        )
    }

    if (storageRepository.existsInSameDirectory(renamePair.directoryUri, renamePair.resolvedNewName)) {
        return RenameResult(
            beforeName = renamePair.sourceFile.displayName,
            afterName = renamePair.resolvedNewName,
            success = false,
            errorMessage = "A file with the same name already exists.",
        )
    }

    return storageRepository.renameFile(
        fileUri = renamePair.sourceFile.uri,
        newName = renamePair.resolvedNewName,
    )
}
```

### RenameMatchingViewModel.kt

```kotlin
fun executeSelectedRename() {
    val selectedFile = currentState.targetFiles.firstOrNull { it.id == currentState.selectedTargetFileId }
    val selectedCandidate = currentState.renameCandidates.firstOrNull { it.id == currentState.selectedCandidateId }

    val resolvedNewName = resolveRenameNameUseCase(selectedFile, selectedCandidate)
    val renamePair = RenamePair(
        sourceFile = selectedFile,
        renameCandidate = selectedCandidate,
        resolvedNewName = resolvedNewName,
        directoryUri = selectedDirectoryUri,
    )

    val result = executeRenameUseCase(renamePair)
    refreshAfterRename(result)
}
```

成功時は対象ファイルを `isRenamed = true`、使用候補を `isUsed = true` にし、選択状態と実行可能状態を解除します。失敗時は選択状態を維持し、`lastResult` と `AppError` を更新します。

### RenameMatchingFragment.kt

```kotlin
resultText.text = state.lastResult?.let { result ->
    if (result.success) {
        "成功: ${result.beforeName} -> ${result.afterName} に変更しました。"
    } else {
        "失敗: ${toResultErrorMessage(result.errorMessage)}"
    }
}.orEmpty()
```

### ErrorDialog.kt

既存の `AppError.FileAlreadyExists`、`AppError.InvalidFileName`、`AppError.RenameFailed` の表示文言を利用しています。

```kotlin
AppError.InvalidFileName -> "リネーム後のファイル名が不正です。"
AppError.FileAlreadyExists -> "同じ名前のファイルが既に存在します。"
AppError.RenameFailed -> "ファイル名の変更に失敗しました。"
```

### その他変更ファイル

- `StorageRepository.kt`
- `StorageRepositoryImpl.kt`
- `SafDocumentDataSource.kt`
- `TakePersistablePermissionUseCase.kt`
- `AppViewModelFactory.kt`

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/model/RenamePair.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/TakePersistablePermissionUseCase.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt`
- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt`
- `doc/STEP4-5_codex.md`

## 変更理由

## 全体アーキテクチャ

本アプリは、既存方針どおり **MVVM + Repository** を維持しています。

- UI層: `HomeFragment` / `RenameMatchingFragment` が画面表示とユーザー操作の受け取りだけを担当します。
- ViewModel層: `HomeViewModel` / `RenameMatchingViewModel` がUI状態を `StateFlow` で管理し、UseCaseを呼び出します。
- Domain / UseCase層: `ResolveRenameNameUseCase`、`ValidateRenameUseCase`、`ExecuteRenameUseCase` がリネーム名解決・検証・実行手順を担当します。
- Data / Repository層: `StorageRepository` が抽象境界になり、`StorageRepositoryImpl` と `SafDocumentDataSource` がSAF / `DocumentFile` 依存を閉じ込めます。

データフローは次の通りです。

```text
ユーザー選択
→ RenameMatchingFragment
→ RenameMatchingViewModel
→ ResolveRenameNameUseCase
→ ExecuteRenameUseCase
→ StorageRepository
→ SafDocumentDataSource / DocumentFile.renameTo
→ RenameResult
→ RenameMatchingUiState
→ UI表示更新
```

依存方向は UI → ViewModel → UseCase → Repository → SAF です。UIからRepositoryやSAF APIを直接呼ばないため、画面実装とストレージ実装の結合を避けられます。これにより、実機のDocumentProvider差異や将来の別ストレージ実装が出た場合でも、影響範囲をData層に閉じ込めやすくなります。

## 採用理由・根拠

`RenamePair` に `directoryUri` を持たせた理由は、リネーム実行に必要な入力を1つの値オブジェクトにまとめられるためです。代替案として `ExecuteRenameUseCase(renamePair, directoryUri)` も可能ですが、UseCase呼び出しごとに引数の組み合わせを意識する必要があり、同名チェックを忘れやすくなります。今回の規模では `RenamePair` に含める方が変更範囲が小さく、ViewModelとUseCaseの責務も読みやすくなります。

同名チェックは `ExecuteRenameUseCase` に置きました。これは「検証に通らなければリネームしない」「同名があればリネームしない」という業務ルールに近い判断だからです。Repositoryにまとめる案もありますが、Repositoryはストレージ操作の抽象化に集中させた方がSRPを守りやすく、テスト時にもUseCase単位で分岐を検証しやすくなります。

URI権限保持は `HomeViewModel` から `TakePersistablePermissionUseCase` 経由で実行します。UIで直接 `contentResolver` を触る案は簡単ですが、UI層にAndroidストレージ操作の詳細が漏れるため採用していません。永続権限保持に失敗しても、Activity Resultで得た一時権限により即時操作できるケースがあるため、失敗は握りつぶして読み込み処理を継続します。

適用した設計原則は以下です。

- SRP: UI、状態管理、業務手順、SAF操作を分離。
- DIP: ViewModelはSAFではなくUseCase / Repository抽象に依存。
- KISS / YAGNI: 一括リネーム、自動連番、サブディレクトリ対応は追加せず、1件リネーム統合に限定。
- DRY: 同名チェックと実リネームの順序をUseCaseに集約し、UI側で重複実装しない。

## 代替案

1. `ExecuteRenameUseCase` の引数に `directoryUri` を追加する案

   小さい変更で実装できますが、`RenamePair` と `directoryUri` が常にセットで必要になるため、呼び出し側の責務が増えます。複数画面や一括リネームに拡張する場合、引数の渡し忘れによるバグが起きやすくなります。

2. ViewModelから `StorageRepository.existsInSameDirectory` を直接呼ぶ案

   実装は短くなりますが、リネーム実行前の検証手順がViewModelに寄り、UseCaseが単なる委譲になってしまいます。今後、実行前ルールが増えるとViewModelが肥大化するため、今回は採用していません。

## 動作確認方法

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

## ビルド確認結果

実行コマンド:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

追加確認:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 未解決事項・リスク

- 実機でのDocumentProvider差異は未検証です。Providerによって `DocumentFile.renameTo` の挙動や権限保持可否が異なる可能性があります。
- 同名チェックは選択ディレクトリ直下のみ対象です。サブディレクトリ対応は今回の範囲外です。
- リネーム成功後、ファイル一覧の再読み込みはまだ行っていません。UI上は `isRenamed` を更新しますが、実ファイルURIや表示名の再取得が必要になる場合があります。
- `RenameResult.errorMessage` の文字列をViewModel / UIで分類しています。将来的には失敗理由をenumやsealed classで持たせると、表示分岐がより堅牢になります。
- 画面回転時の完全な状態復元は未対応です。

## 次に進めるべきSTEP

次は **実機SAF検証とリネーム後再読み込み** に進むのが妥当です。

- 複数DocumentProviderで `renameTo` 成功/失敗挙動を確認する
- リネーム成功後にディレクトリを再読み込みして、実際のファイル名とUI状態を同期する
- エラー理由を文字列ではなく型で扱う設計へ整理する
- RecyclerView化や画面回転復元は、その後のUI安定化STEPで扱う
