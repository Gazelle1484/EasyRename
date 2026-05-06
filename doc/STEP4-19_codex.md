実機検証結果と、codexの回答から、のプロンプトを次に与えるプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-20: 実装（機能単位）
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

# STEP 4-19: UNDOボタンUI追加 Codex回答

## 作業ブランチ

`feature/step4-19-undo-button-ui`

分岐元は `feature/step4-18-rename-history` です。

STEP4-19開始前に、STEP4-18をcommit / pushしました。

```text
STEP4-18 commit: 96259c8 Add in-memory rename history
STEP4-18 push: origin/feature/step4-18-rename-history
```

## 実装内容

- マッチング画面下部エリアに `UNDO` ボタンを追加しました。
- 履歴0件では `UNDO` ボタンをdisabledにします。
- 履歴1件以上、かつリネーム実行中ではない場合に `UNDO` ボタンをenabledにします。
- `UNDO` ボタンの表示テキストに履歴件数を反映します。
- `UNDO` ボタン押下時にViewModelへイベントを渡します。
- 押下時は最新履歴の `beforeName` / `afterName` / `beforeUri` / `afterUri` / `renameMode` / `autoNumber` を `EasyRenameHistory` ログへ出力します。
- STEP4-19では逆リネーム処理は実行しません。
- 履歴削除も行いません。
- STEP4-17/18で調整した `bottomContentContainer` のbottomMargin Insets対応は維持しました。

## 実装コード

### RenameMatchingFragment.kt

下部エリアに `undoButton` を追加しました。

```kotlin
private lateinit var undoButton: Button
```

配置は、既存の `bottomContentContainer` 内です。

```text
1行目: [選択中プレビュー] [変更]
2行目: [UNDO] [LoadingView]
```

ボタン押下時はViewModelへ委譲します。

```kotlin
undoButton.setOnClickListener {
    viewModel.onUndoClicked()
}
```

enabled条件:

```kotlin
undoButton.isEnabled = state.canUndo && !state.isExecuting
```

履歴件数が1件以上ある場合は、件数を表示します。

```kotlin
UNDO (1)
UNDO (2)
```

### RenameMatchingViewModel.kt

以下を追加しました。

```kotlin
fun onUndoClicked()
```

履歴が空の場合:

```text
EasyRenameHistory: undo clicked but history is empty
```

履歴がある場合:

```text
EasyRenameHistory: undo clicked latest beforeName=... afterName=... beforeUri=... afterUri=... renameMode=... autoNumber=...
```

押下後に `updateUndoState()` を呼び、`canUndo` / `renameHistoryCount` を最新化します。

逆リネームは実行していません。`DocumentFile.renameTo()` も呼んでいません。

### RenameMatchingUiState.kt

STEP4-18で追加済みの以下を利用しています。

```kotlin
val canUndo: Boolean = false
val renameHistoryCount: Int = 0
```

今回、新しい状態追加はありません。

### その他変更ファイル

- `doc/STEP4-19.md`
- `doc/STEP4-19_codex.md`

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `doc/STEP4-19.md`
- `doc/STEP4-19_codex.md`

## 変更理由

## 全体アーキテクチャ

既存の MVVM + UseCase + Domain history 構成を維持しました。

```text
UI層 RenameMatchingFragment
→ ViewModel層 RenameMatchingViewModel
→ Domain history RenameHistoryManager
```

Fragmentはボタン表示とクリックイベントだけを担当し、履歴取得やログ出力はViewModelに委譲します。これにより、UI層が `RenameHistoryManager` を直接触らずに済みます。

## 概要

STEP4-18で追加した履歴基盤を使い、UNDOボタンのUIだけを追加しました。今回のSTEPでは、UNDO対象になる最新履歴をログで確認するところまでに留めています。

## 設計詳細

- `RenameMatchingFragment`
  - 責務: UNDOボタン表示、enabled/disabled反映、クリックイベント送信。
  - `state.canUndo && !state.isExecuting` を見てボタンを有効化します。
  - 下部エリアのInsets対応は既存の `bottomContentContainer` をそのまま使います。

- `RenameMatchingViewModel`
  - 責務: 最新履歴取得、UNDO押下ログ出力、UNDO状態更新。
  - `onUndoClicked()` はログ出力だけを行い、逆リネームは行いません。

- `RenameHistoryManager`
  - 責務: STEP4-18で実装済みの履歴保持。
  - 今回は既存API `getLatest()` / `size()` を利用します。

## 採用理由・根拠

UNDOボタン押下処理をFragmentではなくViewModelに置いた理由は、Fragmentが履歴管理の詳細を知らない状態にするためです。UI層は「押された」というイベントだけを渡し、ViewModelが最新履歴をどう扱うかを決めます。これはMVVMの責務分離に合っています。

今回は逆リネームを実装しない方針にしました。SAFではrename後にURIが変わるProviderがあり、UNDO実行では `afterUri`、同名ファイル存在、権限、履歴削除タイミングをまとめて扱う必要があります。UI追加と逆リネームを同時に入れると切り分けが難しくなるため、KISSと段階的実装を優先しました。

下部エリアに配置した理由は、ユーザーがリネーム結果や選択中プレビューを見ながら直前操作を確認する導線に合うためです。既存のInsets対応済みコンテナを使うことで、3ボタンナビゲーションとの重なり対策も再利用できます。

## 代替案

- Fragmentから直接 `RenameHistoryManager.getLatest()` を呼ぶ
  - 有効な条件: プロトタイプでUIと履歴管理を一時的に結合してもよい場合。
  - 今回採用しない理由: UI層が履歴管理へ直接依存し、次STEPの逆リネーム実装時に責務が混ざるため。

- STEP4-19で逆リネームまで実装する
  - 有効な条件: 履歴レコード設計と失敗時仕様がすでに十分固まっている場合。
  - 今回採用しない理由: SAFのafterUri、FileAlreadyExists、履歴削除タイミングが絡み、UI追加と同時に入れるとリスクが大きいため。

## 動作確認方法

```text
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. マッチング画面へ進む
5. 履歴0件の状態でUNDOボタンがdisabledであることを確認する
6. ファイルを1件選択する
7. 候補を1件選択する
8. リネーム実行する
9. リネーム成功後、UNDOボタンがenabledになることを確認する
10. UNDOボタンを押す
11. 逆リネームは実行されず、最新履歴ログだけが出ることを確認する
12. もう1件リネームし、UNDO押下ログが最新履歴を指すことを確認する
13. 3ボタンナビゲーションでUNDOボタン・メッセージ行・LoadingViewが隠れないことを確認する
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

## テスト結果

以下を実行しました。

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

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

確認項目:

```text
- 初期状態でUNDOボタンがdisabled
- リネーム成功後にUNDOボタンがenabled
- UNDO押下時に undo clicked latest が出る
- beforeName / afterName / beforeUri / afterUri / renameMode / autoNumber が出る
- UNDO押下時にrenameToが走らない
- UNDO押下時に履歴が削除されない
- 下部エリアが3ボタンナビゲーションに隠れない
```

## Git操作結果

- STEP4-18 commit hash: `96259c8 Add in-memory rename history`
- STEP4-18 push先: `origin/feature/step4-18-rename-history`
- STEP4-19作業ブランチ名: `feature/step4-19-undo-button-ui`
- STEP4-19分岐元ブランチ: `feature/step4-18-rename-history`
- 実機確認前のビルド結果: `assembleDebug` 成功
- テスト結果: `testDebugUnitTest` 成功
- 実機確認結果: ユーザー確認OK
- STEP4-19 commit hash: コミット後に確認
- STEP4-19 push先: `origin/feature/step4-19-undo-button-ui`
- 未コミット差分: コミット前の差分あり

## 実機ログ確認手順

### 1. 履歴ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

### 2. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 後に回す機能メモ

- UNDOボタン押下時の逆リネーム実行
- UNDO成功後の履歴削除
- UNDO失敗時のエラー表示
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲が選択ディレクトリ内に絞られているかの確認
- 前回のセット再押下で選択解除し、マッチング画面に進むボタンをdisableにする
- 自動連番開始番号ダイアログのインライン分解表示改善
- 画面下部リネーム履歴エリアのさらなるUI改善
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行

## 未解決事項・リスク

- STEP4-19ではUNDOボタンを追加しましたが、逆リネーム処理は未実装です。
- UNDO押下時は最新履歴ログを出すだけです。
- 履歴はSTEP4-18の仕様どおりアプリ起動中のみ保持し、アプリキルで消えます。
- SAFではrename後にUriが変わるProviderがあるため、次STEPのUNDO実行では `afterUri` を使う必要があります。
- UNDO実行時には `FileAlreadyExists` や `afterUri` 無効化への対策が必要です。
- 下部エリアにUNDOボタンを追加したため、画面幅が狭い端末ではレイアウト調整が必要になる可能性があります。

## 次に進めるべきSTEP

実機確認OK後に以下を実行します。

```powershell
git status
git add .
git commit -m "Add undo button UI"
git push -u origin feature/step4-19-undo-button-ui
```

次の候補:

```text
1. UNDO押下時の逆リネーム実行
2. UNDO成功後の履歴削除
3. CSVプレビュー
4. Spinner横幅調整
```
