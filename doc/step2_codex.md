# STEP 2: 設計（構造決定） Codex回答

実装・ファイル変更は行わず、既存プロジェクト構成だけ確認しました。現状は `com.example.easyrename` 名前空間の初期 Android プロジェクトに近く、`minSdk = 33`、`targetSdk = 36`、`compileSdk = 36` です。Kotlin 前提で進める場合、後続 STEP では Kotlin / ViewModel / Activity Result API などの依存関係追加が必要になります。

## 全体アーキテクチャ

本アプリは **MVVM + Repository パターン** を採用します。

```text
UI層
↓
ViewModel層
↓
Domain / UseCase層
↓
Repository / Storage層
↓
Android Storage Access Framework
```

依存方向は上から下です。UI は ViewModel に依存し、ViewModel は UseCase に依存し、UseCase は Repository インターフェースまたは Repository 実装を通じてストレージ操作を呼び出します。

この構成を選ぶ理由は、今回のアプリが「画面状態管理」と「ファイル操作ルール」の両方を持つためです。Activity にすべてを書くと、ディレクトリ選択、CSV 読み込み、選択状態、リネーム検証、エラー表示が混ざり、保守しづらくなります。MVVM により、UI は表示と入力受付、ViewModel は状態管理、UseCase は業務ルール、Repository は Android の URI 操作に責務を分離できます。

## 概要

EasyRename は、ユーザーが選択したディレクトリ内のファイルを、CSV で定義された候補名に基づいて 1 件ずつ手動リネームする Android アプリです。

Android 33 以降のストレージ制約を考慮し、通常のファイルパス操作ではなく **Storage Access Framework の URI ベース操作**を前提にします。これにより、ユーザーが明示的に許可したディレクトリ・CSV ファイルだけを安全に扱えます。

## 推奨パッケージ構成

```text
com.example.easyrename
├── ui
│   ├── MainActivity
│   ├── home
│   │   ├── HomeFragment / HomeScreen
│   │   └── HomeUiState
│   ├── matching
│   │   ├── RenameMatchingFragment / RenameMatchingScreen
│   │   └── RenameMatchingUiState
│   └── common
│       ├── ErrorDialog
│       └── LoadingView
│
├── viewmodel
│   ├── HomeViewModel
│   └── RenameMatchingViewModel
│
├── domain
│   ├── usecase
│   │   ├── LoadDirectoryFilesUseCase
│   │   ├── LoadRenameRulesFromCsvUseCase
│   │   ├── GenerateRenameCandidateUseCase
│   │   ├── ResolveRenameNameUseCase
│   │   ├── ValidateRenameUseCase
│   │   └── ExecuteRenameUseCase
│   └── validator
│       └── FileNameValidator
│
├── data
│   ├── repository
│   │   ├── StorageRepository
│   │   └── StorageRepositoryImpl
│   ├── saf
│   │   └── SafDocumentDataSource
│   └── csv
│       └── CsvRuleParser
│
├── model
│   ├── RenameTargetFile
│   ├── RenameRule
│   ├── RenameCandidate
│   ├── RenamePair
│   ├── RenameResult
│   └── AppError
│
└── util
    ├── DateFormatUtil
    └── UriDisplayNameUtil
```

`Fragment / XML` ベースでも `Compose` ベースでも設計は成立します。既存依存関係は AppCompat / Material ベースなので、後続実装ではまず Fragment + ViewModel 構成にするのが自然です。Compose を採用する場合は依存関係追加が増えるため、初期実装の範囲ではやや重くなります。

## 各層の責務

### UI層

担当すること:

- ディレクトリ選択ボタンの表示
- CSV 選択ボタンの表示
- ファイル一覧・候補一覧の表示
- 選択状態の見た目の反映
- Snackbar / Dialog による結果表示
- Android のファイルピッカー起動

担当しないこと:

- CSV の解析
- `*` の解釈
- リネーム先名の検証
- 実ファイルのリネーム処理
- URI 操作の詳細

UI 層は「ユーザー操作を ViewModel に渡す」「ViewModel の状態を表示する」ことに集中します。

### ViewModel層

担当すること:

- ホーム画面の状態管理
- マッチング画面の状態管理
- 選択中ファイル、選択中候補の管理
- 決定ボタンの有効 / 無効判定
- UseCase の呼び出し
- 成功・失敗イベントの UI への通知

担当しないこと:

- SAF の API 呼び出し詳細
- CSV パース詳細
- ファイル名検証ルールの直接実装

ViewModel は UI と業務ロジックの仲介役です。画面回転や再描画があっても状態を維持しやすく、テストもしやすくなります。

### Domain / UseCase層

担当すること:

- ディレクトリ内ファイル取得のユースケース化
- CSV から RenameRule への変換指示
- RenameRule から RenameCandidate 生成
- `A1-1_*` の `*` に元ファイル名を差し込む処理
- 同名ファイル存在チェック
- 不正ファイル名チェック
- リネーム実行前の業務ルール検証

担当しないこと:

- Android UI 表示
- Snackbar / Dialog 表示
- Android の `ContentResolver` や `DocumentFile` の直接操作

Domain 層は、Android API への依存を薄く保つ方針です。これにより、`FileNameValidator` や `ResolveRenameNameUseCase` は JVM 単体テストで検証しやすくなります。

### Repository / Storage層

担当すること:

- Storage Access Framework によるディレクトリ読み込み
- URI から `DocumentFile` を扱う
- CSV ファイル URI から内容を読み込む
- ファイル名変更処理
- 永続 URI 権限の取得・保持方針
- ファイル存在確認

担当しないこと:

- 画面状態管理
- ユーザー選択状態管理
- `*` の業務的な解釈
- エラーメッセージ文言の最終表示判断

Android 33 ではストレージアクセスの制約が強いため、この層に Android API 依存を閉じ込めることが重要です。

## 設計詳細

### MainActivity

責務:

- アプリ全体のエントリーポイント
- Fragment または画面遷移コンテナの保持
- Activity Result API によるディレクトリ・CSV ピッカーの登録

主な連携:

- `HomeFragment / HomeScreen`
- `RenameMatchingFragment / RenameMatchingScreen`

補足:

- SAF のピッカー起動自体は Activity / Fragment が担当します。
- 取得した URI は ViewModel に渡します。

### HomeViewModel

責務:

- 選択中ディレクトリ情報の保持
- 選択中 CSV 情報の保持
- 読み込み済みファイル数・候補数の保持
- マッチング画面へ進めるかの判定

主な状態:

```text
HomeUiState
- selectedDirectoryName
- selectedCsvFileName
- targetFileCount
- renameCandidateCount
- isReadyToStartMatching
- isLoading
- error
```

主なメソッド:

```text
onDirectorySelected(uri)
onCsvSelected(uri)
loadDirectoryFiles(uri)
loadCsvRules(uri)
clearError()
```

### RenameMatchingViewModel

責務:

- 左側ファイル一覧の状態管理
- 右側候補一覧の状態管理
- 選択中ファイル・選択中候補の管理
- 決定ボタン有効化判定
- リネーム実行

主な状態:

```text
RenameMatchingUiState
- targetFiles
- renameCandidates
- selectedTargetFileId
- selectedCandidateId
- canExecuteRename
- isExecuting
- lastResult
- error
```

主なメソッド:

```text
selectTargetFile(fileId)
selectRenameCandidate(candidateId)
executeSelectedRename()
refreshAfterRename(result)
```

### StorageRepository

責務:

- ストレージ操作の抽象化

想定メソッド:

```text
loadFilesInDirectory(directoryUri): List<RenameTargetFile>
readTextFromUri(csvUri): String
renameFile(fileUri, newName): RenameResult
existsInSameDirectory(directoryUri, fileName): Boolean
takePersistablePermission(uri)
```

採用理由:

Repository インターフェースを置くことで、ViewModel / UseCase が `ContentResolver` や `DocumentFile` に直接依存しなくなります。将来的に SAF 以外の実装へ切り替える場合も影響範囲を限定できます。

### SafDocumentDataSource

責務:

- Android の SAF API との直接接続
- `DocumentFile.fromTreeUri`
- `DocumentFile.renameTo`
- URI 権限処理
- ファイル一覧取得

採用理由:

Repository 実装の中でも Android API に近い処理を DataSource に分けることで、Repository が肥大化しにくくなります。初期実装では RepositoryImpl にまとめても成立しますが、SAF 特有の処理は複雑化しやすいため分離する設計を推奨します。

### CsvRuleParser

責務:

- CSV テキストを `RenameRule` のリストへ変換
- 空行の除外
- 前後空白の扱い
- `*` の有無に基づく `prefix` / `suffix` 分解

注意点:

- 現時点では 1 行 1 ルール前提
- 本格的な CSV のカンマ区切りや引用符対応が必要なら、後で専用ライブラリまたは堅牢な parser に置き換える

### GenerateRenameCandidateUseCase

責務:

- `RenameRule` から画面表示用の `RenameCandidate` を生成
- 候補の使用済み状態を管理しやすい形にする

補足:

この段階では `RenameCandidate.displayName` は CSV の rawPattern を表示してもよいです。実際のリネーム名は元ファイル選択後に `ResolveRenameNameUseCase` で確定します。

### ResolveRenameNameUseCase

責務:

- 元ファイル名と RenameRule / RenameCandidate から実際のリネーム後ファイル名を生成

例:

```text
元ファイル: sample.jpg
ルール: A1-1_*
結果: A1-1_sample.jpg
```

現時点の前提:

- `*` には元ファイル名全体を差し込む
- 拡張子を除くかどうかは未固定なので、後続 STEP 前に仕様確定が必要

### ValidateRenameUseCase

責務:

- リネーム実行前の検証
- 空文字チェック
- 不正文字チェック
- 同名ファイル存在チェック
- 選択状態チェック

採用理由:

検証を ViewModel に書くと UI 状態と業務ルールが混ざります。UseCase に分離することで、リネーム仕様変更時の修正箇所を限定できます。

### ExecuteRenameUseCase

責務:

- `RenamePair` を受け取り、検証後に Repository 経由でリネーム実行
- 成功 / 失敗を `RenameResult` として返す

処理順:

```text
RenamePair作成
→ 新ファイル名解決
→ ファイル名検証
→ 同名存在確認
→ Repository.renameFile
→ RenameResult返却
```

## データ構造

### RenameTargetFile

```text
RenameTargetFile
- id: String
- displayName: String
- uri: Uri
- size: Long?
- lastModified: Long?
- isSelected: Boolean
- isRenamed: Boolean
```

画面左側のリスト表示に使います。`uri` は実ファイル操作に必要なので必須です。

### RenameRule

```text
RenameRule
- id: String
- rawPattern: String
- prefix: String
- suffix: String
- hasWildcard: Boolean
- isUsed: Boolean
```

CSV の 1 行を表すドメインモデルです。

### RenameCandidate

```text
RenameCandidate
- id: String
- ruleId: String
- displayName: String
- rawPattern: String
- isSelected: Boolean
- isUsed: Boolean
```

画面右側に表示する候補です。`RenameRule` と分ける理由は、CSV のルールそのものと、UI 上の選択状態・使用済み状態を分離するためです。

### RenamePair

```text
RenamePair
- sourceFile: RenameTargetFile
- renameCandidate: RenameCandidate
- resolvedNewName: String
```

実行直前に作られる一時的なデータです。

### RenameResult

```text
RenameResult
- beforeName: String
- afterName: String
- success: Boolean
- errorMessage: String?
```

UI はこの結果を受けて Snackbar / Dialog を表示します。

### AppError

追加を推奨します。

```text
AppError
- type: ErrorType
- message: String
- cause: Throwable?
```

想定する `ErrorType`:

```text
CsvReadFailed
DirectoryReadFailed
PermissionDenied
InvalidFileName
FileAlreadyExists
RenameFailed
Unknown
```

エラーを文字列だけで扱うと分岐が弱くなるため、型として整理しておく方が保守しやすいです。

## データフロー

### ディレクトリ読み込み

```text
ユーザーがホーム画面でディレクトリ選択
→ UI が SAF のディレクトリピッカーを起動
→ 選択された directoryUri を HomeViewModel に渡す
→ HomeViewModel が LoadDirectoryFilesUseCase を呼ぶ
→ UseCase が StorageRepository.loadFilesInDirectory を呼ぶ
→ Repository が SAF 経由で直下ファイル一覧を取得
→ RenameTargetFile リストを ViewModel に返す
→ HomeUiState に件数とディレクトリ名を反映
```

### CSV 読み込み

```text
ユーザーが CSV ファイル選択
→ UI が SAF のファイルピッカーを起動
→ 選択された csvUri を HomeViewModel に渡す
→ HomeViewModel が LoadRenameRulesFromCsvUseCase を呼ぶ
→ Repository が csvUri からテキストを読む
→ CsvRuleParser が RenameRule リストに変換
→ GenerateRenameCandidateUseCase が RenameCandidate リストを生成
→ HomeUiState に候補件数と CSV 名を反映
```

### マッチング

```text
マッチング画面を開く
→ RenameMatchingViewModel が targetFiles と renameCandidates を保持
→ ユーザーが左側の元ファイルを選択
→ selectedTargetFileId を更新
→ ユーザーが右側の候補を選択
→ selectedCandidateId を更新
→ 両方選択済みなら canExecuteRename = true
```

### リネーム実行

```text
ユーザーが決定ボタンを押す
→ RenameMatchingViewModel が RenamePair を作成
→ ResolveRenameNameUseCase が実ファイル名を生成
→ ValidateRenameUseCase が検証
→ ExecuteRenameUseCase が Repository.renameFile を呼ぶ
→ Repository が DocumentFile.renameTo を実行
→ RenameResult を ViewModel に返す
→ 成功時:
   - 元ファイルを isRenamed = true に更新
   - 候補を isUsed = true に更新
   - 選択状態を解除
→ UI が成功メッセージを表示
```

## 採用理由・根拠

MVVM を採用する理由は、画面状態の変化が多いからです。今回のアプリでは、単純な入力フォームではなく、左右リストの選択、決定ボタンの有効化、リネーム済み表示、エラー表示、読み込み中表示が発生します。これを Activity に直接書くと、UI と状態管理が密結合になります。

Repository パターンを採用する理由は、Android 33 のストレージ操作が URI ベースで複雑だからです。UI や UseCase が `DocumentFile` に直接依存すると、後から MediaStore や別の保存方式に変えにくくなります。

UseCase を分ける理由は、`*` の解釈、同名チェック、不正ファイル名チェックがアプリ固有の業務ルールだからです。これらは UI 表示とは独立してテストすべきです。

適用する設計原則:

- SRP: UI、状態管理、業務ルール、ストレージ操作を分離する
- DIP: 上位層が Android の具体 API に直接依存しないようにする
- KISS: Clean Architecture を厳密にしすぎず、必要な分離に留める
- YAGNI: サブディレクトリ対応や一括リネームは現時点では拡張余地だけ残し、初期実装には入れない

## 代替案

### 代替案 1: Activity に処理を集約する

小さな試作なら実装は速いです。

ただし、今回のアプリでは SAF、CSV、リネーム検証、左右リスト選択状態が混在します。Activity が肥大化し、後から仕様変更すると影響範囲が読みづらくなります。短期のプロトタイプなら有効ですが、段階的開発には不向きです。

### 代替案 2: 厳密な Clean Architecture

Repository Interface、DataSource、Entity、UseCase を完全に分離する構成です。

大規模開発や複数人開発では有効ですが、今回の初期アプリではクラス数が増えすぎる可能性があります。クラウド同期、履歴管理、一括プレビュー、複数 CSV フォーマット対応などが入る段階で、より厳密な Clean Architecture に寄せるのが現実的です。

## リスク・今後の検討点

- `*` に元ファイル名全体を入れるのか、拡張子を除いた名前を入れるのかは早めに固定が必要です。
- SAF の `DocumentFile.renameTo` は端末やプロバイダによって挙動差が出る可能性があります。
- CSV の文字コードは UTF-8 前提にするか、Shift_JIS 対応も入れるか検討が必要です。
- 同名ファイル存在時はエラー扱いでよいですが、将来は自動連番を求められる可能性があります。
- サブディレクトリ対応を後で追加する場合、`LoadDirectoryFilesUseCase` に再帰取得オプションを追加できる設計にしておくとよいです。
- 現在の `app/build.gradle.kts` には Kotlin Android plugin が見当たらないため、後続 STEP の実装前に Kotlin 対応設定を追加する必要があります。
