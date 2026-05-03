今回のSTEP4-10は、**STEP4-9実装後の実機検証で「1リネーム約3秒」と判明したため、まずは原因をログで分解し、1機能単位で性能改善するプロンプト**にしています。
回答をdoc/STEP4-10_codex.mdに保存すること。
````md
# STEP 4-10: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- STEP4-9で全件再読み込みは削除済みだが、実機ではまだ1リネームに約3秒かかっている
- そのため、今回のSTEP4-10では「リネーム1件あたりの処理時間をログで分解し、最も重い箇所を特定できる状態にする」
- いきなり大きな修正を入れず、まずは性能計測ログの追加だけを実装する

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

### 今回実装する機能

- リネーム1件の処理時間を段階別に計測するログ追加

### 今回実装しない機能

- 自動連番
- 一括リネーム
- RecyclerView化
- CSV仕様変更
- UIレイアウト変更
- RenameMode仕様変更
- ViewModel全体の大規模リファクタ
- `Dispatchers.IO` への本格移行
- 手動更新ボタン
- Undo / 履歴機能

---

# 背景

STEP4-9では、リネーム成功後に毎回 `HomeViewModel.refreshSelectedDirectoryFiles()` を呼び、SAFでディレクトリ全体を再走査していた処理をやめた。

代わりに以下の改善を入れた。

- `RenameResult` に `afterUri` / `sourceFileId` を追加
- SAFリネーム成功後の `afterUri` を返却
- `HomeViewModel.applyRenameResult(result)` を追加
- `RenameMatchingViewModel` 側も成功した1件だけ状態更新
- リネーム後に `DocumentFile.listFiles()` を毎回走らせないように変更

しかし、実機検証ではまだ以下の問題が残っている。

```text
1リネームに3秒ぐらいかかる。もう少し短縮したい。
````

このため、STEP4-10では次の性能改善に進む前に、まず「どこで3秒かかっているのか」を確実に見える化する。

---

# 作業ブランチ

作業開始前に、現在の状態を確認すること。

```powershell
git status
git branch --show-current
```

STEP4-9の実装ブランチを元に、新しいブランチを作成すること。

```powershell
git checkout feature/step4-9-rename-performance
git pull
git checkout -b feature/step4-10-rename-performance-log
```

ブランチ名は以下とする。

```text
feature/step4-10-rename-performance-log
```

作業完了後は、必ず以下を実行すること。

```powershell
git status
git add .
git commit -m "Add rename performance logs"
git push -u origin feature/step4-10-rename-performance-log
```

最終回答では、以下を報告すること。

* 作業ブランチ名
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* commit hash
* push結果
* 未コミット差分の有無

---

# 実装内容

## 実装対象

リネーム1件の処理時間を、以下の段階に分けてログ出力する。

### 1. UIからリネーム実行開始

対象候補:

* `RenameMatchingFragment`
* またはリネームボタンのクリック処理がある箇所

出力したい内容:

```text
EasyRenamePerf: rename click start
```

可能であれば以下も出す。

```text
EasyRenamePerf: selectedTargetName=...
EasyRenamePerf: selectedCandidateName=...
EasyRenamePerf: renameMode=...
```

---

### 2. ViewModelでリネーム開始

対象候補:

* `RenameMatchingViewModel`

出力したい内容:

```text
EasyRenamePerf: viewModel rename start
```

計測すること。

```kotlin
val start = SystemClock.elapsedRealtime()
```

または既存方針に合わせて `System.currentTimeMillis()` でもよいが、処理時間計測には `SystemClock.elapsedRealtime()` を優先すること。

---

### 3. UseCase実行時間

対象候補:

* `ExecuteRenameUseCase`

Repository呼び出しの前後で時間を測る。

出力例:

```text
EasyRenamePerf: useCase start sourceFileId=... beforeName=... afterName=...
EasyRenamePerf: useCase end elapsedMs=...
```

---

### 4. Repository / DataSource / SAF renameTo 実行時間

対象候補:

* `StorageRepositoryImpl`
* `SafDocumentDataSource`

特に `DocumentFile.renameTo(newName)` の直前・直後を必ず計測する。

出力例:

```text
EasyRenamePerf: saf renameTo start beforeName=... afterName=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
```

`DocumentFile.fromSingleUri` や `DocumentFile.fromTreeUri`、対象ファイル探索処理がある場合は、そこも分けて計測する。

出力例:

```text
EasyRenamePerf: saf resolve target start
EasyRenamePerf: saf resolve target end elapsedMs=...
```

---

### 5. ViewModel状態更新時間

対象候補:

* `RenameMatchingViewModel`
* `HomeViewModel.applyRenameResult(result)`

成功後の以下処理を計測する。

* Matching側 `targetFiles` 更新
* Matching側 `candidateFiles` 更新
* Home側 `targetFiles` 更新
* sort処理

出力例:

```text
EasyRenamePerf: matching state update start
EasyRenamePerf: matching state update end elapsedMs=...

EasyRenamePerf: home applyRenameResult start sourceFileId=...
EasyRenamePerf: home applyRenameResult end elapsedMs=...
```

---

### 6. リネーム1件全体の処理時間

ViewModel側で、ボタン押下後またはリネーム処理開始後から、UI状態更新完了までの総時間を出す。

出力例:

```text
EasyRenamePerf: rename total elapsedMs=...
```

---

# ログ出力ルール

## タグ

タグは統一すること。

```kotlin
private const val TAG_PERF = "EasyRenamePerf"
```

または既存のログ設計に合わせてよいが、実機確認で以下のコマンドで絞り込めるようにすること。

```powershell
adb logcat | findstr EasyRenamePerf
```

## ログに含める情報

最低限、以下を含めること。

```text
- 処理名
- elapsedMs
- beforeName
- afterName
- sourceFileId
- afterUri
- success
- errorType
```

ただし、ログ追加のために既存のデータ構造を大きく変更しないこと。

---

# 注意点

## 重要

今回のSTEP4-10では、性能改善の実装本体には踏み込まないこと。

今回やることは、以下だけ。

```text
1. リネーム処理の各段階に計測ログを追加する
2. 既存動作を壊さない
3. 実機で次に見るべきログを明確化する
```

## 禁止事項

* リネーム仕様を変えない
* CSV仕様を変えない
* UI仕様を変えない
* 一括リネームを追加しない
* 自動連番を追加しない
* 既存の `RenameResult` を不要に壊さない
* 大規模リファクタをしない
* 全件再読み込みを復活させない
* 成功後に `refreshSelectedDirectoryFiles()` を自動呼び出ししない

---

# 想定される原因候補

ログ追加後に切り分けたい原因は以下。

```text
1. SAFの DocumentFile.renameTo() 自体が遅い
2. 対象ファイル探索処理が遅い
3. ViewModel側のリスト更新・sortが遅い
4. Home側とMatching側の二重更新が重い
5. UIスレッド上でSAF処理を実行しているため体感が重い
6. ログやToastなどUI反映が詰まっている
```

今回の実装では、これらを判断できるログを出すことを目的にする。

---

# 変更対象ファイル候補

実際の構成に合わせて確認すること。

想定される変更ファイルは以下。

```text
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt
app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
```

ログ用の共通定数を作る場合のみ、以下のような小さい追加は許可する。

```text
app/src/main/java/com/example/easyrename/util/LogTags.kt
```

ただし、共通化のための大規模リファクタはしないこと。

---

# 実装後の動作確認方法

## ビルド確認

以下を実行すること。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 単体テスト

以下を実行すること。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text
BUILD SUCCESSFUL
```

---

# 実機でユーザが確認するべきログ

Codexは、実装後にユーザへ以下の確認手順を提示すること。

## 1. 端末接続確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

期待例:

```text
List of devices attached
ZY32LZ6H5X      device
```

## 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

## 3. 性能ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

## 4. クラッシュ確認

別ターミナルで必要に応じて確認する。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 5. 実機操作

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

## 6. ユーザが記録するべきログ

以下のログを最低3回分控えるように案内すること。

```text
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: matching state update end elapsedMs=...
EasyRenamePerf: home applyRenameResult end elapsedMs=...
```

## 7. 判断基準

ログを見て、次STEPで以下のどれに進むか判断する。

```text
- saf renameTo が大半を占める場合
  → SAF自体が重い可能性が高い。UX改善、非同期化、進捗表示を検討する。

- saf resolve target が重い場合
  → 対象ファイル探索処理のキャッシュ化・ID指定・探索回数削減を検討する。

- ViewModel / Home / Matching state update が重い場合
  → リスト更新・sort・LiveData/StateFlow通知範囲の最小化を検討する。

- totalだけ重く、個別ログが軽い場合
  → UIスレッドブロック、Toast、画面再描画、ログ位置漏れを確認する。
```

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何のログを追加したか
* どの処理時間を計測できるようにしたか
* 既存仕様を変更していないこと

## 2. 変更ファイル一覧

例:

```text
- RenameMatchingViewModel.kt
- HomeViewModel.kt
- ExecuteRenameUseCase.kt
- SafDocumentDataSource.kt
```

## 3. 動作確認方法

* ビルド方法
* 単体テスト方法
* 実機ログ確認方法

## 4. 実機で見るべきログ

ユーザがそのままPowerShellで実行できる形で提示すること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

## 5. Git操作結果

* ブランチ名
* commit message
* commit hash
* push先
* 未コミット差分の有無

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text
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
```

---

# 完了条件

このSTEP4-10の完了条件は以下。

```text
- assembleDebug が成功する
- testDebugUnitTest が成功する
- Prefix / Suffix / Replace のリネーム動作が壊れていない
- リネーム1件ごとの総時間がログで確認できる
- SAF renameTo の時間がログで確認できる
- 対象ファイル探索時間がログで確認できる
- Matching側状態更新時間がログで確認できる
- Home側状態更新時間がログで確認できる
- EasyRenamePerf で adb logcat を絞り込める
- 作業内容が commit / push されている
```

```

**ベスト案**  
このSTEPでは、いきなり高速化コードを書かせず、**3秒の内訳をログで分解**するのが安全です。STEP4-9で全件再読み込みは外しているため、次に疑うべきは `renameTo()` 自体、対象ファイル探索、UIスレッド、状態更新・sort のどれかです。

**代替案**  
すぐに `Dispatchers.IO` 対応へ進める案もありますが、原因が `DocumentFile.renameTo()` 自体なら体感改善は限定的です。先にログを入れた方が、次STEPで無駄な修正を避けられます。

**注意点**  
Codexには、今回のSTEPで「性能改善の本体」まで進ませない方がよいです。ログ追加と小さな確認だけに絞ることで、壊れた場合も原因を戻しやすくなります。
```
