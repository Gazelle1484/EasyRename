実機検証結果と、codexの回答から、STEP4-11のプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-11: 実装（機能単位）
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
# STEP 4-11: リネーム性能計測ログ追加 Codex回答

## 作業ブランチ

`feature/step4-10-rename-performance-log`

作業開始時は `feature/step4-9-rename-performance...origin/feature/step4-9-rename-performance` で、未コミット差分として以下があった。

- `doc/STEP4-9_codex.md`: STEP4-9回答ドキュメントの更新
- `doc/STEP4-10.md`: STEP4-10指示ファイルの追加

STEP4-10はSTEP4-9の計測ログ追加なので、指示どおり `feature/step4-9-rename-performance` を元にブランチを作成した。

## 実装内容

- `EasyRenamePerf` タグでリネーム1件の段階別処理時間を出すようにした。
- UIクリック開始、ViewModel開始、UseCase、Repository、SAF対象探索、SAF `renameTo`、Matching状態更新、Home状態更新、総時間を計測する。
- 計測には `SystemClock.elapsedRealtime()` を使用した。
- リネーム仕様、CSV仕様、RenameMode仕様、UIレイアウトは変更していない。
- 全件再読み込みは復活させていない。
- `Dispatchers.IO` 対応など性能改善本体には進んでいない。

## 実装コード概要

### RenameMatchingFragment.kt

リネームボタン押下時に、UI起点のログを追加した。

```text
EasyRenamePerf: rename click start selectedTargetName=... selectedCandidateName=...
```

### RenameMatchingViewModel.kt

ViewModelでリネーム開始時刻を取り、UI状態更新完了後に総時間を出すようにした。

```text
EasyRenamePerf: viewModel rename start renameMode=...
EasyRenamePerf: matching state update start ...
EasyRenamePerf: matching state update end elapsedMs=...
EasyRenamePerf: rename total elapsedMs=...
```

### HomeViewModel.kt

Home側の `applyRenameResult(result)` の処理時間を計測するようにした。

```text
EasyRenamePerf: home applyRenameResult start sourceFileId=...
EasyRenamePerf: home applyRenameResult end elapsedMs=...
```

### ExecuteRenameUseCase.kt

UseCase全体の実行時間を計測するようにした。

```text
EasyRenamePerf: useCase start sourceFileId=... beforeName=... afterName=...
EasyRenamePerf: useCase end elapsedMs=... success=... errorType=...
```

### StorageRepositoryImpl.kt

RepositoryからDataSource呼び出しまでの時間を計測するようにした。

```text
EasyRenamePerf: repository rename start fileUri=... afterName=...
EasyRenamePerf: repository rename end elapsedMs=...
```

### SafDocumentDataSource.kt

SAF側で以下を計測するようにした。

- 対象ファイル探索時間
- `DocumentFile.renameTo(newName)` 単体の時間
- SAFリネーム処理全体の時間

```text
EasyRenamePerf: saf resolve target start ...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo start beforeName=... afterName=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=...
```

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt`
- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `doc/STEP4-9_codex.md`
- `doc/STEP4-10.md`
- `doc/STEP4-10_codex.md`

## 変更理由

## 全体アーキテクチャ

既存のMVVM + Repository構成は維持した。

```text
UI層
→ ViewModel層
→ UseCase層
→ Repository層
→ SAF DataSource層
```

今回の目的は性能改善の実装ではなく、どの層で時間がかかっているかを分解して把握することである。そのため、各層の入口と出口にログを追加し、責務やデータフローは変えていない。

## 概要

STEP4-9で全件再読み込みは止めたが、実機ではまだ1件リネームに約3秒かかっている。STEP4-10では、`renameTo()` 自体が遅いのか、対象探索が遅いのか、ViewModelの状態更新が遅いのかを判断できるようにした。

## 設計詳細

- UI層
  - クリック開始時点をログ化する。
  - 選択中のファイル名・候補名だけを出す。

- ViewModel層
  - リネーム開始から状態更新完了までの総時間を測る。
  - Matching側とHome側の状態更新時間を分ける。

- UseCase層
  - バリデーション、同名チェック、Repository呼び出しを含む処理時間を測る。

- Repository / DataSource層
  - Repository経由時間とSAF内部時間を分ける。
  - SAF内部では対象探索と `renameTo` を分ける。

## 採用理由・根拠

いきなり `Dispatchers.IO` やキャッシュ化に進まなかった理由は、3秒の原因がまだ特定できていないためである。もし `DocumentFile.renameTo()` 自体が大半を占めているなら、キャッシュ化やViewModel更新最適化では効果が限定的になる。一方で、対象探索が重いなら、URI更新や探索回数削減が有効になる。

今回のログ追加は、次STEPの設計判断を誤らないための観測点である。処理仕様を変えずに観測だけ増やすことで、問題が出た場合も戻しやすい。

## 動作確認方法

### ビルド

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

### 単体テスト

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

### 実機操作

```text
1. アプリを起動する
2. Home画面でリネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. Prefixモードでマッチング画面へ進む
5. ファイルを1件選択する
6. 候補を1件選択する
7. リネーム実行する
8. 成功することを確認する
9. 同じ手順をSuffix / Replaceでも確認する
```

## ビルド結果

```text
assembleDebug: BUILD SUCCESSFUL
```

## テスト結果

```text
testDebugUnitTest: BUILD SUCCESSFUL
```

## 実機で見るべきログ

### 1. 端末接続確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

### 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

### 3. 性能ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

### 4. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 5. 最低3回分控えるログ

```text
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: matching state update end elapsedMs=...
EasyRenamePerf: home applyRenameResult end elapsedMs=...
```

## 判断基準

```text
- saf renameTo が大半を占める場合
  → SAF自体が重い可能性が高い。UX改善、非同期化、進捗表示を検討する。

- saf resolve target が重い場合
  → 対象ファイル探索処理のキャッシュ化、ID指定、探索回数削減を検討する。

- ViewModel / Home / Matching state update が重い場合
  → リスト更新、sort、StateFlow通知範囲の最小化を検討する。

- totalだけ重く、個別ログが軽い場合
  → UIスレッドブロック、画面再描画、ログ位置漏れを確認する。
```

## 実機ログ分析結果

2026-05-03 21:06〜21:07の実機ログでは、Prefix / Suffix / Replace の成功ケースで、おおむね以下の傾向が確認された。

```text
リネーム1件の総時間:
- Prefix: 2186ms
- Suffix: 2213ms
- Replace: 2208ms

UseCase全体:
- 約 2122〜2164ms

Repository + SAF:
- 約 1352〜1405ms

SAF対象探索:
- 約 557〜603ms

DocumentFile.renameTo:
- 約 343〜391ms

Matching側状態更新:
- 約 24〜38ms

Home側状態更新:
- 約 0〜1ms
```

Replaceで `FileAlreadyExists` になったケースもあり、その場合はRepository / SAFへ進まず、約385msで失敗している。これは既に同名ファイルが存在したためで、性能問題とは別の正常な失敗である。

## 性能問題の主因

ログから、`DocumentFile.renameTo()` 自体は約0.35秒であり、総時間2.2秒の主因ではない。

最も大きい問題は、SAFのディレクトリ走査・DocumentFile確認が複数回走っていることである。

特に以下の区間が重い。

```text
useCase start
→ repository rename start
約 0.75〜0.80秒
```

この区間では、`ExecuteRenameUseCase` の事前同名チェックが走っている。

```kotlin
storageRepository.existsInSameDirectory(...)
```

その後、`SafDocumentDataSource.renameFile()` 内でも以下を行っている。

```text
1. directory.listFiles() で対象ファイル探索
2. 同じ listFiles 結果で重複チェック
3. targetFile.exists()
4. targetFile.canWrite()
5. targetFile.isFile
6. targetFile.name
7. renameTo()
```

つまり現状は、1件リネームのために以下のような重複したSAFアクセスが発生している。

```text
1. UseCaseで同名チェック
   → SAFディレクトリ走査相当で約0.8秒

2. DataSourceで対象ファイル探索
   → SAFディレクトリ走査で約0.6秒

3. DataSourceでDocumentFile詳細確認
   → exists / canWrite / isFile / name などProvider呼び出し

4. renameTo
   → 約0.35秒
```

状態更新は軽い。

```text
Matching側状態更新: 24〜38ms
Home側状態更新: 0〜1ms
```

そのため、ViewModelの状態更新やsortは今回の主因ではない。

## 次STEPの推奨修正方針

次STEPでは、自動連番へ進む前に、SAF走査回数を減らす性能改善を行うべきである。

最優先の修正は以下。

```text
ExecuteRenameUseCase側の事前同名チェックをやめる
↓
SafDocumentDataSource.renameFile内で、
対象探索と同名チェックを1回の listFiles 結果でまとめて行う
```

期待効果:

```text
useCase start → repository rename start の約0.75〜0.80秒を削減できる可能性が高い。
```

次点の修正候補。

```text
STEP4-6調査用に追加した重いDocumentFile詳細ログを整理する
```

具体的には、毎回以下を呼ぶ必要があるか見直す。

```kotlin
targetFile.exists()
targetFile.canWrite()
targetFile.isFile
targetFile.name
```

これらは一見軽そうに見えるが、SAF Provider越しではコストが発生する可能性がある。通常成功ケースではログを減らし、失敗時だけ詳細確認する形が望ましい。

## Git操作結果

- 作業ブランチ名: `feature/step4-10-rename-performance-log`
- commit message: `Add rename performance logs`
- push先: `origin/feature/step4-10-rename-performance-log`
- commit hash: コミット作成後に最終回答で報告
- 未コミット差分の有無: コミット後に確認

## 後に回す機能メモ

- SAF処理のDispatchers.IO対応
- リネーム中のローディング表示
- リネームボタンの二重押下防止
- リネーム処理のキャンセル
- 一括リネーム
- 自動連番機能
- 自動連番ON/OFFボタン
- 候補ごとの連番カウンタ管理
- 同じ候補を複数ファイルに使える自動連番モード
- 手動更新ボタン
- リネーム成功後の明示的な再読み込み
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- Edge-to-Edge / WindowInsetsの正式対応
- 画面回転時の完全な状態復元
- CSVプレビュー
- リネーム前後の差分プレビュー
- 同名時の自動連番
- 履歴・取り消し機能
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー

## 未解決事項・リスク

- 今回はログ追加のみで、実際の高速化はしていない。
- まだViewModel内のSAFアクセスは本格的に `Dispatchers.IO` へ移していない。
- ログ量は増えるため、実機確認後に不要な詳細ログを整理する可能性がある。
- `rename total` と各部分ログの差分が大きい場合、今回ログを置いていないUI再描画やStateFlow collect側を追加で見る必要がある。

## 次に進めるべきSTEP

実機で `EasyRenamePerf` ログを最低3回分取得し、3秒の内訳を確認する。

その結果に応じて次STEPを決める。

- `saf renameTo` が重い: 非同期化、進捗表示、UX改善
- `saf resolve target` が重い: 対象ファイル探索の削減
- 状態更新が重い: sortやリスト更新範囲の最小化
- totalだけ重い: UIスレッドや再描画側の追加調査

実機ログの結果、次は以下に進むのが妥当である。

```text
STEP4-11候補:
- UseCase側の事前同名チェックを削除
- 同名チェックをSafDocumentDataSource.renameFile内のlistFiles結果に一本化
- 成功時のDocumentFile詳細確認ログを削減
- Prefix / Suffix / Replace の既存挙動は維持
- FileAlreadyExists の失敗表示は維持
```

この修正により、1件あたり約2.2秒のうち、少なくとも約0.8秒の削減が期待できる。
