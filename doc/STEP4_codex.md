
このSTEPでは、STEPで確定し3た空実装に基づいて、**コンパイル可能な実装コードを生成したいです。codexの回答をもとにSTEP4-2のプロンプトを生成してください。
## フォーマット　STEP 4-2: 実装（機能単位）
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

# STEP 4: 実装（機能単位） Codex回答

今回は、指示の「一度に全機能を実装しない」に従い、**STEP 4-1: ドメインロジック実装** のみを対象にしました。

Repository / SAF、ViewModel、UI、リネーム実行統合は、まだ `TODO("Not yet implemented")` が残っています。これは未対応ではなく、後続STEPで小さく実装するために意図的に残しています。

## 全体アーキテクチャ

採用している構成は、STEP2・STEP3の方針どおり **MVVM + Repository** です。

```text
UI層
↓
ViewModel層
↓
Domain / UseCase層
↓
Repository層
↓
SAF / ContentResolver
```

今回実装したのは、上記のうち **Domain / UseCase層の純粋ロジック** です。CSV文字列の解釈、候補生成、ワイルドカード解決、ファイル名検証はAndroid APIに依存しないため、SAF実装より先に固める価値があります。

この順序を選んだ理由は、後続のRepository / UI実装がこのドメイン仕様に依存するためです。先にSAFや画面から作る代替案もありますが、その場合 `*` の扱いや拡張子維持ルールが後から変わると、UI・ViewModel・Repositoryまで修正範囲が広がります。まずDomain層を固定することで、変更範囲を小さくできます。

## STEP 4-1: ドメインロジック実装

## 実装内容

以下を実装しました。

- `CsvRuleParser`
  - CSVテキストを1行1ルールとして読み込み
  - 空行を無視
  - 前後空白を除去
  - `*` を含む場合は `prefix` / `suffix` / `hasWildcard` を設定

- `GenerateRenameCandidateUseCase`
  - `RenameRule` から `RenameCandidate` を生成
  - 画面表示用の `displayName` にはCSV由来の `rawPattern` を設定
  - `isUsed` をルール側から引き継ぎ

- `ResolveRenameNameUseCase`
  - `*` を含む候補では、元ファイル名から拡張子を除いた名前を差し込み
  - 拡張子は元ファイルの拡張子を維持
  - `*` を含まない候補では、CSV文字列をそのままリネーム先名として扱う

- `FileNameValidator`
  - 空文字・空白のみを不正扱い
  - `.` と `..` を不正扱い
  - 制御文字と `\ / : * ? " < > |` を不正文字として扱う

- `ValidateRenameUseCase`
  - `RenamePair.resolvedNewName` を `FileNameValidator` で検証

## 変更ファイル一覧

```text
app/src/main/java/com/example/easyrename/data/csv/CsvRuleParser.kt
app/src/main/java/com/example/easyrename/domain/usecase/GenerateRenameCandidateUseCase.kt
app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt
app/src/main/java/com/example/easyrename/domain/usecase/ValidateRenameUseCase.kt
app/src/main/java/com/example/easyrename/domain/validator/FileNameValidator.kt
doc/STEP4_codex.md
```

ビルド確認時に、AGP 9.0では `org.jetbrains.kotlin.android` plugin が不要であることも確認しました。現在の構成ではKotlin compileは `assembleDebug` で成功しています。

## 変更理由

### CsvRuleParser

CSVを「1行1ルール」として扱う仕様なので、複雑なCSVライブラリは使っていません。今回の要件ではカンマ区切りの複数列や引用符処理が不要なため、KISSの観点から行単位のパースに留めています。

代替案としてRFC準拠のCSV parserを導入する方法もありますが、現時点では依存追加と仕様複雑化に対してメリットが小さいです。将来、複数列、引用符、エスケープ、文字コード自動判定が必要になった段階で再検討するのが妥当です。

### ResolveRenameNameUseCase

`*` には「元ファイル名から拡張子を除いた名前」を差し込み、拡張子は元ファイル側を維持する実装にしました。

例:

```text
元ファイル: sample.jpg
CSV候補: A1-1_*
結果: A1-1_sample.jpg
```

この設計にした理由は、CSV側が分類名や接頭辞・接尾辞を指定し、ファイル形式は元ファイルに従うという責務分離が明確になるためです。CSVに拡張子まで書かせる代替案もありますが、画像・動画・PDFなど混在時に拡張子の誤指定が起きやすくなります。

### FileNameValidator

AndroidのSAFではプロバイダ差がありますが、まず一般的に危険なファイル名をDomain層で弾く形にしました。Repository層で実際の失敗を受けるだけの代替案もありますが、その場合ユーザー操作後に初めて失敗するため、UI側で事前に実行可能状態を判定しづらくなります。

## 動作確認方法

今回のSTEP4-1はUI未実装のため、画面操作での確認対象ではありません。確認観点は以下です。

```text
1. CSVテキストを CsvRuleParser.parse に渡す
2. RenameRule の rawPattern / prefix / suffix / hasWildcard を確認する
3. GenerateRenameCandidateUseCase で RenameCandidate に変換する
4. ResolveRenameNameUseCase で候補名を解決する
5. ValidateRenameUseCase で不正ファイル名が false になることを確認する
```

代表例:

```text
CSV:
  A1-1_*
  FixedName.txt

元ファイル:
  sample.jpg

期待値:
  A1-1_*       -> A1-1_sample.jpg
  FixedName.txt -> FixedName.txt
```

## ビルド確認結果

以下を実行し、成功を確認しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

既存ユニットテストも確認しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 未解決事項・リスク

- Shift_JIS読み込みは今回未実装です。文字コード対応は `ContentResolver` でバイト列を読むRepository / SAF実装時に扱う方が自然なため、STEP 4-2で検討します。
- 同名ファイル存在チェックは未実装です。これはディレクトリ内ファイル確認が必要なため、Repository / SAF実装に含めます。
- `ValidateRenameUseCase` は現時点ではファイル名文字列の妥当性のみを検証します。同名チェックや権限エラーは後続STEPで統合します。
- `LoadDirectoryFilesUseCase`、`LoadRenameRulesFromCsvUseCase`、`ExecuteRenameUseCase` にはまだ `TODO("Not yet implemented")` が残っています。これらはSTEP 4-2以降の対象です。
- UI操作による「ディレクトリ選択 → CSV選択 → 1件リネーム」は、STEP 4-2からSTEP 4-5まで実装して初めて確認可能になります。

## 次に進めるべきSTEP

次は **STEP 4-2: Repository / SAF実装** に進むのが自然です。

この順序にする理由は、ViewModelやUIが実際に表示するファイル一覧・CSV内容・リネーム結果を、Repositoryが供給する必要があるためです。先にViewModelやUIを作る代替案もありますが、データ取得部分が未実装だと仮データに寄りやすく、今回の禁止事項である「仮ロジック」や「ハードコード」に近づきます。
