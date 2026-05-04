以下をそのまま `doc/STEP4-19.md` として保存して、Codexに渡してください。
STEP4-18では、UNDO用履歴保持の実装・ビルド・テスト・成功時の実機ログ確認までは完了していますが、`commit hash: 未作成` / `push先ブランチ: 未push` の状態なので、STEP4-19では**まずSTEP4-18のcommit/pushを完了させたうえで、UNDOボタンUI追加だけを単機能で実装**する構成にしています。

````md id="z20o91"
# STEP 4-19: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- STEP4-18で追加したUNDO用作業履歴を利用して、画面上にUNDOボタンを表示する
- 今回は「UNDOボタンのUI追加」と「押下時ログ出力」までを実装する
- 実際の逆リネーム処理はまだ実装しない
- 既存のリネーム処理、自動連番、履歴追加処理、下部Insets対応を壊さない

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

## 今回実装する機能

- マッチング画面の下部エリアに `UNDO` ボタンを追加する
- 履歴が0件のときは `UNDO` ボタンをdisabledにする
- 履歴が1件以上あるときは `UNDO` ボタンをenabledにする
- `UNDO` ボタン押下時に、最新履歴の内容をログ出力する
- 実際のUNDO、つまり逆リネーム処理は行わない
- `canUndo` / `renameHistoryCount` をUI表示に反映する
- ボタン追加後も3ボタンナビゲーション・ジェスチャーナビゲーションで下部エリアが隠れないようにする

## 今回実装しない機能

- UNDO押下時の逆リネーム実行
- UNDO成功後の履歴削除
- UNDO失敗時のエラー表示
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲の追加確認
- ディレクトリ読み込み内容の確認ログ
- 前回のセット再押下で選択解除する処理
- 自動連番開始番号ダイアログのインライン分解表示改善
- RecyclerView化
- XMLレイアウト大規模変更
- Material Componentsへの本格移行

---

# 重要: STEP4-18のcommit / pushを先に完了すること

STEP4-18のCodex回答では以下の状態だった。

```text
commit hash: 未作成
push先ブランチ: 未push
未コミット差分: あり
````

そのため、STEP4-19の作業を始める前に、必ずSTEP4-18の実装をcommit / pushすること。

## 1. 現在状態確認

```powershell id="l5jkx1"
git status
git branch --show-current
```

現在ブランチが以下であることを確認する。

```text id="mebqdb"
feature/step4-18-rename-history
```

## 2. STEP4-18のビルド確認

```powershell id="mg4h3o"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text id="f6pz0t"
BUILD SUCCESSFUL
```

## 3. STEP4-18の単体テスト確認

```powershell id="v9nykh"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text id="rxcdkn"
BUILD SUCCESSFUL
```

## 4. STEP4-18をcommit / push

```powershell id="pr0wd3"
git status
git add .
git commit -m "Add in-memory rename history"
git push -u origin feature/step4-18-rename-history
```

commit hashを控えること。

---

# STEP4-19 作業ブランチ

STEP4-18のcommit / push完了後、STEP4-19用ブランチを作成する。

```powershell id="qq5hr7"
git checkout feature/step4-18-rename-history
git pull
git checkout -b feature/step4-19-undo-button-ui
```

ブランチ名は以下とする。

```text id="xskx9u"
feature/step4-19-undo-button-ui
```

作業完了後、実機確認OKのあとに以下を実行すること。

```powershell id="w6kt2f"
git status
git add .
git commit -m "Add undo button UI"
git push -u origin feature/step4-19-undo-button-ui
```

Codexの最終回答では以下を報告すること。

* STEP4-18のcommit hash
* STEP4-18のpush結果
* STEP4-19の作業ブランチ名
* STEP4-19の分岐元ブランチ
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* 実機確認結果
* STEP4-19のcommit hash
* STEP4-19のpush結果
* 未コミット差分の有無
* 回答を保存した `doc/STEP4-19_codex.md`

---

# 背景

STEP4-18では以下を実装済み。

* `RenameHistoryRecord`
* `RenameHistoryManager`
* リネーム成功時だけ履歴追加
* リネーム失敗時は履歴追加しない
* 履歴最大30件
* 31件目追加時は最古削除
* `RenameMatchingUiState.canUndo`
* `RenameMatchingUiState.renameHistoryCount`
* `RenameHistoryManagerTest`
* `AppViewModelFactory` でアプリプロセス中だけ共有する `RenameHistoryManager` を保持

STEP4-18時点で、UIにはまだUNDOボタンを表示していない。

今回のSTEP4-19では、履歴基盤を使ってUNDOボタンを表示する。

---

# 実装方針

## UI表示場所

`RenameMatchingFragment` の下部エリアに `UNDO` ボタンを追加する。

現在、下部には以下の要素がある想定。

```text id="wqkolr"
- 選択中：<リネーム前ファイル名> -> <リネーム後ファイル名>
- 自動連番の「変更」ボタン
- メッセージ行
- LoadingView
```

ここに `UNDO` ボタンを追加する。

## 推奨配置

下部エリアを横並びまたは複数行に整理する。

推奨は以下。

```text id="unxcn8"
1行目:
[選択中：before -> after] [変更]

2行目:
[UNDO] [メッセージ / LoadingView]
```

ただし、既存レイアウトを大きく変えないこと。

既存の `bottomContentContainer` がある場合は、その中にUNDOボタンを追加する。

---

# UNDOボタン仕様

## 表示

ボタン文言:

```text id="unmbdh"
UNDO
```

## enabled / disabled

以下の状態にする。

```kotlin id="aby92x"
undoButton.isEnabled = state.canUndo
```

または、

```kotlin id="ovb781"
undoButton.isEnabled = state.renameHistoryCount > 0
```

## disabled条件

```text id="dv6dtc"
- renameHistoryCount == 0
- canUndo == false
- リネーム処理中
```

リネーム処理中を表す既存状態がある場合は、以下のようにする。

```kotlin id="vltg2j"
undoButton.isEnabled = state.canUndo && !state.isLoading
```

既存の状態名に合わせて調整すること。

## enabled条件

```text id="x88u26"
- renameHistoryCount >= 1
- canUndo == true
- リネーム処理中ではない
```

## 押下時

今回、逆リネームは行わない。

押下時は、ViewModelにイベントを渡して、最新履歴をログ出力するだけにする。

Fragment側で直接 `RenameHistoryManager` を触らないこと。

---

# ViewModel実装

`RenameMatchingViewModel` に以下のようなメソッドを追加する。

```kotlin id="ggpbw2"
fun onUndoClicked()
```

または既存命名規則に合わせて以下でもよい。

```kotlin id="d8wnx8"
fun requestUndo()
```

## onUndoClicked の処理

今回やること。

```text id="t4ax6t"
1. RenameHistoryManager.getLatest() を取得する
2. nullならログを出して何もしない
3. nullでなければ、最新履歴の beforeName / afterName / beforeUri / afterUri / renameMode / autoNumber をログ出力する
4. 実際のリネーム処理は実行しない
```

例:

```kotlin id="h9q4lz"
fun onUndoClicked() {
    val latest = renameHistoryManager.getLatest()

    if (latest == null) {
        Log.d(TAG_HISTORY, "undo clicked but history is empty")
        updateUndoState()
        return
    }

    Log.d(
        TAG_HISTORY,
        "undo clicked latest beforeName=${latest.beforeName} " +
            "afterName=${latest.afterName} " +
            "beforeUri=${latest.beforeUri} " +
            "afterUri=${latest.afterUri} " +
            "renameMode=${latest.renameMode} " +
            "autoNumber=${latest.autoNumber}"
    )

    // STEP4-19では逆リネームは実行しない
}
```

## UiState更新

履歴追加後、またはViewModel初期化時に、以下が正しく更新されるようにする。

```kotlin id="f1kbx2"
canUndo = renameHistoryManager.getLatest() != null
renameHistoryCount = renameHistoryManager.size()
```

既にSTEP4-18で追加済みなら、今回UIに反映するだけでよい。

---

# ログ仕様

タグはSTEP4-18と同じものを使う。

```kotlin id="ufkjm6"
private const val TAG_HISTORY = "EasyRenameHistory"
```

## ViewModel初期化時

既存ログがある場合は維持する。

```text id="b16dvu"
EasyRenameHistory: RenameMatchingViewModel initialized historySize=0 canUndo=false
```

## リネーム成功後

既存ログを維持する。

```text id="l6j0kp"
EasyRenameHistory: add success beforeName=... afterName=... historySize=...
```

## UNDOボタン押下時

今回追加するログ。

履歴あり:

```text id="mdddkc"
EasyRenameHistory: undo clicked latest beforeName=... afterName=... beforeUri=... afterUri=... renameMode=... autoNumber=...
```

履歴なし:

```text id="xuy1z2"
EasyRenameHistory: undo clicked but history is empty
```

UI状態更新ログを追加してもよい。

```text id="cfew04"
EasyRenameHistory: undo state updated canUndo=true historySize=1
```

---

# 下部Insets対応

STEP4-17で、3ボタンナビゲーション使用時に下部メッセージ行とLoadingViewが重なる問題を修正済み。

今回UNDOボタンを下部エリアに追加するため、以下を必ず確認すること。

```text id="pc90ob"
- bottomContentContainer の bottomMargin Insets 対応を壊さない
- UNDOボタン追加後も3ボタンナビゲーションで隠れない
- ジェスチャーナビゲーションでも不自然に隠れない
- LoadingView表示中もナビゲーションバーに重ならない
```

既存の `applyNavigationBarBottomMargin(...)` がある場合は再利用すること。

新しくroot padding方式に戻さないこと。

---

# 変更対象ファイル候補

実際の構成に合わせて確認すること。

想定される変更ファイル:

```text id="cx279t"
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt
```

必要に応じて変更:

```text id="y38960"
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryManager.kt
```

ドキュメント出力:

```text id="e0av1u"
doc/STEP4-19_codex.md
```

---

# 禁止事項

* STEP4-19では逆リネーム処理を実装しない
* `DocumentFile.renameTo()` をUNDO用途で呼ばない
* 履歴を削除しない
* 履歴を永続化しない
* SharedPreferencesやDBを使わない
* リネーム成功後の表示更新を壊さない
* 自動連番のカウンタ挙動を変えない
* CSV仕様を変えない
* RenameMode仕様を変えない
* singleUri / treeUri探索処理を今回変更しない
* CSVプレビューを今回実装しない
* Spinner横幅調整を今回実装しない
* 前回のセット再押下処理を今回実装しない
* ディレクトリ読み込み確認ログを今回実装しない
* 大規模リファクタをしない

---

# 動作確認方法

## 1. ビルド確認

以下を実行すること。

```powershell id="zlzv7a"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text id="mxqwgc"
BUILD SUCCESSFUL
```

## 2. 単体テスト

以下を実行すること。

```powershell id="bsw6dg"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text id="zt5ydp"
BUILD SUCCESSFUL
```

## 3. 実機確認

以下を確認する。

```text id="cvqhoi"
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
12. リネーム前ファイルがリネーム後表示になる既存挙動が維持されていることを確認する
13. もう1件リネームし、UNDO押下ログが最新履歴を指すことを確認する
14. リネーム処理中にUNDOボタンが押せない、または押しても処理が走らないことを確認する
15. 3ボタンナビゲーションでUNDOボタン・メッセージ行・LoadingViewが隠れないことを確認する
16. ジェスチャーナビゲーションでも下部エリアが不自然に隠れないことを確認する
```

---

# 実機で確認するべきログ

## 1. 端末確認

```powershell id="j82a6d"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

## 2. ログクリア

```powershell id="kqvwnu"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

## 3. 履歴ログ確認

```powershell id="tnshri"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 4. 既存ログ確認

```powershell id="jb7j28"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRename
```

## 5. クラッシュ確認

```powershell id="mispp6"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 6. 確認するログ観点

以下を確認する。

```text id="h9fmgt"
- 起動直後またはMatching画面初期表示時に historySize=0 canUndo=false になる
- 履歴0件のときにUNDOがdisabledになる
- リネーム成功時に add success が出る
- リネーム成功後に historySize=1 canUndo=true 相当になる
- UNDO押下時に undo clicked latest が出る
- undo clicked latest の beforeName / afterName / beforeUri / afterUri が空でない
- UNDO押下時に renameTo は実行されない
- UNDO押下時に履歴は削除されない
- AndroidRuntime のクラッシュが出ていない
```

---

# 追加で見るべき既存動作

今回のUI追加で既存動作を壊さないこと。

```text id="vm9f9h"
- リネーム前ファイルがリネームするとリネーム後になる挙動を維持
- 自動連番ONがデフォルトであることを維持
- 自動連番の「変更」ボタンが引き続き動く
- 指定番号でのプレビュー更新が引き続き動く
- 成功時のみ連番カウンタが進む
- 失敗時は連番カウンタが進まない
- 下部メッセージ行とLoadingViewがナビゲーションバーに隠れない
```

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何を追加したか
* UNDOボタンの表示位置
* enabled / disabled 条件
* 押下時にログだけ出すこと
* 逆リネームは未実装であること
* 既存の履歴追加処理をどう利用したか
* 下部Insets対応を壊していないこと

## 2. 変更ファイル一覧

例:

```text id="q19uft"
- RenameMatchingFragment.kt
- RenameMatchingViewModel.kt
- RenameMatchingUiState.kt
- doc/STEP4-19_codex.md
```

## 3. 動作確認方法

* ビルド方法
* 単体テスト方法
* 実機ログ確認方法
* 実機UI確認方法

## 4. 実機で見るべきログ

ユーザがそのままPowerShellで実行できる形で提示すること。

```powershell id="y4dtkq"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. Git操作結果

* STEP4-18のcommit hash
* STEP4-18のpush結果
* STEP4-19の作業ブランチ名
* STEP4-19の分岐元ブランチ
* STEP4-19のcommit message
* STEP4-19のcommit hash
* STEP4-19のpush先
* 未コミット差分の有無

## 6. 保存ファイル

Codexの回答は以下へ保存すること。

```text id="cksq1l"
doc/STEP4-19_codex.md
```

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text id="q0etq9"
- UNDOボタン押下時の逆リネーム実行
- UNDO成功後の履歴削除
- UNDO失敗時のエラー表示
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- CSVプレビュー
- CSVプレビューボタン
- CSV選択後のプレビューボタン有効化・色変更
- Spinner横幅調整
- リネームモード選択式ボタンの最長項目に合わせた幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲が選択ディレクトリ内に絞られているかの確認
- treeUri探索時間削減
- ディレクトリ読み込みがファイル本文を読んでいないことの確認ログ
- 前回のセット再押下で選択解除し、マッチング画面に進むボタンをdisableにする
- 自動連番開始番号ダイアログのインライン分解表示改善
- 画面下部リネーム履歴エリアのさらなるUI改善
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

# 未解決事項・リスクとして明記すること

Codexの最終回答では、以下を未解決事項として明記すること。

```text id="kc5jyf"
- STEP4-19ではUNDOボタンを追加するが、逆リネーム処理は未実装
- UNDO押下時は最新履歴ログを出すだけ
- 履歴はSTEP4-18の仕様どおりアプリ起動中のみ保持し、アプリキルで消える
- SAFではrename後にUriが変わるProviderがあるため、次STEPのUNDO実行では afterUri を使う必要がある
- UNDO実行時には FileAlreadyExists や afterUri 無効化への対策が必要
- 下部エリアにUNDOボタンを追加したため、画面幅が狭い端末ではレイアウト調整が必要になる可能性がある
```

---

# 完了条件

このSTEP4-19の完了条件は以下。

```text id="kqrak1"
- STEP4-18がcommit / pushされている
- assembleDebug が成功する
- testDebugUnitTest が成功する
- マッチング画面にUNDOボタンが表示される
- 履歴0件ではUNDOボタンがdisabledになる
- リネーム成功後にUNDOボタンがenabledになる
- UNDO押下時に EasyRenameHistory ログで最新履歴が確認できる
- UNDO押下時に逆リネームは実行されない
- UNDO押下時に履歴は削除されない
- 既存のリネーム後表示更新が壊れていない
- 自動連番の既存挙動が壊れていない
- 下部エリアが3ボタンナビゲーションで隠れない
- 下部エリアがジェスチャーナビゲーションで不自然に隠れない
- 実機確認後にSTEP4-19がcommit / pushされている
- Codex回答が doc/STEP4-19_codex.md に保存されている
```

```

**ベスト案**  
STEP4-19は、UNDOの逆リネームまで入れずに、**UNDOボタンUI追加＋最新履歴ログ出力**で止めるのが安全です。STEP4-18の履歴基盤がまだcommit/push未完了なので、最初にそこを確定させる指示も入れています。

**代替案**  
STEP4-19でUNDO実行まで進める案もありますが、SAFではリネーム後URIが変わる可能性があり、`FileAlreadyExists` や履歴削除タイミングも絡みます。UIボタンと実行処理を同時に入れると、バグ発生時の切り分けが難しくなります。

**注意点**  
今回のプロンプトでは、UNDOボタンを押しても `DocumentFile.renameTo()` は呼ばせません。ログで「どの履歴をUNDO対象にするか」だけ確認し、次STEPで逆リネーム処理を単機能として実装する流れにしています。
```
