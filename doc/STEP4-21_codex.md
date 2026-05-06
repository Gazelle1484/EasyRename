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
--------- beginning of main
---------------------------- PROCESS STARTED (16482) for package com.example.easyrename ----------------------------
2026-05-06 13:51:20.352 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  rename request start
2026-05-06 13:51:20.352 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 13:51:20.352 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  requestedFileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(22).zip
2026-05-06 13:51:20.352 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  expectedBeforeName=log (22).zip
2026-05-06 13:51:20.352 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  requestedAfterName=A1-1-1_log (22).zip
2026-05-06 13:51:20.352 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  singleUri start expectedName=log (22).zip fileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(22).zip
2026-05-06 13:51:20.402 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  singleUri document resolved name=log (22).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(22).zip exists=true canWrite=true
2026-05-06 13:51:20.402 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  singleUri name compare expected=log (22).zip actual=log (22).zip matches=true
2026-05-06 13:51:20.402 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  singleUri accepted expected=log (22).zip actual=log (22).zip
2026-05-06 13:51:20.402 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  renameTo start sourceName=log (22).zip targetName=A1-1-1_log (22).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(22).zip
2026-05-06 13:51:20.404 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  singleUri failed reason=UnsupportedOperation
2026-05-06 13:51:20.404 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  singleUri end success=false elapsedMs=52
2026-05-06 13:51:20.404 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri fallback start directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test expectedName=log (22).zip
2026-05-06 13:51:20.462 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri scope directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 13:51:20.463 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri listFiles from selected directory only=true
2026-05-06 13:51:20.463 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri listFiles source=selectedDirectory
2026-05-06 13:51:20.692 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[1] candidateName=A1-1-1_log (21).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.697 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[2] candidateName=test.csv expectedName=log (22).zip matches=false
2026-05-06 13:51:20.703 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[3] candidateName=log (6).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.708 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[4] candidateName=A1-3-2_log (10).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.714 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[5] candidateName=log (5).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.718 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[6] candidateName=log (7).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.725 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[7] candidateName=log (22).zip expectedName=log (22).zip matches=true
2026-05-06 13:51:20.725 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri candidate matched name=log (22).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(22).zip
2026-05-06 13:51:20.729 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[8] candidateName=log (23).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.736 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[9] candidateName=A1-1-1_log (1).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.740 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[10] candidateName=A1-1-2_log (11).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.747 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[11] candidateName=log (3).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.752 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[12] candidateName=A1-1-1_log (13).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.759 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[13] candidateName=log (24).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.763 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[14] candidateName=log (9).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.770 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[15] candidateName=A1-1-1_A1-3_log (16).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.779 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[16] candidateName=log (8).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.783 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[17] candidateName=A1-3-3_log (12).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.790 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[18] candidateName=A1-3_log (17).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.794 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[19] candidateName=A1-1-2_log (19).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.801 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[20] candidateName=A1-4-1_log (2).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.805 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[21] candidateName=A1-1-2_log (20).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.809 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[22] candidateName=A1-1-1_log (14).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.816 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[23] candidateName=A1-4-1_log (18).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.819 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[24] candidateName=A1-3-1_log (15).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.825 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[25] candidateName=A1-1-2_log (4).zip expectedName=log (22).zip matches=false
2026-05-06 13:51:20.998 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri found expected=log (22).zip actual=log (22).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(22).zip
2026-05-06 13:51:20.998 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri name compare expected=log (22).zip actual=log (22).zip matches=true
2026-05-06 13:51:20.998 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  singleVsTree compare singleName=log (22).zip treeName=log (22).zip matches=true
2026-05-06 13:51:21.006 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  renameTo start sourceName=log (22).zip targetName=A1-1-1_log (22).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(22).zip
2026-05-06 13:51:21.592 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  renameTo end success=true elapsedMs=585 afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(22).zip
2026-05-06 13:51:21.592 16482-16518 EasyRenameSafResolve    com.example.easyrename               D  treeUri end success=true elapsedMs=1188

# STEP4-21 Codex 作業結果

## 全体アーキテクチャ

本アプリは、AndroidのMVVMを土台に、UseCase / Repository / SAF DataSourceを分けたレイヤード構成で実装している。

- UI / ViewModel層: ユーザー操作、画面状態、通常リネーム・UNDOの起動を担当する
- UseCase層: 通常リネームやUNDOの業務手順を担当する
- Repository層: UseCaseから見たストレージ操作の窓口を担当する
- SAF DataSource層: `DocumentFile` と `ContentResolver` によるAndroid Storage Access Framework操作を担当する

依存方向は `ViewModel -> UseCase -> Repository -> SafDocumentDataSource`。今回のログ追加はSAF探索の可視化が目的なので、探索・計測・比較ログは `SafDocumentDataSource` に集約した。UseCase層には、UI側で把握しているリネーム前ファイル名を `expectedBeforeName` として渡す責務だけを追加した。

この構成にした理由は、SAF固有の診断ログをDataSourceに閉じ込めることで、UIや業務ロジックをログ実装で汚さず、通常リネームとUNDOの両方で同じ診断ログを得られるためである。

## 概要

STEP4-21では、`singleUri fast path` と `treeUri fallback` の探索挙動を実機ログで比較できるようにした。リネーム仕様、UNDO仕様、自動連番仕様、treeUri探索ロジック自体は変更していない。

## 設計詳細

### `SafDocumentDataSource`

- `TAG_SAF_RESOLVE = "EasyRenameSafResolve"` を追加
- リネーム開始時に `directoryUri`、`requestedFileUri`、`expectedBeforeName`、`requestedAfterName` を出力
- singleUri開始、解決結果、期待名との比較、失敗理由、処理時間を出力
- treeUri fallback開始、探索対象directoryUri、`listFiles source=selectedDirectory`、候補比較、発見結果、singleUri名との比較、処理時間を出力
- `renameTo` 前後の `sourceName`、`targetName`、`sourceUri`、`afterUri`、処理時間を出力
- treeUri比較ログは最大50件に制限し、超過時はtruncatedログを出す

### `StorageRepository` / `StorageRepositoryImpl`

- `renameFile` に `expectedBeforeName: String?` を追加
- Repositoryは値を中継するだけで、SAF探索判断は持たない

### `ExecuteRenameUseCase`

- 通常リネーム時、`renamePair.sourceFile.displayName` を `expectedBeforeName` として渡す

### `UndoRenameUseCase`

- UNDO時、履歴の `record.afterName` を `expectedBeforeName` として渡す

## 採用理由・根拠

ログ追加の中心をDataSourceに置いたのは、singleUri / treeUri / renameTo がすべてSAF層の責務だからである。ViewModelやUseCaseに `DocumentFile` の知識を漏らすと、責務分離が崩れ、テストや将来の探索方式変更が難しくなる。

`expectedBeforeName` だけを上位層から渡した理由は、DataSourceだけでは「UIがリネーム対象として認識しているファイル名」を正確に知らないためである。fileUriのlastPathSegmentはProvider依存で、人間が見ているファイル名と一致しないことがある。通常リネームでは `displayName`、UNDOでは履歴の `afterName` を使うことで、実機ログ上で期待名とSAF解決名を比較できる。

設計原則としては、SRPによりログ責務をSAF層へ集約し、KISS / YAGNIにより探索ロジック修正やキャッシュ化は行っていない。今回の目的は「原因を判断できるログ」を先に得ることであり、性能改善やProvider別最適化はログ確認後に判断するのが安全である。

## 代替案

- UseCase層でsingleUri / treeUriを判定する案: UIに近い場所で期待名を扱いやすいが、SAF詳細がUseCaseに漏れて責務が肥大化するため不採用。
- `RenameResult` に診断情報を追加する案: テストや画面表示に再利用できるが、今回はログ確認だけが目的であり、モデル変更の影響範囲が広がるため不採用。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/UndoRenameUseCase.kt`
- `doc/STEP4-21.md`
- `doc/STEP4-21_codex.md`

## 動作確認結果

- `assembleDebug`: BUILD SUCCESSFUL
- `testDebugUnitTest`: BUILD SUCCESSFUL
- `adb devices`: `ZY32LZ6H5X device` を確認
- 実機での通常リネーム / UNDO操作確認: 未実施。端末接続は確認済みだが、手動操作が必要なため実機確認OK待ち。

## 実機で見るべきログ

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

UNDO履歴も見る場合:

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

確認観点:

- `singleUri start expectedName=... fileUri=...`
- `singleUri document resolved name=...` または `singleUri failed reason=...`
- `singleUri name compare expected=... actual=... matches=...`
- `singleUri end success=... elapsedMs=...`
- `treeUri fallback start directoryUri=... expectedName=...`
- `treeUri listFiles source=selectedDirectory`
- `treeUri candidate matched name=... uri=...`
- `treeUri found expected=... actual=... uri=...`
- `singleVsTree compare ...` または `singleVsTree compare skipped ...`
- `treeUri end success=... elapsedMs=...`
- `renameTo start ...`
- `renameTo end success=... afterUri=...`

## Git操作結果

- STEP4-20 commit hash: `17a7160`
- STEP4-20 push結果: `origin/feature/step4-20-undo-rename` へpush成功
- STEP4-21作業ブランチ: `feature/step4-21-saf-resolve-logs`
- STEP4-21分岐元ブランチ: `feature/step4-20-undo-rename`
- STEP4-21 commit message予定: `Add SAF resolve diagnostic logs`
- STEP4-21 commit hash: 未作成
- STEP4-21 push先: `origin/feature/step4-21-saf-resolve-logs` 予定
- 未コミット差分: あり

## リスク・今後の検討点

- STEP4-21ではログ追加のみで、探索ロジック自体は変更していない
- treeUri探索が広すぎる場合でも、今回のSTEPでは修正せずログで確認する
- singleUriが常に失敗するProviderでは、次STEPでfast pathの扱いを見直す必要がある
- ファイル名やUriをログ出力するため、リリース前にはログ抑制方針を検討する必要がある
- ディレクトリ内ファイル数が多い場合、treeUri比較ログは最大50件に制限しているため全件は出ない可能性がある

## 後に回す機能メモ

- singleUri fast pathの仕様見直し
- singleUri結果の検証強化
- treeUri探索範囲の実装修正
- treeUri探索時間削減
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- Provider別fast pathスキップ
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- RecyclerView化
- Material Componentsへの本格移行
