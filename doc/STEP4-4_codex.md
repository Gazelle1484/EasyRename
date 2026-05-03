このSTEPでは、STEPで確定し3た空実装に基づいて、**コンパイル可能な実装コードを生成したいです。codexの回答をもとにフォーマットに従ってSTEP4-の5プロンプトを生成してください。本STEPで解決すべき未解決問題があれば解決してください。
## フォーマット　STEP 4-5: 実装（機能単位）
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
# STEP 4-4: UI層実装 Codex回答

今回は **UI層のみを主対象**として、Home画面、Matching画面、エラー表示、読み込み表示、ViewModel生成Factoryを実装しました。

ただし、UI接続時に `LoadDirectoryFilesUseCase`、`LoadRenameRulesFromCsvUseCase`、`ExecuteRenameUseCase` が未実装のままだと、画面操作時に `TODO("Not yet implemented")` で停止します。そのため、STEP4-4の前提を満たすための最小委譲だけ補いました。ドメイン仕様の変更や新しいリネームルール追加は行っていません。

## 実装内容

- `MainActivity`
  - 起動時に `HomeFragment` を表示
  - Fragment用コンテナをプログラムで作成

- `HomeFragment`
  - ディレクトリ選択ボタン
  - CSV選択ボタン
  - 選択中ディレクトリ名表示
  - 選択中CSV名表示
  - 読み込み済みファイル数表示
  - 読み込み済み候補数表示
  - マッチング画面遷移ボタン
  - `OpenDocumentTree` / `OpenDocument` によるSAFピッカー起動
  - `StateFlow` のライフサイクル安全なcollect

- `RenameMatchingFragment`
  - 元ファイル一覧表示
  - リネーム候補一覧表示
  - 選択状態・リネーム済み・使用済み状態の表示
  - 実行ボタンの有効/無効制御
  - 実行結果表示
  - エラー表示

- `ErrorDialog`
  - `AppError` の種別ごとにユーザー向け文言を表示

- `LoadingView`
  - `ProgressBar` による最小読み込み表示

- `AppViewModelFactory`
  - `SafDocumentDataSource`
  - `StorageRepositoryImpl`
  - 各UseCase
  - `HomeViewModel`
  - `RenameMatchingViewModel`
  を手動生成

- 最小補助修正
  - `HomeUiState` に読み込み済み `targetFiles` / `renameCandidates` を保持
  - `HomeViewModel` で読み込み済みリストを状態に保存
  - 読み込みUseCase / 実行UseCaseの最小委譲を実装

## 実装コード

### MainActivity.kt

`TextView` の仮表示をやめ、Fragmentコンテナを作成して `HomeFragment` を初期表示する構成に変更しました。

```kotlin
val container = FrameLayout(this).apply {
    id = FRAGMENT_CONTAINER_ID
}
setContentView(container)

if (savedInstanceState == null) {
    supportFragmentManager.beginTransaction()
        .replace(FRAGMENT_CONTAINER_ID, HomeFragment())
        .commit()
}
```

### HomeFragment.kt

プログラムmatic Viewで最小UIを作成し、Activity Result APIでSAFピッカーを起動します。

```kotlin
private val directoryPicker = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree(),
) { uri ->
    if (uri != null) {
        viewModel.onDirectorySelected(uri)
    }
}

private val csvPicker = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
) { uri ->
    if (uri != null) {
        viewModel.onCsvSelected(uri)
    }
}
```

StateFlowは `repeatOnLifecycle` でcollectしています。

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            directoryNameText.text = "選択中ディレクトリ: ${state.selectedDirectoryName ?: "未選択"}"
            csvNameText.text = "選択中CSV: ${state.selectedCsvFileName ?: "未選択"}"
            targetFileCountText.text = "読み込み済みファイル数: ${state.targetFileCount}"
            candidateCountText.text = "読み込み済み候補数: ${state.renameCandidateCount}"
            startMatchingButton.isEnabled = state.isReadyToStartMatching
            loadingView.setLoading(state.isLoading)
        }
    }
}
```

### RenameMatchingFragment.kt

`HomeViewModel` をActivityスコープで参照し、読み込み済みファイル一覧・候補一覧を `RenameMatchingViewModel` の初期値として渡します。

```kotlin
homeViewModel = ViewModelProvider(requireActivity(), homeFactory)[HomeViewModel::class.java]
viewModel = ViewModelProvider(
    this,
    AppViewModelFactory(requireContext()) { homeViewModel },
)[RenameMatchingViewModel::class.java]
```

一覧は最小構成として `Button` を動的に追加しています。

```kotlin
filesContainer.addView(
    Button(requireContext()).apply {
        text = buildString {
            if (file.isSelected) append("[選択中] ")
            if (file.isRenamed) append("[リネーム済み] ")
            append(file.displayName)
        }
        isEnabled = !file.isRenamed
        setOnClickListener { viewModel.selectTargetFile(file.id) }
    },
)
```

### ErrorDialog.kt

`MaterialAlertDialogBuilder` を使い、`AppError` をユーザー向け文言に変換して表示します。

```kotlin
MaterialAlertDialogBuilder(context)
    .setTitle("エラー")
    .setMessage(toMessage(error))
    .setPositiveButton("OK", null)
    .show()
```

### LoadingView.kt

`FrameLayout` 上に `ProgressBar` を中央配置する最小UIです。

```kotlin
class LoadingView(context: Context) : FrameLayout(context) {
    fun setLoading(isLoading: Boolean) {
        visibility = if (isLoading) VISIBLE else GONE
    }
}
```

### 追加したレイアウトXML

追加していません。

今回のUIはすべてプログラムmatic Viewで構築しました。理由は、STEP4-4の目的が「見た目の完成」ではなく「ViewModel状態とユーザー操作の接続」であり、XMLを増やすよりも最小構成でビルドと状態反映を優先するためです。

### 追加したAdapter

追加していません。

RecyclerView Adapterを追加する代替案もありますが、今回は最小UIでファイル一覧・候補一覧の表示と選択ができれば十分です。リスト件数が増える場合は、後続STEPでRecyclerView化を検討するのが妥当です。

### 追加したFactory

`AppViewModelFactory.kt` を追加しました。

責務:

- DataSource / Repository / UseCase / ViewModelを手動生成する
- DIライブラリを導入せず、生成処理を1か所に寄せる
- `RenameMatchingViewModel` 生成時に `HomeViewModel` の読み込み済み状態を渡す

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/ui/MainActivity.kt`
- `app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/common/ErrorDialog.kt`
- `app/src/main/java/com/example/easyrename/ui/common/LoadingView.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/LoadDirectoryFilesUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/LoadRenameRulesFromCsvUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `doc/STEP4-4_codex.md`

## 変更理由

UIはViewModelの状態を表示するだけにし、CSV解析、リネーム名解決、SAF操作はUIに書いていません。これはMVVMの責務分離に沿った判断です。

画面間データ受け渡しは、Activityスコープの `HomeViewModel` を共有する方式にしました。Bundleにファイル一覧や候補一覧を詰める代替案は、URIや件数が増えた場合に扱いづらくなるため採用していません。

`AppViewModelFactory` を追加した理由は、ViewModelがUseCaseをコンストラクタ引数で受け取る設計だからです。DIライブラリを追加する代替案もありますが、現段階では依存が重くなりすぎるため、手動Factoryで十分です。

XMLやRecyclerViewを使わなかった理由は、今回は最小UI接続が目的だからです。将来的に一覧件数が増える場合、RecyclerView Adapterへ置き換える価値があります。

## 動作確認方法

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

既存ユニットテストも成功しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 未解決事項・リスク

- 実機でのSAFプロバイダ差異確認は未実施です。
- UIは最小構成です。見た目、余白、リスト性能、空状態表示は後続で調整余地があります。
- 画面回転時の完全な状態復元は未対応です。
- リネーム後のファイル一覧再読み込みは未対応です。
- `DocumentFile.renameTo` の成功/失敗は端末・プロバイダ依存があります。
- 現在のViewModel処理は同期呼び出しです。ファイル数が多い場合は、次STEP以降で `viewModelScope` と `Dispatchers.IO` による非同期化を検討すべきです。
- `takePersistableUriPermission` はUIから明示的には呼んでいません。永続権限の扱いは、Activity Resultのgrant flagsを含めて後続で整理する余地があります。

## 次に進めるべきSTEP

次は **STEP 4-5: リネーム実行統合** に進むのが自然です。

理由は、今回のSTEPでUIから選択と実行ボタン押下までは接続できたためです。次に、同名ファイルチェック、実行前検証、SAFリネーム結果、リネーム後の状態更新を一連のフローとして確認する必要があります。
