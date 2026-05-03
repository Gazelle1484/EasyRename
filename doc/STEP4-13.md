````md id="step4-13-prompt"
# STEP 4-13: 実装（機能単位）用 Codex 指示

STEP4-12では、`singleUri` fast path + `treeUri` fallback が実装されました。実機検証では、`singleUri fast path` は毎回 `UnsupportedOperation` で失敗したものの、`treeUri fallback` によりリネーム自体は成功しています。代表ログでは `singleUri` 失敗は12〜20ms程度で、主な待ち時間は `saf resolve target` の約886〜936msでした。:contentReference[oaicite:0]{index=0}

このSTEPでは、次の段階として **SAF処理の非同期化、リネーム中表示、二重押下防止の強化** のみを対象にしてください。  
`treeUri` 探索自体の根本最適化やキャッシュ設計は、後続STEPへ回してください。
回答はSTEP4-13_codex.mdに保存すること。
---

## 目的

- 1機能ずつ確実に完成させる
- 重いSAF処理でUIスレッドを止めない
- リネーム中であることをユーザーに明示する
- リネームボタンの二重押下を確実に防ぐ
- `treeUri fallback` 前提でも体感上のフリーズを減らす
- Prefix / Suffix / Replace の既存挙動を壊さない
- 既存CSV形式 `A1-1_*` の互換動作を維持する
- ビルドが通る状態を維持する

---

## Gitブランチ運用

機能追加に合わせて、このSTEPの作業は必ず新しいブランチで行ってください。

### 作業開始前に実行

```powershell id="git-start"
git status
git checkout main
git pull
git checkout -b feature/step4-13-rename-async-loading
````

### 注意

STEP4-13はSTEP4-12の実装を前提にします。
`main` にSTEP4-12までの変更が入っていない場合は、作業に必要な最新ブランチを確認し、どのブランチから分岐するべきかを報告してください。

必要であれば、以下のようにSTEP4-12ブランチから分岐してください。

```powershell id="git-alt-start"
git checkout feature/step4-12-single-uri-fast-path
git pull
git checkout -b feature/step4-13-rename-async-loading
```

### 作業後に実行

ビルドとテスト成功後、以下を実行してください。

```powershell id="git-finish"
git status
git add .
git commit -m "Run rename work asynchronously with loading state"
git push -u origin feature/step4-13-rename-async-loading
```

### Git注意事項

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分を確認する
* 未コミット差分がある場合は、内容を報告してから作業する
* 作業後はcommitとpushまで行う
* pushに失敗した場合は、エラー内容を報告する

---

## 実機検証結果

```text id="device-result"
- singleUri fast path は毎回試行されている
- singleUri fast path は毎回 UnsupportedOperation で失敗している
- 失敗後、treeUri fallback でリネーム成功している
- アプリはクラッシュしていない
- saf resolve target が約886〜936msかかっている
- rename total は約1082〜1581ms程度
- singleUri失敗自体は12〜20ms程度で主因ではない
```

---

## 現状の問題

STEP4-12後も、この実機環境では `singleUri` が使えず、毎回 `treeUri fallback` になります。

```text id="current-bottleneck"
singleUri fast path
→ UnsupportedOperation
→ treeUri fallback
→ directory.listFiles()
→ 対象ファイル探索
→ renameTo
```

`treeUri fallback` の `listFiles()` が残るため、1件リネームに1秒前後かかります。
ただし、このSTEPでは `listFiles()` のキャッシュ化や探索方式の根本変更は行わず、**UIスレッドを止めないこと、処理中表示を出すこと、二重押下を防ぐこと**を優先してください。

---

## 今回やること / やらないこと

### 今回やること

```text id="do-this-step"
1. RenameMatchingViewModel.executeSelectedRename を viewModelScope + Dispatchers.IO 対応にする
2. 重いSAF処理をUIスレッドから外す
3. リネーム中は isExecuting = true にする
4. リネーム中は実行ボタンを無効化する
5. リネーム中の表示を分かりやすくする
6. 二重押下を確実に防ぐ
7. 成功 / 失敗後は isExecuting = false に戻す
8. 既存の EasyRenamePerf ログを維持する
```

### 今回やらないこと

```text id="not-this-step"
1. 自動連番機能
2. 一括リネーム
3. RenameMode仕様変更
4. CSV仕様変更
5. singleUri fast pathのON/OFF設定
6. Provider別fast pathスキップ
7. directory.listFiles() のキャッシュ化
8. 対象ファイル情報のキャッシュ設計
9. 独自ファイルピッカー
10. RecyclerView化
11. XMLレイアウト化
12. Material Componentsへの本格移行
13. Edge-to-Edge / WindowInsets正式対応
14. 画面回転時の完全な状態復元
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text id="scope-files"
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt
app/src/main/java/com/example/easyrename/ui/common/LoadingView.kt
```

必要に応じて以下も変更して構いません。

```text id="optional-files"
app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt
app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt
app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt
app/src/main/java/com/example/easyrename/ui/common/ErrorDialog.kt
```

---

## 禁止事項

* `singleUri` 方式だけに戻さない
* `treeUri fallback` を削除しない
* `UnsupportedOperationException` をクラッシュさせない
* `FileAlreadyExists` の検出を削除しない
* 自動連番を今回実装しない
* 一括リネームを追加しない
* Prefix / Suffix / Replace の仕様を変更しない
* 既存CSV形式 `A1-1_*` の挙動を変えない
* リネーム後の状態更新方式を全件再読み込みへ戻さない
* UIを大規模に変更しない
* DIライブラリを追加しない

---

# STEP 4-13-1: RenameMatchingViewModelを非同期化する

## 目的

リネーム処理中にUIスレッドを止めないようにする。

## 対象

* `RenameMatchingViewModel.executeSelectedRename`

## 実装内容

`executeSelectedRename()` を `viewModelScope.launch` + `withContext(Dispatchers.IO)` で実装してください。

推奨構成:

```kotlin id="async-rename-example"
fun executeSelectedRename() {
    if (_uiState.value.isExecuting) return

    viewModelScope.launch {
        _uiState.update {
            it.copy(isExecuting = true, error = null)
        }

        val result = withContext(Dispatchers.IO) {
            // 選択ファイル取得
            // 選択候補取得
            // ResolveRenameNameUseCase
            // RenamePair作成
            // ExecuteRenameUseCase呼び出し
        }

        // メイン側でStateFlow更新
        // 成功/失敗反映
        // isExecuting = false
    }
}
```

## 注意

* `viewModelScope` を使う
* `Dispatchers.IO` を使う
* `isExecuting == true` の場合は何もしない
* 例外発生時も必ず `isExecuting = false` に戻す
* 成功後1件更新方式は維持する
* `lastResult` / `error` の既存挙動を維持する

---

# STEP 4-13-2: リネーム中のUI状態を明確化する

## 目的

1秒前後の待ち時間中、ユーザーに処理中であることを示す。

## 対象

* `RenameMatchingUiState`
* `RenameMatchingFragment`
* 必要に応じて `LoadingView`

## 実装内容

既存の `isExecuting` を使い、以下を実装してください。

```text id="executing-ui"
- isExecuting = true の間、リネーム実行ボタンを無効化する
- isExecuting = true の間、ボタン文言を「リネーム中...」などに変更する
- 可能であれば ProgressBar または LoadingView を表示する
- isExecuting = false になったら通常表示に戻す
```

## UI表示例

```text id="ui-example"
通常:
[リネーム実行]

実行中:
[リネーム中...]
ProgressBar表示
```

## 注意

* 大規模なUI刷新はしない
* 現在のプログラムmatic View構成を維持する
* リネーム結果表示は維持する
* Matching画面の左右2分割は維持する

---

# STEP 4-13-3: 二重押下防止を強化する

## 目的

リネーム中に同じ処理が複数回走らないようにする。

## 対象

* `RenameMatchingViewModel`
* `RenameMatchingFragment`

## 実装内容

以下の両方で防御してください。

```text id="double-click-guard"
1. ViewModel側:
   - executeSelectedRename開始時に isExecuting を確認
   - trueならreturn

2. UI側:
   - state.isExecuting == true の間、実行ボタンを disabled にする
```

## 注意

* UI側だけに頼らない
* ViewModel側でも必ずガードする
* 失敗時にも `isExecuting = false` に戻ること

---

# STEP 4-13-4: HomeViewModelの読み込み処理は必要最小限で扱う

## 目的

今回の主目的から外れないようにする。

## 対象

* `HomeViewModel`

## 方針

このSTEPの主目的はリネーム処理の非同期化です。
Home画面のディレクトリ読み込み / CSV読み込みの非同期化は、変更範囲が小さく安全な場合のみ実施してください。

## 実装してよい条件

```text id="home-async-condition"
- 変更範囲が小さい
- 既存StateFlow構成を壊さない
- isLoadingの挙動を壊さない
- ビルドが通る見込みが高い
```

## 実装しない場合

未解決事項として以下を残してください。

```text id="home-async-deferred"
HomeViewModelのディレクトリ読み込み / CSV読み込みは、後続STEPでDispatchers.IO対応する。
```

---

# STEP 4-13-5: 性能ログを維持する

## 目的

非同期化後も処理時間を確認できるようにする。

## 対象

* `RenameMatchingViewModel`
* `ExecuteRenameUseCase`
* `SafDocumentDataSource`

## 維持するログ

```text id="keep-logs"
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=... path=...
EasyRenamePerf: repository rename end elapsedMs=... path=...
EasyRenamePerf: singleUri fast path failed reason=... elapsedMs=...
EasyRenamePerf: treeUri fallback start
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=... path=...
```

## 追加してよいログ

```text id="new-logs"
EasyRenamePerf: viewModel coroutine start
EasyRenamePerf: viewModel withContext(IO) start
EasyRenamePerf: viewModel withContext(IO) end elapsedMs=...
EasyRenamePerf: viewModel state update after IO elapsedMs=...
```

## 注意

* ログを増やしすぎない
* 実機確認後に不要ログを減らせるよう、タグは `EasyRenamePerf` に統一する

---

# STEP 4-13-6: エラー時のisExecuting解除を保証する

## 目的

エラー時にリネームボタンが永久に無効化されないようにする。

## 対象

* `RenameMatchingViewModel.executeSelectedRename`

## 実装内容

以下の失敗ケースでも `isExecuting = false` に戻ることを保証してください。

```text id="failure-cases"
- 選択ファイルなし
- 選択候補なし
- directoryUriなし
- 不正ファイル名
- FileAlreadyExists
- UnsupportedOperation
- FileNotFound
- SecurityException
- その他Exception
```

## 注意

* `try/catch/finally` または `runCatching` を適切に使う
* 成功時も失敗時もUI Stateが一貫するようにする
* 失敗時は対象ファイルを `isRenamed = true` にしない
* 失敗時は候補を `isUsed = true` にしない

---

## 実装ルール

* 今回は「非同期化・処理中表示・二重押下防止」だけに集中する
* 自動連番は実装しない
* `treeUri fallback` は維持する
* 成功後1件更新方式を維持する
* Prefix / Suffix / Replaceの仕様を変更しない
* 既存CSV互換を壊さない
* `FileAlreadyExists` の保護を維持する
* UI変更は必要最小限にする
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
- HomeViewModelのディレクトリ読み込み / CSV読み込みの本格的なDispatchers.IO対応
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
# STEP 4-13: リネーム非同期化と実行中UI改善 Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### RenameMatchingViewModel.kt

### RenameMatchingFragment.kt

### RenameMatchingUiState.kt

### LoadingView.kt

### HomeViewModel.kt

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
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. Prefixモードでマッチング画面へ進む
5. ファイルを1件選択する
6. 候補を1件選択する
7. リネーム実行ボタンを押す
8. 実行中にボタンが無効化されることを確認する
9. 実行中に「リネーム中...」などの表示が出ることを確認する
10. 実行中に連打しても二重実行されないことを確認する
11. リネーム成功後、ボタンが通常状態に戻ることを確認する
12. リネーム成功後、対象ファイルがリネーム済み表示になることを確認する
13. Suffixモードで1件リネームする
14. Replaceモードで1件リネームする
15. Prefix / Suffix / Replaceの連続リネームでFileNotFoundが再発しないことを確認する
16. FileAlreadyExists失敗時もボタンが通常状態に戻ることを確認する
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

今回の主目的は、重いSAF処理が非同期化され、UIが固まりにくくなったかを確認することです。

### 1. 端末確認

```powershell id="adb-devices"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

### 2. ログクリア

```powershell id="adb-clear"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

### 3. 性能ログ確認

```powershell id="adb-perf-log"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

### 4. クラッシュ確認

```powershell id="adb-crash-log"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 5. 最低3回分控えるログ

```text id="minimum-logs"
EasyRenamePerf: viewModel coroutine start
EasyRenamePerf: viewModel withContext(IO) start
EasyRenamePerf: viewModel withContext(IO) end elapsedMs=...
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=... path=...
EasyRenamePerf: repository rename end elapsedMs=... path=...
EasyRenamePerf: saf renameFile end elapsedMs=... path=...
EasyRenamePerf: viewModel state update after IO elapsedMs=...
```

### 6. 比較観点

```text id="compare-points"
- リネーム総時間そのものはProvider依存で大きく変わらない可能性がある
- UIが固まらず、実行中表示が出るか
- 実行中に二重押下されないか
- isExecuting が成功/失敗後に false へ戻るか
- FileNotFound が再発していないか
- path=TreeUriFallback でも成功しているか
```

---

## Git操作結果として報告してほしい内容

```text id="git-report"
- 作業開始時のgit status
- 作成したブランチ名
- 分岐元ブランチ
- commit hash
- push先ブランチ
- 未コミット差分の有無
```

---

## 完了条件

* 作業ブランチ `feature/step4-13-rename-async-loading` で作業している
* 作業後にcommitしてpushしている
* `RenameMatchingViewModel.executeSelectedRename` が `viewModelScope` + `Dispatchers.IO` 対応になっている
* リネーム中に実行ボタンが無効化される
* リネーム中表示が出る
* 二重押下しても二重実行されない
* 成功時も失敗時も `isExecuting = false` に戻る
* `treeUri fallback` が維持されている
* `UnsupportedOperationException` でクラッシュしない
* `FileAlreadyExists` の保護が維持されている
* 成功後1件更新方式が維持されている
* Prefix / Suffix / Replace の既存動作が壊れていない
* 既存CSV形式 `A1-1_*` が従来どおり動く
* ビルドが通る

```
```
