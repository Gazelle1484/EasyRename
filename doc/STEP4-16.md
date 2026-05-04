STEP4-15では「前回セット記憶」と「マッチング画面へ進む時の最新読み込み」まで進んでおり、次候補として小さなUI改善が残っています。今回は **表示崩れ・選択状態・下部表示の改善** に絞ったSTEP4-16プロンプトにします。

````md
# STEP 4-16: 実装（機能単位）用 Codex 指示

STEP4-15では、前回使ったディレクトリとCSVの記憶、Home画面上部の前回セット表示、「マッチング画面へ進む」押下時の最新読み込みが実装されています。

このSTEPでは、次の段階として **表示・選択状態・下部UIの改善** のみを対象にしてください。

今回の目的は、既存のリネーム処理、自動連番、前回セット記憶、最新読み込みを壊さずに、実機で気になる表示上の問題を小さく修正することです。
回答はSTEP4-16_codex.mdに保存すること。

---

## 目的

- 1機能ずつ確実に完成させる
- 小文字ファイル名が大文字表示される問題を修正する
- 選択中のファイル・候補を色で分かりやすくする
- 「[選択中]」の文字表記を削除する
- 画面下部の表示が3ボタンナビゲーションに隠れないようにする
- 選択中のリネーム予定名を画面下部に表示する
- 既存のリネーム処理・自動連番・前回セット記憶を壊さない
- ビルドが通る状態を維持する

---

## Gitブランチ運用

このSTEPの作業は必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell
git status
git checkout main
git pull
git checkout -b feature/step4-16-matching-ui-display
````

### 注意

STEP4-16はSTEP4-15の実装を前提にします。
`main` にSTEP4-15までの変更が入っていない場合は、作業に必要な最新ブランチを確認し、どのブランチから分岐するべきかを報告してください。

必要であれば、以下のようにSTEP4-15ブランチから分岐してください。

```powershell
git checkout feature/step4-15-last-used-set
git pull
git checkout -b feature/step4-16-matching-ui-display
```

### commit / push のタイミング

このSTEPでは、**ビルド確認後、実機確認手順を提示し、実機確認OKを確認してから commit / push** してください。

実機確認OK後に以下を実行してください。

```powershell
git status
git add .
git commit -m "Improve matching screen display states"
git push -u origin feature/step4-16-matching-ui-display
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
- ホーム画面・マッチング画面で、小文字のファイル名が大文字になっている問題を修正する
- 小文字は小文字、大文字は大文字で表示する
- マッチング画面で選択中のカードの色をハイライトする
- 「[選択中]」の表記は不要なので削除する
- 画面下のリネーム履歴・結果表示部分が、3ボタンナビゲーションで隠れないようにする
- ジェスチャーナビゲーションと3ボタンナビゲーションの両方に対応する
- リネーム前ファイルとリネーム候補が両方選択されたら、画面下部に
  「選択中：＜リネーム前ファイル名＞ -> ＜リネーム後ファイル名＞」
  を表示する
- リネーム前ファイルがリネーム後にリネーム済み表示になる既存挙動は維持する
```

---

## 今回やらないこと

以下は重要ですが、STEP4-16では実装しないでください。後続STEPへ回してください。

```text
- 自動連番デフォルトON
- 自動連番開始番号指定
- 自動連番開始番号変更ダイアログ
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
- 独自ファイルピッカー
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text
app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt
```

必要に応じて以下も変更して構いません。

```text
app/src/main/java/com/example/easyrename/ui/common/LoadingView.kt
app/src/main/java/com/example/easyrename/util/
app/src/main/res/values/colors.xml
```

---

## 禁止事項

* リネーム処理の仕様を変更しない
* 自動連番の仕様を変更しない
* Prefix / Suffix / Replace の仕様を変更しない
* 既存CSV形式 `A1-1_*` の挙動を変えない
* 前回セット記憶の処理を壊さない
* 「マッチング画面へ進む」押下時の最新読み込みを壊さない
* `treeUri fallback` を削除しない
* `FileAlreadyExists` の検出を削除しない
* リネーム後の成功1件更新方式を全件再読み込みへ戻さない
* 大規模なUI刷新をしない
* DIライブラリを追加しない

---

# STEP 4-16-1: 小文字ファイル名が大文字表示される原因を修正する

## 目的

ファイル名の表示で、元の大文字・小文字を維持する。

## 対象

* `HomeFragment`
* `RenameMatchingFragment`
* 必要に応じて表示用ヘルパー

## 実装内容

ファイル名・候補名を表示している箇所を確認し、以下を修正してください。

```text
- displayName.uppercase() のような変換があれば削除する
- textAllCaps 相当の設定があれば無効化する
- Button表示で自動的に全大文字化されている場合は allCaps = false にする
- TextViewで表示できる箇所はTextViewを使う
- 表示用文字列は RenameTargetFile.displayName / RenameCandidate.displayName をそのまま使う
```

## 注意

* ソート用に lowercase() を使うのは維持してよい
* ただし表示文字列には lowercase() / uppercase() を適用しない
* ファイル本体名やリネーム結果名を変更しない
* 表示だけを修正する

---

# STEP 4-16-2: 選択中カードの色ハイライトを追加する

## 目的

選択状態を文字ではなく色で分かるようにする。

## 対象

* `RenameMatchingFragment`

## 実装内容

マッチング画面の以下に対して、選択中状態の背景色または枠線色を変えてください。

```text
- 左側: リネーム対象ファイル
- 右側: リネーム候補
```

## 表示方針

```text
未選択:
- 通常背景

選択中:
- primaryContainer相当または淡い強調色

リネーム済み:
- 選択中とは別の控えめな色または表示

使用済み候補:
- 選択中とは別の控えめな色または無効風表示
```

## 注意

* 色はハードコードしすぎない
* 既存テーマ色を使えるなら使う
* 難しければ最小限の背景色指定でよい
* 選択中が一目で分かればよい
* UIの大規模刷新はしない

---

# STEP 4-16-3: 「[選択中]」表記を削除する

## 目的

選択状態を色で示すため、文字による「[選択中]」表示をなくす。

## 対象

* `RenameMatchingFragment`

## 実装内容

現在、選択中のファイルまたは候補に以下のような表記を付けている場合は削除してください。

```text
[選択中]
```

## 注意

* 「リネーム済み」「使用済み」など、必要な状態表示は残してよい
* 選択状態は色で示す
* 表示から削除するだけで、ViewModelの選択状態は維持する

---

# STEP 4-16-4: 画面下部がナビゲーションバーに隠れないようにする

## 目的

画面下部のリネーム履歴・結果表示・選択中表示が、3ボタンナビゲーションやジェスチャーナビゲーションで隠れないようにする。

## 対象

* `RenameMatchingFragment`
* 必要に応じて `HomeFragment`

## 実装内容

Matching画面のルートViewまたは下部表示コンテナに、システムナビゲーションバー分のbottom paddingを追加してください。

候補:

```text
- ViewCompat.setOnApplyWindowInsetsListener
- WindowInsetsCompat.Type.navigationBars()
- 既存のpaddingにbottom insetを加算
```

## 方針

```text
- 3ボタンナビゲーションでも下部表示が隠れない
- ジェスチャーナビゲーションでも不自然な余白になりすぎない
- TopAppBar対応済みの上部余白を壊さない
- Home画面にも同様の問題がある場合は最小限で同じ対応を入れる
```

## 注意

* Edge-to-Edge全面対応は今回やらない
* 既存レイアウトを大きく作り直さない
* 下部表示だけが隠れないことを優先する

---

# STEP 4-16-5: 選択中のリネーム予定名を画面下部に表示する

## 目的

リネーム実行前に、どのファイルがどの名前になるか確認できるようにする。

## 対象

* `RenameMatchingUiState`
* `RenameMatchingViewModel`
* `RenameMatchingFragment`
* `ResolveRenameNameUseCase`

## 実装内容

リネーム前ファイルとリネーム候補が両方選択された場合、画面下部に以下を表示してください。

```text
選択中：＜リネーム前ファイル名＞ -> ＜リネーム後ファイル名＞
```

例:

```text
選択中：logs1223.txt -> A1-1-1_logs1223.txt
```

## 要件

```text
- Prefix / Suffix / Replace を反映する
- 自動連番ON/OFFを反映する
- 自動連番ON時は次に使う予定番号を反映する
- まだ実行していないので、カウンタは進めない
- ファイルまたは候補の片方だけ選択されている場合は表示しない、または「選択中：未確定」とする
```

## 実装案

`RenameMatchingUiState` に以下を追加してもよいです。

```kotlin
val selectedPreviewText: String? = null
```

または、Fragment側でStateから算出してもよいです。

## 推奨

ViewModel側で、選択状態が変わるたびにプレビュー文字列を更新してください。
理由は、Fragmentにリネーム名生成ロジックを持たせないためです。

## 注意

* プレビュー生成時にファイルI/Oはしない
* リネーム実行はしない
* 自動連番カウンタは進めない
* 既存の結果表示・履歴表示を壊さない
* 表示位置は画面下部の既存リネーム履歴・結果表示部分を流用してよい

---

# STEP 4-16-6: 実行後も既存のリネーム済み表示を維持する

## 目的

リネーム前ファイルがリネーム後にリネーム済み表示になる既存挙動を壊さない。

## 対象

* `RenameMatchingViewModel`
* `RenameMatchingFragment`

## 確認内容

以下の既存挙動を維持してください。

```text
- リネーム成功後、対象ファイルの displayName がリネーム後名になる
- 対象ファイルが isRenamed = true になる
- 自動連番OFF時、使用候補が isUsed = true になる
- 自動連番ON時、候補は再利用可能なまま
- 失敗時、対象ファイルはリネーム済みにならない
- 失敗時、候補は使用済みにならない
```

---

## 実装ルール

* 今回は「表示改善」と「選択中プレビュー」だけに集中する
* 自動連番開始番号指定は実装しない
* UNDOは実装しない
* CSVプレビューは実装しない
* ファイル探索ロジックは変更しない
* リネーム処理は変更しない
* 既存の非同期リネーム処理を維持する
* 成功後1件更新方式を維持する
* Prefix / Suffix / Replaceの仕様を変更しない
* 自動連番仕様を変更しない
* `FileAlreadyExists` の保護を維持する
* ビルドが通る状態を維持する

---

## 後に回す機能メモ

以下は今回のSTEPでは実装しないでください。後続STEPの候補としてメモに残してください。

```text
- 自動連番デフォルトON
- 自動連番開始番号指定
- 自動連番開始番号変更ダイアログ
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
# STEP 4-16: 表示改善と選択中プレビュー Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### RenameMatchingUiState.kt

### RenameMatchingViewModel.kt

### RenameMatchingFragment.kt

### HomeFragment.kt

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
2. Home画面で小文字ファイル名・CSV名が大文字化されていないことを確認する
3. リネーム対象ディレクトリを選択する
4. CSVファイルを選択する
5. マッチング画面へ進む
6. リネーム前ファイル名が小文字・大文字を維持して表示されることを確認する
7. リネーム候補名が小文字・大文字を維持して表示されることを確認する
8. ファイルを1件選択する
9. 選択中ファイルのカード色が変わることを確認する
10. 候補を1件選択する
11. 選択中候補のカード色が変わることを確認する
12. 「[選択中]」表記が表示されないことを確認する
13. 両方選択後、画面下部に「選択中：<元ファイル名> -> <リネーム後名>」が表示されることを確認する
14. 自動連番ON時、プレビューに次番号が反映されることを確認する
15. プレビュー表示だけでは連番カウンタが進まないことを確認する
16. リネーム実行後、対象ファイルがリネーム後名になり、リネーム済み表示になることを確認する
17. 3ボタンナビゲーションで画面下部表示が隠れないことを確認する
18. ジェスチャーナビゲーションでも画面下部表示が不自然に隠れないことを確認する
19. Prefix / Suffix / Replaceで既存のリネーム結果が壊れていないことを確認する
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
- 小文字ファイル名が大文字化されていない
- 選択中カードが色で分かる
- 「[選択中]」表記が消えている
- 画面下部の選択中プレビューが表示される
- 3ボタンナビゲーションで下部表示が隠れない
- 既存のリネーム処理が成功する
- 自動連番ON時のプレビューが期待どおり
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
- selectedTargetFile.displayName
- selectedCandidate.displayName
- selectedPreviewText
- isAutoNumberingEnabled
- previewAutoNumber
- displayNamePreserveCase=true
- bottomInsetApplied=...
```

### 7. 確認するログ観点

```text
- プレビュー生成でファイルI/Oが発生していないこと
- プレビュー表示だけでautoNumberCountersが進んでいないこと
- リネーム実行時だけカウンタが進むこと
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

* 作業ブランチ `feature/step4-16-matching-ui-display` で作業している
* 実機確認後にcommitしてpushしている
* 小文字ファイル名が大文字化されない
* 選択中のファイルカードが色でハイライトされる
* 選択中の候補カードが色でハイライトされる
* 「[選択中]」表記が削除されている
* 両方選択時に「選択中：<元ファイル名> -> <リネーム後名>」が表示される
* 自動連番ON時、プレビューに次番号が反映される
* プレビュー表示だけでは連番カウンタが進まない
* 下部表示が3ボタンナビゲーションで隠れない
* ジェスチャーナビゲーションでも表示が破綻しない
* 既存のリネーム処理が壊れていない
* 自動連番機能が壊れていない
* 前回セット記憶と最新読み込みが壊れていない
* ビルドが通る

```
```
