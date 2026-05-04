以下をそのまま `doc/STEP4-18.md` として保存して、Codexに渡してください。
STEP4-17では自動連番開始番号指定と下部Insets対応まで進んでいるため、STEP4-18では要望の中から依存関係が強い **UNDO用の作業履歴30件保持** を単機能で実装する形に切っています。

````md
# STEP 4-18: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- 今回は「UNDO機能の土台」として、アプリ起動中だけ保持するリネーム作業履歴を実装する
- リネーム1件を履歴レコード1つとして保持する
- 履歴は最大30件まで保持する
- アプリをキルしたら履歴はクリアされてよい
- まだ実際のUNDO実行処理は実装しない
- まずは履歴保持・履歴表示用状態・ログ確認までを完成させる

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

## 今回実装する機能

- リネーム成功時に、UNDO用の作業履歴を1件追加する
- 作業履歴はアプリ起動中だけ保持する
- 最大30件まで保持する
- 31件目を追加する場合は、最古の履歴を削除する
- 履歴レコードには、将来UNDOに必要な情報を保持する
- 実機確認用ログを出力する
- UIにはまだUNDOボタンを出さない、または表示だけに留める

## 今回実装しない機能

- UNDOボタンの本格実装
- UNDO押下時の逆リネーム実行
- 複数件UNDO
- 履歴の永続化
- アプリ再起動後の履歴復元
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲の追加確認
- 自動連番開始番号ダイアログのインライン分解表示改善
- RecyclerView化
- XMLレイアウト大規模変更
- Material Componentsへの本格移行

---

# 背景

STEP4-17では以下を実装済み。

- 自動連番をデフォルトON化
- 自動連番開始番号指定
- 画面下部の選択中プレビュー右側に「変更」ボタン追加
- 番号入力ダイアログ追加
- 成功時のみ連番カウンタを進める仕様維持
- 3ボタンナビゲーションで下部メッセージ行とLoadingViewが隠れる問題を修正
- `bottomContentContainer` に対して `bottomMargin` でInsetsを反映

STEP4-18では、追加要望のうち以下に対応する。

```text
「UNDO」ボタンを作ること。そのために、アプリ起動時からの作業記録を保持すること。
ただし、アプリをキルしたらクリアすること。
作業記録は、リネーム一件をレコード1つとして最大30件まで保持できる構造とする。
より効率のよい実装パターンがあれば提案してください。
````

ただし、今回は安全に進めるため、UNDO実行までは行わず、まず履歴保持の土台だけを作る。

---

# 作業ブランチ

作業開始前に、必ず現在の状態を確認すること。

```powershell
git status
git branch --show-current
```

STEP4-17の作業ブランチを元に、新しいブランチを作成すること。

```powershell
git checkout feature/step4-17-auto-number-start
git pull
git checkout -b feature/step4-18-rename-history
```

ブランチ名は以下とする。

```text
feature/step4-18-rename-history
```

## 重要

git操作の前に、必ずビルド確認と実機確認を行うこと。

実機確認OK後に、以下を実行すること。

```powershell
git status
git add .
git commit -m "Add in-memory rename history"
git push -u origin feature/step4-18-rename-history
```

Codexの最終回答では以下を報告すること。

* 作業ブランチ名
* 分岐元ブランチ
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* 実機確認結果
* commit hash
* push結果
* 未コミット差分の有無

---

# 設計方針

## 履歴の保持場所

履歴はアプリ起動中だけ保持すればよい。

そのため、永続化はしない。

候補案は以下。

### ベスト案

`RenameHistoryManager` のようなアプリ内メモリ管理クラスを作る。

例:

```text
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryRecord.kt
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryManager.kt
```

または既存構成に合わせて以下でもよい。

```text
app/src/main/java/com/example/easyrename/model/RenameHistoryRecord.kt
app/src/main/java/com/example/easyrename/history/RenameHistoryManager.kt
```

### 採用理由

* ViewModelをまたいで履歴を扱いやすい
* 将来UNDOボタンをHome側またはMatching側に置く場合にも参照しやすい
* RepositoryやDataSourceにUI操作履歴を混ぜずに済む
* アプリキルで自然にクリアされる
* SharedPreferencesやDBを使わないため実装が小さい

---

# 履歴レコード仕様

リネーム1件を1レコードとして保持する。

最低限、以下を保持すること。

```kotlin
data class RenameHistoryRecord(
    val id: String,
    val timestampMillis: Long,
    val beforeName: String,
    val afterName: String,
    val beforeUri: Uri,
    val afterUri: Uri,
    val directoryUri: Uri?,
    val renameMode: RenameMode,
    val sourceFileIdBefore: String,
    val sourceFileIdAfter: String,
    val candidateRawPattern: String?,
    val autoNumber: Int?,
)
```

実際の既存型・取得可能情報に合わせて調整してよい。

## 必須フィールド

以下は必ず入れること。

```text
- beforeName
- afterName
- beforeUri
- afterUri
- timestampMillis
- renameMode
```

## 可能なら入れるフィールド

以下は取得可能なら入れること。

```text
- directoryUri
- sourceFileIdBefore
- sourceFileIdAfter
- candidateRawPattern
- autoNumber
```

## 注意

* 将来UNDOするには「現在のafterUriをbeforeNameへ戻す」必要がある
* SAFではrename後にUriが変わる場合がある
* そのため、履歴には必ず `afterUri` を保持すること
* `beforeUri` だけではUNDO時に古いUri問題が再発する可能性がある

---

# RenameHistoryManager仕様

履歴管理クラスを追加する。

仕様は以下。

```kotlin
class RenameHistoryManager {
    private val maxSize = 30

    fun add(record: RenameHistoryRecord)
    fun getAll(): List<RenameHistoryRecord>
    fun getLatest(): RenameHistoryRecord?
    fun clear()
    fun size(): Int
}
```

## 挙動

* `add()` で最新履歴を追加する
* 履歴は新しい順、または古い順のどちらでもよいが、仕様をコメントで明記する
* 最大30件を超えたら最古の履歴を削除する
* 外部へ返すListはコピーにする
* 外部から内部MutableListを直接変更できないようにする

## 実装パターン

効率重視なら `ArrayDeque` を使ってよい。

例:

```kotlin
private val records = ArrayDeque<RenameHistoryRecord>()
```

30件固定なので、`MutableList` でも問題ない。

ただし、外部公開は必ず `List` にする。

---

# 履歴追加タイミング

履歴は、リネーム成功時のみ追加する。

失敗時は追加しない。

対象候補:

```text
RenameMatchingViewModel.kt
```

STEP4-17時点で、リネーム成功時には以下の情報が取れている想定。

```text
- RenameResult.beforeName
- RenameResult.afterName
- RenameResult.afterUri
- sourceFile.id
- sourceFile.uri
- renameMode
- selectedCandidate.rawPattern
- autoNumber
```

リネーム成功後、Matching側とHome側の状態更新が成功したタイミング、または `RenameResult.success == true` を確認した直後に履歴を追加する。

## 重要

既存仕様を維持すること。

* リネーム前ファイルがリネーム後になる挙動は維持する
* Matching側の表示更新は壊さない
* Home側の `applyRenameResult(result)` の挙動は壊さない
* 自動連番成功時だけカウンタが進む挙動は維持する
* 失敗時に連番が進まない挙動は維持する
* `FileAlreadyExists` 等の失敗時は履歴を追加しない

---

# UI反映について

今回、UNDOボタンの本格実装はしない。

ただし、今後のSTEPでUIを追加しやすいように、UiStateに以下のような状態を追加してよい。

```kotlin
val canUndo: Boolean = false
val renameHistoryCount: Int = 0
```

または、既存UIに影響を出さないため、今回はログだけでもよい。

## 今回の推奨

安全優先で、UI変更は最小限にする。

以下のどちらかを選ぶこと。

### 案A: UI変更なし

* 履歴追加のみ
* ログで履歴件数を確認する
* 次STEPでUNDOボタンを追加する

### 案B: UI状態だけ追加

* `canUndo`
* `renameHistoryCount`
* 画面表示は変えない
* 次STEPでボタン表示に使う

今回は **案B** を推奨する。

---

# ログ仕様

実機確認用に、以下のログを追加する。

タグは以下で統一する。

```kotlin
private const val TAG_HISTORY = "EasyRenameHistory"
```

## 履歴追加時

```text
EasyRenameHistory: add success beforeName=... afterName=... historySize=...
EasyRenameHistory: beforeUri=...
EasyRenameHistory: afterUri=...
EasyRenameHistory: renameMode=... autoNumber=...
```

## 履歴上限超過時

31件目追加で最古を削除した場合。

```text
EasyRenameHistory: trim oldest removed beforeName=... afterName=...
```

## 失敗時

失敗時には履歴を追加しない。

必要なら以下を出す。

```text
EasyRenameHistory: skip add because rename failed beforeName=... afterName=... errorType=...
```

---

# 実装対象ファイル候補

実際の構成に合わせて判断すること。

想定される追加ファイル:

```text
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryRecord.kt
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryManager.kt
```

想定される変更ファイル:

```text
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt
```

必要に応じて変更:

```text
app/src/main/java/com/example/easyrename/model/RenameResult.kt
app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt
```

ただし、`RenameResult` やUseCaseの変更は最小限にすること。

---

# 禁止事項

* UNDOの逆リネーム処理を今回実装しない
* UNDOボタンの本格動作を今回実装しない
* 履歴をSharedPreferencesやDBに保存しない
* アプリ再起動後に履歴を復元しない
* リネーム成功後の表示更新を壊さない
* 自動連番のカウンタ挙動を変えない
* CSV仕様を変えない
* RenameMode仕様を変えない
* singleUri / treeUri探索処理を今回変更しない
* CSVプレビューを今回実装しない
* Spinner横幅調整を今回実装しない
* 大規模リファクタをしない

---

# 動作確認方法

## 1. ビルド確認

以下を実行すること。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 2. 単体テスト

以下を実行すること。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 3. 実機確認

以下を確認する。

```text
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. マッチング画面へ進む
5. ファイルを1件選択する
6. 候補を1件選択する
7. リネーム実行する
8. リネーム成功後、履歴追加ログが1件出ることを確認する
9. もう1件リネームする
10. 履歴件数が2件になることを確認する
11. FileAlreadyExistsなどで失敗させる
12. 失敗時に履歴件数が増えないことを確認する
13. 自動連番ONでリネームし、autoNumberがログに出ることを確認する
14. 自動連番OFFでリネームし、autoNumber=null相当になることを確認する
15. リネーム前ファイルがリネーム後表示になる既存挙動が維持されていることを確認する
```

## 4. 履歴上限確認

可能なら31件のリネームを行い、最古履歴が削除されることを確認する。

実機で31件が大変な場合は、`RenameHistoryManager` の単体テストで確認すること。

期待動作:

```text
- 30件までは履歴が増える
- 31件目追加時に履歴件数は30件のまま
- 最古の1件が削除される
```

---

# 実機で確認するべきログ

## 1. 端末確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

## 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

## 3. 履歴ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 4. 既存ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRename
```

## 5. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 6. 確認するログ観点

以下を確認する。

```text
- リネーム成功時だけ EasyRenameHistory: add success が出る
- beforeName がリネーム前ファイル名になっている
- afterName がリネーム後ファイル名になっている
- beforeUri が入っている
- afterUri が入っている
- renameMode が入っている
- autoNumber が自動連番ON時に入っている
- 自動連番OFF時は autoNumber=null 相当になる
- 失敗時に履歴が増えない
- 31件目追加時に trim oldest が出る
- AndroidRuntime のクラッシュが出ていない
```

---

# 単体テスト方針

可能なら `RenameHistoryManager` の単体テストを追加する。

テスト対象:

```text
1. addで履歴が増える
2. getLatestで最新履歴が取れる
3. 最大30件を超えない
4. 31件目追加時に最古が削除される
5. clearで履歴が空になる
6. getAllで返したListから内部状態を破壊できない
```

テストファイル候補:

```text
app/src/test/java/com/example/easyrename/domain/history/RenameHistoryManagerTest.kt
```

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何を追加したか
* 履歴レコードにどの情報を保持しているか
* 最大30件制限をどう実装したか
* 失敗時に履歴追加しないこと
* アプリキルで履歴がクリアされる設計であること

## 2. 変更ファイル一覧

例:

```text
- RenameHistoryRecord.kt
- RenameHistoryManager.kt
- RenameMatchingViewModel.kt
- RenameMatchingUiState.kt
- RenameHistoryManagerTest.kt
```

## 3. 動作確認方法

* ビルド方法
* 単体テスト方法
* 実機ログ確認方法

## 4. 実機で見るべきログ

ユーザがそのままPowerShellで実行できる形で提示すること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. Git操作結果

* 作業ブランチ名
* 分岐元ブランチ
* commit message
* commit hash
* push先
* 未コミット差分の有無

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text
- UNDOボタンのUI追加
- UNDOボタン押下時の逆リネーム実行
- UNDO成功後の履歴更新
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

```text
- 今回は履歴保持のみで、UNDO実行は未実装
- 履歴はアプリ起動中のみ保持し、アプリキルで消える
- SAFではrename後にUriが変わるProviderがあるため、UNDO実装時はafterUriを使う必要がある
- 履歴に保持したUriがProvider都合で無効になる可能性は残る
- 30件上限はメモリ使用量と実装単純性を優先した暫定仕様
- UI上のUNDOボタン表示は次STEPで実装する
```

---

# 完了条件

このSTEP4-18の完了条件は以下。

```text
- assembleDebug が成功する
- testDebugUnitTest が成功する
- RenameHistoryRecord が追加されている
- RenameHistoryManager が追加されている
- リネーム成功時だけ履歴が追加される
- リネーム失敗時に履歴が追加されない
- 履歴が最大30件に制限されている
- 31件目追加時に最古履歴が削除される
- beforeName / afterName / beforeUri / afterUri / renameMode が履歴に保持される
- EasyRenameHistory ログで履歴追加を確認できる
- 既存のリネーム後表示更新が壊れていない
- 自動連番の既存挙動が壊れていない
- 実機確認後にcommit / pushされている
```

```

**ベスト案**
STEP4-18は、UNDOボタンそのものではなく、まず **作業履歴30件保持** を実装するのが安全です。UNDOはSAFのURI更新やリネーム失敗時の扱いが絡むため、履歴設計を先に固めた方が後戻りが少ないです。

**代替案**
CSVプレビューをSTEP4-18にする案もあります。ただし、CSVプレビューはUI追加・CSV読込・プレビュー整形が絡むため、UNDO履歴より変更範囲が広くなります。今の流れでは、リネーム操作の信頼性を上げる履歴基盤を先に入れる方が自然です。

**注意点**
今回のプロンプトでは、CodexにUNDO実行まで実装させないようにしています。履歴保持とログ確認に限定することで、既存のリネーム処理、自動連番、下部Insets対応を壊すリスクを抑えられます。
```
