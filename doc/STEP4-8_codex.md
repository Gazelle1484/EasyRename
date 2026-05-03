# STEP 4-8: ソートとリネームモード追加 Codex回答

## 作業ブランチ

`feature/step4-8-rename-mode-sort`

作業開始時は `develop...origin/develop` で、未コミット差分として `doc/STEP4-8.md` の追加があった。内容は今回のSTEP4-8指示ファイルで、コード差分はなかったため保持したまま作業ブランチを作成した。

## 実装内容

- `RenameMode` を追加し、`Prefix` / `Suffix` / `Replace` の3モードを定義した。
- Home画面にリネームモード選択用の `Spinner` を追加した。
- `HomeUiState` に `renameMode` を追加し、デフォルトを `RenameMode.Prefix` にした。
- `HomeViewModel` に `onRenameModeSelected(mode)` を追加した。
- ディレクトリ読み込み後の対象ファイル一覧を `displayName.lowercase()` で辞書順ソートした。
- CSV読み込み後のリネーム候補一覧を `displayName.lowercase()` で辞書順ソートした。
- CSVピッカーのMIME type指定をCSV / text系優先にした。
- `AppViewModelFactory` でHome画面の `renameMode` を `RenameMatchingViewModel` に渡すようにした。
- `RenameMatchingViewModel` でMatching画面オープン時点の `renameMode` を保持し、リネーム実行時に `ResolveRenameNameUseCase` へ渡すようにした。
- `ResolveRenameNameUseCase` をRenameMode対応にし、`*` を含む既存CSVは従来ルールを優先するようにした。
- リネーム名確認用に `EasyRename` ログへ `selectedRenameMode`、元ファイル名、候補、解決後ファイル名を出すようにした。

## 実装コード

### RenameMode.kt

新規追加。

```kotlin
enum class RenameMode(
    val displayName: String,
) {
    Prefix("先頭に追加"),
    Suffix("末尾に追加"),
    Replace("置き換え"),
}
```

UI表示名をModel側に持たせた。現時点では表示文言が少なく、専用Formatterを増やすよりも単純で読みやすいためである。

### HomeUiState.kt

`renameMode` を追加した。

```kotlin
val renameMode: RenameMode = RenameMode.Prefix
```

Home画面で選択したモードを、画面状態として一元管理する。

### HomeViewModel.kt

対象ファイルと候補を読み込み後に辞書順でソートした。

```kotlin
val sortedFiles = files.sortedBy { it.displayName.lowercase() }
val sortedCandidates = candidates.sortedBy { it.displayName.lowercase() }
```

また、モード選択用に以下を追加した。

```kotlin
fun onRenameModeSelected(mode: RenameMode)
```

### HomeFragment.kt

Home画面に `Spinner` を追加した。

```kotlin
renameModeSpinner = Spinner(context).apply {
    adapter = ArrayAdapter(
        context,
        android.R.layout.simple_spinner_dropdown_item,
        RenameMode.entries.map { it.displayName },
    )
}
```

選択変更時は `HomeViewModel.onRenameModeSelected()` を呼ぶ。

CSVピッカーは以下のMIME typeを指定した。

```kotlin
arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/vnd.ms-excel",
)
```

Android標準SAFピッカー内の並び順制御やCSV完全限定はProvider依存があるため、今回の実装対象外とした。

### AppViewModelFactory.kt

`HomeViewModel.uiState.renameMode` を `RenameMatchingViewModel` へ渡すようにした。

```kotlin
renameMode = homeState?.renameMode ?: RenameMode.Prefix
```

HomeからMatchingへ遷移した時点のモードを使う構成である。

### RenameMatchingViewModel.kt

初期ファイル一覧・候補一覧を辞書順にしてUI Stateへ入れるようにした。

```kotlin
targetFiles = initialTargetFiles.sortedBy { it.displayName.lowercase() }
renameCandidates = initialRenameCandidates.sortedBy { it.displayName.lowercase() }
```

リネーム実行時は `renameMode` を `ResolveRenameNameUseCase` に渡す。

```kotlin
val resolvedNewName = resolveRenameNameUseCase(selectedFile, selectedCandidate, renameMode)
```

成功後の `isRenamed` / `isUsed` 更新は既存のmap処理を維持しており、表示順は大きく崩れない。

### ResolveRenameNameUseCase.kt

`*` を含む候補は従来互換のため、RenameModeよりもCSV内のワイルドカード解釈を優先した。

```kotlin
if (!candidate.rawPattern.contains('*')) {
    val resolvedBaseName = when (renameMode) {
        RenameMode.Prefix -> "${candidate.rawPattern}_$baseName"
        RenameMode.Suffix -> "${baseName}_${candidate.rawPattern}"
        RenameMode.Replace -> removeExtension(candidate.rawPattern)
    }
    return resolvedBaseName + extension
}
```

仕様例:

- Prefix: `logs1223.txt` + `A1-1` -> `A1-1_logs1223.txt`
- Suffix: `logs1223.txt` + `A1-1` -> `logs1223_A1-1.txt`
- Replace: `logs1223.txt` + `A1-1` -> `A1-1.txt`
- 互換: `logs1223.txt` + `A1-1_*` -> `A1-1_logs1223.txt`

ReplaceではCSV候補に拡張子が含まれる場合も元ファイルの拡張子を維持する。

### RenameMatchingFragment.kt

変更なし。

表示対象の順序はViewModel側で整える方針にした。Fragmentにソート責務を持たせると、状態更新時や再描画時にUI層へドメイン寄りの判断が漏れるためである。

### その他変更ファイル

- `doc/STEP4-8.md`
- `doc/STEP4-8_codex.md`

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/model/RenameMode.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt`
- `doc/STEP4-8.md`
- `doc/STEP4-8_codex.md`

## 変更理由

## 全体アーキテクチャ

既存のMVVM + Repository構成を維持した。

```text
UI層
→ ViewModel層
→ UseCase層
→ Repository層
→ SAF DataSource層
```

今回の変更は「一覧の見つけやすさ」と「リネーム名生成ルール」の追加である。SAFで実ファイルを操作する処理やRepository層には触れず、Homeの状態、Matchingの実行前解決、Domainの名前解決だけに閉じ込めた。

## 概要

Home画面でリネームモードを選び、Matching画面を開いた時点のモードでリネーム後ファイル名を生成する。リスト表示はViewModelでソート済みの状態として保持し、UIは受け取った順序を描画するだけにした。

## 設計詳細

- `RenameMode`
  - 責務: リネーム方式を型として表す。
  - 理由: 文字列分岐を避け、Prefix / Suffix / Replace以外をコンパイル時に排除しやすくするため。

- `HomeUiState`
  - 責務: Home画面で選ばれたモードを保持する。
  - 理由: モードはユーザーの画面選択状態であり、Home画面からMatching画面へ渡す必要があるため。

- `ResolveRenameNameUseCase`
  - 責務: 元ファイル名、候補、モードからリネーム後ファイル名を決定する。
  - 理由: ファイル名生成ルールはUIではなくDomainの判断であり、将来テストや自動連番を追加しやすい。

## 採用理由・根拠

モードを `enum class` にしたのは、現時点で値が3つに固定されており、sealed classほどの拡張性は不要だからである。KISSとYAGNIを優先し、後続STEPで自動連番を追加する場合も、RenameModeに混ぜず別状態として扱える。

ソートはFragmentではなくViewModelで行った。UI層は「表示する」責務に寄せ、並び順という状態の決定はViewModel側へ置いた方が、再描画や成功後更新でも一貫しやすい。これはSRPに沿う。

RenameModeをMatching画面オープン時点のスナップショットとして渡したのは、Homeに戻ってから変更した値が次回Matchingに反映されればよい、という要件と一致するためである。リアルタイム同期にすると、Matching画面上でモードが見えないまま結果だけ変わるリスクがあり、今回のUI要件には過剰である。

## 代替案

- CSV読み込み時にモード別の候補文字列へ変換する
  - 有効な条件: 候補一覧に「実際の適用パターン」を表示したい場合。
  - 今回採用しない理由: 元ファイル名が決まるまで最終名は決まらず、候補表示と解決責務が混ざるため。

- BundleでRenameModeをMatchingFragmentへ渡す
  - 有効な条件: Fragment単体で完全に状態復元したい場合。
  - 今回採用しない理由: 既存がActivityスコープのHomeViewModel共有方針であり、データ受け渡しを増やす必要がないため。

## 動作確認方法

```text
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

## ビルド確認結果

以下を実行した。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果。

```text
BUILD SUCCESSFUL
```

以下も実行した。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果。

```text
BUILD SUCCESSFUL
```

## Git操作結果

- 作業開始時のgit status: `develop...origin/develop`、`doc/STEP4-8.md` の未コミット追加あり
- 作成したブランチ名: `feature/step4-8-rename-mode-sort`
- commit message: `Add rename mode selection and list sorting`
- push先ブランチ: `origin/feature/step4-8-rename-mode-sort`
- commit hash: コミット作成後に最終応答で報告
- 未コミット差分の有無: コミット後に確認

## 実機ログ確認手順

今回の主目的はソートとリネーム名生成ルールの追加である。リネーム名が期待と違う場合のみ、以下のログを確認する。

### 1. 端末確認

```powershell
adb devices
```

### 2. ログクリア

```powershell
adb logcat -c
```

### 3. EasyRenameログ確認

```powershell
adb logcat | findstr EasyRename
```

### 4. 確認する内容

```text
- selectedRenameMode
- sourceFile.displayName
- candidate.rawPattern
- candidate.displayName
- resolvedNewName
- renameTo result=true が維持されているか
```

### 5. クラッシュ確認

```powershell
adb logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 後に回す機能メモ

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

## 未解決事項・リスク

- Android標準SAFピッカー内の並び順やディレクトリ優先表示はアプリ側で制御していない。Providerや端末標準ファイルアプリの仕様に依存する。
- CSVのみを完全に表示することも端末によっては保証できないため、CSV / text系MIME typeを優先指定する対応に留めた。
- `RenameTargetFile` が `android.net.Uri` を持つため、現状のJUnit構成では `ResolveRenameNameUseCase` の純粋単体テストを追加しづらい。後続でテスト容易性を上げるなら、名前解決に使う文字列処理を小さな純Kotlin関数へ分ける案がある。
- Prefix / SuffixでCSV候補にドットを含む場合、その文字列は候補名の一部として扱われる。最終拡張子は元ファイル側を維持する。

## 次に進めるべきSTEP

次は実機でSTEP4-8のモード別リネーム結果を確認する。

優先確認は以下。

1. Prefix / Suffix / Replaceで期待どおりのファイル名になること。
2. `A1-1_*` 形式の既存CSVが従来どおり動くこと。
3. 対象ファイルと候補が辞書順に表示されること。
4. Homeに戻ってモードを変更し、再度Matchingに入ったときに反映されること。

実機確認後は、自動連番を別状態として追加するSTEPへ進むのがよい。
