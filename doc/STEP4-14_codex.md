# STEP 4-14: 自動連番モード追加 Codex回答

## 作業ブランチ

`feature/step4-14-auto-numbering`

STEP4-13までを `develop` にmergeしてpushした後、`develop` からこのブランチを作成した。

作業開始前の整理。

- `doc/STEP4-13_codex.md` の実機ログ追記は `feature/step4-13-rename-async-loading` に追加コミットし、pushした。
- `feature/step4-13-rename-async-loading` を `develop` に `--no-ff` mergeし、`origin/develop` へpushした。
- `doc/STEP4-14.md` は次STEP指示書として一時退避後、`feature/step4-14-auto-numbering` に戻した。

## 実装内容

- Matching画面に `自動連番: OFF / ON` ボタンを追加した。
- 自動連番のON/OFF状態を `RenameMatchingUiState` に追加した。
- `RenameMatchingViewModel.toggleAutoNumbering()` を追加した。
- リネーム実行中は自動連番ON/OFFを切り替えられないようにした。
- 候補ごとの連番カウンタを `RenameMatchingViewModel` 内で管理するようにした。
- カウンタキーは `RenameCandidate.rawPattern` を採用した。
- 自動連番ON時は、候補名に `-1`, `-2` のような番号を付けてから Prefix / Suffix / Replace を適用するようにした。
- リネーム成功時のみカウンタを進めるようにした。
- リネーム失敗時はカウンタを進めないようにした。
- 自動連番ON時は、成功後も候補を `isUsed = true` にせず、同じ候補を再利用可能にした。
- 自動連番OFF時は、既存どおり成功後に候補を使用済みにする挙動を維持した。
- 既存CSV形式 `A1-1_*` の `*` 置換と自動連番を組み合わせ、`A1-1-1_*` のように変換するようにした。
- STEP4-13の非同期リネーム処理、リネーム中表示、二重押下防止は維持した。

## 実装コード

### RenameMatchingUiState.kt

自動連番ON/OFF状態を追加した。

```kotlin
val isAutoNumberingEnabled: Boolean = false
```

デフォルトはOFFで、Home画面ではなくMatching画面内だけで扱う。

### RenameMatchingViewModel.kt

候補ごとの連番カウンタを追加した。

```kotlin
private val autoNumberCounters: MutableMap<String, Int> = mutableMapOf()
```

自動連番の切り替えメソッドを追加した。

```kotlin
fun toggleAutoNumbering() {
    if (_uiState.value.isExecuting) return

    _uiState.update { state ->
        state.copy(isAutoNumberingEnabled = !state.isAutoNumberingEnabled, error = null)
    }
}
```

リネーム実行時、自動連番ONなら現在番号を取得する。

```kotlin
val autoNumberKey = selectedCandidate.rawPattern
val autoNumber = if (isAutoNumberingEnabled) {
    autoNumberCounters[autoNumberKey] ?: INITIAL_AUTO_NUMBER
} else {
    null
}
```

名前解決へ番号を渡す。

```kotlin
val resolvedNewName = resolveRenameNameUseCase(
    sourceFile = selectedFile,
    candidate = selectedCandidate,
    renameMode = renameMode,
    autoNumber = autoNumber,
)
```

成功時のみカウンタを進める。

```kotlin
if (result.success && autoNumber != null) {
    autoNumberCounters[autoNumberKey] = autoNumber + 1
}
```

自動連番ON時は候補を使用済みにしない。

```kotlin
candidate.copy(
    isSelected = false,
    isUsed = if (state.isAutoNumberingEnabled) candidate.isUsed else true,
)
```

### RenameMatchingFragment.kt

画面上部の操作エリアに自動連番ボタンを追加した。

```text
[戻る] [自動連番: OFF/ON] [リネーム実行]
```

ON/OFF状態で文言と色を切り替える。

```kotlin
autoNumberButton.text = if (state.isAutoNumberingEnabled) {
    "自動連番: ON"
} else {
    "自動連番: OFF"
}
```

実行中はdisabledにする。

```kotlin
autoNumberButton.isEnabled = !state.isExecuting
```

### ResolveRenameNameUseCase.kt

自動連番番号を任意で受け取れるようにした。

```kotlin
operator fun invoke(
    sourceFile: RenameTargetFile,
    candidate: RenameCandidate,
    renameMode: RenameMode = RenameMode.Prefix,
    autoNumber: Int? = null,
): String
```

自動連番OFF時は既存どおり。

```text
Prefix: A1-1_logs1223.txt
Suffix: logs1223_A1-1.txt
Replace: A1-1.txt
```

自動連番ON時は候補パターンに番号を付けてから既存のRenameModeを適用する。

```text
Prefix: A1-1-1_logs1223.txt
Suffix: logs1223_A1-1-1.txt
Replace: A1-1-1.txt
```

`*` 付き候補では、最初の `*` の直前へ番号を差し込む。ただし `A1-1_*` のように `*` の直前が `_` の場合は、`_` の前へ差し込む。

```text
A1-1_* + 1
→ A1-1-1_*
→ A1-1-1_logs1223.txt
```

### その他変更ファイル

- `doc/STEP4-14.md`
  - STEP4-14指示書。
- `doc/STEP4-14_codex.md`
  - 本回答。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/ResolveRenameNameUseCase.kt`
- `doc/STEP4-14.md`
- `doc/STEP4-14_codex.md`

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

自動連番は、ファイルアクセスではなく「画面上で候補をどう適用するか」という操作状態である。そのため、SAF DataSourceやRepositoryには入れず、Matching画面のViewModelに状態とカウンタを持たせた。実際のファイル名生成だけは既存の `ResolveRenameNameUseCase` に寄せ、Prefix / Suffix / Replace の既存ルールと一箇所で合成する。

## 概要

自動連番ON時だけ、選択した候補に `-番号` を付けてから既存のRenameModeを適用する。リネーム成功時だけ番号を進めるため、`FileAlreadyExists` や権限エラーなどの失敗では番号が飛ばない。

## 設計詳細

- `RenameMatchingUiState`
  - 責務: Matching画面の表示状態。
  - `isAutoNumberingEnabled`: 自動連番ON/OFF。

- `RenameMatchingViewModel`
  - 責務: 自動連番状態、候補ごとのカウンタ、選択状態、非同期リネーム実行、成功後UI更新。
  - `autoNumberCounters`: `rawPattern` ごとに次番号を保持する。
  - `toggleAutoNumbering()`: 実行中でなければON/OFF切替。

- `ResolveRenameNameUseCase`
  - 責務: 元ファイル名、候補、RenameMode、自動連番番号から最終ファイル名を作る。
  - `autoNumber` はnullableにし、通常モードの呼び出しを壊さない。

- `RenameMatchingFragment`
  - 責務: 自動連番ボタン表示、ON/OFFの色・文言反映。

## 採用理由・根拠

自動連番を `RenameMode` に追加しなかった理由は、Prefix / Suffix / Replace と自動連番は排他的なモードではなく、組み合わせる設定だからである。`RenameMode.AutoNumberingPrefix` のように増やすと、今後ゼロ埋めや開始番号指定を追加するたびにenumが増え、責務が混ざる。

候補ごとのカウンタキーに `rawPattern` を使った理由は、今回の要件が「同じCSV候補を複数ファイルに連続適用する」ことだからである。`candidate.id` は行単位の識別に向くが、候補の意味として同じかどうかを見るなら `rawPattern` の方が分かりやすい。将来、同じ文字列の重複行を別カウンタにしたい場合は `ruleId` へ切り替える余地がある。

カウンタをViewModelに置いた理由は、番号が画面セッション内の操作状態だからである。永続化やRepository層に入れると、履歴・取り消し・リセットUIなどの設計が必要になり、今回の単機能STEPを超える。

## 代替案

- `RenameMode` に自動連番モードを追加する
  - 有効な条件: 自動連番がPrefix/Suffix/Replaceとは別の排他的なリネーム方式になる場合。
  - 採用しない理由: 今回はPrefix/Suffix/Replaceと組み合わせる設定であり、enumに混ぜると状態設計が複雑になるため。

- カウンタをRepositoryやDataSourceに持たせる
  - 有効な条件: アプリ全体で連番状態を永続化し、画面をまたいで共有する場合。
  - 採用しない理由: 今回はMatching画面内の連続操作だけが対象であり、永続化は不要なため。

- `candidate.id` をカウンタキーにする
  - 有効な条件: CSVに同じ候補文字列が複数行あり、それぞれ別カウンタとして扱いたい場合。
  - 採用しない理由: 現時点の要件では「同じ候補 A1-1」を意味単位で連番化する方が自然なため。

## 動作確認方法

```text
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. Prefixモードでマッチング画面へ進む
5. 自動連番ボタンが表示されることを確認する
6. 自動連番がデフォルトOFFであることを確認する
7. 自動連番OFFで従来どおり1件リネームできることを確認する
8. 自動連番ONにする
9. logs1223.txt と A1-1 を選択する
10. リネーム結果が A1-1-1_logs1223.txt になることを確認する
11. 次に logs1226.txt と同じ A1-1 を選択する
12. リネーム結果が A1-1-2_logs1226.txt になることを確認する
13. Suffixモードで logs1223_A1-1-1.txt のようになることを確認する
14. Replaceモードで A1-1-1.txt のようになることを確認する
15. 自動連番ON時、同じ候補を複数回使えることを確認する
16. 自動連番OFF時、既存どおり候補が使用済みになることを確認する
17. リネーム中表示と二重押下防止が維持されていることを確認する
18. FileAlreadyExists失敗時に連番が進まないことを確認する
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

- 作業開始時のgit status: `feature/step4-13-rename-async-loading...origin/feature/step4-13-rename-async-loading`、`doc/STEP4-13_codex.md` 変更、`doc/STEP4-14.md` 追加あり
- STEP4-13追記コミット: `b6a2fe0 Update STEP4-13 device verification notes`
- develop merge commit: `b9faf2a`
- 作成したブランチ名: `feature/step4-14-auto-numbering`
- 分岐元ブランチ: `develop`
- commit message: `Add auto numbering rename mode`
- push先ブランチ: `origin/feature/step4-14-auto-numbering`
- commit hash: コミット作成後に最終応答で報告
- 未コミット差分の有無: コミット後に確認

## 実機ログ確認手順

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

### 6. 確認するログ観点

```text
- isAutoNumberingEnabled=true で resolvedNewName に -1, -2 が付くこと
- selectedRenameMode が Prefix / Suffix / Replace で期待どおりであること
- selectedCandidate.rawPattern が同じ場合に counterBefore / counterAfter が進むこと
- rename success=true のときだけ counterAfter が進むこと
- rename success=false のとき counterAfter が進まないこと
- 別候補 A1-2 では -1 から始まること
```

## 後に回す機能メモ

- 一括リネーム
- 連番開始番号の指定
- 連番のゼロ埋め
- 連番リセットボタン
- 候補ごとの連番状態一覧
- 自動連番状態の永続化
- CSVプレビュー
- リネーム前後の差分プレビュー
- Undo / 取り消し
- 履歴機能
- HomeViewModelのディレクトリ読み込み / CSV読み込みのDispatchers.IO対応
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
- Android標準ファイルピッカー内の並び順制御
- 独自ファイルピッカー

## 未解決事項・リスク

- 連番カウンタは画面セッション内だけで保持される。画面を戻る、アプリを再起動する、画面回転する場合はリセットされる。
- `rawPattern` をキーにしているため、CSVに同じ文字列の候補が複数行ある場合は同一カウンタになる。重複行を別管理したい場合は `ruleId` キーへ変更する。
- `*` 付き候補の自動連番は、最初の `*` の直前に挿入する単純ルールである。複雑なパターンを許可する場合は後続STEPで仕様化が必要。
- 自動連番ON時でも、同名ファイルが存在すれば既存の `FileAlreadyExists` 保護で失敗する。失敗時は番号を進めない。
- 自動連番の次番号表示は今回はボタンON/OFF表示までに留めた。詳細な「次の番号」表示は後続STEPで検討する。

## 次に進めるべきSTEP

実機で自動連番ON/OFFの基本動作を確認する。

確認後、次に進める候補は以下。

```text
1. 自動連番の次番号表示
   - 選択中候補: A1-1
   - 次の番号: 3

2. 連番開始番号指定
   - 1以外から開始したいケース

3. ゼロ埋め指定
   - -01 / -001 形式

4. 候補ごとの連番状態一覧
   - A1-1 次: 3
   - A1-2 次: 1

5. 失敗時テストケースの自動化
   - FileAlreadyExists時にカウンタが進まないこと
```
