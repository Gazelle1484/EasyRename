````md id="step4-9-prompt"
# STEP 4-9: 実装（機能単位）用 Codex 指示

STEP4-8では、リネームモード `Prefix` / `Suffix` / `Replace` の追加、Home画面のモード選択、リストの辞書順ソート、CSV MIME type指定の改善が実装されています。実機確認では、Prefix / Suffix / Replace の全モードでリネーム成功が確認されています。:contentReference[oaicite:0]{index=0}

一方で、STEP4-8では `FileNotFound` 対策として、リネーム成功後にディレクトリ全体を再読み込みする暫定処理が追加されました。その結果、全モードで動作が遅くなっています。:contentReference[oaicite:1]{index=1}

このSTEPでは、**自動連番機能には進まず、まずリネーム成功後の性能改善のみ**を対象にしてください。

---

## 目的

- 1機能ずつ確実に完成させる
- リネーム成功後の全件再読み込みをやめる
- 成功した1件だけを状態更新する
- SAFの `DocumentFile.listFiles()` 再走査を最小化する
- 既存のPrefix / Suffix / Replaceモードを壊さない
- 既存の1件リネーム成功フローを維持する
- ビルドが通る状態を維持する

---

## Gitブランチ運用

機能追加に合わせて、このSTEPの作業は必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell id="git-start"
git status
git checkout main
git pull
git checkout -b feature/step4-9-rename-performance
````

### 作業後に実行

ビルドとテスト成功後、以下を実行してください。

```powershell id="git-finish"
git status
git add .
git commit -m "Optimize post-rename state update"
git push -u origin feature/step4-9-rename-performance
```

### 注意

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分を確認する
* 未コミット差分がある場合は、内容を報告してから作業する
* 作業後はcommitとpushまで行う
* pushに失敗した場合は、エラー内容を報告する

---

## 現状の問題

STEP4-8時点では、リネーム成功後に以下のような暫定対策が入っています。

```text id="current-problem"
リネーム成功
→ HomeViewModel.refreshSelectedDirectoryFiles()
→ StorageRepository.loadFilesInDirectory()
→ SafDocumentDataSource.loadFilesInDirectory()
→ DocumentFile.listFiles()
→ ディレクトリ全体を再走査
```

この処理により、次回リネーム時に古いURIを使って `FileNotFound` になる問題は回避できています。
ただし、毎回ディレクトリ全体を再読み込みするため、ファイル数が増えるほど動作が遅くなります。

---

## 今回やること / やらないこと

### 今回やること

```text id="do-this-step"
1. リネーム成功後の全件再読み込みをやめる
2. 成功した1件だけHome側のtargetFilesを更新する
3. Matching側のtargetFilesも成功した1件だけ更新する
4. 新しいファイル名・新しいURIを状態に反映する
5. 必要に応じてStorageRepository.renameFileの戻り値を拡張する
6. Prefix / Suffix / Replaceの既存動作を維持する
7. 既存CSV形式 A1-1_* の互換動作を維持する
```

### 今回やらないこと

```text id="not-this-step"
1. 自動連番機能
2. 一括リネーム
3. RecyclerView化
4. XMLレイアウト化
5. Material Componentsへの本格移行
6. Edge-to-Edge / WindowInsetsの正式対応
7. 画面回転時の完全な状態復元
8. 履歴・取り消し機能
9. 独自ファイルピッカー
10. SAFピッカー内の並び順制御
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text id="scope-files"
app/src/main/java/com/example/easyrename/model/RenameResult.kt
app/src/main/java/com/example/easyrename/model/RenameTargetFile.kt
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt
app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt
app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
```

必要に応じて以下も変更して構いません。

```text id="optional-files"
app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt
app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt
```

---

## 禁止事項

* 自動連番を今回実装しない
* 一括リネームを追加しない
* RenameModeの仕様を変えない
* CSV仕様を変えない
* SAF / Repository層を大規模に作り直さない
* UIを大規模に変更しない
* RecyclerView化しない
* Composeへ移行しない
* DIライブラリを追加しない
* リネーム成功済みフローを壊さない

---

# STEP 4-9-1: 現在の全件再読み込み箇所を特定して削除する

## 目的

遅さの主因である、リネーム成功後のディレクトリ全体再読み込みを停止する。

## 対象

* `RenameMatchingFragment`
* `HomeViewModel`
* 必要に応じて関連箇所

## 実装内容

* リネーム成功後に `HomeViewModel.refreshSelectedDirectoryFiles()` を毎回呼んでいる箇所を特定する
* この自動呼び出しを削除する
* `refreshSelectedDirectoryFiles()` 自体は、将来の手動更新用に残してもよい
* 残す場合は、自動では呼ばれないようにする

## 注意

* ただ削除するだけだと、古いURI問題が再発する可能性がある
* 次のSTEP 4-9-2以降で、成功した1件だけ状態更新する

---

# STEP 4-9-2: RenameResultに新しいURI情報を持たせる

## 目的

リネーム成功後、全件再読み込みせずに対象ファイルの状態を更新できるようにする。

## 対象

* `RenameResult`
* `SafDocumentDataSource.renameFile`
* `StorageRepository`
* `StorageRepositoryImpl`
* `ExecuteRenameUseCase`

## 実装内容

`RenameResult` に、成功後の新しいファイルURIを持たせることを検討してください。

推奨:

```kotlin id="rename-result-new-uri"
data class RenameResult(
    val beforeName: String,
    val afterName: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val errorType: RenameErrorType? = null,
    val afterUri: Uri? = null,
)
```

### `afterUri` の設定方針

* `renameTo(newName)` 成功後、同じ `DocumentFile` から取得できる `uri` を `afterUri` として返す
* Providerによって `renameTo` 後のURIが変わらない場合もあるため、その場合は既存URIでもよい
* `afterUri` が取得できない場合は `null` でもよい
* `afterUri == null` の場合はViewModel側で元URIを維持する

## 注意

* 既存の `RenameResult` 呼び出し箇所が壊れないよう、デフォルト値を設定する
* 失敗時は `afterUri = null` でよい

---

# STEP 4-9-3: 成功した1件だけMatching側の状態を更新する

## 目的

リネーム後、Matching画面の一覧を全件再読み込みせず正しく更新する。

## 対象

* `RenameMatchingViewModel`

## 実装内容

リネーム成功時に、対象ファイルだけを以下のように更新してください。

```text id="matching-state-update"
対象ファイル:
- displayName = result.afterName
- uri = result.afterUri ?: 既存uri
- isSelected = false
- isRenamed = true

使用した候補:
- isSelected = false
- isUsed = true

その他:
- selectedTargetFileId = null
- selectedCandidateId = null
- canExecuteRename = false
- lastResult = result
- error = null
```

## 注意

* ファイル一覧全体の再読み込みは禁止
* リネーム済み表示は維持する
* 辞書順ソートを維持するかどうかは、以下の方針にしてください

### ソート方針

リネーム成功後は以下のどちらかを採用してください。

#### ベスト案

更新後に `displayName.lowercase()` で再ソートする。

#### 代替案

選択中の画面内では位置を維持し、Homeに戻るまで再ソートしない。

### 採用基準

* 実装が小さく安全ならベスト案
* 選択状態更新が複雑になるなら代替案

---

# STEP 4-9-4: 成功した1件だけHome側の状態を更新する

## 目的

Homeに戻って再度Matching画面へ進んだときに、古いURIを使わないようにする。

## 対象

* `HomeViewModel`
* `HomeUiState`
* `RenameMatchingFragment`

## 実装内容

Home側に、リネーム成功結果を反映するメソッドを追加してください。

例:

```kotlin id="home-apply-result"
fun applyRenameResult(result: RenameResult, sourceFileId: String)
```

このメソッドで、`targetFiles` の該当1件だけ更新してください。

```text id="home-state-update"
対象ファイル:
- displayName = result.afterName
- uri = result.afterUri ?: 既存uri
- isSelected = false
- isRenamed = true
```

### 呼び出し元

`RenameMatchingFragment` から、リネーム成功時に1回だけ呼び出してください。

既存の `lastSyncedSuccessResult` ガードがある場合は、それを利用して重複呼び出しを防いでください。

## 注意

* `refreshSelectedDirectoryFiles()` を毎回呼ばない
* `targetFileCount` は変えなくてよい
* リネーム成功後も辞書順が必要であれば、該当1件更新後にソートしてよい
* `HomeViewModel` がRepositoryを再呼び出ししないようにする

---

# STEP 4-9-5: FileNotFound再発防止ログを追加する

## 目的

全件再読み込みをやめても、古いURI問題が再発していないか確認できるようにする。

## 対象

* `SafDocumentDataSource.renameFile`
* `RenameMatchingViewModel`
* `HomeViewModel`

## 実装内容

必要最小限で、以下を `EasyRename` タグに出してください。

```text id="log-items"
- beforeName
- afterName
- sourceFile.id
- sourceFile.uri
- result.afterUri
- Home側targetFiles更新後の対象uri
- Matching側targetFiles更新後の対象uri
- FileNotFound発生時のdirectoryUri / fileUri / beforeName
```

## 注意

* ログはデバッグ目的の最小限にする
* 既存のログが十分なら追加しすぎない

---

# STEP 4-9-6: 必要ならファイルI/OをDispatchers.IOへ逃がす検討をする

## 目的

SAFアクセスやディレクトリ走査がUIスレッドを塞がないようにする。

## 対象

* `HomeViewModel`
* `RenameMatchingViewModel`

## 実装方針

今回の主目的は全件再読み込み削除です。
そのため、`Dispatchers.IO` 対応は以下の条件を満たす場合のみ実装してください。

```text id="io-condition"
- 変更範囲が小さい
- 既存StateFlow構成を壊さない
- ビルドが通る見込みが高い
- viewModelScope が既に利用可能または依存追加不要
```

## 実装する場合

* `onDirectorySelected`
* `onCsvSelected`
* `executeSelectedRename`

などのI/Oを含む処理を `viewModelScope.launch(Dispatchers.IO)` に移す

UI State更新は安全に行う。

## 実装しない場合

以下を未解決事項として報告してください。

```text id="io-deferred"
ViewModel内のSAFアクセスはまだ同期実行のため、ファイル数が多い場合はUIが重くなる可能性がある。後続STEPでDispatchers.IO対応を行う。
```

---

## 実装ルール

* 今回は「リネーム成功後の性能改善」だけに集中する
* 自動連番は実装しない
* 既存のリネームモード仕様を変更しない
* 既存CSV互換を壊さない
* UI変更は必要最小限にする
* SAFの再走査を毎回行わない
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
- ViewModel内I/Oの本格的なDispatchers.IO対応
- 手動更新ボタン
- リネーム成功後の明示的な再読み込み
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
# STEP 4-9: リネーム後状態更新の性能改善 Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### RenameResult.kt

### SafDocumentDataSource.kt

### StorageRepository.kt

### StorageRepositoryImpl.kt

### ExecuteRenameUseCase.kt

### HomeViewModel.kt

### RenameMatchingViewModel.kt

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
2. Home画面でリネームモードをPrefixにする
3. リネーム対象ディレクトリを選択する
4. CSVファイルを選択する
5. マッチング画面へ進む
6. ファイルを1件選択する
7. 候補を1件選択する
8. リネーム実行する
9. 成功後、動作がSTEP4-8より軽くなっていることを確認する
10. 成功後、リネーム済み表示になることを確認する
11. Homeへ戻る
12. 再度マッチング画面へ進む
13. 直前にリネームしたファイルが新しい名前・新しいURI相当で扱われることを確認する
14. Prefix / Suffix / Replaceで連続リネームしてFileNotFoundが再発しないことを確認する
15. 既存CSV形式 A1-1_* でも従来どおり動くことを確認する
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

今回の主目的は、リネーム成功後に全件再読み込みしなくても古いURI問題が再発しないことの確認です。

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

### 4. クラッシュ確認

```powershell id="adb-crash-log"
adb logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 5. 確認する内容

```text id="log-check-items"
- beforeName
- afterName
- sourceFile.uri
- result.afterUri
- Home側targetFiles更新後のuri
- Matching側targetFiles更新後のuri
- renameTo result=true が維持されているか
- FileNotFound が出ていないか
- リネーム成功後に loadFilesInDirectory / listFiles が毎回走っていないか
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

* 作業ブランチ `feature/step4-9-rename-performance` で作業している
* 作業後にcommitしてpushしている
* リネーム成功後に毎回ディレクトリ全体再読み込みをしない
* 成功した1件だけHome側の状態が更新される
* 成功した1件だけMatching側の状態が更新される
* Prefix / Suffix / Replace の既存動作が壊れていない
* 既存CSV形式 `A1-1_*` が従来どおり動く
* Prefix / Suffix / Replace の連続リネームで `FileNotFound` が再発しない
* STEP4-8よりリネーム後の体感待ち時間が短くなっている
* ビルドが通る

```
```
