# EasyRename 引き継ぎメモ

## 完了したSTEP

- STEP2: 設計整理
  - MVVM + Repository 方針を確定
  - UI / ViewModel / Domain / Data / Model / Util の責務分離を定義

- STEP3: スケルトン生成
  - Kotlinクラス群を作成
  - 空実装ベースで構造を固定

- STEP4-1: Domain層実装
  - CSVルール解析
  - リネーム候補生成
  - `*` の名前解決
  - ファイル名検証

- STEP4-2: Repository / SAF層実装
  - `DocumentFile` によるディレクトリ直下ファイル取得
  - CSV読み込み UTF-8優先、Shift_JIS fallback
  - `renameTo`
  - 同名チェック
  - URI権限保持

- STEP4-3: ViewModel層実装
  - `StateFlow` による状態管理
  - Home画面状態
  - Matching画面状態
  - 選択状態・実行可能判定・結果反映

- STEP4-4: UI層実装
  - `MainActivity`
  - `HomeFragment`
  - `RenameMatchingFragment`
  - `ErrorDialog`
  - `LoadingView`
  - `AppViewModelFactory`
  - 最小UIとSAFピッカー接続

## 変更ファイル一覧

主な変更・追加ファイル:

- `app/build.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`

ソース:

- `app/src/main/java/com/example/easyrename/ui/MainActivity.kt`
- `app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt`
- `app/src/main/java/com/example/easyrename/ui/common/ErrorDialog.kt`
- `app/src/main/java/com/example/easyrename/ui/common/LoadingView.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/*.kt`
- `app/src/main/java/com/example/easyrename/domain/validator/FileNameValidator.kt`
- `app/src/main/java/com/example/easyrename/data/repository/*.kt`
- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/data/csv/CsvRuleParser.kt`
- `app/src/main/java/com/example/easyrename/model/*.kt`
- `app/src/main/java/com/example/easyrename/util/*.kt`

ドキュメント:

- `doc/STEP3_codex.md`
- `doc/STEP4_codex.md`
- `doc/STEP4-2_codex.md`
- `doc/STEP4-3_codex.md`
- `doc/STEP4-4_codex.md`
- `doc/HANDOFF.md`

## 未実装のTODO

現時点で `app/src/main/java` 配下に `TODO(` / `TODO:` は検出されません。

ただし、機能面では以下が未完了です。

- 実機でのSAF動作確認
- リネーム後のファイル一覧再読み込み
- 画面回転時の完全な状態復元
- ViewModel処理の `Dispatchers.IO` 化
- 永続URI権限のgrant flagsを含めた厳密な整理
- UIデザイン調整
- RecyclerView化
- エラー表示の重複抑制強化
- 同名チェックをリネーム実行フローに完全統合する最終確認

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

ユニットテスト:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 次に実装すべきSTEP

次は **STEP4-5: リネーム実行統合**。

重点:

- 選択ファイル取得
- 候補取得
- 新ファイル名解決
- ファイル名検証
- 同名ファイル確認
- SAFリネーム実行
- 成功/失敗結果のUI反映
- リネーム後の状態更新確認

特に `ExecuteRenameUseCase` 周辺で、`existsInSameDirectory` を使った同名チェックを統合するのが次の中心。

## 注意すべき設計方針

- アーキテクチャは **MVVM + Repository** を維持する。
- UIはViewModelのStateFlowを表示するだけにする。
- UIにCSV解析、SAF操作、リネーム名解決を書かない。
- ViewModelはUseCase経由でのみ処理する。
- Repository / DataSourceにAndroid API依存を閉じ込める。
- DIライブラリはまだ追加しない。
- Composeへ移行しない。
- サブディレクトリ対応、一括リネーム、自動連番は追加しない。
- 仮データ・ハードコードでMatching画面を成立させない。
- 大規模リファクタリングより、STEP単位の小さい完成を優先する。
