````md id="step4-8-prompt"
# STEP 4-8: 実装（機能単位）用 Codex 指示

STEP4-7では、マッチング画面のUI改善として、主要ボタン色の改善、文字サイズ調整、TopAppBar被り修正、左右2分割、独立スクロール、戻るボタン追加が完了しています。実機確認では、Home画面、ディレクトリ選択、CSV選択、マッチング画面遷移、左右リスト表示、独立スクロール、選択、リネーム実行、戻る操作まで成功しています。:contentReference[oaicite:0]{index=0}

このSTEPでは、次の段階として **一覧ソートとリネームモード選択の追加** を対象にしてください。

自動連番機能は要望として重要ですが、リネームモードの仕様に依存するため、今回は実装せず後続STEPへ回してください。
回答はdoc/STEP4-8_codex.mdに保存すること。

---

## 目的

- 1機能ずつ確実に完成させる
- リネーム対象ファイル一覧とリネーム候補一覧を見つけやすくする
- ホーム画面でリネームモードを選択できるようにする
- 選択したリネームモードに応じて、リネーム後ファイル名の生成方法を変える
- 既存の1件リネーム成功フローを壊さない
- ビルドが通る状態を維持する

---

## Gitブランチ運用

現在 `main、develop` にpush済みのため、このSTEPの作業はdevelopから切った必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell id="git-start"

git pull
git checkout -b feature/step4-8-rename-mode-sort
````

### 作業後に実行

ビルドとテスト成功後、以下を実行してください。

```powershell id="git-finish"
git status
git add .
git commit -m "Add rename mode selection and list sorting"
git push -u origin feature/step4-8-rename-mode-sort
```

### 注意

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分を確認する
* 未コミット差分がある場合は、内容を報告してから作業する
* 作業後はcommitとpushまで行う
* pushに失敗した場合は、エラー内容を報告する

---

## 実機確認結果

```text id="device-result"
1. アプリ起動: OK
2. Home画面でボタンがTopAppBarに隠れていない: OK
3. 「マッチング画面へ進む」ボタンの色が分かりやすい: OK
4. 全体の文字サイズが以前より少し読みやすい: OK
5. リネーム対象ディレクトリ選択: OK
6. CSVファイル選択: OK
7. マッチング画面へ進む: OK
8. Matching画面上部がTopAppBarに隠れていない: OK
9. 左側に元ファイル一覧、右側に候補一覧が表示される: OK
10. 左右列が独立してスクロールできる: OK
11. 元ファイルを1件選択: OK
12. 候補を1件選択: OK
13. 「リネーム実行」ボタンが有効になり、色が分かりやすい: OK
14. リネーム実行成功: OK
15. 「戻る」ボタンでHome画面へ戻れる: OK
```

---

## 今回の追加要望

```text id="requirements"
- 可能であれば、ディレクトリ選択時に、変更日時が新しい順にソートし、ディレクトリがトップに来るようにしたい
- 可能であれば、CSV選択時に、変更日時が新しい順にソートし、CSVがトップに来るようにしたい
- またはCSV選択ではCSVのみが表示されるようにしたい
- マッチング画面では、リネーム対象ファイルとリネーム候補をそれぞれ辞書順にソートしたい
- ホーム画面にリネームモード選択欄を追加したい
- リネームモードは選択式リストにしたい
- モードはまず3つ作る
  1. 先頭に文字列を追加する
  2. 拡張子の直前、ファイル名の最後に文字列を追加する
  3. ファイル名を置き換える
- デフォルトは「先頭に文字列を追加する」モードにする
- 先頭モードの場合、CSVから読み込んだリネーム候補にすべて「*_」を付けて扱う
- 末尾モードも同様に、CSVから読み込んだリネーム候補にすべて「_*」を付けて扱う
- リネーム前ファイルが、リネーム後にリネーム済み表示になる既存挙動は維持したい
- 自動連番機能を将来的に追加したい
```

---

## 今回やること / やらないこと

### 今回やること

```text id="do-this-step"
1. マッチング画面のリストを辞書順にソートする
2. CSVファイルピッカーでCSV / text系を優先して選択しやすくする
3. Home画面にリネームモード選択欄を追加する
4. 選択されたリネームモードをViewModel状態に保持する
5. リネームモードに応じてリネーム名を生成する
6. 既存のリネーム成功後状態更新を維持する
```

### 今回やらないこと

```text id="not-this-step"
1. Android標準ファイルピッカー内の並び順制御
2. Android標準ファイルピッカー内でディレクトリをトップに固定する処理
3. 自動連番機能
4. 一括リネーム機能
5. RecyclerView化
6. XMLレイアウト化
7. Material Componentsへの本格移行
8. Edge-to-Edge / WindowInsetsの正式対応
```

### 理由

Android標準のSAFピッカーでは、アプリ側からファイルピッカー内の並び順やディレクトリ優先表示を細かく制御できない場合があります。
そのため、今回のSTEPではアプリ内部で表示しているリストのソートと、CSV MIME type指定の改善を優先してください。

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text id="scope-files"
app/src/main/java/com/example/easyrename/model/
app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt
app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt
app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt
```

必要に応じて以下も変更して構いません。

```text id="optional-files"
app/src/main/java/com/example/easyrename/model/RenameRule.kt
app/src/main/java/com/example/easyrename/model/RenameCandidate.kt
app/src/main/java/com/example/easyrename/data/csv/CsvRuleParser.kt
app/src/main/java/com/example/easyrename/domain/usecase/GenerateRenameCandidateUseCase.kt
```

---

## 禁止事項

* リネーム成功済みフローを壊さない
* SAF / Repository層を大きく変更しない
* CSV読み込み方式を大きく変更しない
* 一括リネームを追加しない
* 自動連番を今回実装しない
* RecyclerView化しない
* Composeへ移行しない
* DIライブラリを追加しない
* 大規模なUI刷新をしない
* XMLレイアウト化しない
* ファイルピッカー自体を自作しない

---

# STEP 4-8-1: マッチング画面のリストを辞書順にソートする

## 目的

マッチング画面で、元ファイルとリネーム候補を探しやすくする。

## 対象

* `HomeViewModel`
* `RenameMatchingViewModel`
* 必要に応じて `HomeUiState`
* 必要に応じて `RenameMatchingFragment`

## 実装内容

* リネーム対象ファイル一覧を `displayName` の辞書順でソートする
* リネーム候補一覧を `displayName` または実表示名の辞書順でソートする
* 大文字小文字の差で順番が不自然にならないよう、可能であれば `lowercase()` を使う
* ソートはUI表示前に行う
* リネーム後に `isRenamed` になったファイルの表示位置が大きく崩れないようにする

## 推奨

```kotlin id="sort-example"
sortedBy { it.displayName.lowercase() }
```

## 注意

* サブディレクトリ対応は追加しない
* 変更日時順ソートは今回のアプリ内マッチング一覧には入れない
* 既存の選択状態・リネーム済み状態を壊さない

---

# STEP 4-8-2: CSVファイル選択をCSV優先にする

## 目的

CSV選択時に、CSVファイルを選びやすくする。

## 対象

* `HomeFragment`

## 実装内容

* `ActivityResultContracts.OpenDocument()` のMIME type指定を見直す
* 可能であればCSV / text系を指定する
* 端末によって `text/csv` が効かない場合もあるため、fallbackを考慮する

## 推奨候補

```kotlin id="mime-types"
arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/vnd.ms-excel"
)
```

現在の実装で単一MIME typeのみ対応している場合は、可能な範囲でCSVが出やすい設定にしてください。

## 注意

* SAFピッカー内の並び順制御は今回の対象外
* CSV以外を完全に非表示にできない端末があるため、その場合は仕様上の制約として報告する
* CSV読み込み自体の仕様は変更しない

---

# STEP 4-8-3: RenameModeモデルを追加する

## 目的

リネーム方式を型として管理する。

## 対象

* `model` 配下に新規ファイル追加
* 必要に応じて `HomeUiState`

## 実装内容

`RenameMode` を追加してください。

```kotlin id="rename-mode"
enum class RenameMode {
    Prefix,
    Suffix,
    Replace,
}
```

ユーザー表示名が必要な場合は、以下のようなプロパティまたは関数を持たせてもよいです。

```kotlin id="rename-mode-label"
fun displayName(): String
```

表示名例:

```text id="rename-mode-labels"
Prefix: 先頭に追加
Suffix: 末尾に追加
Replace: 置き換え
```

## 注意

* 自動連番はRenameModeに入れない
* 自動連番は後続STEPで別状態として扱う

---

# STEP 4-8-4: Home画面にリネームモード選択欄を追加する

## 目的

ユーザーがリネーム方式を選べるようにする。

## 対象

* `HomeFragment`
* `HomeViewModel`
* `HomeUiState`

## 実装内容

* Home画面にリネームモード選択欄を追加する
* 選択式リストで実装する
* プログラムmatic View方針を維持する
* Spinnerなど、最小実装でよい
* デフォルトは `RenameMode.Prefix`
* 選択変更時に `HomeViewModel.onRenameModeSelected(mode)` を呼ぶ
* `HomeUiState` に `renameMode` を保持する

## 表示例

```text id="mode-ui"
リネームモード:
[先頭に追加 ▼]
```

## 注意

* UIを大規模に作り直さない
* Material Components本格導入はしない
* モード選択はHome画面のみで行う

---

# STEP 4-8-5: Matching画面へRenameModeを渡す

## 目的

Home画面で選択したリネームモードを、Matching画面のリネーム実行に反映する。

## 対象

* `HomeViewModel`
* `AppViewModelFactory`
* `RenameMatchingViewModel`

## 実装内容

* Activityスコープの `HomeViewModel.uiState.renameMode` を `RenameMatchingViewModel` に渡す
* `RenameMatchingViewModel` は現在の `renameMode` を保持する
* Matching画面を開いた時点のモードを使う
* Homeに戻ってモード変更した場合、次にMatching画面を開いたときに反映されればよい

## 注意

* Bundleに大量データを詰めない方針を維持する
* 共有ViewModel方針を維持する
* DIライブラリは追加しない

---

# STEP 4-8-6: ResolveRenameNameUseCaseをRenameMode対応にする

## 目的

選択されたモードに応じてリネーム後ファイル名を生成する。

## 対象

* `ResolveRenameNameUseCase`
* `RenameMatchingViewModel`
* 必要に応じて `RenameCandidate`

## 現在仕様

これまでの仕様では、`*` には元ファイル名から拡張子を除いた名前を差し込み、拡張子は元ファイル側を維持します。

例:

```text id="current-rule"
元ファイル: logs1223.txt
CSV候補: A1-1_*
結果: A1-1_logs1223.txt
```

## 新仕様

CSVに書かれた値を `candidateText` として扱い、RenameModeで以下のように解決してください。

### Prefix: 先頭に追加

CSV候補を元ファイル名の先頭に追加する。

```text id="prefix-rule"
元ファイル: logs1223.txt
CSV候補: A1-1
結果: A1-1_logs1223.txt
```

内部的には、CSV候補を `candidateText + "_*"` と同等に扱ってよい。

CSV候補が既に `*` を含む場合は、既存のワイルドカード解釈を優先してもよい。

### Suffix: 末尾に追加

CSV候補を拡張子の直前、ファイル名本体の末尾に追加する。

```text id="suffix-rule"
元ファイル: logs1223.txt
CSV候補: A1-1
結果: logs1223_A1-1.txt
```

内部的には、CSV候補を `"*_" + candidateText` と同等に扱ってよい。

CSV候補が既に `*` を含む場合は、既存のワイルドカード解釈を優先してもよい。

### Replace: 置き換え

CSV候補でファイル名本体を置き換える。拡張子は元ファイル側を維持する。

```text id="replace-rule"
元ファイル: logs1223.txt
CSV候補: A1-1
結果: A1-1.txt
```

CSV候補に拡張子が含まれる場合の扱いは、今回は以下で固定してください。

```text id="replace-extension-rule"
CSV候補に拡張子が含まれていても、元ファイルの拡張子を維持する。
例:
元ファイル: logs1223.txt
CSV候補: A1-1.csv
結果: A1-1.txt
```

## 注意

* 既存CSV `A1-1_*` のように `*` を含む場合は、互換性のため既存ワイルドカード解釈を優先する
* CSV候補が `A1-1` のように `*` を含まない場合にRenameModeを適用する
* 拡張子は必ず元ファイル側を維持する
* Domain層にUI依存を入れない

---

# STEP 4-8-7: 既存CSVとの互換性を維持する

## 目的

これまで使っていた `A1-1_*` 形式のCSVが壊れないようにする。

## 対象

* `ResolveRenameNameUseCase`
* `CsvRuleParser`
* `GenerateRenameCandidateUseCase`

## 実装内容

以下のルールにしてください。

```text id="compat-rule"
1. CSV候補に * が含まれる場合:
   - RenameModeよりもCSV内の * を優先する
   - 既存どおり、* に元ファイル名本体を差し込む
   - 元ファイルの拡張子を維持する

2. CSV候補に * が含まれない場合:
   - RenameModeに応じて解決する
   - Prefix: 候補_元ファイル名本体.拡張子
   - Suffix: 元ファイル名本体_候補.拡張子
   - Replace: 候補.拡張子
```

---

## 実装ルール

* 今回は「ソート」と「リネームモード選択」だけに集中する
* 自動連番は実装しない
* UIの大幅な刷新をしない
* リネーム成功済みフローを壊さない
* 失敗時処理を大きく変更しない
* Repository / SAF層は原則触らない
* ビルドが通る状態を維持する
* 変更は小さく段階的に行う

---

## 後に回す機能メモ

以下は今回のSTEPでは実装しないでください。後続STEPの候補としてメモに残してください。

```text id="deferred-items"
- 自動連番機能
- 自動連番ON/OFFボタン
- 候補ごとの連番カウンタ管理
- 同じ候補を複数ファイルに使える自動連番モード
- リネーム成功後のファイル一覧再読み込み
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

## 出力させるもの

以下の形式で出力してください。

```md id="output-format"
# STEP 4-8: ソートとリネームモード追加 Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### RenameMode.kt

### HomeUiState.kt

### HomeViewModel.kt

### HomeFragment.kt

### AppViewModelFactory.kt

### RenameMatchingViewModel.kt

### ResolveRenameNameUseCase.kt

### RenameMatchingFragment.kt

### その他変更ファイル

## 変更ファイル一覧

## 変更理由

## 動作確認方法

## ビルド確認結果

## Git操作結果

## 実機ログ確認手順

## 後に回す機能メモ

## 未解決事項・リスク

## 次に進めるべきSTEP
```

---

## 動作確認方法として含めてほしい内容

```text id="manual-check"
1. アプリを起動する
2. Home画面にリネームモード選択欄が表示されることを確認する
3. デフォルトが「先頭に追加」になっていることを確認する
4. リネーム対象ディレクトリを選択する
5. CSVファイルを選択する
6. マッチング画面へ進む
7. リネーム対象ファイル一覧が辞書順になっていることを確認する
8. リネーム候補一覧が辞書順になっていることを確認する
9. Prefixモードで logs1223.txt + A1-1 が A1-1_logs1223.txt になることを確認する
10. Suffixモードで logs1223.txt + A1-1 が logs1223_A1-1.txt になることを確認する
11. Replaceモードで logs1223.txt + A1-1 が A1-1.txt になることを確認する
12. 既存CSV形式 A1-1_* でも従来どおり A1-1_logs1223.txt になることを確認する
13. リネーム成功後、元ファイルがリネーム済み表示になることを確認する
14. 使用済み候補が使用済み表示になることを確認する
15. Homeに戻ってモードを変更し、再度Matching画面に入ったときにモードが反映されることを確認する
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

今回の主目的はソートとリネーム名生成ルールの追加です。
リネーム名が期待と違う場合のみ、以下のログを確認してください。

### 1. 端末確認

```powershell id="adb-devices"
adb devices
```

### 2. ログクリア

```powershell id="adb-clear"
adb logcat -c
```

### 3. EasyRenameログ確認

```powershell id="adb-log"
adb logcat | findstr EasyRename
```

### 4. 追加でログに出すとよい内容

Codexは、必要に応じて以下を `EasyRename` タグでログ出力できるようにしてください。

```text id="log-items"
- selectedRenameMode
- sourceFile.displayName
- candidate.rawPattern
- candidate.displayName
- resolvedNewName
- RenameMode適用前後の文字列
```

### 5. クラッシュ確認

```powershell id="adb-crash-log"
adb logcat | findstr "AndroidRuntime EasyRename Exception"
```

---

## Git操作結果として報告してほしい内容

```text id="git-report"
- 作業開始時のgit status
- 作成したブランチ名
- commit hash
- push先ブランチ
- 未コミット差分の有無
```

---

## 完了条件

* 作業ブランチ `feature/step4-8-rename-mode-sort` で作業している
* 作業後にcommitしてpushしている
* Home画面にリネームモード選択欄がある
* デフォルトがPrefixモードになっている
* Matching画面の元ファイル一覧が辞書順に表示される
* Matching画面の候補一覧が辞書順に表示される
* Prefix / Suffix / Replace の3モードでリネーム名が変わる
* `A1-1_*` の既存CSV形式は従来どおり動く
* リネーム成功後、元ファイルがリネーム済み表示になる
* リネーム成功後、候補が使用済み表示になる
* 既存の1件リネーム成功フローが壊れていない
* ビルドが通る

```
```
