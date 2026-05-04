実機検証結果と、codexの回答から、のプロンプトを次に与えるプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-19: 実装（機能単位）
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
- git操作の前に実機確認
- 回答は本ファイル名+*_codexした.mdファイルに保存
###### 追加の要望
- リネーム前ファイルがリネームするとリネーム後になるのは維持してほしい。
- ディレクトリの読み込みは、ファイルの中身を読み込まず、中のファイル名のみの取得かを確認すること。
- 「前回のセット」を押下し、「マッチング画面に進む」が有効化したあと、もう一度前回のセットを押下すると選択を解除し、「マッチング画面に進む」をdisableにする
- リネームモードの選択式ボタンは、選択する項目の中で最も長い項目に合わせて横幅を小さくする。
- 、画面下のリネーム履歴を表示している部分が、3ボタンジェスチャーナビゲーションで隠れてしまうので、ジェスチャーナビゲーションと3ボタンジェスチャーナビゲーションの両方に対応すること。
- singleUri探索で探索しようとしたファイル名とtreeUri探索で見つかったファイル名をくらべ、singleUri探索が正しいかを確認したい。
- treeUri探索の場合、探索範囲を選択したディレクトリ内に絞っていますか？絞ると探索時間がさくげんできるのではないか。
- 自動連番機能はデフォルトでONにしておくこと。
- 自動連番機能に、連番開始番号指定を実装する。画面下のリネーム履歴を表示している部分に「選択中：＜リネーム前ファイル名＞->＜リネーム後ファイル名＞」を表示する部分と左側に、ボタン「変更」を右側に配置する。ボタンを押下するとポップアップダイヤログが出現し、「＜ファイル名の自動連番部より左側＞<数字を入力するテキストボックス><ファイル名の自動連番部より右側>」を表示し、同時に数字を入力するためのテンキーを表示する。テンキーのsubmitを押下するとポップアップが消えて、画面下部に指定した番号が入った状態の「選択中：＜リネーム前ファイル名＞->＜リネーム後ファイル名＞」を表示する。
- 「UNDO」ボタンを作ること。そのために、アプリ起動時からの作業記録を保持すること。ただし、アプリをキルしたらクリアすること。作業記録は、リネーム一件をレコード1つとして最大30件まで保持できる構造とする。より効率のよい実装パターンがあれば提案してください。
- ホーム画面でcsvプレビュー機能をつけたい。「csvプレビュー」ボタンを作り、デフォルトはグレーアウト、タップを無効にする。csvを選択後、ボタンを有効化し色を変える。

# STEP 4-18: UNDO用作業履歴30件保持 Codex回答

## 作業ブランチ

`feature/step4-18-rename-history`

分岐元は `feature/step4-17-auto-number-start` です。

作業開始時点で以下の未コミット差分がありました。

```text
 M doc/STEP4-17_codex.md
AM doc/STEP4-18.md
```

これらはSTEP指示書・前STEP回答のドキュメント差分として扱い、作業ブランチへ持ち越しました。

## 実装内容

- アプリ起動中だけ保持する `RenameHistoryManager` を追加しました。
- リネーム1件を表す `RenameHistoryRecord` を追加しました。
- リネーム成功時だけ履歴を1件追加するようにしました。
- リネーム失敗時は履歴を追加せず、`EasyRenameHistory` ログだけ出すようにしました。
- 履歴は最大30件に制限し、31件目追加時は最古の履歴を削除します。
- 履歴は新しい順で保持します。
- 外部へ返す履歴リストはコピーにし、内部の `ArrayDeque` を直接変更できないようにしました。
- `RenameMatchingUiState` に `canUndo` と `renameHistoryCount` を追加しました。UI表示はまだ変更していません。
- `RenameHistoryManagerTest` を追加し、最大30件制限と最古削除をユニットテストで確認しました。

## 実装コード

### RenameHistoryRecord.kt

追加ファイル:

```text
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryRecord.kt
```

保持する主な情報:

```kotlin
val beforeName: String
val afterName: String
val beforeUri: String
val afterUri: String
val timestampMillis: Long
val renameMode: RenameMode
val directoryUri: String?
val sourceFileIdBefore: String?
val sourceFileIdAfter: String?
val candidateRawPattern: String?
val autoNumber: Int?
```

`beforeUri` / `afterUri` は `Uri` オブジェクトではなく文字列で保持しています。理由は、履歴ManagerをAndroid Frameworkに強く依存させず、ローカルJVMテストで扱いやすくするためです。将来UNDO実行時には必要に応じて `Uri.parse()` で復元できます。

### RenameHistoryManager.kt

追加ファイル:

```text
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryManager.kt
```

主なAPI:

```kotlin
fun add(record: RenameHistoryRecord)
fun getAll(): List<RenameHistoryRecord>
fun getLatest(): RenameHistoryRecord?
fun clear()
fun size(): Int
```

履歴は `ArrayDeque` で保持し、`addFirst()` で最新を先頭に追加します。最大30件を超えた場合は `removeLast()` で最古の履歴を削除します。

`add()` では `EasyRenameHistory` ログを出します。

```text
add success beforeName=... afterName=... historySize=...
beforeUri=...
afterUri=...
renameMode=... autoNumber=...
trim oldest removed beforeName=... afterName=...
```

単体テスト時にAndroidの `Log` へ依存しないよう、loggerはコンストラクタで差し替え可能にしています。

### RenameMatchingViewModel.kt

`RenameHistoryManager` を受け取り、リネーム成功時に履歴を追加します。

履歴追加タイミング:

```text
executeRenameUseCase 成功
→ RenameResult.success == true
→ autoNumber更新
→ RenameHistoryRecord作成
→ RenameHistoryManager.add(record)
→ refreshAfterRename(result)
```

失敗時は履歴を追加しません。

```text
EasyRenameHistory: skip add because rename failed beforeName=... afterName=... errorType=...
```

履歴に入れるURI:

```text
beforeUri = selectedFile.uri.toString()
afterUri = result.afterUri ?: selectedFile.uri
```

SAFではrename後にURIが変わるProviderがあるため、将来UNDOでは `afterUri` が重要になります。

### RenameMatchingUiState.kt

以下を追加しました。

```kotlin
val canUndo: Boolean = false
val renameHistoryCount: Int = 0
```

今回UIには表示していません。次STEPでUNDOボタンを表示する時の状態として使えます。

### AppViewModelFactory.kt

`RenameHistoryManager` をFactoryのcompanion objectで1つだけ保持し、`RenameMatchingViewModel` へ渡すようにしました。

```kotlin
private companion object {
    val renameHistoryManager = RenameHistoryManager()
}
```

これにより、アプリプロセス内では履歴が共有されます。アプリをキルすれば自然にクリアされます。

### RenameHistoryManagerTest.kt

追加ファイル:

```text
app/src/test/java/com/example/easyrename/domain/history/RenameHistoryManagerTest.kt
```

確認内容:

```text
- addで履歴が増える
- getLatestで最新履歴が取れる
- getAllは新しい順で返る
- 31件目追加時に最古が削除される
- clearで履歴が空になる
- getAllはスナップショットとして返る
```

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/domain/history/RenameHistoryRecord.kt`
- `app/src/main/java/com/example/easyrename/domain/history/RenameHistoryManager.kt`
- `app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/test/java/com/example/easyrename/domain/history/RenameHistoryManagerTest.kt`
- `doc/STEP4-18_codex.md`

作業開始時点からのドキュメント差分:

- `doc/STEP4-17_codex.md`
- `doc/STEP4-18.md`

## 変更理由

## 全体アーキテクチャ

既存の MVVM + UseCase 構成を維持しました。

```text
UI層 Fragment
→ ViewModel層
→ Domain history / UseCase層
→ Repository / SAF DataSource層
```

作業履歴はファイルシステムの永続データではなく、アプリ起動中の操作状態です。そのため、RepositoryやDataSourceには入れず、`domain/history` に小さなメモリ管理クラスとして分離しました。

依存方向は、ViewModelが `RenameHistoryManager` に依存する形です。`RenameHistoryManager` はRepositoryやAndroid UIに依存しません。これにより、将来UNDOボタンをMatching画面やHome画面へ追加しても、履歴管理の責務をUIへ漏らさずに済みます。

## 概要

UNDO実行の前段階として、リネーム成功履歴を最大30件までメモリ上に保持する基盤を追加しました。今回のSTEPでは逆リネーム処理やUNDOボタンUIは実装していません。

## 設計詳細

- `RenameHistoryRecord`
  - 責務: 将来UNDOに必要な1件分のリネーム情報を保持します。
  - `beforeName` / `afterName` / `beforeUri` / `afterUri` / `renameMode` は必須情報として保持します。
  - `candidateRawPattern` と `autoNumber` も保持し、自動連番ON/OFFの実機確認に使えるようにします。

- `RenameHistoryManager`
  - 責務: 履歴の追加、最新取得、一覧取得、削除、最大件数制限。
  - `ArrayDeque` を使い、新しい履歴を先頭に追加します。
  - `getAll()` はコピーを返すため、外部から内部状態を破壊できません。

- `RenameMatchingViewModel`
  - 責務: リネーム成功時に履歴レコードを作成し、Managerへ追加します。
  - `RenameResult.success == true` の場合だけ履歴追加します。
  - 失敗時は履歴を追加せず、既存のエラー表示・自動連番カウンタ維持を壊しません。

- `AppViewModelFactory`
  - 責務: アプリプロセス中だけ共有する `RenameHistoryManager` をViewModelへ渡します。
  - SharedPreferencesやDBは使いません。

## 採用理由・根拠

`RenameHistoryManager` を独立クラスにした理由は、履歴管理をViewModel内の単なる `MutableList` にすると、将来Home画面や別画面からUNDO状態を参照しづらくなるためです。Managerへ分離しておくことで、UI追加時の変更範囲を小さくできます。

履歴を永続化しなかった理由は、要件が「アプリをキルしたらクリア」であり、SharedPreferencesやDBを使うと削除タイミングや復元仕様が増えるためです。KISSとYAGNIの観点で、今回の目的にはメモリ保持が合っています。

`ArrayDeque` を採用した理由は、最新を先頭に追加し、上限超過時に末尾の最古履歴を削除する操作に向いているためです。ただし上限30件なので、性能差よりもコードの意図が明確である点を重視しています。

履歴レコードでURIを文字列保持にした理由は、Android `Uri` 型をDomainの履歴テストへ持ち込むとローカルJVMテストが難しくなるためです。将来UNDO実行時にはUI/ViewModel層またはUseCase層で `Uri.parse()` できます。

## 代替案

- `RenameMatchingViewModel` に `MutableList<RenameHistoryRecord>` を直接持つ
  - 有効な条件: 履歴をMatching画面内だけで完結させる場合。
  - 今回採用しない理由: 将来UNDOボタンをHome側に置く可能性があり、ViewModelローカルに閉じると再利用しづらいため。

- SharedPreferencesやDBへ永続化する
  - 有効な条件: アプリ再起動後も履歴復元したい場合。
  - 今回採用しない理由: 要件ではアプリキルで履歴クリアでよく、永続化は仕様過多です。

## 動作確認方法

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

## ビルド確認結果

以下を実行しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

## テスト結果

以下を実行しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 実機確認手順

実機で以下のログを確認しました。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

確認できた内容:

```text
- Matching画面生成時に RenameMatchingViewModel initialized historySize=0 canUndo=false が出る
- 1件目リネーム成功時に add success が出る
- 1件目リネーム成功時に historySize=1 になる
- 2件目リネーム成功時に historySizeBefore=1 から開始する
- 2件目リネーム成功時に historySize=2 になる
- beforeName / afterName が期待どおり
- beforeUri / afterUri が空でない
- renameMode=Prefix が出る
- 自動連番ON時に autoNumber=1, autoNumber=2 が記録される
- singleUri fast path が UnsupportedOperationException になっても、TreeUriFallback 成功後に履歴追加される
```

実機ログ抜粋:

```text
EasyRenameHistory D RenameMatchingViewModel initialized historySize=0 canUndo=false
EasyRenameHistory D executeSelectedRename requested historySizeBefore=0
EasyRenameHistory D rename result received success=true beforeName=A1-3_log (16).zip afterName=A1-1-1_A1-3_log (16).zip ...
EasyRenameHistory D add success beforeName=A1-3_log (16).zip afterName=A1-1-1_A1-3_log (16).zip historySize=1
EasyRenameHistory D renameMode=Prefix autoNumber=1
EasyRenameHistory D executeSelectedRename requested historySizeBefore=1
EasyRenameHistory D add success beforeName=log (20).zip afterName=A1-1-2_log (20).zip historySize=2
EasyRenameHistory D renameMode=Prefix autoNumber=2
```

未確認:

```text
- FileAlreadyExistsなどの失敗時に skip add because rename failed が出て、historySizeが増えないこと
- 自動連番OFF時に autoNumber=null 相当になること
```

## Git操作結果

- 作業ブランチ名: `feature/step4-18-rename-history`
- 分岐元ブランチ: `feature/step4-17-auto-number-start`
- 実機確認前のビルド結果: `assembleDebug` 成功
- テスト結果: `testDebugUnitTest` 成功
- 実機確認結果: 成功時履歴追加はユーザー実機ログで確認済み。`historySize=1` / `historySize=2` と自動連番ON時の `autoNumber=1` / `autoNumber=2` を確認済み。失敗時skipと自動連番OFFログは未確認。
- commit hash: 未作成
- push先ブランチ: 未push
- 未コミット差分: あり

## 実機ログ確認手順

### 1. 端末確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

### 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

### 3. 履歴ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

### 4. 既存ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRename
```

### 5. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 後に回す機能メモ

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

## 未解決事項・リスク

- 今回は履歴保持のみで、UNDO実行は未実装です。
- 履歴はアプリ起動中のみ保持し、アプリキルで消えます。
- SAFではrename後にUriが変わるProviderがあるため、UNDO実装時はafterUriを使う必要があります。
- 履歴に保持したURI文字列がProvider都合で無効になる可能性は残ります。
- 30件上限はメモリ使用量と実装単純性を優先した暫定仕様です。
- UI上のUNDOボタン表示は次STEPで実装します。

## 次に進めるべきSTEP

実機確認OK後に以下を実行します。

```powershell
git status
git add .
git commit -m "Add in-memory rename history"
git push -u origin feature/step4-18-rename-history
```

次の候補:

```text
1. UNDOボタンのUI追加
2. UNDO押下時の逆リネーム実行
3. CSVプレビュー
4. Spinner横幅調整
```
