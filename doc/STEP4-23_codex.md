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
# 実機確認結果
2026-05-06 15:12:25.495 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  rename request start
2026-05-06 15:12:25.495 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  directoryUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test
2026-05-06 15:12:25.495 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  requestedFileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(24).zip
2026-05-06 15:12:25.495 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  expectedBeforeName=log (24).zip
2026-05-06 15:12:25.495 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  requestedAfterName=A1-1-1_log (24).zip
2026-05-06 15:12:25.495 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  singleUri start expectedName=log (24).zip fileUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(24).zip
2026-05-06 15:12:25.549 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  singleUri document resolved name=log (24).zip uri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(24).zip exists=true canWrite=true
2026-05-06 15:12:25.549 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  singleUri name compare expected=log (24).zip actual=log (24).zip matches=true
2026-05-06 15:12:25.549 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename start sourceName=log (24).zip targetName=A1-1-1_log (24).zip sourceUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(24).zip
2026-05-06 15:12:26.099 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  documentsContract rename success elapsedMs=549 afterUri=content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2FA1-1-1_log%20(24).zip
2026-05-06 15:12:26.102 32163-32230 EasyRenameSafResolve    com.example.easyrename               D  singleUri end success=true elapsedMs=607


# STEP4-23 Codex 作業結果

## 全体アーキテクチャ

本アプリは、AndroidのMVVMを土台に、UseCase / Repository / SAF DataSourceを分けたレイヤード構成で実装している。

- UI / ViewModel層: ユーザー操作、画面状態、通常リネーム・UNDOの起動を担当する
- UseCase層: 通常リネームやUNDOの業務手順を担当する
- Repository層: UseCaseから見たストレージ操作の窓口を担当する
- SAF DataSource層: `DocumentFile` / `DocumentsContract` によるStorage Access Framework操作を担当する

依存方向は `ViewModel -> UseCase -> Repository -> SafDocumentDataSource`。STEP4-23の変更はSAF DataSource層だけに閉じた。理由は、`DocumentsContract.renameDocument()` はAndroid SAF固有APIであり、UseCaseやViewModelへ漏らすと責務分離が崩れるためである。

## 概要

STEP4-23では、singleUri由来の `DocumentFile.renameTo()` をfast pathとして使うのをやめ、`DocumentsContract.renameDocument()` をfast pathとして試すようにした。成功時は戻りUriを `RenameResult.afterUri` に入れて即成功扱いにし、失敗時は既存のtreeUri fallbackへ進む。

## 設計詳細

### `SafDocumentDataSource`

- `DocumentsContract.renameDocument(context.contentResolver, fileUri, afterName)` をfast pathとして追加
- singleUriの `DocumentFile` は名前・Uri確認用に限定し、`renameTo()` は呼ばない
- `DocumentsContract.renameDocument()` 成功時:
  - `RenameResult.success=true`
  - `RenameResult.afterUri=renamedUri`
  - `RenameResult.renamePath=RenamePath.SingleUri`
  - treeUri fallbackへ進まない
- `DocumentsContract.renameDocument()` 失敗時:
  - `returned_null` / `FileNotFound` / `UnsupportedOperation` / `SecurityException` / その他例外をログ出力
  - `documentsContract rename fallback to treeUri reason=...` を出して既存fallbackへ進む
- treeUri fallbackの `listFiles source=selectedDirectory` とSTEP4-22の早期終了は維持
- treeUri側の `renameTo` ログには `source=treeUri` を付け、DocumentsContract経路と区別できるようにした

## 採用理由・根拠

STEP4-21/22の実機ログでは、singleUriの名前解決とUri一致は正しいが、`DocumentFile.fromSingleUri(...).renameTo(...)` が `UnsupportedOperation` になることが確認されていた。問題は「対象ファイルの解決」ではなく「DocumentFile経由のrename実行」にあるため、Android標準の低レベルAPIである `DocumentsContract.renameDocument()` をfast pathとして試す設計にした。

Providerによっては `DocumentsContract.renameDocument()` も失敗する可能性があるため、失敗を最終エラーにせずtreeUri fallbackへ進める。これにより、fast pathが効くProviderでは高速化し、効かないProviderでも既存の成功経路を維持できる。

代替としてProvider別skip状態を保存する案もあるが、今回はまずDocumentsContractが実機Providerで有効かを確認する段階である。KISS / YAGNIの観点から、永続化やProvider別分岐は後続STEPへ回した。

## 代替案

- Provider別fast pathスキップ: 毎回UnsupportedになるProviderでは有効だが、Provider識別・保存・失敗履歴の設計が必要になるため今回は不採用。
- treeUri fallbackのみへ一本化: 動作は単純になるが、`DocumentsContract.renameDocument()` が成功するProviderでの高速化機会を捨てるため不採用。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `doc/STEP4-23.md`
- `doc/STEP4-23_codex.md`

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

- `documentsContract rename start` が出る
- 成功時は `documentsContract rename success ... afterUri=...` が出る
- 成功時は `treeUri fallback start` が出ない
- 失敗時は `documentsContract rename failed ...` または `returned null` が出る
- 失敗時は `documentsContract rename fallback to treeUri reason=...` が出る
- fallback時は `treeUri listFiles source=selectedDirectory` が維持されている
- fallback時は `treeUri ... scannedCount=... matchedIndex=...` が維持されている
- singleUri由来の `DocumentFile.renameTo()` による `UnsupportedOperation` が出ない
- `AndroidRuntime` のクラッシュが出ていない

## Git操作結果

- STEP4-22 commit hash: `9679b93`
- STEP4-22 push結果: `origin/feature/step4-22-tree-uri-early-exit` へpush済み
- STEP4-23作業ブランチ: `feature/step4-23-documents-contract-rename`
- STEP4-23分岐元ブランチ: `feature/step4-22-tree-uri-early-exit`
- STEP4-23 commit message予定: `Use DocumentsContract rename fast path`
- STEP4-23 commit hash: 未作成
- STEP4-23 push先: `origin/feature/step4-23-documents-contract-rename` 予定
- 未コミット差分: あり

## リスク・今後の検討点

- STEP4-23では `DocumentsContract.renameDocument()` をfast pathとして試す
- Providerによっては `DocumentsContract.renameDocument()` もUnsupported / null / FileNotFoundになる可能性がある
- DocumentsContract失敗時はtreeUri fallbackへ進む設計
- DocumentsContract成功時に返るafterUriはProviderにより元Uriと同じ場合も変わる場合もある
- afterUri更新が正しくないと、次回リネームやUNDOに影響する
- コピー削除方式は未実装
- `ContentResolver.update()` によるDISPLAY_NAME変更は未実装
- `directory.listFiles()` 自体のProvider側コストはfallback時に残る
- ファイル名やUriログはリリース前に抑制方針を検討する必要がある

## 後に回す機能メモ

- DocumentsContract成功時のafterUri詳細検証
- DocumentsContract失敗Providerの記録
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
