````md id="step4-7-prompt"
# STEP 4-7: 実装（機能単位）用 Codex 指示

STEP4-6では、SAFリネーム失敗対策として、`treeUri` 配下から対象ファイルを再探索して `renameTo` する方式、`RenameErrorType`、詳細ログ、`UnsupportedOperationException` の安全処理、Home画面のTopAppBar隠れ対策が実装されています。実機検証では、リネーム実行まで成功し、`UnsupportedOperationException` は再発しませんでした。:contentReference[oaicite:0]{index=0}

このSTEPでは、次の段階として **マッチング画面のレイアウト改善と操作性改善のみ** を対象にしてください。
回答はdoc/STEP4-7_codex.mdに保存してください。

---

## 目的

- 1機能ずつ確実に完成させる
- 実機で確認されたマッチング画面のUI問題を修正する
- リネーム操作に必要なボタンの視認性を上げる
- マッチング画面を左右2分割にして、元ファイル一覧と候補一覧を比較しやすくする
- 画面上部がTopAppBarに隠れる問題を修正する
- ビルドが通る状態を維持する

---

## Gitブランチ運用

現在 `main` にpush済みのため、このSTEPの作業は必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell id="branch-start"
git status
git checkout main
git pull
git checkout -b feature/step4-7-matching-ui
````

### 注意

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分がないか確認する
* 既に未コミット差分がある場合は、作業前に内容を報告する
* ブランチ作成後にSTEP4-7の修正を行う

---

## 実機確認結果

```text id="device-result"
1. アプリ起動: OK
2. Home画面で一番上のボタンがTopAppBarに隠れていない: OK
3. リネーム対象ディレクトリ選択: OK
4. CSVファイル選択: OK
5. マッチング画面へ進む: OK
6. 元ファイル選択: OK
7. リネーム候補選択: OK
8. 実行ボタン押下: OK
9. 成功時にファイル名変更: OK
10. 失敗時にアプリが落ちず失敗理由が表示されるか: 未確認
11. UnsupportedOperationException: 発生しなかった
```

---

## 今回の追加要望

```text id="requirements"
- 「マッチング画面へ進む」ボタンをAndroidの推奨に沿って、色を分かりやすくする
- 全体的に文字サイズを2dp程度上げる
- 「リネーム実行」ボタンをAndroidの推奨に沿って、色を分かりやすくする
- マッチング画面の上部要素がTopAppBarに隠れているので修正する
- マッチング画面を左右2分割する
- 左側にリネーム前ファイル一覧を表示する
- 右側にリネーム候補一覧を表示する
- 左右それぞれの列は独立してスクロールできるようにする
- マッチング画面からホーム画面に戻るための「戻る」ボタンを追加する
- 「戻る」ボタンもAndroidの推奨に沿って、色を分かりやすくする
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text id="scope-files"
app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/ui/common/LoadingView.kt
app/src/main/java/com/example/easyrename/ui/common/ErrorDialog.kt
```

必要に応じて以下も変更して構いません。

```text id="optional-files"
app/src/main/java/com/example/easyrename/ui/MainActivity.kt
app/src/main/res/values/colors.xml
app/src/main/res/values/themes.xml
app/src/main/res/values/styles.xml
```

---

## 禁止事項

* リネーム処理の仕様変更
* SAF / Repository層の変更
* Domain層の `*` 解釈変更
* CSV仕様変更
* 一括リネーム機能追加
* 自動連番機能追加
* サブディレクトリ対応追加
* RecyclerView化
* Compose移行
* DIライブラリ追加
* 大規模な画面刷新
* 既存の成功済みリネームフローを壊す変更

---

# STEP 4-7-1: Home画面の主要ボタン色を改善する

## 目的

「マッチング画面へ進む」ボタンを、他の補助ボタンよりも目立たせる。

## 対象

* `HomeFragment`

## 実装内容

* 「マッチング画面へ進む」ボタンを主要アクションとして視認性の高い色にする
* Android / Materialの考え方に沿い、以下のような役割分けにする

    * 通常操作: 標準ボタン色
    * 主要操作: primary相当
    * 戻る・キャンセル系: secondaryまたはoutlined相当
* 現在プログラムmatic Viewで構築している場合は、その方針を維持して最小修正する

## 注意

* 色をハードコードしすぎない
* 可能であればテーマ色、`MaterialColors`、または既存theme属性を使う
* ビルド成功を優先する

---

# STEP 4-7-2: 全体文字サイズを少し上げる

## 目的

実機での視認性を改善する。

## 対象

* `HomeFragment`
* `RenameMatchingFragment`

## 実装内容

* 既存のTextView / Buttonの文字サイズを、おおむね2sp程度上げる
* ユーザー表現では「2dp程度」だが、文字サイズはAndroidでは `sp` を使う
* 既存が未指定の場合は、本文相当を16sp程度、見出しを18sp程度にする

## 注意

* すべてを巨大化しない
* 画面に収まらない場合はスクロールで吸収する
* 文字サイズ変更だけに留め、UI構造はこの段階では大きく変えない

---

# STEP 4-7-3: Matching画面のTopAppBar被りを修正する

## 目的

マッチング画面上部の要素がTopAppBarに隠れる問題を修正する。

## 対象

* `RenameMatchingFragment`

## 実装内容

* Home画面で行った対応と同様に、ActionBar / TopAppBar分の上部余白を追加する
* ルートViewまたはScrollViewに適切なpaddingを設定する
* 既存構造を大きく変えず、最小修正する

## 注意

* Home画面の余白修正ロジックと重複が大きい場合は、共通化してもよい
* ただし大規模なInsets設計変更はしない
* Edge-to-Edge対応の全面見直しは後回しにする

---

# STEP 4-7-4: Matching画面を左右2分割にする

## 目的

元ファイルとリネーム候補を左右で比較しながら選択できるようにする。

## 対象

* `RenameMatchingFragment`

## 実装内容

* 画面を左右2列に分割する
* 左列: リネーム前ファイル一覧
* 右列: リネーム候補一覧
* 左右それぞれに見出しを付ける
* 左右それぞれ独立してスクロールできるようにする

## 推奨構成

プログラムmatic Viewを維持する場合、以下のような構成にしてください。

```text id="matching-layout"
Root LinearLayout vertical
├── Header / 操作ボタンエリア
├── Content LinearLayout horizontal
│   ├── Left Column LinearLayout vertical
│   │   ├── 見出し: リネーム前ファイル
│   │   └── ScrollView
│   │       └── filesContainer
│   └── Right Column LinearLayout vertical
│       ├── 見出し: リネーム候補
│       └── ScrollView
│           └── candidatesContainer
└── Result / Error表示
```

## 注意

* RecyclerView化はしない
* 左右列は `layout_weight` で均等にする
* 横幅が狭い端末でも最低限見えるようにする
* 各列は独立してスクロールできること
* ボタンの選択状態表示は維持する

---

# STEP 4-7-5: リネーム実行ボタンの色を改善する

## 目的

リネーム実行ボタンを主要アクションとして分かりやすくする。

## 対象

* `RenameMatchingFragment`

## 実装内容

* 「リネーム実行」ボタンをprimary相当の色にする
* 無効時は無効状態が分かる見た目にする
* 有効/無効制御は既存の `canExecuteRename` を維持する

## 注意

* 実行ロジックは変更しない
* ボタン色と状態表示のみ修正する

---

# STEP 4-7-6: Matching画面にホームへ戻るボタンを追加する

## 目的

マッチング画面からHome画面へ戻れるようにする。

## 対象

* `RenameMatchingFragment`

## 実装内容

* 「戻る」ボタンを追加する
* 押下時にHome画面へ戻る
* Fragment back stackを使っている場合は `popBackStack()` を使う
* back stack未使用の場合は `HomeFragment` にreplaceする
* ボタン色はsecondaryまたはoutlined相当にする

## 注意

* Androidの戻るボタン動作と矛盾しないようにする
* 状態を破棄してよいかは現状仕様では未確定のため、まずはHomeへ戻れることを優先する
* 画面遷移の大規模設計変更はしない

---

# STEP 4-7-7: 失敗時表示の最低限確認を残す

## 背景

実機検証では成功ケースは確認済みですが、失敗時にアプリが落ちず、失敗理由が表示されるかは未確認です。

## 実装内容

今回は失敗処理の大きな修正は行わず、以下を確認しやすい状態にしてください。

* 同名ファイルエラー
* 不正ファイル名エラー
* 権限不足エラー
* UnsupportedOperationエラー

## 注意

* 失敗処理の再設計はしない
* 必要なら表示文言の微修正だけに留める

---

## 後に回す機能メモ

以下は今回のSTEPでは実装しないでください。後続STEPの候補としてメモに残してください。

```text id="deferred-items"
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- Edge-to-Edge / WindowInsetsの正式対応
- 画面回転時の完全な状態復元
- リネーム成功後のファイル一覧再読み込み
- リネーム履歴
- 取り消し機能
- 一括リネーム
- CSVプレビュー
- リネーム前後の差分プレビュー
- 同名時の自動連番
- 失敗時テストケースの自動化
```

---

## 実装ルール

* 今回はUI改善だけに集中する
* 成功済みのリネーム実行フローを壊さない
* 変更は小さく段階的に行う
* ビルドが通る状態を維持する
* UIの見た目改善は最小限に留める
* Android / Materialの標準的な色・状態表現に寄せる
* ハードコードが避けられる場合はテーマ色を使う

---

## 出力させるもの

以下の形式で出力してください。

```md id="output-format"
# STEP 4-7: マッチング画面UI改善 Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### HomeFragment.kt

### RenameMatchingFragment.kt

### MainActivity.kt

### その他変更ファイル

## 変更ファイル一覧

## 変更理由

## 動作確認方法

## ビルド確認結果

## 実機ログ確認手順

## 後に回す機能メモ

## 未解決事項・リスク

## 次に進めるべきSTEP
```

---

## 動作確認方法として含めてほしい内容

```text id="manual-check"
1. アプリを起動する
2. Home画面でボタンがTopAppBarに隠れていないことを確認する
3. 「マッチング画面へ進む」ボタンの色が分かりやすいことを確認する
4. 全体の文字サイズが以前より少し読みやすくなっていることを確認する
5. リネーム対象ディレクトリを選択する
6. CSVファイルを選択する
7. マッチング画面へ進む
8. マッチング画面上部がTopAppBarに隠れていないことを確認する
9. 左側に元ファイル一覧、右側にリネーム候補一覧が表示されることを確認する
10. 左右の列が独立してスクロールできることを確認する
11. 元ファイルを1件選択する
12. 候補を1件選択する
13. 「リネーム実行」ボタンが有効になり、色が分かりやすいことを確認する
14. リネームを実行し、成功することを確認する
15. マッチング画面の「戻る」ボタンでHome画面へ戻れることを確認する
```

---

## ビルド確認

以下を実行し、結果を報告してください。

```powershell id="build-check"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

可能であれば以下も実行してください。

```powershell id="test-check"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

---

## 実機で確認するべきログ

今回の主目的はUI改善のため、常時ログ確認は不要です。
ただし、リネーム実行や戻る操作でクラッシュ・失敗が出た場合は以下を確認してください。

### 1. 端末確認

```powershell id="adb-devices"
adb devices
```

### 2. ログクリア

```powershell id="adb-clear"
adb logcat -c
```

### 3. EasyRenameログ確認

```powershell id="adb-log-easyrename"
adb logcat | findstr EasyRename
```

### 4. クラッシュ確認

```powershell id="adb-log-crash"
adb logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 確認する内容

```text id="log-check-items"
- リネーム実行時に例外が出ていないか
- 戻るボタン押下時にFragment遷移エラーが出ていないか
- TopAppBar余白調整でレイアウト例外が出ていないか
- renameTo result=true が維持されているか
```

---

## 完了条件

* 作業ブランチ `feature/step4-7-matching-ui` で作業している
* Home画面の主要ボタン色が分かりやすくなっている
* 全体文字サイズが少し読みやすくなっている
* Matching画面上部がTopAppBarに隠れない
* Matching画面が左右2分割になっている
* 左右の一覧が独立してスクロールできる
* 「リネーム実行」ボタンの色が分かりやすくなっている
* Matching画面からHome画面へ戻るボタンがある
* 既存の1件リネーム成功フローが壊れていない
* ビルドが通る

```
```
