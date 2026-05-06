以下をそのまま `doc/STEP4-21.md` として保存して、Codexに渡してください。
STEP4-20では最新1件UNDOの実装・ビルド・テストまで完了し、`STEP4-20 commit hash: 実機確認OK後に作成` / `未コミット差分: あり` の状態なので、STEP4-21では**まずSTEP4-20のcommit/pushを完了**させ、その後に **singleUri / treeUri探索確認ログ追加** だけを単機能で実装する形にしています。

````md
# STEP 4-21: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- singleUri fast path と treeUri fallback の探索挙動を実機ログで確認できるようにする
- singleUriで探索しようとしたファイル名と、treeUri探索で見つかったファイル名を比較できるようにする
- treeUri探索が選択したディレクトリ内に絞られているかをログで確認できるようにする
- 探索時間をログで確認し、次STEPで性能改善するか判断できる状態にする
- 今回はログ追加と確認に限定し、探索ロジックの大幅変更は行わない

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

## 今回実装する機能

- singleUri探索開始時のログを追加する
- singleUriで探索しようとしたファイル名をログ出力する
- singleUriで取得できた `DocumentFile.name` をログ出力する
- singleUri成功/失敗理由をログ出力する
- singleUri探索時間をログ出力する
- treeUri fallback開始時のログを追加する
- treeUri探索対象の `directoryUri` をログ出力する
- treeUri探索で比較しているファイル名をログ出力する
- treeUri探索で見つかったファイル名をログ出力する
- singleUriで期待したファイル名とtreeUriで見つかったファイル名が一致するかログ出力する
- treeUri探索時間をログ出力する
- treeUri探索が選択ディレクトリ内だけを対象にしているか判断できるログを出す
- 既存のリネーム処理、UNDO、自動連番、下部Insets対応を壊さない

## 今回実装しない機能

- singleUri fast pathの廃止
- singleUri fast pathの仕様変更
- treeUri fallbackの探索ロジック大幅変更
- Provider別最適化
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のキャッシュ設計
- treeUri探索範囲削減の実装本体
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- リネームモード選択式ボタンの幅調整
- 前回のセット再押下で選択解除する処理
- 自動連番開始番号ダイアログのインライン分解表示改善
- 複数件UNDO
- 作業履歴一覧表示
- RecyclerView化
- XMLレイアウト大規模変更
- Material Componentsへの本格移行

---

# 重要: STEP4-20のcommit / pushを先に完了すること

STEP4-20のCodex回答では以下の状態だった。

```text
STEP4-20 commit hash: 実機確認OK後に作成
STEP4-20 push先: 実機確認OK後に origin/feature/step4-20-undo-rename
未コミット差分: あり
````

そのため、STEP4-21の作業を始める前に、必ずSTEP4-20をcommit / pushすること。

## 1. 現在状態確認

```powershell
git status
git branch --show-current
```

現在ブランチが以下であることを確認する。

```text
feature/step4-20-undo-rename
```

## 2. STEP4-20のビルド確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 3. STEP4-20の単体テスト確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 4. STEP4-20をcommit / push

```powershell
git status
git add .
git commit -m "Implement single undo rename"
git push -u origin feature/step4-20-undo-rename
```

commit hashを控えること。

---

# STEP4-21 作業ブランチ

STEP4-20のcommit / push完了後、STEP4-21用ブランチを作成する。

```powershell
git checkout feature/step4-20-undo-rename
git pull
git checkout -b feature/step4-21-saf-resolve-logs
```

ブランチ名は以下とする。

```text
feature/step4-21-saf-resolve-logs
```

作業完了後、実機確認OKのあとに以下を実行すること。

```powershell
git status
git add .
git commit -m "Add SAF resolve diagnostic logs"
git push -u origin feature/step4-21-saf-resolve-logs
```

Codexの最終回答では以下を報告すること。

* STEP4-20のcommit hash
* STEP4-20のpush結果
* STEP4-21の作業ブランチ名
* STEP4-21の分岐元ブランチ
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* 実機確認結果
* STEP4-21のcommit hash
* STEP4-21のpush結果
* 未コミット差分の有無
* 回答を保存した `doc/STEP4-21_codex.md`

---

# 背景

STEP4-20では以下を実装済み。

* STEP4-19のUNDOボタンから最新履歴1件を逆リネーム
* UNDOでは履歴の `afterUri` を使用
* `afterName -> beforeName` の方向でリネーム
* UNDO成功時のみ履歴から最新1件を削除
* UNDO失敗時は履歴を削除しない
* Matching画面とHome画面の表示をUNDO結果に同期
* 自動連番カウンタは巻き戻さない
* 下部Insets対応を維持

一方で、実機ログでは過去に以下のような挙動が確認されている。

```text
singleUri fast path が UnsupportedOperationException になっても、TreeUriFallback 成功後に履歴追加される
```

現状、treeUri fallbackで成功しているため動作は成立しているが、以下がまだ十分に確認できていない。

```text
- singleUriで探索しようとした対象ファイル名
- singleUriで実際に取得できたDocumentFile名
- singleUriが失敗した理由
- treeUri fallbackで見つかったファイル名
- singleUriで期待したファイル名とtreeUriで見つかったファイル名が一致しているか
- treeUri探索が選択ディレクトリ内に絞られているか
- singleUri / treeUri のどちらに時間がかかっているか
```

STEP4-21では、これらを確認するためのログを追加する。

---

# 実装方針

## 1. ログ追加に限定する

今回のSTEPでは、原則として探索ロジックを変更しない。

やることは以下。

```text
- 既存処理の前後にログを追加する
- 既存処理の計測時間を出す
- 既存処理で扱っているファイル名・Uri・directoryUriを出す
- singleUri結果とtreeUri結果を比較できるようにする
```

やらないことは以下。

```text
- singleUriの成功条件を変える
- treeUriの探索方法を変える
- fallback条件を変える
- キャッシュ化する
- Provider別分岐を追加する
```

ただし、ログを出すために小さなヘルパー関数を追加することは許可する。

---

# ログタグ

タグは以下で統一する。

```kotlin
private const val TAG_SAF_RESOLVE = "EasyRenameSafResolve"
```

既存ログ設計に合わせる必要がある場合でも、実機確認で以下のコマンドで絞り込めるようにすること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

---

# ログ仕様

## 1. リネーム開始時

リネーム対象の基本情報を出す。

```text
EasyRenameSafResolve: rename request start
EasyRenameSafResolve: directoryUri=...
EasyRenameSafResolve: requestedFileUri=...
EasyRenameSafResolve: expectedBeforeName=...
EasyRenameSafResolve: requestedAfterName=...
```

`expectedBeforeName` は、リネーム前ファイルとしてUI側またはUseCase側で把握しているファイル名を指す。

---

## 2. singleUri探索開始

singleUri fast pathを試す直前に出す。

```text
EasyRenameSafResolve: singleUri start expectedName=... fileUri=...
```

計測開始する。

```kotlin
val singleStart = SystemClock.elapsedRealtime()
```

---

## 3. singleUri成功時

`DocumentFile.fromSingleUri(...)` などで取得できた場合、以下を出す。

```text
EasyRenameSafResolve: singleUri document resolved name=... uri=... exists=... canWrite=...
```

可能なら、期待名との一致結果も出す。

```text
EasyRenameSafResolve: singleUri name compare expected=... actual=... matches=true/false
```

singleUriでそのままrenameToへ進む場合は以下を出す。

```text
EasyRenameSafResolve: singleUri accepted expected=... actual=...
```

---

## 4. singleUri失敗時

例外や不正状態でfallbackする場合、理由を出す。

```text
EasyRenameSafResolve: singleUri failed reason=... exception=...
```

例:

```text
EasyRenameSafResolve: singleUri failed reason=UnsupportedOperationException exception=...
EasyRenameSafResolve: singleUri failed reason=name_mismatch expected=... actual=...
EasyRenameSafResolve: singleUri failed reason=document_not_found
EasyRenameSafResolve: singleUri failed reason=not_writable
```

処理時間を出す。

```text
EasyRenameSafResolve: singleUri end success=false elapsedMs=...
```

---

## 5. treeUri fallback開始

treeUri fallbackへ入る直前に出す。

```text
EasyRenameSafResolve: treeUri fallback start directoryUri=... expectedName=...
```

計測開始する。

```kotlin
val treeStart = SystemClock.elapsedRealtime()
```

---

## 6. treeUri探索範囲確認

treeUri探索対象が選択ディレクトリ内であるかを確認できるログを出す。

```text
EasyRenameSafResolve: treeUri scope directoryUri=...
EasyRenameSafResolve: treeUri listFiles from selected directory only=true
```

実装上、選択ディレクトリの `DocumentFile.fromTreeUri(context, directoryUri)` に対して `listFiles()` しているなら、以下のように明記する。

```text
EasyRenameSafResolve: treeUri listFiles source=selectedDirectory
```

もし親ディレクトリや全ストレージ相当を探索している実装がある場合は、今回ロジック変更はせず、以下のようにログで分かるようにする。

```text
EasyRenameSafResolve: treeUri listFiles source=unknown_or_wider_scope
```

---

## 7. treeUri探索中

全ファイル名を大量に出しすぎないこと。

ただし、比較対象が分かるように、以下は出す。

```text
EasyRenameSafResolve: treeUri compare candidateName=... expectedName=... matches=true/false
```

ファイル数が多い場合にログが多すぎるため、候補比較ログは以下のどちらかにする。

### 推奨

一致したもの、または一致候補だけ出す。

```text
EasyRenameSafResolve: treeUri candidate matched name=... uri=...
```

### デバッグ優先で全件出す場合

最大件数を制限する。

```kotlin
private const val TREE_URI_COMPARE_LOG_LIMIT = 50
```

ログ例:

```text
EasyRenameSafResolve: treeUri compare[1] candidateName=...
EasyRenameSafResolve: treeUri compare log truncated count=...
```

---

## 8. treeUri成功時

treeUriで対象ファイルを見つけた場合、以下を出す。

```text
EasyRenameSafResolve: treeUri found expected=... actual=... uri=...
EasyRenameSafResolve: treeUri name compare expected=... actual=... matches=true
```

singleUri側で取得できていた名前がある場合は比較する。

```text
EasyRenameSafResolve: singleVsTree compare singleName=... treeName=... matches=true/false
```

singleUriが例外で名前を取れていない場合は、その旨を出す。

```text
EasyRenameSafResolve: singleVsTree compare skipped reason=singleUriNameUnavailable
```

処理時間を出す。

```text
EasyRenameSafResolve: treeUri end success=true elapsedMs=...
```

---

## 9. treeUri失敗時

対象ファイルが見つからない場合。

```text
EasyRenameSafResolve: treeUri not found expectedName=... scannedCount=... elapsedMs=...
```

---

## 10. renameTo前後

既存で出している場合は維持する。

なければ以下を追加する。

```text
EasyRenameSafResolve: renameTo start sourceName=... targetName=... sourceUri=...
EasyRenameSafResolve: renameTo end success=... elapsedMs=... afterUri=...
```

通常リネームとUNDOの両方で通る処理なら、どちらなのか分かる情報を可能なら出す。

```text
EasyRenameSafResolve: operation=normalRename
```

または、

```text
EasyRenameSafResolve: operation=undoRename
```

既存構成上、DataSourceでoperationを判別できない場合は無理に追加しなくてよい。

---

# 実装対象候補

実際の構成に合わせて確認すること。

最有力変更ファイル:

```text
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
```

必要に応じて変更:

```text
app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt
app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt
app/src/main/java/com/example/easyrename/domain/usecase/UndoRenameUseCase.kt
app/src/main/java/com/example/easyrename/model/RenameResult.kt
```

ただし、今回の目的はログ追加なので、既存データ構造の変更は最小限にすること。

ドキュメント出力:

```text
doc/STEP4-21_codex.md
```

---

# 注意点

## ログ量を増やしすぎない

treeUri fallbackでディレクトリ内ファイルが多い場合、全ファイル名をログ出力するとlogcatが読みにくくなる。

そのため以下のどちらかにする。

```text
- 一致した候補だけログ出力する
- 比較ログは最大50件までに制限する
```

## 個人情報・ファイル名ログについて

実機確認のためファイル名とUriをログ出力するが、これは開発中確認用である。

最終的にリリースビルドでログを抑制する必要がある場合は後続STEPで対応する。

今回のSTEPでは、確認しやすさを優先する。

## ロジック変更しない

今回のSTEPで、以下を変更しない。

```text
- singleUri成功/失敗の判定
- treeUri fallback条件
- treeUri探索ロジック
- renameTo実行ロジック
```

もしログ追加の過程で明らかにバグを発見した場合は、修正せずに `doc/STEP4-21_codex.md` の未解決事項へ記録すること。

---

# 禁止事項

* singleUri fast pathを削除しない
* treeUri fallbackを削除しない
* treeUri探索範囲を今回変更しない
* `DocumentFile.renameTo()` の呼び出し箇所を大きく変えない
* FragmentやViewModelから直接 `DocumentFile.renameTo()` を呼ばない
* リネーム仕様を変えない
* UNDO仕様を変えない
* 自動連番仕様を変えない
* CSV仕様を変えない
* CSVプレビューを今回実装しない
* Spinner横幅調整を今回実装しない
* 大規模リファクタをしない

---

# 動作確認方法

## 1. ビルド確認

以下を実行すること。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 2. 単体テスト

以下を実行すること。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 3. 実機確認

以下を確認する。

```text
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. マッチング画面へ進む
5. Prefixモードでファイルを1件リネームする
6. singleUri / treeUri 関連ログを確認する
7. Suffixモードでファイルを1件リネームする
8. Replaceモードでファイルを1件リネームする
9. UNDOを1件実行する
10. UNDO時にも必要なSAF探索ログが出ることを確認する
11. リネーム前ファイルがリネーム後になる既存挙動が壊れていないことを確認する
12. UNDO成功時にファイル名が戻る既存挙動が壊れていないことを確認する
13. 自動連番ON/OFFの既存挙動が壊れていないことを確認する
```

---

# 実機で確認するべきログ

## 1. 端末確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

## 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

## 3. SAF探索ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

## 4. 既存履歴ログ確認

UNDO確認時に必要なら見る。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. 既存ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRename
```

## 6. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 7. 確認するログ観点

以下を確認する。

```text
- singleUri start が出る
- singleUri expectedName がリネーム対象ファイル名になっている
- singleUri document resolved name が出る、または failed reason が出る
- singleUri name compare expected / actual / matches が確認できる
- singleUri elapsedMs が出る
- treeUri fallback start が出る
- treeUri directoryUri が選択したディレクトリUriになっている
- treeUri listFiles source=selectedDirectory 相当が出る
- treeUri found expected / actual / uri が出る
- treeUri name compare expected / actual / matches=true が出る
- singleVsTree compare が出る、または skipped reason が出る
- treeUri elapsedMs が出る
- renameTo start / end が出る
- renameTo afterUri が出る
- AndroidRuntime のクラッシュが出ていない
```

---

# 実機ログを見た後の判断基準

## 1. singleUriが常に失敗し、treeUri fallbackが成功している場合

次STEP候補:

```text
STEP4-22: singleUri fast pathの必要性見直し、またはProvider別fallback方針整理
```

## 2. singleUriのactualNameとexpectedNameが一致しない場合

次STEP候補:

```text
STEP4-22: singleUri結果の検証強化
```

この場合、singleUriで得たDocumentFileをそのまま信用すると危険。

## 3. treeUri探索が選択ディレクトリ内に絞られていない場合

次STEP候補:

```text
STEP4-22: treeUri探索範囲を選択ディレクトリ内に限定
```

## 4. treeUri探索時間が長い場合

次STEP候補:

```text
STEP4-22: treeUri探索回数削減または対象ファイルMap化
```

## 5. singleUriもtreeUriも問題なく、探索時間も短い場合

次STEP候補:

```text
STEP4-22: CSVプレビュー
```

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何のログを追加したか
* singleUriで何を確認できるようになったか
* treeUriで何を確認できるようになったか
* singleUri名とtreeUri名の比較方法
* treeUri探索範囲の確認方法
* 探索時間ログ
* 既存ロジックを変更していないこと

## 2. 変更ファイル一覧

例:

```text
- SafDocumentDataSource.kt
- doc/STEP4-21_codex.md
```

必要に応じて:

```text
- StorageRepositoryImpl.kt
- ExecuteRenameUseCase.kt
- UndoRenameUseCase.kt
```

## 3. 動作確認方法

* ビルド方法
* 単体テスト方法
* 実機ログ確認方法
* 通常リネーム確認方法
* UNDO確認方法

## 4. 実機で見るべきログ

ユーザがそのままPowerShellで実行できる形で提示すること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

必要に応じて以下も提示すること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. Git操作結果

* STEP4-20のcommit hash
* STEP4-20のpush結果
* STEP4-21の作業ブランチ名
* STEP4-21の分岐元ブランチ
* STEP4-21のcommit message
* STEP4-21のcommit hash
* STEP4-21のpush先
* 未コミット差分の有無

## 6. 保存ファイル

Codexの回答は以下へ保存すること。

```text
doc/STEP4-21_codex.md
```

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text
- singleUri fast pathの仕様見直し
- singleUri結果の検証強化
- treeUri探索範囲の実装修正
- treeUri探索時間削減
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- Provider別fast pathスキップ
- CSVプレビュー
- CSVプレビューボタン
- CSV選択後のプレビューボタン有効化・色変更
- CSVプレビュー専用画面
- CSVプレビューの表形式表示
- CSV文字コード自動判定
- Spinner横幅調整
- リネームモード選択式ボタンの最長項目に合わせた幅調整
- 前回のセット再押下で選択解除し、マッチング画面に進むボタンをdisableにする
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- UNDO成功時の自動連番カウンタ巻き戻し
- UNDO失敗時の詳細ダイアログ表示
- 自動連番開始番号ダイアログのインライン分解表示改善
- 画面下部リネーム履歴エリアのさらなるUI改善
- HomeViewModelの読み込み処理の本格的なDispatchers.IO対応
- 手動更新ボタン
- リネーム成功後の明示的な再読み込み
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- Edge-to-Edge / WindowInsets全面対応
- 画面回転時の完全な状態復元
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー
```

---

# 未解決事項・リスクとして明記すること

Codexの最終回答では、以下を未解決事項として明記すること。

```text
- STEP4-21ではログ追加のみで、探索ロジック自体は変更していない
- treeUri探索が広すぎる場合でも、今回のSTEPでは修正せずログで確認する
- singleUriが常に失敗するProviderでは、次STEPでfast pathの扱いを見直す必要がある
- ファイル名やUriをログ出力するため、リリース前にはログ抑制方針を検討する必要がある
- ディレクトリ内ファイル数が多い場合、treeUri比較ログは制限しているため全件は出ない可能性がある
```

---

# 完了条件

このSTEP4-21の完了条件は以下。

```text
- STEP4-20がcommit / pushされている
- assembleDebug が成功する
- testDebugUnitTest が成功する
- singleUri start ログが出る
- singleUri expectedName が確認できる
- singleUri actualName または失敗理由が確認できる
- singleUri elapsedMs が確認できる
- treeUri fallback start ログが出る
- treeUri directoryUri が確認できる
- treeUri探索が選択ディレクトリ由来か判断できる
- treeUri found expected / actual が確認できる
- singleUri名とtreeUri名の比較結果が確認できる
- treeUri elapsedMs が確認できる
- renameTo start / end / afterUri が確認できる
- 通常リネームが壊れていない
- UNDOが壊れていない
- 自動連番が壊れていない
- 実機確認後にSTEP4-21がcommit / pushされている
- Codex回答が doc/STEP4-21_codex.md に保存されている
```

```

**ベスト案**  
今回はCSVプレビューではなく、`singleUri / treeUri` の探索確認ログを先に入れるのが妥当です。通常リネームとUNDOの両方がSAF探索に依存しているため、ここを先に見える化すると、次の性能改善やFileNotFound対策の判断材料になります。

**代替案**  
STEP4-21で探索ロジック修正まで進める案もありますが、今はまだ「本当に探索範囲が広いのか」「singleUriが何を返しているのか」が見えていません。まずログ追加に限定した方が安全です。

**注意点**  
このSTEPではロジック変更をCodexにさせない方がよいです。ログで `expectedName`、`actualName`、`directoryUri`、`elapsedMs` を確認してから、STEP4-22で探索範囲削減やfast path見直しに進む流れが安全です。
```
