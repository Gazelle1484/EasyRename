実機検証結果と、codexの回答から、のプロンプトを次に与えるプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-21: 実装（機能単位）
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
- git操作の前に実機確認
- 回答は本ファイル名+*_codexした.mdファイルに保存
# 実機実行結果
2026-05-06 15:25:51.023  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  rename request start
2026-05-06 15:25:51.023  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 15:25:51.023  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedFileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(5).zip
2026-05-06 15:25:51.023  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  expectedBeforeName=log (5).zip
2026-05-06 15:25:51.023  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedAfterName=A1-1-1_log (5).zip
2026-05-06 15:25:51.023  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri start expectedName=log (5).zip fileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(5).zip
2026-05-06 15:25:51.073  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri document resolved name=log (5).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(5).zip exists=true canWrite=true
2026-05-06 15:25:51.073  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri name compare expected=log (5).zip actual=log (5).zip matches=true
2026-05-06 15:25:51.073  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename start sourceName=log (5).zip targetName=A1-1-1_log (5).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(5).zip
2026-05-06 15:25:51.719  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename success elapsedMs=646 afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(5).zip
2026-05-06 15:25:51.719  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result path=DocumentsContract success=true beforeName=log (5).zip afterName=A1-1-1_log (5).zip afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(5).zip
2026-05-06 15:25:51.719  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result renamePath=SingleUri afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(5).zip
2026-05-06 15:25:51.720  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri end success=true elapsedMs=696
2026-05-06 15:25:55.737  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  rename request start
2026-05-06 15:25:55.737  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 15:25:55.737  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedFileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(3).zip
2026-05-06 15:25:55.737  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  expectedBeforeName=log (3).zip
2026-05-06 15:25:55.737  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedAfterName=A1-1-2_log (3).zip
2026-05-06 15:25:55.737  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri start expectedName=log (3).zip fileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(3).zip
2026-05-06 15:25:55.794  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri document resolved name=log (3).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(3).zip exists=true canWrite=true
2026-05-06 15:25:55.794  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri name compare expected=log (3).zip actual=log (3).zip matches=true
2026-05-06 15:25:55.794  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename start sourceName=log (3).zip targetName=A1-1-2_log (3).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(3).zip
2026-05-06 15:25:56.149  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename success elapsedMs=356 afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-2_log%20(3).zip
2026-05-06 15:25:56.149  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result path=DocumentsContract success=true beforeName=log (3).zip afterName=A1-1-2_log (3).zip afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-2_log%20(3).zip
2026-05-06 15:25:56.150  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result renamePath=SingleUri afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-2_log%20(3).zip
2026-05-06 15:25:56.150  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri end success=true elapsedMs=413
2026-05-06 15:25:57.764  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  rename request start
2026-05-06 15:25:57.764  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 15:25:57.764  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedFileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-2_log%20(3).zip
2026-05-06 15:25:57.764  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  expectedBeforeName=A1-1-2_log (3).zip
2026-05-06 15:25:57.764  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedAfterName=log (3).zip
2026-05-06 15:25:57.764  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri start expectedName=A1-1-2_log (3).zip fileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-2_log%20(3).zip
2026-05-06 15:25:57.813  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri document resolved name=A1-1-2_log (3).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-2_log%20(3).zip exists=true canWrite=true
2026-05-06 15:25:57.813  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri name compare expected=A1-1-2_log (3).zip actual=A1-1-2_log (3).zip matches=true
2026-05-06 15:25:57.813  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename start sourceName=A1-1-2_log (3).zip targetName=log (3).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-2_log%20(3).zip
2026-05-06 15:25:57.900  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename success elapsedMs=87 afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(3).zip
2026-05-06 15:25:57.900  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result path=DocumentsContract success=true beforeName=A1-1-2_log (3).zip afterName=log (3).zip afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(3).zip
2026-05-06 15:25:57.900  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result renamePath=SingleUri afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(3).zip
2026-05-06 15:25:57.900  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri end success=true elapsedMs=136
2026-05-06 15:25:59.261  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  rename request start
2026-05-06 15:25:59.261  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 15:25:59.261  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedFileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(5).zip
2026-05-06 15:25:59.261  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  expectedBeforeName=A1-1-1_log (5).zip
2026-05-06 15:25:59.261  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  requestedAfterName=log (5).zip
2026-05-06 15:25:59.261  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri start expectedName=A1-1-1_log (5).zip fileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(5).zip
2026-05-06 15:25:59.325  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri document resolved name=A1-1-1_log (5).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(5).zip exists=true canWrite=true
2026-05-06 15:25:59.326  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri name compare expected=A1-1-1_log (5).zip actual=A1-1-1_log (5).zip matches=true
2026-05-06 15:25:59.326  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename start sourceName=A1-1-1_log (5).zip targetName=log (5).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(5).zip
2026-05-06 15:25:59.425  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename success elapsedMs=99 afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(5).zip
2026-05-06 15:25:59.425  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result path=DocumentsContract success=true beforeName=A1-1-1_log (5).zip afterName=log (5).zip afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(5).zip
2026-05-06 15:25:59.425  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  result renamePath=SingleUri afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(5).zip
2026-05-06 15:25:59.425  5134-5209  EasyRenameSafResolve    com.example.easyrename               D  singleUri end success=true elapsedMs=164

# STEP4-24 Codex 作業結果

## 全体アーキテクチャ

本アプリは、AndroidのMVVMを土台に、UseCase / Repository / SAF DataSourceを分けたレイヤード構成で実装している。

- UI / ViewModel層: ユーザー操作、画面状態、通常リネーム・UNDO後の表示更新を担当する
- UseCase層: 通常リネームやUNDOの業務手順を担当する
- Repository層: UseCaseから見たストレージ操作の窓口を担当する
- SAF DataSource層: `DocumentsContract` / `DocumentFile` によるStorage Access Framework操作を担当する

依存方向は `ViewModel -> UseCase -> Repository -> SafDocumentDataSource`。STEP4-24では、この依存方向を変えずに、SAF結果、履歴、Matching状態、Home状態、UNDO入力Uriのログを追加した。理由は、`afterUri` の伝播確認は複数レイヤーをまたぐが、各層の責務は既存のまま維持した方が影響範囲を小さくできるためである。

## 概要

STEP4-24では、STEP4-23で導入した `DocumentsContract.renameDocument()` 成功経路の `afterUri` が、`RenameResult`、履歴、Matching側状態、Home側状態、UNDO入力Uriへ正しく伝播しているか確認できるログを追加した。

既存コードはすでに `result.afterUri` を状態更新と履歴保存に使っており、UNDOも `record.afterUri` をsourceUriとして使っていたため、今回は仕様変更ではなく確認ログ追加が中心である。

## 設計詳細

### `SafDocumentDataSource`

- DocumentsContract成功時に以下を追加
  - `result path=DocumentsContract success=true beforeName=... afterName=... afterUri=...`
  - `result renamePath=SingleUri afterUri=...`

### `RenameMatchingViewModel`

- リネーム履歴追加時に、`RenameResult.afterUri` 由来のUriが保存されているかログ出力
- Matching側リネーム更新前後の `displayName` / `uri` / `id` / `isRenamed` を `EasyRenameStateSync` で出力
- UNDO押下時に `record.afterUri` をsourceUriとして使うことをログ出力
- Matching側UNDO更新前後の `displayName` / `uri` / `id` / `isRenamed` を `EasyRenameStateSync` で出力

### `HomeViewModel`

- Home側リネーム更新前後の `displayName` / `uri` / `id` / `isRenamed` を `EasyRenameStateSync` で出力
- Home側UNDO更新前後の `displayName` / `uri` / `id` / `isRenamed` を `EasyRenameStateSync` で出力

### `UndoRenameUseCase`

- `record.afterUri` を `sourceUri` として使うことをログ出力

## 採用理由・根拠

STEP4-23でDocumentsContract経路が成功すると、Providerによってはリネーム後Uriが変わる。ここで古いUriを履歴や画面状態に残すと、次回リネームやUNDOでFileNotFoundにつながる。そのため、SAFの成功結果だけでなく、履歴・Matching・Home・UNDO入力まで同じUriが流れているかログで追えるようにした。

代替として状態更新処理を共通関数に切り出す案もあるが、今回は確認ログが目的であり、UI状態更新の大きなリファクタは不要である。KISS / YAGNIの観点から、既存の責務境界を維持してログだけを足した。

## 代替案

- URI伝播用の共通Mapperを作る案: 重複は減るが、Matching/Homeの状態差分に踏み込み影響範囲が広がるため今回は不採用。
- `RenameResult` に `renameEngine=DocumentsContract` のような新フィールドを追加する案: 意味は明確になるが、モデル変更と利用箇所への影響が出るため今回はログで補足した。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/UndoRenameUseCase.kt`
- `doc/STEP4-24.md`
- `doc/STEP4-24_codex.md`

## 動作確認結果

- `assembleDebug`: BUILD SUCCESSFUL
- `testDebugUnitTest`: BUILD SUCCESSFUL
- `adb devices`: `ZY32LZ6H5X device` を確認
- 実機での通常リネーム / UNDO操作確認: 未実施。端末接続は確認済みだが、手動操作が必要なため実機確認OK待ち。

## 実機で見るべきログ

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameStateSync
```

確認観点:

- `documentsContract rename success` の `afterUri` がリネーム後Uriである
- `result path=DocumentsContract` の `afterUri` が同じである
- `history add` の `afterUri` が同じである
- `matching applyRenameResult after uri` が同じである
- `home applyRenameResult after uri` が同じである
- `undo sourceUriFromHistoryAfterUri` が同じである
- `undo usecase sourceUri=record.afterUri` が出る
- UNDO成功後のMatching/Home更新ログで元ファイル名とUNDO結果Uriへ戻っている
- `AndroidRuntime` のクラッシュが出ていない

## Git操作結果

- STEP4-23 commit hash: `8c72ff4`
- STEP4-23 push結果: `origin/feature/step4-23-documents-contract-rename` へpush済み
- STEP4-24作業ブランチ: `feature/step4-24-verify-documents-contract-uri`
- STEP4-24分岐元ブランチ: `feature/step4-23-documents-contract-rename`
- STEP4-24 commit message予定: `Verify DocumentsContract rename URI propagation`
- STEP4-24 commit hash: 未作成
- STEP4-24 push先: `origin/feature/step4-24-verify-documents-contract-uri` 予定
- 未コミット差分: あり

## リスク・今後の検討点

- STEP4-24ではDocumentsContract成功経路のafterUri伝播確認を行う
- CSVプレビューは未実装
- DocumentsContractが失敗するProviderではtreeUri fallbackに依存する
- DocumentsContract成功時のafterUriがProviderによって元Uriと同じ場合も変わる場合もある
- ファイル名やUriログはリリース前に抑制方針を検討する必要がある
- UNDO成功時に自動連番カウンタは巻き戻していない

## 後に回す機能メモ

- CSVプレビュー
- CSVプレビューボタン
- DocumentsContract失敗Providerの記録
- Provider別fast pathスキップ
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- リリースビルド向けログ抑制
