STEP4-16では表示改善・選択中プレビュー・下部Insets対応が完了し、次候補として「自動連番開始番号指定」「Undo用作業履歴」「CSVプレビュー」「Spinner横幅調整」が残っています。

````md
# STEP 4-17: 実装（機能単位）用 Codex 指示

STEP4-16では、表示改善、選択中カードのハイライト、「[選択中]」表記削除、選択中プレビュー、ナビゲーションバーInsets対応が実装されています。  
このSTEPでは、次の段階として **自動連番のデフォルトON化と開始番号指定** のみを対象にしてください。

今回の目的は、既存の自動連番機能を壊さず、ユーザーが次に使う連番番号を手動で変更できるようにすることです。
回答はSTEP4-17_codex.mdに保存。
---

## 目的

- 1機能ずつ確実に完成させる
- 自動連番機能をデフォルトONにする
- 自動連番の開始番号をユーザーが指定できるようにする
- 選択中プレビューに指定番号を即時反映する
- リネーム成功時のみ連番カウンタを進める既存仕様を維持する
- 既存のPrefix / Suffix / Replace仕様を壊さない
- 既存CSV形式 `A1-1_*` の互換動作を維持する
- 前回セット記憶、最新読み込み、非同期リネーム、選択中ハイライトを壊さない
- ビルドが通る状態を維持する

---

## Gitブランチ運用

このSTEPの作業は必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell
git status
git checkout main
git pull
git checkout -b feature/step4-17-auto-number-start
````

### 注意

STEP4-17はSTEP4-16の実装を前提にします。
`main` にSTEP4-16までの変更が入っていない場合は、作業に必要な最新ブランチを確認し、どのブランチから分岐するべきかを報告してください。

必要であれば、以下のようにSTEP4-16ブランチから分岐してください。

```powershell
git checkout feature/step4-16-matching-ui-display
git pull
git checkout -b feature/step4-17-auto-number-start
```

### commit / push のタイミング

このSTEPでは、**ビルド確認後、実機確認手順を提示し、実機確認OKを確認してから commit / push** してください。

実機確認OK後に以下を実行してください。

```powershell
git status
git add .
git commit -m "Add auto number start selection"
git push -u origin feature/step4-17-auto-number-start
```

### Git注意事項

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分を確認する
* 未コミット差分がある場合は、内容を報告してから作業する
* 実機確認前にcommit / pushしない
* pushに失敗した場合は、エラー内容を報告する

---

## 今回の対象要望

今回扱う要望は以下に限定します。

```text
- 自動連番機能はデフォルトでONにしておく
- 自動連番機能に、連番開始番号指定を実装する
- 画面下部の「選択中：＜リネーム前ファイル名＞ -> ＜リネーム後ファイル名＞」表示の右側に「変更」ボタンを配置する
- 「変更」ボタン押下でポップアップダイアログを表示する
- ダイアログでは、現在のリネーム予定名のうち自動連番部分だけを数値入力欄として編集できるようにする
- 数字入力用のテンキーを表示する
- submit / OK 押下でダイアログを閉じる
- 指定した番号を反映した「選択中：＜リネーム前ファイル名＞ -> ＜リネーム後ファイル名＞」を画面下部に表示する
- 指定番号でリネーム成功した場合、次回は指定番号 + 1 から進める
```

---

## 今回やらないこと

以下は重要ですが、STEP4-17では実装しないでください。後続STEPへ回してください。

```text
- UNDOボタン
- 作業履歴30件保持
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲の追加確認
- Provider別fast pathスキップ
- HomeViewModelの読み込み処理の本格的なDispatchers.IO対応
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のキャッシュ設計
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- 画面回転時の完全な状態復元
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt
```

必要に応じて以下も変更して構いません。

```text
app/src/main/java/com/example/easyrename/ui/common/
app/src/main/res/values/strings.xml
app/src/main/res/values/colors.xml
```

---

## 禁止事項

* リネーム処理の仕様を変更しない
* Prefix / Suffix / Replace の仕様を変更しない
* 既存CSV形式 `A1-1_*` の挙動を変えない
* 前回セット記憶の処理を壊さない
* 「マッチング画面へ進む」押下時の最新読み込みを壊さない
* 選択中プレビューを削除しない
* 選択中カードのハイライトを削除しない
* 自動連番ON時の候補再利用仕様を壊さない
* リネーム成功時のみ連番カウンタを進める仕様を壊さない
* 失敗時に連番カウンタを進めない仕様を壊さない
* `treeUri fallback` を削除しない
* `FileAlreadyExists` の検出を削除しない
* リネーム後の成功1件更新方式を全件再読み込みへ戻さない
* 大規模なUI刷新をしない
* DIライブラリを追加しない

---

# STEP 4-17-1: 自動連番をデフォルトONにする

## 目的

マッチング画面を開いた時点で、自動連番がONになっている状態にする。

## 対象

* `RenameMatchingUiState`
* `RenameMatchingViewModel`

## 実装内容

現在、自動連番の初期値がOFFの場合、ONに変更してください。

例:

```kotlin
val isAutoNumberingEnabled: Boolean = true
```

## 注意

* 自動連番ボタン自体は残す
* ユーザーがOFFへ切り替えられる挙動は維持する
* 自動連番OFF時の既存挙動も維持する
* 自動連番ON時は候補を再利用可能にする既存仕様を維持する

---

# STEP 4-17-2: 次に使う自動連番番号を取得・変更できるようにする

## 目的

現在選択中の候補に対して、次に使う番号をユーザーが指定できるようにする。

## 対象

* `RenameMatchingViewModel`

## 実装内容

ViewModelに、選択中候補の次番号を取得する処理と、番号を指定する処理を追加してください。

想定メソッド:

```kotlin
fun getNextAutoNumberForSelectedCandidate(): Int?
fun setNextAutoNumberForSelectedCandidate(number: Int)
```

または、UI Stateに次番号を持たせてもよいです。

```kotlin
val selectedAutoNumber: Int? = null
```

## 番号ルール

```text
- 自動連番ON時のみ有効
- 選択中候補がある場合のみ変更可能
- 最小値は 1
- 0以下、空文字、不正な数字は受け付けない
- 指定番号は候補ごとのカウンタに反映する
- 指定後、選択中プレビューを即時更新する
- リネーム成功時は指定番号 + 1 に進む
- リネーム失敗時は指定番号を維持する
```

## 注意

* プレビュー表示だけではカウンタを進めない
* 番号指定はカウンタの「次回使用値」を更新する操作として扱う
* 候補未選択時に変更ボタンを押せないようにする

---

# STEP 4-17-3: 画面下部の選択中プレビューに「変更」ボタンを追加する

## 目的

ユーザーが選択中プレビューから連番番号を変更できるようにする。

## 対象

* `RenameMatchingFragment`

## 実装内容

画面下部の選択中プレビュー表示部分に、右側へ「変更」ボタンを追加してください。

表示イメージ:

```text
選択中：logs1223.txt -> A1-1-1_logs1223.txt        [変更]
```

## 表示条件

```text
- 自動連番ON
- リネーム前ファイルが選択済み
- リネーム候補が選択済み
- selectedPreviewText が存在する
```

## 無効化条件

```text
- 自動連番OFF
- ファイルまたは候補が未選択
- リネーム実行中
```

## 注意

* 既存の下部Insets対応を壊さない
* 3ボタンナビゲーションで隠れない状態を維持する
* UIを大規模に作り直さない
* 既存の成功/失敗結果表示を壊さない

---

# STEP 4-17-4: 連番開始番号変更ダイアログを追加する

## 目的

テンキー入力で連番開始番号を変更できるようにする。

## 対象

* `RenameMatchingFragment`
* 必要に応じて小さなDialog生成関数

## 実装内容

「変更」ボタン押下時に、番号入力ダイアログを表示してください。

ダイアログ要件:

```text
- タイトル: 連番番号を変更
- 現在のリネーム予定名を分解して表示
- 自動連番部分を EditText として表示
- EditText は数字入力のみ
- ソフトキーボードは数字テンキーを表示
- OK / キャンセルを持つ
- OK押下時に番号をViewModelへ渡す
- キャンセル時は変更しない
```

## 表示イメージ

Prefixの場合:

```text
A1-1- [ 3 ] _logs1223.txt
```

Suffixの場合:

```text
logs1223_A1-1- [ 3 ] .txt
```

Replaceの場合:

```text
A1-1- [ 3 ] .txt
```

## 実装方針

最初から完全な文字列分解が難しい場合は、以下の簡易表示でもよいです。

```text
現在の予定名:
A1-1-1_logs1223.txt

次に使う番号:
[ 1 ]
```

ただし、番号入力後にプレビューが正しく更新されることを優先してください。

## 注意

* 今回は「数値入力と反映」が主目的
* 完璧なインライン分解表示が大きくなる場合は簡易表示を採用する
* 入力値が空、不正、0以下の場合はエラー表示またはOK無効化
* ダイアログ表示中にリネーム処理を開始しない

---

# STEP 4-17-5: 番号指定後に選択中プレビューを更新する

## 目的

指定した番号が画面下部のプレビューにすぐ反映されるようにする。

## 対象

* `RenameMatchingViewModel`
* `RenameMatchingFragment`

## 実装内容

番号指定後、以下を即時更新してください。

```text
- autoNumberCounters[候補キー] = 指定番号
- selectedPreviewText を再生成
- 画面下部の表示を更新
```

## 例

現在:

```text
選択中：logs1223.txt -> A1-1-1_logs1223.txt
```

番号を5に変更後:

```text
選択中：logs1223.txt -> A1-1-5_logs1223.txt
```

その状態でリネーム成功後:

```text
次回同じ候補を選ぶと番号は6
```

## 注意

* 番号変更だけではファイル名変更しない
* 番号変更だけではカウンタを進めた扱いにしない
* 実行成功時だけ次番号へ進める
* 実行失敗時は指定番号を維持する

---

# STEP 4-17-6: 自動連番OFF時の既存挙動を維持する

## 目的

自動連番開始番号指定の追加によって、通常リネームを壊さない。

## 対象

* `RenameMatchingViewModel`
* `RenameMatchingFragment`
* `ResolveRenameNameUseCase`

## 確認内容

```text
自動連番OFF:
- Prefix: A1-1_logs1223.txt
- Suffix: logs1223_A1-1.txt
- Replace: A1-1.txt
- 候補は成功後に使用済みになる
- 変更ボタンは表示しない、またはdisabled
```

## 注意

* 自動連番OFF時に `-1` などを付けない
* 自動連番OFF時にカウンタを進めない
* 自動連番OFF時の候補使用済み挙動を維持する

---

## 実装ルール

* 今回は「自動連番デフォルトON」と「開始番号指定」だけに集中する
* UNDOは実装しない
* CSVプレビューは実装しない
* Spinner横幅調整は実装しない
* singleUri/treeUri探索ログ強化は実装しない
* ファイル探索ロジックは変更しない
* リネーム処理自体は変更しない
* 既存の非同期リネーム処理を維持する
* 成功後1件更新方式を維持する
* Prefix / Suffix / Replaceの仕様を変更しない
* `FileAlreadyExists` の保護を維持する
* ビルドが通る状態を維持する

---

## 後に回す機能メモ

以下は今回のSTEPでは実装しないでください。後続STEPの候補としてメモに残してください。

```text
- UNDOボタン
- 作業履歴30件保持
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲の追加確認
- Provider別fast pathスキップ
- HomeViewModelの読み込み処理の本格的なDispatchers.IO対応
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のキャッシュ設計
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

## 出力させるもの

以下の形式で出力してください。

```md
# STEP 4-17: 自動連番開始番号指定 Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### RenameMatchingUiState.kt

### RenameMatchingViewModel.kt

### RenameMatchingFragment.kt

### ResolveRenameNameUseCase.kt

### その他変更ファイル

## 変更ファイル一覧

## 変更理由

## 動作確認方法

## ビルド確認結果

## 実機確認手順

## Git操作結果

## 実機ログ確認手順

## 後に回す機能メモ

## 未解決事項・リスク

## 次に進めるべきSTEP
```

---

## 動作確認方法として含めてほしい内容

```text
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. マッチング画面へ進む
5. 自動連番がデフォルトONであることを確認する
6. ファイルと候補を選択する
7. 画面下部に「選択中：<元ファイル名> -> <リネーム後名>」が表示されることを確認する
8. プレビュー右側に「変更」ボタンが表示されることを確認する
9. 「変更」ボタンを押す
10. 番号入力ダイアログが表示されることを確認する
11. テンキーで `5` を入力する
12. OKを押す
13. プレビューが `-5` を含む名前に更新されることを確認する
14. リネーム実行する
15. 指定番号 `5` の名前でリネームされることを確認する
16. 同じ候補を次に使うと `6` になることを確認する
17. FileAlreadyExistsなどで失敗した場合、次番号が進まないことを確認する
18. 自動連番OFFに切り替える
19. 変更ボタンが表示されない、またはdisabledになることを確認する
20. 自動連番OFF時、既存どおり `-番号` なしでリネームされることを確認する
21. Prefix / Suffix / Replace それぞれで指定番号が期待位置に反映されることを確認する
22. `A1-1_*` 形式のCSVでも既存互換が壊れていないことを確認する
```

---

## ビルド確認

以下を実行し、結果を報告してください。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

可能であれば以下も実行してください。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

---

## 実機確認

このSTEPでは、**commit / push の前に実機確認を行ってください。**

Codexは、ビルド成功後に以下をユーザーへ提示してください。

```text
実機確認が必要です。
以下の項目を確認してください。
確認OKなら commit / push に進みます。
```

確認項目:

```text
- 自動連番がデフォルトONになっている
- 変更ボタンが表示される
- 番号入力ダイアログが表示される
- テンキー入力になる
- 指定番号がプレビューに反映される
- 指定番号でリネームされる
- 成功時だけ次番号に進む
- 失敗時は番号が進まない
- 自動連番OFF時の既存挙動が壊れていない
```

---

## 実機で確認するべきログ

### 1. 端末確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

### 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

### 3. EasyRenameログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRename
```

### 4. 性能ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

### 5. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 6. 追加でログに出すとよい内容

必要最小限で、以下を `EasyRename` に出してください。

```text
- isAutoNumberingEnabled
- selectedCandidate.rawPattern
- currentAutoNumberBeforeDialog
- requestedAutoNumber
- selectedPreviewText after number change
- counterBeforeRename
- counterAfterRename
- rename success / failure
```

### 7. 確認するログ観点

```text
- 番号変更時点で selectedPreviewText が更新されること
- 番号変更だけで counterAfterRename が進まないこと
- リネーム成功時だけ counterAfterRename が +1 されること
- リネーム失敗時は counterAfterRename が変わらないこと
- 自動連番OFF時に番号処理が走らないこと
- AndroidRuntime のクラッシュログが出ていないこと
```

---

## Git操作結果として報告してほしい内容

```text
- 作業開始時のgit status
- 作成したブランチ名
- 分岐元ブランチ
- 実機確認前のビルド結果
- 実機確認結果
- commit hash
- push先ブランチ
- 未コミット差分の有無
```

---

## 完了条件

* 作業ブランチ `feature/step4-17-auto-number-start` で作業している
* 実機確認後にcommitしてpushしている
* 自動連番がデフォルトONである
* 選択中プレビュー右側に「変更」ボタンがある
* 変更ボタン押下で番号入力ダイアログが出る
* 数字テンキーで番号入力できる
* 0以下や空文字など不正値を受け付けない
* 指定番号が選択中プレビューに即時反映される
* 指定番号でリネーム実行できる
* 成功時だけ次番号へ進む
* 失敗時は番号が進まない
* 自動連番OFF時の既存挙動が壊れていない
* Prefix / Suffix / Replace の既存動作が壊れていない
* 既存CSV形式 `A1-1_*` が壊れていない
* 前回セット記憶と最新読み込みが壊れていない
* 選択中ハイライトと下部Insets対応が壊れていない
* ビルドが通る

```
```
