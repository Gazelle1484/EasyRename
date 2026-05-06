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
---------------------------- PROCESS STARTED (22941) for package com.example.easyrename ----------------------------
2026-05-06 14:24:42.798 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  rename request start
2026-05-06 14:24:42.798 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 14:24:42.798 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  requestedFileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip
2026-05-06 14:24:42.798 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  expectedBeforeName=log (23).zip
2026-05-06 14:24:42.798 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  requestedAfterName=A1-1-1_log (23).zip
2026-05-06 14:24:42.798 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  singleUri start expectedName=log (23).zip fileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip
2026-05-06 14:24:42.858 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  singleUri document resolved name=log (23).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip exists=true canWrite=true
2026-05-06 14:24:42.858 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  singleUri name compare expected=log (23).zip actual=log (23).zip matches=true
2026-05-06 14:24:42.858 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  singleUri accepted expected=log (23).zip actual=log (23).zip
2026-05-06 14:24:42.859 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  renameTo start sourceName=log (23).zip targetName=A1-1-1_log (23).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip
2026-05-06 14:24:42.860 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  singleUri failed reason=UnsupportedOperation
2026-05-06 14:24:42.860 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  singleUri end success=false elapsedMs=61
2026-05-06 14:24:42.860 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri fallback start directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test expectedName=log (23).zip
2026-05-06 14:24:42.913 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri scope directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 14:24:42.913 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri listFiles from selected directory only=true
2026-05-06 14:24:42.913 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri listFiles source=selectedDirectory
2026-05-06 14:24:43.241 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[1] candidateName=A1-1-1_log (21).zip expectedName=log (23).zip matches=false
2026-05-06 14:24:43.250 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[2] candidateName=test.csv expectedName=log (23).zip matches=false
2026-05-06 14:24:43.255 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[3] candidateName=log (6).zip expectedName=log (23).zip matches=false
2026-05-06 14:24:43.264 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[4] candidateName=A1-3-2_log (10).zip expectedName=log (23).zip matches=false
2026-05-06 14:24:43.273 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[5] candidateName=log (5).zip expectedName=log (23).zip matches=false
2026-05-06 14:24:43.284 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[6] candidateName=log (7).zip expectedName=log (23).zip matches=false
2026-05-06 14:24:43.288 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri compare[7] candidateName=log (23).zip expectedName=log (23).zip matches=true
2026-05-06 14:24:43.288 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri candidate matched name=log (23).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip matchedIndex=7
2026-05-06 14:24:43.445 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri found expected=log (23).zip actual=log (23).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip scannedCount=7 matchedIndex=7
2026-05-06 14:24:43.445 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri name compare expected=log (23).zip actual=log (23).zip matches=true
2026-05-06 14:24:43.445 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  singleVsTree compare singleName=log (23).zip treeName=log (23).zip matches=true
2026-05-06 14:24:43.453 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  renameTo start sourceName=log (23).zip targetName=A1-1-1_log (23).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip
2026-05-06 14:24:43.883 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  renameTo end success=true elapsedMs=430 afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(23).zip
2026-05-06 14:24:43.884 22941-22972 EasyRenameSafResolve    com.example.easyrename               D  treeUri end success=true elapsedMs=1025 scannedCount=7 matchedIndex=7

# STEP4-22 Codex 作業結果

## 全体アーキテクチャ

本アプリは、AndroidのMVVMを土台に、UseCase / Repository / SAF DataSourceを分けたレイヤード構成で実装している。

- UI / ViewModel層: ユーザー操作、画面状態、通常リネーム・UNDOの起動を担当する
- UseCase層: 通常リネームやUNDOの業務手順を担当する
- Repository層: UseCaseから見たストレージ操作の窓口を担当する
- SAF DataSource層: `DocumentFile` と `ContentResolver` によるStorage Access Framework操作を担当する

依存方向は `ViewModel -> UseCase -> Repository -> SafDocumentDataSource`。STEP4-22の変更はSAF DataSource層だけに閉じた。理由は、treeUri fallbackの候補走査はAndroid SAFの実装詳細であり、UseCaseやViewModelへ漏らすと責務分離が崩れるためである。

## 概要

STEP4-22では、treeUri fallback探索で対象ファイルが見つかった時点でKotlin側の候補比較ループを終了するようにした。singleUri fast path、treeUri探索範囲、通常リネーム、UNDO、自動連番の仕様は変更していない。

## 設計詳細

### `SafDocumentDataSource`

- `findTargetFileInTreeUri(...)` を追加
- `directory.listFiles().filter { it.isFile }` で得た選択ディレクトリ内ファイルを、先頭から順に比較
- `file.uri == fileUri` または `candidateName == expectedName / beforeName` で一致したら即return
- `treeUri candidate matched name=... uri=... matchedIndex=...` を一致直後に出力
- `treeUri found ... scannedCount=... matchedIndex=...` を出力
- `treeUri end ... elapsedMs=... scannedCount=... matchedIndex=...` を出力
- compareログは最大50件制限を維持
- 一致後は残り候補のcompareログを出さない

`TreeUriSearchResult` を小さな内部データクラスとして追加し、`targetFile`、`scannedCount`、`matchedIndex` を呼び出し元へ返す構成にした。

## 採用理由・根拠

この設計を選んだ理由は、STEP4-21の実機ログで「7件目で一致しているのに25件目まで比較が続く」ことが確認されたためである。問題は探索範囲そのものではなく、一致後もKotlin側の比較・ログ出力が続く点なので、最小変更として早期終了だけを入れた。

代替としてMap化やキャッシュ化も考えられるが、今回は要件が「1機能だけ」であり、Provider側の`listFiles()`コストとKotlin側の無駄ループを分けて判断する段階である。KISS / YAGNIの観点から、まず明確に見えている無駄だけを削った。

既存アーキテクチャとの整合性として、SAF固有の最適化はDataSourceに閉じている。UseCaseやRepositoryのインターフェースは変更していないため、通常リネームとUNDOの業務フローには影響を広げていない。

## 代替案

- `directory.listFiles()` の結果をMap化する案: 複数回リネームでは有効だが、キャッシュの鮮度管理やUNDO時の更新が必要になるため今回は不採用。
- singleUri fast pathをProvider別にスキップする案: `UnsupportedOperation` が毎回出るProviderでは有効だが、Provider判定と失敗履歴の扱いが必要になり設計判断が重いため今回は不採用。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `doc/STEP4-22.md`
- `doc/STEP4-22_codex.md`

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

- `treeUri listFiles source=selectedDirectory` が維持されている
- `treeUri compare[N] ... matches=true` の後に `compare[N+1]` が出ない
- `treeUri candidate matched ... matchedIndex=N` が出る
- `treeUri found ... scannedCount=N matchedIndex=N` が出る
- `treeUri end ... elapsedMs=... scannedCount=N matchedIndex=N` が出る
- `renameTo end success=true` が出る
- `AndroidRuntime` のクラッシュが出ていない

## Git操作結果

- STEP4-21 commit hash: `20fb42c`
- STEP4-21 push結果: `origin/feature/step4-21-saf-resolve-logs` へpush済み
- STEP4-22作業ブランチ: `feature/step4-22-tree-uri-early-exit`
- STEP4-22分岐元ブランチ: `feature/step4-21-saf-resolve-logs`
- STEP4-22 commit message予定: `Stop tree URI search after first match`
- STEP4-22 commit hash: 未作成
- STEP4-22 push先: `origin/feature/step4-22-tree-uri-early-exit` 予定
- 未コミット差分: あり

## リスク・今後の検討点

- STEP4-22ではtreeUri探索の早期終了のみ対応
- `directory.listFiles()` 自体のProvider側コストは残る可能性がある
- singleUriは名前解決できても`renameTo`で`UnsupportedOperation`になるケースがある
- singleUri fast pathの扱いは今回変更していない
- Provider別fast pathスキップは未実装
- キャッシュ化や対象ファイルMap化は未実装
- ファイル名やUriログはリリース前に抑制方針を検討する必要がある

## 後に回す機能メモ

- singleUri fast pathの仕様見直し
- singleUri結果の検証強化
- Provider別fast pathスキップ
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- treeUri探索時間のさらなる削減
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- RecyclerView化
- Material Componentsへの本格移行
