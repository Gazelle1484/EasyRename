実機検証結果と、codexの回答から、STEP4-14のプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-14: 実装（機能単位）
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

###### 追加の要望
- （可能であれば）ディレクトリを選択する際、変更日時が新しい順にあらかじめソートし、ディレクトリがトップに来るようにしてほしい
- （可能であれば）csvを選択する際、変更日時が新しい順にあらかじめソートし、csvがトップに来るようにしてほしい。または、csvのみが表示されるようにしてほしい
- マッチング画面では、リネーム対象/リネーム候補それぞれが辞書順にソートされてほしい。
- リネーム前ファイルがリネームするとリネーム後になるのは維持してほしい。
- 自動連番機能をつけたい。これは実装例です。「画面上部に自動連番ボタンを追加し、自動連番ボタンを押下すると自動連番モードになる。自動連番モードでリネーム候補とリネーム前ファイルを押下すると、リネーム候補の末尾に「-1」のようにつけて扱う。つまり、logs1223.txtとlogs1226.txtをそれぞれ「A1-1」の先頭モードで、この順にリネームすると「A1-1-1_logs1223.txt」,「A1-1-2_logs1226.txt」になる。末尾モードであれば「logs1223_A1-1-1.txt」, 「logs1226_A1-1-2.txt」となる。

# STEP 4-13: リネーム非同期化と実行中UI改善 Codex回答

## 作業ブランチ

`feature/step4-13-rename-async-loading`

STEP4-13はSTEP4-12の実装を前提にするため、`main` ではなく `feature/step4-12-single-uri-fast-path` から分岐した。

作業開始時点で未コミット差分があった。

- `doc/STEP4-12_codex.md`: STEP4-12実機ログ分析の追記
- `doc/STEP4-13.md`: STEP4-13指示ファイルの追加

## 実装内容

- `RenameMatchingViewModel.executeSelectedRename()` を `viewModelScope.launch` + `withContext(Dispatchers.IO)` に変更した。
- SAF / Repository / UseCase を含む重いリネーム処理を `Dispatchers.IO` 上で実行するようにした。
- `isExecuting == true` の場合、ViewModel側で二重実行を即returnするガードを追加した。
- リネーム中はファイル選択・候補選択もViewModel側で無視するようにした。
- UI側ではリネーム中に実行ボタンを無効化し、文言を「リネーム中...」へ変更するようにした。
- リネーム中はファイル一覧・候補一覧のボタンも無効化するようにした。
- 既存の `LoadingView` を `isExecuting` と連動させ、処理中にProgressBarを表示するようにした。
- 成功 / 失敗 / 例外 / 選択不足の各ケースで `isExecuting = false` に戻る既存挙動を維持した。
- Prefix / Suffix / Replace、CSV仕様、`treeUri fallback`、成功後1件更新方式は変更していない。

## 実装コード

### RenameMatchingViewModel.kt

`viewModelScope` と `Dispatchers.IO` を使い、重い処理をUIスレッドから外した。

```kotlin
viewModelScope.launch {
    _uiState.update { state ->
        state.copy(isExecuting = true, error = null)
    }

    runCatching {
        withContext(Dispatchers.IO) {
            executeRenameUseCase(renamePair)
        }
    }.onSuccess { result ->
        refreshAfterRename(result)
    }.onFailure { throwable ->
        _uiState.update { state ->
            state.copy(isExecuting = false, error = AppError.Unknown(...))
        }
    }
}
```

二重押下防止として、実行開始時に `isExecuting` を確認する。

```kotlin
if (_uiState.value.isExecuting) {
    Log.d(TAG_PERF, "viewModel rename ignored because already executing")
    return
}
```

選択中の変更による状態不整合を避けるため、リネーム実行中のファイル選択・候補選択も無視する。

```kotlin
fun selectTargetFile(fileId: String) {
    if (_uiState.value.isExecuting) return
    ...
}

fun selectRenameCandidate(candidateId: String) {
    if (_uiState.value.isExecuting) return
    ...
}
```

追加した性能ログ。

```text
EasyRenamePerf: viewModel coroutine start
EasyRenamePerf: viewModel withContext(IO) start
EasyRenamePerf: viewModel withContext(IO) end elapsedMs=...
EasyRenamePerf: viewModel state update after IO elapsedMs=...
```

### RenameMatchingFragment.kt

`isExecuting` に応じて、実行ボタンの無効化と文言変更を行う。

```kotlin
executeButton.isEnabled = state.canExecuteRename && !state.isExecuting
executeButton.text = if (state.isExecuting) {
    "リネーム中..."
} else {
    "リネーム実行"
}
```

処理中表示は既存の `LoadingView` を利用した。

```kotlin
loadingView.setLoading(state.isExecuting)
```

処理中は一覧側の選択ボタンも無効化する。

```kotlin
renderFiles(state.targetFiles, state.isExecuting)
renderCandidates(state.renameCandidates, state.isExecuting)
```

### RenameMatchingUiState.kt

変更なし。

既存の `isExecuting` をそのまま利用した。新しい状態プロパティは追加していない。

### LoadingView.kt

変更なし。

既存の `ProgressBar` 表示を `RenameMatchingFragment` から `isExecuting` で切り替える構成を維持した。

### HomeViewModel.kt

変更なし。

今回の対象はリネーム実行処理の非同期化であり、Home画面のディレクトリ読み込み / CSV読み込みの本格的な `Dispatchers.IO` 対応は後続STEPへ回した。

### その他変更ファイル

- `doc/STEP4-12_codex.md`
  - STEP4-12実機ログ分析結果を追記済み。
- `doc/STEP4-13.md`
  - STEP4-13指示ファイル。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `doc/STEP4-12_codex.md`
- `doc/STEP4-13.md`
- `doc/STEP4-13_codex.md`

## 変更理由

## 全体アーキテクチャ

既存の MVVM + UseCase + Repository + SAF DataSource 構成を維持した。

```text
UI層
→ ViewModel層
→ UseCase層
→ Repository層
→ SAF DataSource層
→ Android SAF / DocumentProvider
```

今回の問題は、`treeUri fallback` の `listFiles()` と `renameTo()` が1秒前後かかり、その処理がUIスレッド上で実行されていたことである。根本的なI/O時間はProvider依存だが、UIスレッドから外すことで、画面が固まって見える体感問題を減らせる。

## 概要

リネーム実行の業務手順は引き続き `ExecuteRenameUseCase` に置き、ViewModelは「いつ非同期で呼び出すか」「実行中状態をどうUIに反映するか」を担当する。SAFの具体処理はDataSource層に閉じ込めたままにしている。

## 設計詳細

- `RenameMatchingViewModel`
  - 責務: 画面状態管理、選択状態管理、リネーム実行の非同期起動、成功 / 失敗結果の反映。
  - `viewModelScope.launch`: 画面ライフサイクルに合わせてCoroutineを管理する。
  - `withContext(Dispatchers.IO)`: SAF処理をUIスレッドから外す。
  - `isExecuting` ガード: 二重実行を防ぐ。

- `RenameMatchingFragment`
  - 責務: StateFlowをUIへ反映する。
  - `isExecuting` に応じてボタン無効化、文言変更、LoadingView表示を行う。

- `ExecuteRenameUseCase`
  - 責務: ファイル名検証、Repository呼び出し、`sourceFileId` 付与。
  - 非同期化の詳細は持たせず、呼び出し側のViewModelでスレッドを切り替える。

## 採用理由・根拠

`Dispatchers.IO` をViewModel側で使った理由は、今回の変更範囲を小さく保ちながら、UIスレッド停止を避けるためである。RepositoryやDataSourceをsuspend化する案もあるが、インターフェース変更が広がりやすく、STEP4-13の「非同期化・処理中表示・二重押下防止」に対しては過剰になる。

ViewModel側とUI側の両方で二重押下を防いだ理由は、防御層を分けるためである。UI側のdisabledはユーザー操作を防ぐため、ViewModel側の `isExecuting` ガードは連打や状態反映遅延、将来的な別UI経路からの呼び出しを防ぐために置いている。

この設計はSRPに沿っている。Fragmentは表示、ViewModelは状態と非同期実行、UseCaseは業務手順、DataSourceはSAF依存処理を担当する。依存方向を変えていないため、既存のテスト・保守構造を壊しにくい。

## 代替案

- Repository / DataSourceのメソッドをすべて `suspend` 化する
  - 有効な条件: Storage処理全体をCoroutine前提で再設計する場合。
  - 採用しない理由: インターフェース変更範囲が広がり、今回の単機能STEPとしては大きすぎるため。

- Fragment側で `lifecycleScope.launch(Dispatchers.IO)` を使う
  - 有効な条件: ViewModelを使わない小規模画面の場合。
  - 採用しない理由: EasyRenameではViewModelが状態管理を担当しているため、Fragmentに業務実行の詳細を寄せると責務が混ざるため。

- `directory.listFiles()` のキャッシュ化を同時に行う
  - 有効な条件: 連続リネームの整合性、外部変更、同名チェックの鮮度まで設計できる場合。
  - 採用しない理由: 今回はUIスレッド停止対策が主目的であり、キャッシュは別STEPで検証すべきため。

## 動作確認方法

```text
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. Prefixモードでマッチング画面へ進む
5. ファイルを1件選択する
6. 候補を1件選択する
7. リネーム実行ボタンを押す
8. 実行中にボタンが無効化されることを確認する
9. 実行中に「リネーム中...」表示とProgressBarが出ることを確認する
10. 実行中に連打しても二重実行されないことを確認する
11. リネーム成功後、ボタンが通常状態に戻ることを確認する
12. リネーム成功後、対象ファイルがリネーム済み表示になることを確認する
13. Suffixモードで1件リネームする
14. Replaceモードで1件リネームする
15. Prefix / Suffix / Replaceの連続リネームでFileNotFoundが再発しないことを確認する
16. FileAlreadyExists失敗時もボタンが通常状態に戻ることを確認する
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

- 作業開始時のgit status: `feature/step4-12-single-uri-fast-path...origin/feature/step4-12-single-uri-fast-path`、`doc/STEP4-12_codex.md` 変更、`doc/STEP4-13.md` 追加あり
- 作成したブランチ名: `feature/step4-13-rename-async-loading`
- 分岐元ブランチ: `feature/step4-12-single-uri-fast-path`
- commit message: `Run rename work asynchronously with loading state`
- push先ブランチ: `origin/feature/step4-13-rename-async-loading`
- commit hash: `3c14df0`
- 未コミット差分の有無: push直後はなし。実機ログ分析追記により、このドキュメントのみ追加更新。

## 実機ログ確認手順

### 1. 端末確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

### 2. ログクリア

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

### 3. 性能ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenamePerf
```

### 4. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

### 5. 最低3回分控えるログ

```text
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

```text
- リネーム総時間そのものはProvider依存で大きく変わらない可能性がある
- UIが固まらず、実行中表示が出るか
- 実行中に二重押下されないか
- isExecuting が成功/失敗後に false へ戻るか
- FileNotFound が再発していないか
- path=TreeUriFallback でも成功しているか
```

## 実機ログ確認結果

STEP4-13を端末へインストール後、Prefixモードで複数回リネームを実行した。

確認できた代表ログ。

```text
1回目:
viewModel coroutine start renameMode=Prefix
viewModel withContext(IO) start
useCase start ... thread=3658-4289
saf resolve target elapsedMs=707
saf renameTo elapsedMs=1421
saf renameFile elapsedMs=2181 path=TreeUriFallback
viewModel withContext(IO) end elapsedMs=2189
viewModel state update after IO elapsedMs=48
rename total elapsedMs=2239 path=TreeUriFallback

2回目:
viewModel withContext(IO) start
saf resolve target elapsedMs=587
saf renameTo elapsedMs=354
saf renameFile elapsedMs=971 path=TreeUriFallback
viewModel withContext(IO) end elapsedMs=974
viewModel state update after IO elapsedMs=40
rename total elapsedMs=1015 path=TreeUriFallback

3回目:
viewModel withContext(IO) start
saf resolve target elapsedMs=694
saf renameTo elapsedMs=70
saf renameFile elapsedMs=792 path=TreeUriFallback
viewModel withContext(IO) end elapsedMs=797
viewModel state update after IO elapsedMs=41
rename total elapsedMs=838 path=TreeUriFallback
```

実機ログから分かること。

- `viewModel coroutine start` と `viewModel withContext(IO) start` が出ており、ViewModel起点のCoroutine実行が確認できた。
- UseCase / Repository / SAF のログは `3658-4289` 側で出ている。一方、ViewModel開始・終了・状態更新ログは `3658-3658` 側で出ている。
- つまり、重いSAF処理はUIスレッドから外れ、`Dispatchers.IO` 上で動作している。
- `path=TreeUriFallback` で成功しており、STEP4-12までのfallback方式は維持されている。
- `singleUri fast path` は引き続き `UnsupportedOperation` で失敗しているが、クラッシュせずfallbackしている。
- `FileNotFound` は提示ログ内では再発していない。
- `viewModel state update after IO` は約40〜48msで、I/O完了後の状態反映は軽い。
- リネーム総時間は838〜2239msでばらつきがある。特に1回目は `renameTo` が1421msと重く、Provider依存の待ち時間が大きい。

結論。

```text
STEP4-13の主目的である「SAF処理をUIスレッドから外す」は達成。
ただし、総リネーム時間そのものはProvider依存のため短縮されるとは限らない。
```

今回の改善は「処理時間の短縮」ではなく、「重い処理中もUIを固めない」ための改善である。ログ上は、処理本体がI/O側スレッドで動いているため、設計どおりの状態になっている。

次に実機で確認すべきUI観点。

```text
1. 実行中に「リネーム中...」が表示されること
2. ProgressBarが見えること
3. 実行中にリネーム実行ボタンが無効化されること
4. 実行中に元ファイル一覧・候補一覧が選択できないこと
5. 実行中に連打しても二重実行されないこと
6. 成功後にボタンが通常状態へ戻ること
7. 失敗時もボタンが通常状態へ戻ること
```

二重押下確認時のログ観点。

```text
理想:
- 実行中はUIボタンがdisabledになり、2回目の rename click start 自体が出ない

もしクリックログが出る場合:
- viewModel rename ignored because already executing
  が出て、2回目のUseCase / Repository / SAFが走らないこと
```

## 後に回す機能メモ

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

## 未解決事項・リスク

- リネーム総時間自体はProvider依存のため、`Dispatchers.IO` 対応だけでは短くならない可能性がある。今回の主目的はUI停止の軽減である。
- 実機ログでも、総時間は838〜2239msとばらついた。特に `renameTo` が70msの回もあれば1421msの回もあり、Provider側の状態に大きく依存している。
- Home画面のディレクトリ読み込み / CSV読み込みは今回非同期化していない。
- `directory.listFiles()` 相当の `saf resolve target` は約587〜707ms残っている。キャッシュ化は外部変更や同名チェックの整合性が必要なため後続STEPで扱う。
- `singleUri` fast pathが毎回 `UnsupportedOperation` になるProviderでは、次STEP以降でfast pathスキップを検討できる。

## 次に進めるべきSTEP

STEP4-13後のログでは、非同期化は成立している。次はUIの見え方と二重押下防止を実機で確認する。

確認後、次に進める候補は以下。

```text
1. 実行中表示の実機調整
   - ProgressBarの見え方
   - ボタン文言の視認性
   - 連打時のログ確認

2. Provider別fast pathスキップ
   - UnsupportedOperationが連続するProviderではsingleUri試行を省略

3. HomeViewModelの読み込み処理のDispatchers.IO対応
   - ディレクトリ読み込み
   - CSV読み込み

4. directory.listFiles() のキャッシュ設計
   - 連続リネームの高速化
   - 外部変更と同名チェックの整合性を含めて検討
```
