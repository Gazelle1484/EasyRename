実機検証結果とSTEP4-13回答をもとに、STEP4-14は **自動連番機能の設計・最小実装** に絞るのが安全です。STEP4-13では非同期化と実行中UI改善が完了し、次候補として自動連番が残っています。

````md
# STEP 4-14: 実装（機能単位）用 Codex 指示

STEP4-13では、リネーム処理の非同期化、リネーム中表示、二重押下防止が実装されています。  
このSTEPでは、次の機能追加として **自動連番モード** のみを対象に実装してください。

今回の目的は、既存の Prefix / Suffix / Replace モードを壊さずに、必要な場合だけリネーム候補へ連番を付与できるようにすることです。
回答はSTEP4-14_codex.mdに保存すること。
---

## 目的

- 1機能ずつ確実に完成させる
- 自動連番モードを追加する
- 同じリネーム候補を複数ファイルへ連続適用できるようにする
- Prefix / Suffix / Replace の既存挙動を維持する
- 既存CSV形式 `A1-1_*` の互換動作を維持する
- リネーム成功後の1件更新方式を維持する
- 非同期リネーム処理と実行中UIを壊さない
- ビルドが通る状態を維持する

---

## Gitブランチ運用

機能追加に合わせて、このSTEPの作業は必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell
git status
git checkout main
git pull
git checkout -b feature/step4-14-auto-numbering
````

### 注意

STEP4-14はSTEP4-13の実装を前提にします。
`main` にSTEP4-13までの変更が入っていない場合は、作業に必要な最新ブランチを確認し、どのブランチから分岐するべきかを報告してください。

必要であれば、以下のようにSTEP4-13ブランチから分岐してください。
もしくは、STEP4-13ブランチをdevelopにマージし、新しくブランチを切ってください。
```powershell
git checkout feature/step4-13-rename-async-loading
git pull
git checkout -b feature/step4-14-auto-numbering
```

### 作業後に実行

ビルドとテスト成功後、以下を実行してください。

```powershell
git status
git add .
git commit -m "Add auto numbering rename mode"
git push -u origin feature/step4-14-auto-numbering
```

### Git注意事項

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分を確認する
* 未コミット差分がある場合は、内容を報告してから作業する
* 作業後はcommitとpushまで行う
* pushに失敗した場合は、エラー内容を報告する

---

## 前提

既に以下は実装済みです。

```text
- Prefix / Suffix / Replace のリネームモード
- Home画面でのリネームモード選択
- Matching画面の左右2分割
- リネーム処理の非同期化
- リネーム中表示
- 二重押下防止
- 成功後1件更新方式
- treeUri fallback による安定リネーム
```

---

## 今回の追加要望

```text
自動連番機能を追加したい。

画面上部に自動連番ボタンを追加する。
自動連番ボタンを押下すると自動連番モードになる。

自動連番モードでリネーム候補とリネーム前ファイルを押下すると、
リネーム候補の末尾に「-1」のようにつけて扱う。

例:
logs1223.txt と logs1226.txt を、
それぞれ「A1-1」の先頭モードでこの順にリネームする場合:

A1-1-1_logs1223.txt
A1-1-2_logs1226.txt

末尾モードの場合:

logs1223_A1-1-1.txt
logs1226_A1-1-2.txt
```

---

## 今回やること / やらないこと

### 今回やること

```text
1. 自動連番ON/OFF状態を追加する
2. Matching画面上部に自動連番ボタンを追加する
3. 自動連番ON時、候補ごとの連番カウンタを管理する
4. リネーム成功時のみ連番カウンタを進める
5. Prefix / Suffix / Replace と自動連番を組み合わせる
6. 同じ候補を複数ファイルに使えるようにする
7. 既存の通常リネーム挙動を維持する
```

### 今回やらないこと

```text
1. 一括リネーム
2. 連番開始番号のユーザー指定
3. 連番桁数指定
4. 01 / 001 のようなゼロ埋め
5. 候補ごとの連番リセットUI
6. 自動連番履歴
7. Undo / 取り消し
8. RecyclerView化
9. XMLレイアウト化
10. Material Componentsへの本格移行
11. directory.listFiles() のキャッシュ化
12. Provider別fast pathスキップ
13. HomeViewModelの読み込み処理のDispatchers.IO対応
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt
app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt
```

必要に応じて以下も変更して構いません。

```text
app/src/main/java/com/example/easyrename/model/RenameCandidate.kt
app/src/main/java/com/example/easyrename/model/RenamePair.kt
app/src/main/java/com/example/easyrename/model/RenameMode.kt
```

---

## 禁止事項

* Prefix / Suffix / Replace の既存仕様を変更しない
* 既存CSV形式 `A1-1_*` の挙動を壊さない
* 通常モード時の候補使用済み処理を壊さない
* リネーム処理を同期処理へ戻さない
* treeUri fallback を削除しない
* FileAlreadyExists 保護を削除しない
* UnsupportedOperationException をクラッシュさせない
* 一括リネームを追加しない
* UIを大規模に作り直さない
* DIライブラリを追加しない

---

# STEP 4-14-1: 自動連番ON/OFF状態を追加する

## 目的

Matching画面上で、自動連番を使うかどうかを切り替えられるようにする。

## 対象

* `RenameMatchingUiState`
* `RenameMatchingViewModel`

## 実装内容

`RenameMatchingUiState` に以下を追加してください。

```kotlin
val isAutoNumberingEnabled: Boolean = false
```

`RenameMatchingViewModel` に以下のメソッドを追加してください。

```kotlin
fun toggleAutoNumbering()
```

動作:

```text
- false のとき押すと true
- true のとき押すと false
- リネーム実行中は切り替え不可
```

## 注意

* デフォルトはOFF
* Home画面ではなくMatching画面で切り替える
* 既存のRenameModeとは別状態として扱う
* RenameMode enum に自動連番を混ぜない

---

# STEP 4-14-2: Matching画面に自動連番ボタンを追加する

## 目的

ユーザーが自動連番ON/OFFを切り替えられるようにする。

## 対象

* `RenameMatchingFragment`

## 実装内容

Matching画面上部の操作エリアに、自動連番ボタンを追加してください。

表示例:

```text
[戻る] [自動連番: OFF] [リネーム実行]
```

ON時:

```text
[自動連番: ON]
```

## UI要件

```text
- OFF時とON時で見た目が分かるようにする
- ON時はprimaryまたはsecondary相当の色で強調する
- 実行中は自動連番ボタンをdisabledにする
- 既存の戻るボタン、リネーム実行ボタンを壊さない
```

## 注意

* UIの大規模刷新はしない
* 現在のプログラムmatic View構成を維持する
* 左右2分割レイアウトは維持する

---

# STEP 4-14-3: 候補ごとの連番カウンタを管理する

## 目的

同じリネーム候補を複数ファイルに使った場合、候補ごとに `-1`, `-2`, `-3` と連番を進める。

## 対象

* `RenameMatchingViewModel`

## 実装内容

ViewModel内に候補ごとの連番カウンタを持ってください。

例:

```kotlin
private val autoNumberCounters: MutableMap<String, Int> = mutableMapOf()
```

キーは以下のいずれかにしてください。

```text
ベスト案:
- candidate.ruleId または candidate.rawPattern

代替案:
- candidate.id
```

## 採用方針

* 同じCSV候補を複数ファイルに使う前提なので、候補の意味が同じものを同一カウンタとして扱う
* まずは `candidate.rawPattern` をキーにするのが分かりやすい
* 将来CSVの重複行を区別したい場合は `ruleId` へ変更する余地を残す

## カウンタ進行ルール

```text
- 自動連番OFF時: カウンタは使わない
- 自動連番ON時: リネーム実行時に候補の現在番号を使う
- リネーム成功時のみ、その候補の番号を+1する
- リネーム失敗時は番号を進めない
```

## 初期値

```text
最初に使う番号は 1
```

---

# STEP 4-14-4: 自動連番時のリネーム名生成ルールを追加する

## 目的

RenameModeと自動連番を組み合わせて、新しいリネーム名を生成する。

## 対象

* `ResolveRenameNameUseCase`
* `RenameMatchingViewModel`

## 実装方針

`ResolveRenameNameUseCase` に、自動連番用の番号を渡せるようにしてください。

例:

```kotlin
operator fun invoke(
    sourceFile: RenameTargetFile,
    candidate: RenameCandidate,
    renameMode: RenameMode,
    autoNumber: Int? = null,
): String
```

## ルール

### 自動連番OFF

既存どおり。

```text
Prefix:
logs1223.txt + A1-1
→ A1-1_logs1223.txt

Suffix:
logs1223.txt + A1-1
→ logs1223_A1-1.txt

Replace:
logs1223.txt + A1-1
→ A1-1.txt
```

### 自動連番ON

候補名に `-番号` を付けてから、RenameModeを適用する。

#### Prefix

```text
元ファイル:
logs1223.txt

候補:
A1-1

番号:
1

結果:
A1-1-1_logs1223.txt
```

2件目:

```text
logs1226.txt + A1-1 + 2
→ A1-1-2_logs1226.txt
```

#### Suffix

```text
logs1223.txt + A1-1 + 1
→ logs1223_A1-1-1.txt
```

2件目:

```text
logs1226.txt + A1-1 + 2
→ logs1226_A1-1-2.txt
```

#### Replace

```text
logs1223.txt + A1-1 + 1
→ A1-1-1.txt
```

2件目:

```text
logs1226.txt + A1-1 + 2
→ A1-1-2.txt
```

---

# STEP 4-14-5: `*` 付き既存CSVとの互換性を維持する

## 目的

既存CSV形式 `A1-1_*` が壊れないようにする。

## 対象

* `ResolveRenameNameUseCase`

## 方針

CSV候補に `*` が含まれる場合は、既存どおり `*` を元ファイル名本体に置き換える仕様を維持してください。

自動連番ON時に `*` 付き候補をどう扱うかは、以下で固定してください。

```text
CSV候補:
A1-1_*

自動連番ON
番号: 1
元ファイル: logs1223.txt

結果:
A1-1-1_logs1223.txt
```

## 実装ルール

`*` を含む場合、自動連番ONなら `*` の直前側にある候補部分へ `-番号` を付けてください。

例:

```text
A1-1_* + 1
→ A1-1-1_*
→ A1-1-1_logs1223.txt
```

より単純な実装として、`*` を含む候補では以下でもよいです。

```text
rawPatternの最初の * の直前に "-番号" を挿入する
```

例:

```text
A1-1_* → A1-1_-1* にならないよう注意
```

期待は以下です。

```text
A1-1_* → A1-1-1_*
```

## 注意

この仕様が複雑になりすぎる場合は、まず `*` なし候補だけ自動連番対応し、`*` あり候補は後続STEPに回す案を提示してください。
ただし、実装可能なら今回対応してください。

---

# STEP 4-14-6: 自動連番ON時は候補を使用済みにしない

## 目的

同じ候補を複数ファイルに使えるようにする。

## 対象

* `RenameMatchingViewModel.refreshAfterRename`

## 実装内容

現在はリネーム成功時に、使用した候補を `isUsed = true` にしているはずです。

自動連番ON時は、同じ候補を何度も使うため、以下の挙動にしてください。

```text
自動連番OFF:
- 成功時、候補を isUsed = true にする
- 既存挙動を維持

自動連番ON:
- 成功時、候補を isUsed = false のままにする
- 候補を再選択可能にする
- ただし選択状態は解除する
```

## 注意

* 元ファイル側は成功時に `isRenamed = true` にする
* 候補側だけ再利用可能にする
* 失敗時は候補を使用済みにしない
* 自動連番OFFの既存挙動は壊さない

---

# STEP 4-14-7: 自動連番の状態表示を追加する

## 目的

現在の自動連番状態と次の番号が分かるようにする。

## 対象

* `RenameMatchingUiState`
* `RenameMatchingFragment`
* `RenameMatchingViewModel`

## 実装内容

可能であれば、選択中候補に対して次に使う番号を表示してください。

表示例:

```text
自動連番: ON
選択中候補: A1-1
次の番号: 3
```

## 注意

* 実装が大きくなる場合は、今回はボタン表示だけでよい
* 後続STEPで詳細表示へ回してもよい
* ビルド成功と基本動作を優先する

---

## 実装ルール

* 今回は「自動連番ON/OFF」と「候補ごとの連番付与」だけに集中する
* 一括リネームは実装しない
* 連番開始番号指定は実装しない
* ゼロ埋めは実装しない
* 既存の非同期リネーム処理を維持する
* 既存の実行中UIを維持する
* 成功後1件更新方式を維持する
* Prefix / Suffix / Replaceの仕様を変更しない
* 既存CSV互換をできるだけ壊さない
* `FileAlreadyExists` の保護を維持する
* ビルドが通る状態を維持する
* 変更は小さく段階的に行う

---

## 後に回す機能メモ

以下は今回のSTEPでは実装しないでください。後続STEPの候補としてメモに残してください。

```text
- 一括リネーム
- 連番開始番号の指定
- 連番のゼロ埋め
- 連番リセットボタン
- 候補ごとの連番状態一覧
- 自動連番状態の永続化
- CSVプレビュー
- リネーム前後の差分プレビュー
- Undo / 取り消し
- 履歴機能
- HomeViewModelのディレクトリ読み込み / CSV読み込みのDispatchers.IO対応
- Provider別fast pathスキップ
- singleUri fast pathのON/OFF設定
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のキャッシュ設計
- 手動更新ボタン
- リネーム成功後の明示的な再読み込み
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- Edge-to-Edge / WindowInsets正式対応
- 画面回転時の完全な状態復元
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー
```

---

## 出力させるもの

以下の形式で出力してください。

```md
# STEP 4-14: 自動連番モード追加 Codex回答

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
4. Prefixモードでマッチング画面へ進む
5. 自動連番ボタンが表示されることを確認する
6. 自動連番がデフォルトOFFであることを確認する
7. 自動連番OFFで従来どおり1件リネームできることを確認する
8. 自動連番ONにする
9. logs1223.txt と A1-1 を選択する
10. リネーム結果が A1-1-1_logs1223.txt になることを確認する
11. 次に logs1226.txt と同じ A1-1 を選択する
12. リネーム結果が A1-1-2_logs1226.txt になることを確認する
13. Suffixモードで logs1223_A1-1-1.txt のようになることを確認する
14. Replaceモードで A1-1-1.txt のようになることを確認する
15. 自動連番ON時、同じ候補を複数回使えることを確認する
16. 自動連番OFF時、既存どおり候補が使用済みになることを確認する
17. リネーム中表示と二重押下防止が維持されていることを確認する
18. FileAlreadyExists失敗時に連番が進まないことを確認する
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

## 実機で確認するべきログ

今回の主目的は、自動連番の番号付与とカウンタ進行が正しいか確認することです。

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

必要最小限で、以下を `EasyRename` または `EasyRenamePerf` に出してください。

```text
- isAutoNumberingEnabled
- selectedRenameMode
- selectedCandidate.rawPattern
- autoNumber
- resolvedNewName
- counterBefore
- counterAfter
- rename success / failure
```

### 7. 確認するログ観点

```text
- 成功時だけ counterAfter が進んでいること
- 失敗時に counterAfter が進んでいないこと
- 同じ候補 A1-1 で -1, -2 と進むこと
- 別候補 A1-2 では -1 から始まること
- Prefix / Suffix / Replace で resolvedNewName が期待どおりであること
```

---

## Git操作結果として報告してほしい内容

```text
- 作業開始時のgit status
- 作成したブランチ名
- 分岐元ブランチ
- commit hash
- push先ブランチ
- 未コミット差分の有無
```

---

## 完了条件

* 作業ブランチ `feature/step4-14-auto-numbering` で作業している
* 作業後にcommitしてpushしている
* Matching画面に自動連番ON/OFFボタンがある
* 自動連番のデフォルトがOFFである
* 自動連番ON時、候補ごとに `-1`, `-2` と番号が進む
* 自動連番ON時、同じ候補を複数ファイルに使える
* 自動連番OFF時、既存どおり候補が使用済みになる
* Prefix / Suffix / Replace で自動連番が期待どおり反映される
* 失敗時に連番カウンタが進まない
* リネーム中表示と二重押下防止が維持されている
* `treeUri fallback` が維持されている
* `UnsupportedOperationException` でクラッシュしない
* `FileAlreadyExists` の保護が維持されている
* 成功後1件更新方式が維持されている
* 既存CSV形式 `A1-1_*` が壊れていない
* ビルドが通る

```
```
