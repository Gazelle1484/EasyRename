実機検証結果と、codexの回答から、STEP4-18のプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-18: 実装（機能単位）
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
###### 追加の要望
- リネーム前ファイルがリネームするとリネーム後になるのは維持してほしい。
- ディレクトリの読み込みは、ファイルの中身を読み込まず、中のファイル名のみの取得かを確認すること。
- 「前回のセット」を押下し、「マッチング画面に進む」が有効化したあと、もう一度前回のセットを押下すると選択を解除し、「マッチング画面に進む」をdisableにする
- リネームモードの選択式ボタンは、選択する項目の中で最も長い項目に合わせて横幅を小さくする。
- 、画面下のリネーム履歴を表示している部分が、3ボタンジェスチャーナビゲーションで隠れてしまうので、ジェスチャーナビゲーションと3ボタンジェスチャーナビゲーションの両方に対応すること。
- singleUri探索で探索しようとしたファイル名とtreeUri探索で見つかったファイル名をくらべ、singleUri探索が正しいかを確認したい。
- treeUri探索の場合、探索範囲を選択したディレクトリ内に絞っていますか？絞ると探索時間がさくげんできるのではないか。
- 自動連番機能はデフォルトでONにしておくこと。
- 自動連番機能に、連番開始番号指定を実装する。画面下のリネーム履歴を表示している部分に「選択中：＜リネーム前ファイル名＞->＜リネーム後ファイル名＞」を表示する部分と左側に、ボタン「変更」を右側に配置する。ボタンを押下するとポップアップダイヤログが出現し、「＜ファイル名の自動連番部より左側＞<数字を入力するテキストボックス><ファイル名の自動連番部より右側>」を表示し、同時に数字を入力するためのテンキーを表示する。テンキーのsubmitを押下するとポップアップが消えて、画面下部に指定した番号が入った状態の「選択中：＜リネーム前ファイル名＞->＜リネーム後ファイル名＞」を表示する。
- 「UNDO」ボタンを作ること。そのために、アプリ起動時からの作業記録を保持すること。ただし、アプリをキルしたらクリアすること。作業記録は、リネーム一件をレコード1つとして最大30件まで保持できる構造とする。より効率のよい実装パターンがあれば提案してください。
- ホーム画面でcsvプレビュー機能をつけたい。「csvプレビュー」ボタンを作り、デフォルトはグレーアウト、タップを無効にする。csvを選択後、ボタンを有効化し色を変える。

# STEP 4-17: 自動連番開始番号指定 Codex回答

## 作業ブランチ

`feature/step4-17-auto-number-start`

分岐元は `feature/step4-16-matching-ui-display` の `7828c75 Improve matching screen display states` です。

作業開始時点で以下の未コミット差分がありました。

```text
 M doc/STEP4-16_codex.md
AM doc/STEP4-17.md
```

これらはSTEP指示書・前STEP回答のドキュメント差分として扱い、今回の実装ではアプリ側の変更と分けて確認しています。

## 実装内容

- 自動連番をデフォルトONにしました。
- 選択中候補ごとの「次に使う連番番号」を取得・変更できるようにしました。
- 画面下部の選択中プレビュー右側に「変更」ボタンを追加しました。
- 「変更」ボタン押下で、連番番号入力ダイアログを表示するようにしました。
- ダイアログの入力欄は数字入力のみとし、ソフトキーボードはテンキー表示を要求します。
- 0以下、空文字、不正な数字は受け付けず、入力欄にエラーを表示します。
- 番号変更後、選択中プレビューを即時再生成するようにしました。
- リネーム成功時のみ連番カウンタを `指定番号 + 1` に進める既存仕様を維持しました。
- リネーム失敗時は連番カウンタを進めない既存仕様を維持しました。
- 自動連番OFF時は「変更」ボタンを非表示にし、既存どおり番号なしのリネーム動作を維持しました。
- 追加修正として、3ボタンナビゲーション使用時にマッチング画面下部メッセージ行と `LoadingView` がナビゲーションバーに重なる問題を修正しました。

## 実装コード

### RenameMatchingUiState.kt

```kotlin
val isAutoNumberingEnabled: Boolean = true
val selectedAutoNumber: Int? = null
```

自動連番の初期値をONにしました。

`selectedAutoNumber` は、現在選択中の候補に対して次に使う番号をUIへ公開するための状態です。番号未適用、自動連番OFF、候補未選択時は `null` になります。

### RenameMatchingViewModel.kt

追加した主なメソッドは以下です。

```kotlin
fun getNextAutoNumberForSelectedCandidate(): Int?
fun setNextAutoNumberForSelectedCandidate(number: Int)
```

`getNextAutoNumberForSelectedCandidate()` は、自動連番ONかつ候補選択済みの場合だけ、候補ごとの次番号を返します。

`setNextAutoNumberForSelectedCandidate(number)` は、1以上の番号だけを受け付け、選択中候補の `autoNumberCounters[rawPattern]` を更新します。その後 `selectedPreviewText` を再生成するため、画面下部のプレビューへ即時反映されます。

プレビュー更新は以下の小さなヘルパーに集約しました。

```kotlin
private fun RenameMatchingUiState.withSelectedPreview(): RenameMatchingUiState
```

このヘルパーで `selectedAutoNumber` と `selectedPreviewText` を同時に更新します。別々に更新すると、プレビューは5なのにUI状態の番号は1のまま、というズレが起きるためです。

ログは必要最小限で以下を追加しました。

```text
- currentAutoNumberBeforeDialog
- requestedAutoNumber
- selectedCandidate.rawPattern
- selectedPreviewText after number change
- selectedAutoNumber
```

既存のリネーム実行ログには、`autoNumber`、`counterBefore`、`counterAfter`、成功/失敗が既に出ているため、それを維持しています。

### RenameMatchingFragment.kt

画面下部の `resultText` を横並びの小さな行にし、右側に「変更」ボタンを追加しました。

表示条件は以下です。

```kotlin
val shouldShowChangeButton = state.isAutoNumberingEnabled &&
    state.selectedTargetFileId != null &&
    state.selectedCandidateId != null &&
    state.selectedPreviewText != null
```

ダイアログは `AlertDialog` + `EditText` で実装しました。

```kotlin
inputType = InputType.TYPE_CLASS_NUMBER
```

OKボタン押下時に `toIntOrNull()` で検証し、`1` 未満または不正値ならダイアログを閉じずに入力欄へエラーを表示します。

```kotlin
numberInput.error = "1以上の数字を入力してください"
```

今回の主目的は番号入力と即時反映なので、ダイアログの表示は簡易形式にしています。

```text
現在の予定名:
選択中：logs1223.txt -> A1-1-1_logs1223.txt

次に使う番号:
[ 1 ]
```

追加修正として、下部メッセージ行と `LoadingView` を `bottomContentContainer` にまとめ、ナビゲーションバーの高さ分だけ `bottomMargin` を加算するようにしました。

```kotlin
private fun applyNavigationBarBottomMargin(insetsSource: View, target: View)
```

当初はrootへbottom paddingを足していましたが、今回の画面構造では下部メッセージエリア自体が画面下端側に配置されており、root paddingだけでは対象Viewそのものが十分に上へ逃げませんでした。成功した方式は、rootまたはdecorViewから `RootWindowInsets` を取得し、実際に重なっていた `bottomContentContainer` の `bottomMargin` へ反映する方法です。

```text
root / decorView の RootWindowInsets を取得
→ navigationBars / systemBars のbottomの大きい方を使う
→ bottomContentContainer.bottomMargin に加算
```

一時的に `EasyRenameInsets` ログで確認し、実機では以下のように `bottomInset` と `appliedBottomMargin` が一致することを確認しました。

```text
navigationBottomInset=117
systemBottomInset=117
appliedBottomMargin=117
```

確認後、一時ログは削除しています。

### ResolveRenameNameUseCase.kt

変更なし。

既存の `autoNumber: Int?` 引数をそのまま使います。番号の挿入位置、Prefix / Suffix / Replace、`A1-1_*` 互換動作は既存実装に委譲しています。

### その他変更ファイル

なし。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `doc/STEP4-17_codex.md`

作業開始時点から存在していた以下のドキュメント差分も作業ツリーに残っています。

- `doc/STEP4-16_codex.md`
- `doc/STEP4-17.md`

## 変更理由

## 全体アーキテクチャ

既存の MVVM + UseCase 構成を維持しました。

```text
UI層 Fragment
→ ViewModel層
→ UseCase層
```

自動連番の次番号は「画面操作中の状態」です。そのため、永続化層やファイル操作層には入れず、Matching画面の `RenameMatchingViewModel` に閉じ込めています。

リネーム後ファイル名の生成は既存どおり `ResolveRenameNameUseCase` が担当します。ViewModelは「どの番号を使うか」を決め、UseCaseは「その番号をどこに挿入して最終ファイル名にするか」を決める責務分離です。

この構成にした理由は、番号指定UIを追加しても Prefix / Suffix / Replace や `A1-1_*` 互換のルールを二重実装しないためです。UI側で文字列を組み立てると、実行時のファイル名とプレビューがズレるリスクがあります。

## 概要

自動連番を初期ONにし、ユーザーが選択中候補の次番号を手動で指定できるようにしました。指定番号は候補ごとの次回使用値として扱い、プレビューには即時反映します。実際のカウンタ進行は、これまでどおりリネーム成功時だけです。

## 設計詳細

- `RenameMatchingUiState`
  - 責務: Matching画面の表示状態。
  - `isAutoNumberingEnabled` をデフォルトONに変更しました。
  - `selectedAutoNumber` で、選択中候補の次番号をUIへ公開します。

- `RenameMatchingViewModel`
  - 責務: 選択状態、自動連番ON/OFF、候補ごとの次番号、プレビュー生成、リネーム実行。
  - `autoNumberCounters` は `rawPattern` ごとに次回使用番号を保持します。
  - `setNextAutoNumberForSelectedCandidate()` は、番号指定を「次回使用値の上書き」として扱います。
  - `withSelectedPreview()` で番号状態とプレビュー文字列を同時更新します。

- `RenameMatchingFragment`
  - 責務: 下部プレビュー表示、「変更」ボタン、番号入力ダイアログ。
  - 自動連番OFF、未選択、実行中では「変更」ボタンを表示しない、または押せないようにします。
  - 入力検証はUIで行い、不正値をViewModelへ渡さないようにします。
  - 下部メッセージエリアは `bottomContentContainer` にまとめ、3ボタンナビゲーションと重ならないよう `bottomMargin` でInsetsを反映します。

- `ResolveRenameNameUseCase`
  - 責務: 元ファイル名、候補、RenameMode、自動連番番号から最終ファイル名を生成。
  - 今回は変更せず、既存仕様を維持しました。

## 採用理由・根拠

番号カウンタをViewModelに置いた理由は、番号がファイルそのものの属性ではなく、Matching画面での操作状態だからです。RepositoryやDataSourceへ置くと、永続化やSAFアクセスとUI操作状態が混ざります。SRPの観点で、画面状態はViewModelに閉じ込める方が保守しやすいです。

`selectedAutoNumber` をUI Stateに追加した理由は、Fragmentが `autoNumberCounters` を直接知るべきではないためです。Fragmentは表示と入力イベントだけを扱い、次番号の管理はViewModelに委譲します。これにより、将来「ゼロ埋め」「開始番号の永続化」「候補ごとの番号リセット」を追加する場合も、UIの変更範囲を小さくできます。

ダイアログ表示を簡易形式にした理由は、今回の完了条件が「数値入力とプレビュー反映」であり、ファイル名を自動連番部分の左右に完全分解する実装は文字列解析の複雑さが増えるためです。特に `A1-1_*` や拡張子付き候補では分解ルールを増やすほどバグの余地が増えます。今回はYAGNIに従い、正しい番号反映を優先しました。

`ResolveRenameNameUseCase` を変更しなかった理由は、既に自動連番番号をnullableで受け取れる設計になっており、Prefix / Suffix / Replace と `*` 互換の合成ルールが集約されているためです。ここを再実装しないことで、DRYとKISSを維持しています。

下部メッセージのナビゲーションバー重なり対応で `paddingBottom` ではなく `bottomMargin` を採用した理由は、今回重なっていたのがリスト末尾ではなく、画面下部に固定的に配置されるメッセージコンテナだったためです。paddingはコンテナ内部の余白を増やしますが、コンテナ自体の下端位置は上がりません。marginならコンテナ自体をナビゲーションバー領域の上に押し上げられるため、今回の症状に合っています。

## 代替案

- Fragment側でプレビュー文字列から番号部分を解析して置換する
  - 有効な条件: 表示文字列と実行文字列が完全に同じ形式で固定される場合。
  - 今回採用しない理由: Prefix / Suffix / Replace、拡張子、`A1-1_*` 互換があり、表示文字列の解析は壊れやすいです。実行ロジックと同じUseCaseで再生成する方が安全です。

- 自動連番開始番号をRepositoryやSharedPreferencesへ永続化する
  - 有効な条件: アプリ再起動後も候補ごとの次番号を維持したい場合。
  - 今回採用しない理由: STEP4-17の対象は画面セッション内の開始番号指定です。永続化を入れると前回セット記憶とは別の状態管理が増え、今回の範囲を超えます。

- rootのbottom paddingだけでナビゲーションバーを避ける
  - 有効な条件: root直下の通常フロー内コンテンツ全体がpadding内に収まる場合。
  - 今回採用しない理由: 下部メッセージコンテナ自体が下端側に配置され、paddingだけでは対象Viewが十分に上へ移動しなかったため。

## 動作確認方法

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
23. 3ボタンナビゲーションで下部メッセージ行がナビゲーションボタンに重ならないことを確認する
24. LoadingView表示中もナビゲーションボタンに重ならないことを確認する
```

## ビルド確認結果

以下を実行しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

以下も実行しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 実機確認手順

実機確認が必要です。
以下の項目を確認してください。
確認OKなら commit / push に進みます。

```text
- 自動連番がデフォルトONになっている
- 変更ボタンが表示される
- 番号入力ダイアログが表示される
- テンキー入力になる
- 0、空文字、不正な数字でOKしてもダイアログが閉じずエラーになる
- 指定番号がプレビューに反映される
- 指定番号でリネームされる
- 成功時だけ次番号に進む
- 失敗時は番号が進まない
- 自動連番OFF時の既存挙動が壊れていない
```

## Git操作結果

- 作業開始時のgit status:

```text
 M doc/STEP4-16_codex.md
AM doc/STEP4-17.md
```

- 作成したブランチ名: `feature/step4-17-auto-number-start`
- 分岐元ブランチ: `feature/step4-16-matching-ui-display`
- 実機確認前のビルド結果: `assembleDebug` / `testDebugUnitTest` ともに `BUILD SUCCESSFUL`
- 実機確認結果: ユーザー確認OK
- commit hash: `8015cbd Add auto number start selection`
- push先ブランチ: `origin/feature/step4-17-auto-number-start`
- 未コミット差分: 下部メッセージエリアのInsets追加修正あり

## 実機ログ確認手順

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

### 6. 確認するログ観点

```text
- showAutoNumberDialog で currentAutoNumberBeforeDialog が現在番号になっていること
- submitAutoNumber で requestedAutoNumber が入力値になっていること
- setNextAutoNumber 後に selectedPreviewText が指定番号を含んでいること
- 番号変更だけでは autoNumberCounter success=true の counterAfter が出ないこと
- リネーム成功時だけ autoNumberCounter success=true の counterAfter が +1 されること
- リネーム失敗時は autoNumberCounter success=false で counterAfter が変わらないこと
- 自動連番OFF時に autoNumber=null でリネームされること
- AndroidRuntime のクラッシュログが出ていないこと
- 3ボタンナビゲーションで下部メッセージ行とLoadingViewが重ならないこと
```

## 後に回す機能メモ

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

## 未解決事項・リスク

- ダイアログは簡易表示です。自動連番部分の左右に分解してインライン編集する完全表示は未実装です。
- 自動連番の次番号は画面セッション内のViewModel状態です。アプリ再起動後やMatching画面作り直し後の永続化は今回対象外です。
- ファイルピッカー復帰や画面回転時の完全な状態復元は今回対象外です。
- 「変更」ボタンを下部行に追加したため、長いプレビュー文字列では横幅が狭くなる可能性があります。必要なら後続STEPで折り返しやレイアウト調整を検討します。
- 下部メッセージエリアのInsets対応は、Android 15 / targetSdk 36 のEdge-to-Edge挙動を踏まえ、対象Viewの `bottomMargin` へ反映しています。将来XML/RecyclerView化する場合は、下部固定エリアとスクロール領域でInsets適用先を再確認してください。

## 次に進めるべきSTEP

下部メッセージエリアの追加修正を確認後、必要に応じて追加commitします。

```powershell
git status
git add .
git commit -m "Avoid bottom message navigation overlap"
git push
```

次の候補は、今回後回しにした以下です。

```text
1. UNDO用の作業履歴設計
2. CSVプレビュー
3. Spinner横幅調整
4. 自動連番開始番号ダイアログのインライン分解表示改善
```
