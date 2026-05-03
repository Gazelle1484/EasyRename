# EasyRename 引き継ぎメモ v2

## 完了したSTEP

- STEP2: 設計整理
  - MVVM + Repository 方針を確定。
  - UI / ViewModel / Domain / Data / Model / Util の責務分離を定義。

- STEP3: スケルトン生成
  - Kotlinクラス群を作成。
  - 空実装ベースで全体構造を固定。

- STEP4-1: Domain層実装
  - CSVルール解析。
  - リネーム候補生成。
  - `*` を元ファイル名に置換する名前解決。
  - ファイル名検証。

- STEP4-2: Repository / SAF層実装
  - `DocumentFile` によるディレクトリ直下ファイル取得。
  - CSV読み込み。UTF-8優先、Shift_JIS fallback。
  - `DocumentFile.renameTo` によるリネーム処理。
  - 同名ファイル存在チェック。
  - URI永続権限保持の土台。

- STEP4-3: ViewModel層とUI State実装
  - `StateFlow` による状態管理。
  - Home画面状態。
  - Matching画面状態。
  - 選択状態、実行可能判定、結果反映。

- STEP4-4: UI層実装
  - `MainActivity`
  - `HomeFragment`
  - `RenameMatchingFragment`
  - `ErrorDialog`
  - `LoadingView`
  - `AppViewModelFactory`
  - 最小UIとSAFピッカー接続。
  - `LoadDirectoryFilesUseCase`、`LoadRenameRulesFromCsvUseCase`、`ExecuteRenameUseCase` の最小委譲を補完。

- STEP4-5: リネーム実行統合
  - `RenamePair` に `directoryUri` を追加。
  - `HomeUiState` に `selectedDirectoryUri` / `selectedCsvUri` を追加。
  - `ExecuteRenameUseCase` に「ファイル名検証 → 同名チェック → SAFリネーム」を統合。
  - `RenameMatchingViewModel` で選択ファイル・候補・ディレクトリURIから `RenamePair` を作成し、実行結果をUI Stateに反映。
  - 成功時に対象ファイルを `isRenamed = true`、候補を `isUsed = true` に更新。
  - 失敗時に `lastResult` と `AppError` を更新。
  - `RenameMatchingFragment` で成功/失敗結果を画面表示。
  - `TakePersistablePermissionUseCase` を追加し、URI永続権限保持をRepository経由に整理。

- 端末インストール
  - `.\gradlew.bat installDebug` で接続端末へインストール済み。
  - 対象端末: `moto g66j 5G - Android 15`

## 変更ファイル一覧

主なGradle / Manifest:

- `app/build.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`

UI:

- `app/src/main/java/com/example/easyrename/ui/MainActivity.kt`
- `app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt`
- `app/src/main/java/com/example/easyrename/ui/common/ErrorDialog.kt`
- `app/src/main/java/com/example/easyrename/ui/common/LoadingView.kt`

ViewModel:

- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`

Domain / UseCase:

- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/GenerateRenameCandidateUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/LoadDirectoryFilesUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/LoadRenameRulesFromCsvUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/TakePersistablePermissionUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ValidateRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/validator/FileNameValidator.kt`

Data:

- `app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt`
- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/data/csv/CsvRuleParser.kt`

Model:

- `app/src/main/java/com/example/easyrename/model/AppError.kt`
- `app/src/main/java/com/example/easyrename/model/RenameCandidate.kt`
- `app/src/main/java/com/example/easyrename/model/RenamePair.kt`
- `app/src/main/java/com/example/easyrename/model/RenameResult.kt`
- `app/src/main/java/com/example/easyrename/model/RenameRule.kt`
- `app/src/main/java/com/example/easyrename/model/RenameTargetFile.kt`

Util:

- `app/src/main/java/com/example/easyrename/util/DateFormatUtil.kt`
- `app/src/main/java/com/example/easyrename/util/UriDisplayNameUtil.kt`

ドキュメント:

- `doc/HANDOFF.md`
- `doc/HANDOFF_v2.md`
- `doc/STEP3_codex.md`
- `doc/STEP4_codex.md`
- `doc/STEP4-2_codex.md`
- `doc/STEP4-3_codex.md`
- `doc/STEP4-4_codex.md`
- `doc/STEP4-5_codex.md`

補足:

- `git status --short` では `.idea/*`、Gradle関連、`app/src/main/java/`、`doc/` が未追跡または変更ありとして表示されている。
- 既存作業分を含む状態なので、次セッションで差分確認する場合は、ユーザー変更を不用意に戻さないこと。

## 未実装のTODO

コード上の `TODO(` / `TODO:`:

- `app/src/main/java` 配下では検出なし。

機能面で未完了または実機検証で判明した課題:

- `DocumentFile.renameTo` 実行時に `UnsupportedOperationException` が発生し、実機でリネームできなかった。
  - 次STEPでは最優先で原因調査する。
  - 可能性として、`DocumentFile.fromSingleUri(context, fileUri)` で得たURIに対してProviderがrenameをサポートしていない、またはTree URI配下の子Documentの扱いがProvider依存になっている可能性がある。
  - `directoryUri` から親ディレクトリを開き、対象ファイルを `findFile` / `listFiles` で再解決してから `renameTo` する実装を検討する。

- Home画面で一番上のボタンがTopAppBarに隠れている。
  - `FragmentContainerView` またはFragment rootにTopAppBar分の余白/insetsが正しく入っていない可能性がある。
  - STEP4-6ではリネーム失敗修正を優先し、UI余白修正は別機能として小さく扱うのがよい。

- リネーム成功後のファイル一覧再読み込みは未対応。
  - 現在はUI上で `isRenamed = true`、候補側で `isUsed = true` にするだけ。
  - 実ファイル名・URIの再取得は今後の課題。

- 画面回転時の完全な状態復元は未対応。

- ViewModel処理の `Dispatchers.IO` 化は未対応。
  - 現状はUseCase呼び出しが同期実行。
  - ファイル数やCSVサイズが増える場合はUIスレッド負荷になる可能性がある。

- エラー理由が一部 `RenameResult.errorMessage` の文字列分類に依存している。
  - 将来的には `RenameFailureReason` のようなenum / sealed class化を検討する。

- RecyclerView化、UIデザイン調整、一括リネーム、履歴/取り消し、自動連番、サブディレクトリ対応は未対応。

## 実機検証結果

確認済み:

- ホーム画面でリネーム対象ディレクトリを選択できた。
- ホーム画面でリネーム先名が書かれたCSVを選択できた。
- リネームマッチング画面に遷移できた。
- リネームするファイルを選択できた。
- リネーム候補一覧からリネーム名を選択できた。

問題:

- ホーム画面で一番上のボタンがTopAppBarで隠れている。
- リネーム実行時に `UnsupportedOperationException` が発生し、リネームできなかった。

検証に使ったCSV内容:

```text
A1-1_*
A1-2_*
A1-3_*
```

## ビルド確認結果

assembleDebug:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

testDebugUnitTest:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

installDebug:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat installDebug
```

結果:

```text
Installed on 1 device.
BUILD SUCCESSFUL
```

インストール先:

```text
moto g66j 5G - Android 15
```

## 次に実装すべきSTEP

次は **STEP4-6: 実機SAFリネーム失敗の修正** を推奨。

優先順位:

1. `UnsupportedOperationException` の原因調査とリネーム処理修正
   - 対象: `SafDocumentDataSource.renameFile`
   - 目的: 実機で1件リネームを成功させる。
   - 方針: `fromSingleUri` だけに依存せず、選択ディレクトリのTree URIから対象ファイルを再解決する案を検討する。
   - 必要ならRepository / UseCaseの引数に `directoryUri` を渡してData層まで到達させる。ただしUIからRepositoryを直接呼ばない。

2. リネーム失敗時のログ強化
   - 対象: `SafDocumentDataSource`、必要に応じて `RenameResult`
   - 目的: Provider名、URI種別、例外クラス、例外メッセージを実機確認できるようにする。
   - 注意: ユーザー向けUIには詳細な内部ログを出しすぎない。

3. Home画面TopAppBar重なり修正
   - 対象: `MainActivity` / `HomeFragment`
   - 目的: 一番上のボタンがTopAppBarに隠れないようにする。
   - リネーム修正とは別機能として扱う。

4. リネーム成功後の再読み込み
   - 対象: `RenameMatchingViewModel`、必要に応じてUseCase追加。
   - 目的: 実ファイル名とUI状態を同期する。

STEP4-6で実機確認すべきログ:

- `SafDocumentDataSource.renameFile` に渡された `fileUri`
- 選択済み `directoryUri`
- `DocumentFile.fromSingleUri(context, fileUri)` の結果がnullかどうか
- `documentFile.name`
- `documentFile.canWrite()`
- `documentFile.exists()`
- 例外クラス名
- 例外メッセージ
- 可能なら `DocumentFile.fromTreeUri(context, directoryUri).listFiles()` で得られる各ファイルの `name` / `uri`

## 注意すべき設計方針

- アーキテクチャは **MVVM + Repository** を維持する。
- UIはViewModelのStateFlowを表示し、ユーザー操作をViewModelへ渡すだけにする。
- UIにCSV解析、SAF操作、リネーム名解決、同名チェックを書かない。
- ViewModelはUseCase経由で処理し、SAF APIを直接呼ばない。
- Repository / DataSourceにAndroid API依存を閉じ込める。
- `DocumentFile` や `ContentResolver` 依存はData層に限定する。
- DIライブラリはまだ追加しない。
- Composeへ移行しない。
- サブディレクトリ対応、一括リネーム、自動連番、履歴/取り消しはまだ追加しない。
- 同名ファイル存在時は自動連番にせず、エラーとして扱う。
- 大規模リファクタリングより、STEP単位の小さい完成を優先する。
- 次STEPでは「実機で1件リネームを成功させる」ことを最優先にし、UI余白修正などは別の小タスクとして扱う。
