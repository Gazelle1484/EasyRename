以下をそのまま `doc/STEP4-20.md` として保存して、Codexに渡してください。
STEP4-19では `UNDO` ボタンUIと最新履歴ログ出力まで完了し、逆リネーム処理は未実装のため、STEP4-20では **UNDO押下時の逆リネーム実行** だけを単機能として実装するプロンプトにしています。

````md
# STEP 4-20: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- STEP4-19で追加した `UNDO` ボタンから、最新履歴1件を使って逆リネームを実行する
- 今回は「最新1件のUNDO実行」だけを実装する
- UNDO成功時は履歴から最新1件を削除する
- UNDO失敗時は履歴を削除しない
- 既存のリネーム処理、自動連番、リネーム後表示更新、下部Insets対応を壊さない

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

## 今回実装する機能

- `UNDO` ボタン押下時に、最新の `RenameHistoryRecord` を使って逆リネームする
- 逆リネームは `afterName -> beforeName` の方向で行う
- 逆リネーム対象Uriは、履歴に保存された `afterUri` を使う
- UNDO成功時のみ、最新履歴1件を履歴から削除する
- UNDO成功後、Matching画面上のファイル表示を `afterName` から `beforeName` に戻す
- UNDO成功後、Home側の対象ファイル一覧も `afterName` から `beforeName` に戻す
- UNDO失敗時は履歴を削除しない
- UNDO失敗時はエラーメッセージとログを出す
- UNDO中は `UNDO` ボタンをdisabledにする
- `EasyRenameHistory` ログで処理の流れを確認できるようにする

## 今回実装しない機能

- 複数件UNDO
- 履歴一覧表示
- 履歴の永続化
- アプリ再起動後のUNDO
- UNDO成功後に自動連番カウンタを巻き戻す処理
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲の追加確認
- ディレクトリ読み込み内容の確認ログ
- 前回のセット再押下で選択解除する処理
- 自動連番開始番号ダイアログのインライン分解表示改善
- RecyclerView化
- XMLレイアウト大規模変更
- Material Componentsへの本格移行

---

# 重要: STEP4-19のcommit / pushを先に完了すること

STEP4-19のCodex回答では以下の状態だった。

```text
STEP4-19 commit hash: コミット後に確認
STEP4-19 push先: origin/feature/step4-19-undo-button-ui
未コミット差分: コミット前の差分あり
````

そのため、STEP4-20の作業を始める前に、必ずSTEP4-19をcommit / pushすること。

## 1. 現在状態確認

```powershell
git status
git branch --show-current
```

現在ブランチが以下であることを確認する。

```text
feature/step4-19-undo-button-ui
```

## 2. STEP4-19のビルド確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 3. STEP4-19の単体テスト確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 4. STEP4-19をcommit / push

```powershell
git status
git add .
git commit -m "Add undo button UI"
git push -u origin feature/step4-19-undo-button-ui
```

commit hashを控えること。

---

# STEP4-20 作業ブランチ

STEP4-19のcommit / push完了後、STEP4-20用ブランチを作成する。

```powershell
git checkout feature/step4-19-undo-button-ui
git pull
git checkout -b feature/step4-20-undo-rename
```

ブランチ名は以下とする。

```text
feature/step4-20-undo-rename
```

作業完了後、実機確認OKのあとに以下を実行すること。

```powershell
git status
git add .
git commit -m "Implement single undo rename"
git push -u origin feature/step4-20-undo-rename
```

Codexの最終回答では以下を報告すること。

* STEP4-19のcommit hash
* STEP4-19のpush結果
* STEP4-20の作業ブランチ名
* STEP4-20の分岐元ブランチ
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* 実機確認結果
* STEP4-20のcommit hash
* STEP4-20のpush結果
* 未コミット差分の有無
* 回答を保存した `doc/STEP4-20_codex.md`

---

# 背景

STEP4-18では以下を実装済み。

* `RenameHistoryRecord`
* `RenameHistoryManager`
* リネーム成功時だけ履歴追加
* リネーム失敗時は履歴追加しない
* 履歴最大30件
* `RenameMatchingUiState.canUndo`
* `RenameMatchingUiState.renameHistoryCount`

STEP4-19では以下を実装済み。

* マッチング画面下部に `UNDO` ボタン追加
* 履歴0件ではdisabled
* 履歴1件以上でenabled
* UNDO押下時に最新履歴を `EasyRenameHistory` ログへ出力
* 逆リネーム処理は未実装
* 履歴削除も未実装

STEP4-20では、STEP4-19のボタンから実際に最新1件だけUNDOする。

---

# UNDO仕様

## 基本仕様

最新履歴1件を使って、以下の逆リネームを行う。

```text
afterName -> beforeName
```

例:

```text
A1-1-1_log.zip -> log.zip
```

## 使うUri

UNDO対象は、履歴レコードの `afterUri` を使うこと。

理由:

```text
SAFではrename後にUriが変わるProviderがある。
beforeUriを使うと古いUriになり、FileNotFoundが再発する可能性がある。
```

## 成功時

UNDO成功時は以下を行う。

```text
1. afterName から beforeName へリネームする
2. RenameHistoryManager から最新履歴1件を削除する
3. Matching画面の対象ファイル表示を beforeName に戻す
4. Home側の対象ファイル一覧も beforeName に戻す
5. canUndo / renameHistoryCount を更新する
6. UNDO成功ログを出す
```

## 失敗時

UNDO失敗時は以下。

```text
1. 履歴は削除しない
2. Matching画面・Home画面の表示は変更しない
3. エラーメッセージを表示する
4. UNDO失敗ログを出す
5. canUndo / renameHistoryCount は履歴が残っている状態を維持する
```

## UNDO中

UNDO中は以下。

```text
- UNDOボタンをdisabledにする
- 通常リネーム実行ボタンも既存仕様に合わせて必要ならdisabledにする
- LoadingViewがある場合は表示してよい
```

---

# 実装方針

## 1. RenameHistoryManagerに最新履歴削除APIを追加する

STEP4-18の `RenameHistoryManager` には以下がある想定。

```kotlin
fun add(record: RenameHistoryRecord)
fun getAll(): List<RenameHistoryRecord>
fun getLatest(): RenameHistoryRecord?
fun clear()
fun size(): Int
```

STEP4-20で以下を追加する。

```kotlin
fun removeLatest(): RenameHistoryRecord?
```

仕様:

```text
- 履歴が空なら null を返す
- 履歴があれば最新1件を削除して返す
- 履歴は新しい順で保持されているため、先頭を削除する
```

`ArrayDeque` で `addFirst()` している場合は、`removeFirstOrNull()` 相当の処理にする。

Kotlinバージョンによって `removeFirstOrNull()` が使えない場合は、空判定してから `removeFirst()` する。

ログ例:

```text
EasyRenameHistory: remove latest beforeName=... afterName=... historySize=...
```

## 2. UNDO用UseCaseを追加する

既存の通常リネームUseCaseをそのまま無理に流用してもよいが、責務を分けるなら以下を追加する。

推奨:

```text
app/src/main/java/com/example/easyrename/domain/usecase/UndoRenameUseCase.kt
```

想定API:

```kotlin
class UndoRenameUseCase(
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(record: RenameHistoryRecord): RenameResult
}
```

処理内容:

```text
1. record.afterUri を Uri.parse() する
2. record.afterName を beforeName として扱う
3. record.beforeName を afterName として扱う
4. StorageRepository.renameFile(...) を呼び、afterUriのファイルをbeforeNameへ戻す
5. RenameResult を返す
```

命名が混乱しないよう、ログでは以下のように明示すること。

```text
undo sourceName = record.afterName
undo targetName = record.beforeName
undo sourceUri = record.afterUri
```

## 3. Repository / DataSourceの既存renameFileを再利用する

既存の通常リネームで使っている処理を再利用すること。

禁止:

```text
- Fragmentから直接DocumentFile.renameTo()を呼ぶ
- ViewModelから直接DocumentFile.renameTo()を呼ぶ
- UNDO専用にDataSourceを重複実装する
```

推奨:

```text
ViewModel
→ UndoRenameUseCase
→ StorageRepository
→ SafDocumentDataSource
```

既存の `RenameResult.afterUri` 取得処理を再利用すること。

## 4. ViewModelにUNDO実行処理を追加する

STEP4-19の `onUndoClicked()` はログのみだった。

STEP4-20では、以下の処理へ変更する。

```text
1. 最新履歴を取得する
2. nullならログを出して終了
3. isUndoExecuting をtrueにする
4. UndoRenameUseCaseを実行する
5. 成功なら履歴から最新1件を削除する
6. 成功ならMatching側状態をbeforeNameへ戻す
7. 成功ならHome側へUNDO結果を反映する
8. 失敗なら履歴は削除しない
9. canUndo / renameHistoryCount を更新する
10. isUndoExecuting をfalseにする
```

## 5. UiStateにUNDO実行中状態を追加する

必要なら追加する。

```kotlin
val isUndoExecuting: Boolean = false
```

UNDOボタンenabled条件は以下。

```kotlin
undoButton.isEnabled = state.canUndo && !state.isExecuting && !state.isUndoExecuting
```

既存の `isExecuting` に統合できる場合は、無理に追加しなくてよい。

ただし、ログやUI上で通常リネーム実行中とUNDO実行中を区別できる方が望ましい。

## 6. HomeViewModelへの反映

通常リネーム成功時は、STEP4-9以降で `HomeViewModel.applyRenameResult(result)` のような処理がある想定。

UNDO成功時もHome側の対象ファイル一覧を戻す必要がある。

推奨案:

```kotlin
homeViewModel.applyUndoRenameResult(record, result)
```

または既存の `applyRenameResult(result)` で対応できるよう、UNDO用 `RenameResult` に以下を入れる。

```text
beforeName = record.afterName
afterName = record.beforeName
sourceFileId = record.sourceFileIdAfter
afterUri = result.afterUri
```

ただし、通常リネームとUNDOで意味が混乱する場合は、専用メソッドを追加する方が安全。

## 7. Matching側表示更新

UNDO成功後、Matching側の対象ファイル一覧を以下のように更新する。

```text
displayName: record.beforeName
uri: result.afterUri ?: Uri.parse(record.beforeUri)
id: 更新後Uri文字列
isRenamed: false または既存仕様に合わせる
isSelected: false
```

`isRenamed` の扱い:

```text
UNDOにより元名に戻ったため、原則 false を推奨する。
ただし、既存UIでリネーム済み表示を履歴として残したい場合は仕様を明記する。
```

今回は以下を採用すること。

```text
UNDO成功後の対象ファイルは isRenamed=false に戻す。
```

---

# ログ仕様

タグはSTEP4-18/19と同じものを使う。

```kotlin
private const val TAG_HISTORY = "EasyRenameHistory"
```

## UNDO開始

```text
EasyRenameHistory: undo start beforeName=... afterName=... historySize=...
EasyRenameHistory: undo sourceName=... targetName=...
EasyRenameHistory: undo sourceUri=... targetUriName=...
```

## UNDO成功

```text
EasyRenameHistory: undo success restoredName=... resultAfterUri=... historySize=...
```

## UNDO失敗

```text
EasyRenameHistory: undo failed sourceName=... targetName=... errorType=... errorMessage=...
```

## 履歴削除

```text
EasyRenameHistory: remove latest beforeName=... afterName=... historySize=...
```

## 空履歴

```text
EasyRenameHistory: undo clicked but history is empty
```

## 既存renameToログ

既存の `EasyRename` / `EasyRenamePerf` ログがあれば維持する。

UNDO時に `renameTo` が走っていること、かつ通常リネームではなくUNDO用途であることが分かるログを追加してよい。

---

# エラー処理

## FileAlreadyExists

UNDO先の `beforeName` が既に存在する場合、UNDOは失敗扱いにする。

```text
- 履歴は削除しない
- UI表示は変更しない
- エラーメッセージを表示する
- EasyRenameHistory に errorType=FileAlreadyExists を出す
```

自動で連番を付けて戻すことは禁止。

## FileNotFound

`afterUri` が無効で対象ファイルが見つからない場合、UNDOは失敗扱いにする。

```text
- 履歴は削除しない
- UI表示は変更しない
- errorType=FileNotFound を出す
```

## その他例外

既存の `RenameErrorType` に合わせる。

なければ既存の通常リネーム失敗処理と同じ扱いにする。

---

# 自動連番カウンタについて

今回、UNDO成功時に自動連番カウンタは巻き戻さない。

理由:

```text
- 自動連番カウンタの巻き戻しは、候補ごとの履歴・失敗時・複数UNDOと絡む
- 今回のSTEP4-20は最新1件のファイル名UNDOだけに限定する
- カウンタ巻き戻しまで同時に行うと変更範囲が広がる
```

Codexの最終回答でも、未解決事項として明記すること。

---

# 下部Insets対応

STEP4-17で、3ボタンナビゲーション使用時に下部メッセージ行とLoadingViewが重なる問題を修正済み。

STEP4-19でUNDOボタンを下部エリアに追加済み。

今回も以下を維持すること。

```text
- bottomContentContainer の bottomMargin Insets 対応を壊さない
- UNDOボタンが3ボタンナビゲーションで隠れない
- ジェスチャーナビゲーションでも不自然に隠れない
- LoadingView表示中もナビゲーションバーに重ならない
```

新しくroot padding方式に戻さないこと。

---

# 変更対象ファイル候補

実際の構成に合わせて確認すること。

想定される追加ファイル:

```text
app/src/main/java/com/example/easyrename/domain/usecase/UndoRenameUseCase.kt
```

想定される変更ファイル:

```text
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryManager.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt
app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt
```

必要に応じて変更:

```text
app/src/main/java/com/example/easyrename/model/RenameResult.kt
app/src/main/java/com/example/easyrename/domain/repository/StorageRepository.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
```

ただし、Repository / DataSource の変更は最小限にすること。

テスト候補:

```text
app/src/test/java/com/example/easyrename/domain/history/RenameHistoryManagerTest.kt
```

ドキュメント出力:

```text
doc/STEP4-20_codex.md
```

---

# 禁止事項

* 複数件UNDOを今回実装しない
* 履歴一覧UIを今回実装しない
* 履歴をSharedPreferencesやDBへ保存しない
* 自動連番カウンタをUNDOで巻き戻さない
* UNDO失敗時に履歴を削除しない
* UNDO失敗時に画面表示を戻さない
* UNDO先に同名ファイルがある場合に自動連番で回避しない
* FragmentやViewModelから直接 `DocumentFile.renameTo()` を呼ばない
* CSV仕様を変えない
* RenameMode仕様を変えない
* singleUri / treeUri探索処理を今回大きく変更しない
* CSVプレビューを今回実装しない
* Spinner横幅調整を今回実装しない
* 前回のセット再押下処理を今回実装しない
* ディレクトリ読み込み確認ログを今回実装しない
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
5. 履歴0件の状態でUNDOボタンがdisabledであることを確認する
6. ファイルを1件選択する
7. 候補を1件選択する
8. リネーム実行する
9. リネーム成功後、リネーム前ファイル表示がリネーム後ファイル名になることを確認する
10. UNDOボタンがenabledになることを確認する
11. UNDOボタンを押す
12. ファイル名がリネーム前の名前に戻ることを確認する
13. Matching画面上の表示もリネーム前の名前に戻ることを確認する
14. Homeへ戻り、対象ファイル一覧でもリネーム前の名前に戻っていることを確認する
15. UNDO成功後、履歴件数が1減ることを確認する
16. 履歴0件になった場合、UNDOボタンがdisabledになることを確認する
17. もう一度UNDOを押せない、または押しても何も起きないことを確認する
18. 3ボタンナビゲーションでUNDOボタン・メッセージ行・LoadingViewが隠れないことを確認する
```

## 4. 失敗ケース確認

可能なら以下も確認する。

```text
1. リネーム後、UNDO先のbeforeNameと同名のファイルを別途作成する
2. UNDOを押す
3. FileAlreadyExists相当で失敗することを確認する
4. 履歴が削除されないことを確認する
5. UI表示が勝手に戻らないことを確認する
```

実機で同名ファイル作成が難しい場合は、ログ確認のみでもよい。

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

## 5. 性能ログ確認

必要に応じて確認する。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

## 6. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 7. 確認するログ観点

以下を確認する。

```text
- undo start が出る
- undo sourceName が履歴の afterName になっている
- undo targetName が履歴の beforeName になっている
- undo sourceUri が履歴の afterUri になっている
- undo success が出る
- remove latest が出る
- UNDO成功後に historySize が1減る
- UNDO成功後に canUndo / renameHistoryCount が更新される
- UNDO失敗時は undo failed が出る
- UNDO失敗時は remove latest が出ない
- AndroidRuntime のクラッシュが出ていない
```

---

# 単体テスト方針

可能なら `RenameHistoryManagerTest` に以下を追加する。

```text
1. removeLatestで最新履歴が削除される
2. removeLatestは削除した履歴を返す
3. removeLatest後にsizeが1減る
4. 空履歴でremoveLatestするとnullを返す
5. removeLatest後のgetLatestが次の履歴を返す
```

`UndoRenameUseCase` はRepositoryモックが既存テストにない場合、無理に単体テストを増やさなくてよい。
その場合は実機ログ確認を重視すること。

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何を追加したか
* UNDO実行の流れ
* `afterUri` を使って逆リネームしていること
* UNDO成功時のみ履歴を削除すること
* UNDO失敗時は履歴を残すこと
* Matching側表示更新
* Home側表示更新
* 自動連番カウンタは巻き戻していないこと
* 下部Insets対応を壊していないこと

## 2. 変更ファイル一覧

例:

```text
- UndoRenameUseCase.kt
- RenameHistoryManager.kt
- RenameMatchingViewModel.kt
- RenameMatchingUiState.kt
- RenameMatchingFragment.kt
- HomeViewModel.kt
- AppViewModelFactory.kt
- RenameHistoryManagerTest.kt
- doc/STEP4-20_codex.md
```

## 3. 動作確認方法

* ビルド方法
* 単体テスト方法
* 実機ログ確認方法
* 実機UI確認方法
* 失敗ケース確認方法

## 4. 実機で見るべきログ

ユーザがそのままPowerShellで実行できる形で提示すること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. Git操作結果

* STEP4-19のcommit hash
* STEP4-19のpush結果
* STEP4-20の作業ブランチ名
* STEP4-20の分岐元ブランチ
* STEP4-20のcommit message
* STEP4-20のcommit hash
* STEP4-20のpush先
* 未コミット差分の有無

## 6. 保存ファイル

Codexの回答は以下へ保存すること。

```text
doc/STEP4-20_codex.md
```

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- UNDO成功時の自動連番カウンタ巻き戻し
- UNDO失敗時の詳細ダイアログ表示
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
- STEP4-20では最新1件のUNDOのみ対応
- 複数件UNDOは未実装
- UNDO成功時に自動連番カウンタは巻き戻していない
- 履歴はアプリ起動中のみ保持し、アプリキルで消える
- SAF Providerによっては履歴のafterUriが無効になる可能性がある
- UNDO先のbeforeNameが既に存在する場合は失敗扱い
- UNDO失敗時の詳細なリカバリUIは未実装
```

---

# 完了条件

このSTEP4-20の完了条件は以下。

```text
- STEP4-19がcommit / pushされている
- assembleDebug が成功する
- testDebugUnitTest が成功する
- UNDOボタン押下で最新履歴1件の逆リネームが実行される
- UNDO時に record.afterUri を使っている
- UNDO成功時にファイル名が beforeName に戻る
- UNDO成功時にMatching画面表示が beforeName に戻る
- UNDO成功時にHome側表示も beforeName に戻る
- UNDO成功時のみ履歴が1件削除される
- UNDO失敗時は履歴が削除されない
- UNDO失敗時はUI表示が勝手に戻らない
- 履歴0件になったらUNDOボタンがdisabledになる
- 自動連番の既存挙動が壊れていない
- リネーム前ファイルがリネーム後になる既存挙動が壊れていない
- 下部エリアが3ボタンナビゲーションで隠れない
- 下部エリアがジェスチャーナビゲーションで不自然に隠れない
- 実機確認後にSTEP4-20がcommit / pushされている
- Codex回答が doc/STEP4-20_codex.md に保存されている
```

```

**ベスト案**
STEP4-20は、`UNDO` の逆リネーム実行だけに絞るのが安全です。履歴一覧や複数UNDO、自動連番カウンタ巻き戻しまで同時に入れると、失敗時の切り分けが難しくなります。

**代替案**
CSVプレビューへ進む案もありますが、STEP4-18/19でUNDO基盤とUIを作った流れなので、ここで最新1件のUNDOまで閉じる方が自然です。

**注意点**
UNDOでは必ず `beforeUri` ではなく `afterUri` を使わせてください。通常リネーム後にURIが変わるProviderがあるため、古いURIで戻そうとすると `FileNotFound` 系の再発リスクがあります。
```
