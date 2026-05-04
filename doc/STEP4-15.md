````md
# STEP 4-15: 実装（機能単位）用 Codex 指示

STEP4-14では、自動連番ON/OFF、自動連番ボタン、候補ごとの連番カウンタ、Prefix / Suffix / Replaceとの組み合わせ、自動連番ON時の候補再利用が実装されています。STEP4-14時点では、自動連番は画面セッション内だけで保持され、次番号表示・開始番号指定・Undo・CSVプレビューなどは後続候補として残っています。:contentReference[oaicite:0]{index=0}

このSTEPでは、要望が多いため一度に全部は実装しません。  
**STEP4-15では「前回セットの記憶」と「マッチング画面へ進む直前の最新読み込み」だけを対象**にしてください。
回答はSTEP4-15_codex.mdに保存すること。

---

## 目的

- 1機能ずつ確実に完成させる
- 前回使ったディレクトリとCSVを端末内に保存する
- アプリを再起動しても、前回セットをHome画面から再選択できるようにする
- 「マッチング画面へ進む」押下時に、ディレクトリとCSVを毎回読み直して最新化する
- 既存の手動ディレクトリ選択・CSV選択は維持する
- 自動連番、Prefix / Suffix / Replace、非同期リネーム、成功後1件更新を壊さない
- ビルドが通る状態を維持する

---

## Gitブランチ運用

このSTEPの作業は必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell
git status
git checkout main
git pull
git checkout -b feature/step4-15-last-used-set
````

### 注意

STEP4-15はSTEP4-14の実装を前提にします。
`main` にSTEP4-14までの変更が入っていない場合は、作業に必要な最新ブランチを確認し、どのブランチから分岐するべきかを報告してください。

必要であれば、以下のようにSTEP4-14ブランチから分岐してください。

```powershell
git checkout feature/step4-14-auto-numbering
git pull
git checkout -b feature/step4-15-last-used-set
```

### commit / push のタイミング

このSTEPでは、**ビルド確認後、実機確認手順を提示し、実機確認OKを確認してから commit / push** してください。

実機確認OK後に以下を実行してください。

```powershell
git status
git add .
git commit -m "Remember last used directory and CSV"
git push -u origin feature/step4-15-last-used-set
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
- 前回使ったディレクトリとCSVのパス相当情報を記憶する
- アプリがキルされても、アンインストールされない限り保持する
- Home画面の一番上に「前回のセット：＜ディレクトリ名＞ / ＜CSV名＞」を表示する
- その表示を押下すると、前回のディレクトリとCSVを再選択状態にする
- 既存の「ディレクトリを選択」「CSVを選択」はそのまま残す
- 「マッチング画面へ進む」は常時表示する
- デフォルトはグレーアウトしてタップ無効にする
- ディレクトリとCSVの両方を選択すると有効化する
- 「マッチング画面へ進む」を押下するたびに、ディレクトリとCSVを読み込んでマッチング画面表示を最新化する
- ディレクトリ読み込みでは、ファイルの中身を読まず、ファイル名などメタデータのみ取得していることを確認する
```

---

## 今回やらないこと

以下は重要ですが、STEP4-15では実装しないでください。後続STEPへ回してください。

```text
- 自動連番の開始番号指定
- 自動連番開始番号変更ダイアログ
- UNDO機能
- 作業履歴30件保持
- CSVプレビュー
- リネーム候補・リネーム対象の選択カード色ハイライト
- 「[選択中]」表記削除
- 小文字ファイル名が大文字表示される問題修正
- 画面下部履歴表示のナビゲーションバーInsets対応
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ強化
- treeUri探索範囲の追加調査
- 自動連番デフォルトONへの変更
- 選択中プレビュー表示
- Home画面CSVプレビューボタン
- RecyclerView化
- XMLレイアウト化
- Material Components本格移行
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text
app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt
app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt
app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt
```

必要に応じて以下を追加して構いません。

```text
app/src/main/java/com/example/easyrename/data/preferences/LastUsedSetStore.kt
app/src/main/java/com/example/easyrename/model/LastUsedSet.kt
```

---

## 禁止事項

* 自動連番の仕様を変更しない
* Prefix / Suffix / Replace の仕様を変更しない
* 既存CSV形式 `A1-1_*` の挙動を変えない
* リネーム処理を同期処理へ戻さない
* `treeUri fallback` を削除しない
* `FileAlreadyExists` の検出を削除しない
* 既存の「ディレクトリを選択」「CSVを選択」を消さない
* 「マッチング画面へ進む」ボタンを非表示にしない
* 大規模なUI刷新をしない
* DIライブラリを追加しない
* DataStoreなど新規依存ライブラリを追加しない

---

# STEP 4-15-1: 前回セット保存用モデルを追加する

## 目的

前回使ったディレクトリとCSVを1セットとして扱えるようにする。

## 対象

* `model` または `data/preferences`

## 実装内容

以下に相当するモデルを追加してください。

```kotlin
data class LastUsedSet(
    val directoryUriString: String,
    val directoryDisplayName: String,
    val csvUriString: String,
    val csvDisplayName: String,
)
```

## 注意

* `Uri` そのものではなく、保存時は文字列化する
* 復元時に `Uri.parse(...)` する
* 表示名も保存する
* パス文字列には依存しない
* Android 33 / SAF前提なのでURIベースを維持する

---

# STEP 4-15-2: SharedPreferencesで前回セットを保存・復元する

## 目的

アプリがキルされても、アンインストールされない限り前回セットを保持する。

## 対象

* 新規 `LastUsedSetStore`
* 必要に応じて `AppViewModelFactory`
* 必要に応じて `HomeViewModel`

## 実装内容

新規クラスを追加してください。

```kotlin
class LastUsedSetStore(context: Context)
```

想定メソッド:

```kotlin
fun save(lastUsedSet: LastUsedSet)
fun load(): LastUsedSet?
fun clear()
```

保存先は `SharedPreferences` を使ってください。

## 採用理由

今回は依存追加を避けるため、DataStoreではなくSharedPreferencesでよいです。

## 注意

* DataStore依存は追加しない
* URI文字列と表示名を保存する
* 保存失敗時にクラッシュしないようにする
* 保存データが壊れている場合は `null` 扱いでよい

---

# STEP 4-15-3: URI永続権限の扱いを確認する

## 目的

前回セットから再利用するために、SAF URIの永続権限を可能な範囲で保持する。

## 対象

* `HomeViewModel`
* `StorageRepository`
* `StorageRepositoryImpl`
* `SafDocumentDataSource`
* `HomeFragment`

## 実装内容

ディレクトリ選択時・CSV選択時に、既存の `takePersistablePermission` 相当処理がある場合は確実に呼ばれているか確認してください。

必要に応じて、以下の方針にしてください。

```text
- ディレクトリURI: READ + WRITE を保持
- CSV URI: READ を保持
- 失敗してもクラッシュさせず、警告ログまたはAppErrorで扱う
```

## 注意

* 永続権限が取れないProviderもあり得る
* 権限が失効した場合は、前回セット押下時にエラー表示して、再選択を促す
* URI権限に失敗しても、即時操作可能なら処理継続してよい

---

# STEP 4-15-4: HomeUiStateに前回セット表示状態を追加する

## 目的

Home画面に「前回のセット」を表示できるようにする。

## 対象

* `HomeUiState`
* `HomeViewModel`

## 実装内容

`HomeUiState` に以下を追加してください。

```kotlin
val lastUsedSet: LastUsedSet? = null
val isLastUsedSetAvailable: Boolean = false
```

`HomeViewModel` 初期化時、`LastUsedSetStore.load()` を呼び、状態へ反映してください。

## 表示条件

```text
- 前回セットが存在する場合: 表示する
- 前回セットが存在しない場合: 「前回のセット：なし」または非表示
```

今回は、Home画面の一番上に常時表示してください。

---

# STEP 4-15-5: Home画面に「前回のセット」ボタンを追加する

## 目的

ユーザーが前回セットをワンタップで再選択できるようにする。

## 対象

* `HomeFragment`

## 実装内容

Home画面の一番上に、以下の形式のボタンまたはTextView風ボタンを追加してください。

```text
前回のセット：＜ディレクトリ名＞ / ＜CSV名＞
```

前回セットがない場合:

```text
前回のセット：なし
```

## 押下時の動作

前回セットがある場合、押下時に以下を呼んでください。

```kotlin
homeViewModel.selectLastUsedSet()
```

前回セットがない場合は何もしない、またはdisabledにしてください。

## 注意

* 既存の「ディレクトリを選択」「CSVを選択」は残す
* TopAppBarに隠れないようにする
* UIの大規模刷新はしない
* プログラムmatic View構成を維持する

---

# STEP 4-15-6: 前回セット選択時は読み込み予約状態にする

## 目的

前回セット押下時に即座に重い読み込みを行わず、「選択済み」として扱い、マッチング画面へ進む時に最新読み込みする。

## 対象

* `HomeViewModel`

## 実装内容

`selectLastUsedSet()` を追加してください。

```kotlin
fun selectLastUsedSet()
```

処理内容:

```text
- lastUsedSetのdirectoryUri / csvUriを選択中URIへ反映
- selectedDirectoryName を保存済みディレクトリ名にする
- selectedCsvFileName を保存済みCSV名にする
- isReadyToStartMatching = true にする
- この時点では重いファイル一覧読み込み・CSV解析はしない
```

## 注意

* 前回セットのURIが不正な場合はエラー表示
* 読み込みは「マッチング画面へ進む」押下時に行う
* 前回セット押下時に古いファイル一覧を使わない

---

# STEP 4-15-7: 「マッチング画面へ進む」押下時に毎回最新読み込みする

## 目的

マッチング画面の表示を毎回最新化する。

## 対象

* `HomeViewModel`
* `HomeFragment`
* 必要に応じて `AppViewModelFactory`

## 実装内容

「マッチング画面へ進む」ボタン押下時に、以下を実行してください。

```text
1. 選択中ディレクトリURIを使って、ファイル一覧を読み込む
2. 選択中CSV URIを使って、CSVを読み込む
3. RenameCandidateを生成する
4. targetFiles / renameCandidates をHomeUiStateへ反映する
5. 前回セットとして保存する
6. 読み込み成功後にMatching画面へ遷移する
```

## メソッド例

```kotlin
fun prepareMatchingData(
    onPrepared: () -> Unit
)
```

または、Fragment側でStateFlowを見て遷移してもよいです。

## 注意

* 読み込み中は `isLoading = true`
* 失敗時はMatching画面へ進まない
* 成功時のみ前回セットを保存する
* 既存の読み込み処理と重複しすぎないようにする
* 可能なら `viewModelScope + Dispatchers.IO` で読み込みを行う
* ただし変更範囲が大きい場合は、I/O化は後続STEPへ回してよい

---

# STEP 4-15-8: 「マッチング画面へ進む」ボタンを常時表示・状態制御する

## 目的

Home画面上でボタンの位置を安定させ、選択状態を分かりやすくする。

## 対象

* `HomeFragment`
* `HomeUiState`

## 実装内容

以下の挙動にしてください。

```text
- 「マッチング画面へ進む」は常時表示
- ディレクトリとCSVの両方が未選択の場合はdisabled
- 片方だけ選択済みの場合もdisabled
- 両方選択済みの場合のみenabled
- disabled時はグレーアウト
- enabled時は現在のprimary色
```

## 注意

* ボタンを非表示にしない
* 既存の色設計を維持する
* 押下時はSTEP4-15-7の最新読み込みを実行する

---

# STEP 4-15-9: ディレクトリ読み込みがファイル名のみ取得であることを確認する

## 目的

ディレクトリ読み込みで、ファイル本体を読み込んでいないことを確認する。

## 対象

* `SafDocumentDataSource.loadFilesInDirectory`
* 必要に応じてログ追加

## 確認内容

以下のような実装であることを確認してください。

```text
- DocumentFile.listFiles() で直下ファイルを列挙
- name / uri / length / lastModified などメタデータのみ取得
- openInputStream でファイル本体を読まない
```

## 実装内容

必要であれば、デバッグログに以下を出してください。

```text
EasyRename: loadFilesInDirectory metadataOnly=true fileCount=...
```

## 注意

* ファイル本体を読み込まない
* サブディレクトリ対応は追加しない
* 読み込み対象は従来どおり直下ファイルのみ

---

## 実装ルール

* 今回は「前回セットの記憶」と「マッチング遷移時の最新読み込み」だけに集中する
* 自動連番の新機能追加はしない
* UNDOは実装しない
* CSVプレビューは実装しない
* UI修正を広げすぎない
* 既存のリネーム処理を壊さない
* 既存の非同期リネーム処理を維持する
* 成功後1件更新方式を維持する
* Prefix / Suffix / Replaceの仕様を変更しない
* `FileAlreadyExists` の保護を維持する
* ビルドが通る状態を維持する

---

## 後に回す機能メモ

以下は今回のSTEPでは実装しないでください。後続STEPの候補としてメモに残してください。

```text
- 自動連番デフォルトON
- 自動連番開始番号指定
- 自動連番開始番号変更ダイアログ
- 選択中プレビュー表示
- UNDOボタン
- 作業履歴30件保持
- CSVプレビュー
- CSVプレビューボタン
- 小文字ファイル名が大文字表示される問題修正
- 選択中カードの色ハイライト
- 「[選択中]」表記削除
- ナビゲーションバーInsets対応
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
- Edge-to-Edge / WindowInsets正式対応
- 画面回転時の完全な状態復元
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー
```

---

## 出力させるもの

以下の形式で出力してください。

```md
# STEP 4-15: 前回セット記憶と最新読み込み Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### LastUsedSet.kt

### LastUsedSetStore.kt

### HomeUiState.kt

### HomeViewModel.kt

### HomeFragment.kt

### AppViewModelFactory.kt

### SafDocumentDataSource.kt

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
2. Home画面の一番上に「前回のセット：なし」または前回セットが表示されることを確認する
3. 初回起動時、「マッチング画面へ進む」が表示されているがdisabledであることを確認する
4. ディレクトリだけ選択しても「マッチング画面へ進む」がdisabledであることを確認する
5. CSVも選択すると「マッチング画面へ進む」がenabledになることを確認する
6. 「マッチング画面へ進む」を押す
7. 押下時にディレクトリとCSVが読み込まれることを確認する
8. Matching画面に最新のファイル一覧と候補一覧が表示されることを確認する
9. 1件リネームして成功することを確認する
10. アプリを終了する
11. アプリを再起動する
12. Home画面上部に「前回のセット：＜ディレクトリ名＞ / ＜CSV名＞」が表示されることを確認する
13. 前回セットを押下する
14. ディレクトリ名とCSV名が選択済みになることを確認する
15. 「マッチング画面へ進む」がenabledになることを確認する
16. 「マッチング画面へ進む」を押すたびに最新読み込みされることを確認する
17. ディレクトリ読み込みでファイル本体を読んでいないことをログで確認する
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
- 前回セットがHome画面上部に表示される
- 前回セット押下でディレクトリとCSVが選択済みになる
- アプリ再起動後も前回セットが残る
- 「マッチング画面へ進む」は常時表示され、条件を満たすまでdisabled
- 「マッチング画面へ進む」を押すたびに最新読み込みされる
- 既存のリネーム処理が成功する
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

```text
- LastUsedSetStore.load success / null / failed
- LastUsedSetStore.save directoryName=... csvName=...
- selectLastUsedSet directoryUri=... csvUri=...
- prepareMatchingData start
- prepareMatchingData loadDirectory success fileCount=...
- prepareMatchingData loadCsv success candidateCount=...
- loadFilesInDirectory metadataOnly=true fileCount=...
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

* 作業ブランチ `feature/step4-15-last-used-set` で作業している
* 実機確認後にcommitしてpushしている
* 前回使ったディレクトリとCSVが端末内に保存される
* アプリ再起動後も前回セットがHome画面に表示される
* 前回セット押下でディレクトリとCSVが選択済みになる
* 既存の「ディレクトリを選択」「CSVを選択」が残っている
* 「マッチング画面へ進む」が常時表示される
* 条件を満たすまで「マッチング画面へ進む」がdisabledである
* 条件を満たすと「マッチング画面へ進む」がenabledになる
* 「マッチング画面へ進む」押下時に毎回ディレクトリとCSVを読み込む
* ディレクトリ読み込みでファイル本体を読んでいない
* 既存のリネーム処理が壊れていない
* 自動連番機能が壊れていない
* ビルドが通る

```
```
