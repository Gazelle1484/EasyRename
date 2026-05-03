````md
# STEP 4-2: 実装（機能単位）用 Codex 指示

STEP4-1ではDomain層のロジック実装が完了しています。  
このSTEPでは、次の段階として **Repository / SAF（Storage Access Framework）周りのみを対象に実装**してください。

このSTEPでは他の層（ViewModel / UI / リネーム統合）は触らず、**データ取得・ファイル操作の実体を確定させること**に集中します。
回答はSTEP4-2_codex.mdに保存してください。

---

## 目的

- 1機能ずつ確実に完成させる
- SAFを使ったファイル操作を実装する
- Repository層の `TODO` を解消する
- 実データ（ファイル・CSV）を扱える状態にする

---

## 対象範囲（今回実装する範囲）

以下のクラスのみ実装してください。

### Data層

- `StorageRepositoryImpl`
- `SafDocumentDataSource`
- `UriDisplayNameUtil`
- `DateFormatUtil`

---

## 指示内容

### STEP 4-2-1: ディレクトリ内ファイル取得

#### 対象
- `SafDocumentDataSource.loadFilesInDirectory`
- `StorageRepositoryImpl.loadFilesInDirectory`

#### 実装内容

- `DocumentFile.fromTreeUri` を使用する
- ディレクトリ直下のファイルのみ取得する
- サブディレクトリは無視する
- 各ファイルを `RenameTargetFile` に変換する

#### 取得する情報

- id: 一意（uri.toStringで可）
- displayName: ファイル名
- uri: DocumentFileのURI
- size: length()
- lastModified: lastModified()

---

### STEP 4-2-2: CSV読み込み

#### 対象
- `SafDocumentDataSource.readTextFromUri`
- `StorageRepositoryImpl.readTextFromUri`

#### 実装内容

- `ContentResolver.openInputStream` を使用する
- UTF-8で読み込む
- 可能であればShift_JISにも対応
  - 失敗した場合はUTF-8のみでよい
- 全テキストをStringとして返す

---

### STEP 4-2-3: ファイルリネーム

#### 対象
- `SafDocumentDataSource.renameFile`
- `StorageRepositoryImpl.renameFile`

#### 実装内容

- `DocumentFile.fromSingleUri` を使用
- `renameTo(newName)` を呼ぶ
- 成功/失敗を `RenameResult` で返す

#### エラー処理

- rename失敗 → `success = false`
- 例外 → `RenameResult` にメッセージ格納

---

### STEP 4-2-4: 同名ファイル存在チェック

#### 対象
- `SafDocumentDataSource.existsInSameDirectory`
- `StorageRepositoryImpl.existsInSameDirectory`

#### 実装内容

- ディレクトリ内のファイル一覧を取得
- 同名ファイルが存在するか確認
- 完全一致で判定する

---

### STEP 4-2-5: URI権限保持

#### 対象
- `SafDocumentDataSource.takePersistablePermission`
- `StorageRepositoryImpl.takePersistablePermission`

#### 実装内容

- `ContentResolver.takePersistableUriPermission` を使用
- フラグ:
  - READ
  - WRITE

---

### STEP 4-2-6: Util実装

#### UriDisplayNameUtil

- `ContentResolver.query` でDISPLAY_NAME取得
- 取得できない場合はuriの末尾を使用

#### DateFormatUtil

- timestampを人間可読形式に変換
- 例: `yyyy/MM/dd HH:mm`

---

## 実装ルール（重要）

- Android APIはこの層でのみ使用する
- null安全を考慮する
- 例外は握りつぶさず、結果に反映する
- 仮実装・ダミーデータは禁止
- ログ出力は最小限でよい

---

## 出力させるもの

```md
## 実装コード

（変更ファイルすべて）

## 変更ファイル一覧

- StorageRepositoryImpl.kt
- SafDocumentDataSource.kt
- UriDisplayNameUtil.kt
- DateFormatUtil.kt

## 変更理由

（なぜこの実装にしたか）

## 動作確認方法

1. SAFでディレクトリ選択
2. ファイル一覧取得確認
3. CSV読み込み確認
4. リネーム実行確認
5. 同名チェック確認

## ビルド確認結果

（成功 or エラー内容）

## 未解決事項・リスク

（SAFの制約・端末依存など）
````

---

## ポイント

* 一度に全部やらない（今回はData層のみ）
* UIやViewModelはまだ触らない
* Domainロジックは既に完成している前提
* SAFは端末依存があるため安全側で実装する

---

## 完了条件

* Data層の `TODO` がすべて解消されている
* ビルドが通る
* 以下が動作可能

  * ディレクトリ内ファイル取得
  * CSV読み込み
  * ファイルリネーム
  * 同名チェック

```
```
