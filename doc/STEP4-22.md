以下をそのまま `doc/STEP4-22.md` として保存して、Codexに渡してください。
実機ログでは、`singleUri` は名前解決までは成功していますが、`renameTo` が `UnsupportedOperation` になり、`treeUri fallback` に移行しています。また、`treeUri` は選択ディレクトリ内に絞れている一方、7件目で一致しているのに、その後25件目まで比較が続いています。したがってSTEP4-22は、**treeUri探索で一致したら即終了する最小性能改善**に切るのが妥当です。

````md id="step4-22-prompt"
# STEP 4-22: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- STEP4-21で追加したSAF探索ログをもとに、treeUri fallback探索の無駄を減らす
- treeUri探索で対象ファイル名に一致した時点で探索を終了する
- 選択ディレクトリ内に探索範囲が絞られている既存挙動は維持する
- リネーム仕様、UNDO仕様、自動連番仕様は変更しない
- 今回は「treeUri探索の早期終了」だけを実装する

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

## 今回実装する機能

- treeUri fallback探索で、対象ファイル名に一致した `DocumentFile` を見つけたら、その時点で探索を終了する
- 一致後に残りのファイル比較を続けない
- `treeUri found` ログのタイミングを、一致検出直後に近づける
- `scannedCount` をログ出力し、何件目で見つかったか確認できるようにする
- `matchedIndex` をログ出力し、何件目で一致したか確認できるようにする
- `treeUri elapsedMs` を維持し、改善前後で比較できるようにする
- 既存の `EasyRenameSafResolve` ログを維持する
- 通常リネームとUNDOの両方で既存挙動が壊れていないことを確認する

## 今回実装しない機能

- singleUri fast pathの廃止
- singleUri fast pathの仕様変更
- Provider別fast pathスキップ
- treeUri探索範囲の変更
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- リネームモード選択式ボタンの幅調整
- 前回のセット再押下で選択解除する処理
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- RecyclerView化
- XMLレイアウト大規模変更
- Material Componentsへの本格移行

---

# 重要: STEP4-21のcommit / pushを先に完了すること

STEP4-21のCodex回答では以下の状態だった。

```text
STEP4-21 commit hash: 未作成
STEP4-21 push先: origin/feature/step4-21-saf-resolve-logs 予定
未コミット差分: あり
````

そのため、STEP4-22の作業を始める前に、必ずSTEP4-21をcommit / pushすること。

## 1. 現在状態確認

```powershell
git status
git branch --show-current
```

現在ブランチが以下であることを確認する。

```text
feature/step4-21-saf-resolve-logs
```

## 2. STEP4-21のビルド確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 3. STEP4-21の単体テスト確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 4. STEP4-21をcommit / push

```powershell
git status
git add .
git commit -m "Add SAF resolve diagnostic logs"
git push -u origin feature/step4-21-saf-resolve-logs
```

commit hashを控えること。

---

# STEP4-22 作業ブランチ

STEP4-21のcommit / push完了後、STEP4-22用ブランチを作成する。

```powershell
git checkout feature/step4-21-saf-resolve-logs
git pull
git checkout -b feature/step4-22-tree-uri-early-exit
```

ブランチ名は以下とする。

```text
feature/step4-22-tree-uri-early-exit
```

作業完了後、実機確認OKのあとに以下を実行すること。

```powershell
git status
git add .
git commit -m "Stop tree URI search after first match"
git push -u origin feature/step4-22-tree-uri-early-exit
```

Codexの最終回答では以下を報告すること。

* STEP4-21のcommit hash
* STEP4-21のpush結果
* STEP4-22の作業ブランチ名
* STEP4-22の分岐元ブランチ
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* 実機確認結果
* STEP4-22のcommit hash
* STEP4-22のpush結果
* 未コミット差分の有無
* 回答を保存した `doc/STEP4-22_codex.md`

---

# 背景

STEP4-21では、singleUri fast path と treeUri fallback の探索挙動を確認するログを追加した。

実機ログでは以下が確認できた。

```text
singleUri document resolved name=log (22).zip
singleUri name compare expected=log (22).zip actual=log (22).zip matches=true
singleUri accepted expected=log (22).zip actual=log (22).zip
renameTo start sourceName=log (22).zip targetName=A1-1-1_log (22).zip
singleUri failed reason=UnsupportedOperation
singleUri end success=false elapsedMs=52
```

つまり、singleUriは対象ファイル名の解決までは正しいが、`renameTo` がProvider側の制約で `UnsupportedOperation` になっている。

その後、treeUri fallbackに移行している。

```text
treeUri fallback start directoryUri=... expectedName=log (22).zip
treeUri listFiles from selected directory only=true
treeUri listFiles source=selectedDirectory
```

このため、treeUri探索範囲は選択ディレクトリ内に絞れている。

一方で、以下の問題が見える。

```text
treeUri compare[7] candidateName=log (22).zip expectedName=log (22).zip matches=true
treeUri candidate matched name=log (22).zip uri=...
...
treeUri compare[25] ...
treeUri found expected=log (22).zip actual=log (22).zip uri=...
```

7件目で一致しているのに、25件目まで比較が続いている。

そのため、STEP4-22では、treeUri探索で一致した時点で即終了するようにする。

---

# 実装方針

## 1. treeUri fallback探索を早期終了する

`SafDocumentDataSource` のtreeUri fallback探索処理を確認する。

現在、以下のような流れになっている可能性がある。

```kotlin
val matchedFile = directory.listFiles()
    .also { files -> files.forEachIndexed { ... log ... } }
    .firstOrNull { it.name == expectedName }
```

または、

```kotlin
var matchedFile: DocumentFile? = null
directory.listFiles().forEachIndexed { index, file ->
    val matches = file.name == expectedName
    logCompare(...)
    if (matches) {
        matchedFile = file
    }
}
```

この場合、一致後も残りのファイルを走査してしまう。

以下のように、見つけた時点でbreakする形へ変更する。

```kotlin
var matchedFile: DocumentFile? = null
var scannedCount = 0
var matchedIndex: Int? = null

for (file in directory.listFiles()) {
    scannedCount += 1

    val candidateName = file.name
    val matches = candidateName == expectedName

    if (shouldLogCompare(scannedCount, matches)) {
        Log.d(
            TAG_SAF_RESOLVE,
            "treeUri compare[$scannedCount] candidateName=$candidateName expectedName=$expectedName matches=$matches"
        )
    }

    if (matches) {
        matchedFile = file
        matchedIndex = scannedCount
        Log.d(
            TAG_SAF_RESOLVE,
            "treeUri candidate matched name=$candidateName uri=${file.uri} matchedIndex=$matchedIndex"
        )
        break
    }
}
```

## 2. listFiles自体の挙動について

注意点として、`directory.listFiles()` は呼び出した時点で配列を返すため、Providerによってはこの時点でディレクトリ全体を取得している可能性がある。

そのため、今回の早期終了で確実に減らせるのは主に以下。

```text
- Kotlin側の比較処理
- Kotlin側のログ出力
- 一致後の不要なループ
```

`DocumentFile.listFiles()` 自体のProvider側コストは残る可能性がある。

この点はCodexの最終回答でリスクとして明記すること。

## 3. ログを整理する

既存の `EasyRenameSafResolve` ログを維持する。

追加・変更するログ:

```text
treeUri candidate matched name=... uri=... matchedIndex=...
treeUri found expected=... actual=... uri=... scannedCount=... matchedIndex=...
treeUri end success=true elapsedMs=... scannedCount=... matchedIndex=...
```

失敗時:

```text
treeUri not found expectedName=... scannedCount=... elapsedMs=...
```

## 4. 比較ログの出力制限を維持する

STEP4-21で導入した `TREE_URI_COMPARE_LOG_LIMIT = 50` 相当の制限は維持する。

ただし、一致した候補はログ制限に関係なく必ず出す。

仕様:

```text
- compareログは最大50件まで
- 一致した候補は必ず treeUri candidate matched として出す
- 一致後は探索終了
- 一致後のcompareログは出ない
```

---

# 変更対象ファイル候補

最有力変更ファイル:

```text
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
```

必要に応じて変更:

```text
doc/STEP4-22_codex.md
```

原則として、以下は変更しない。

```text
StorageRepository.kt
StorageRepositoryImpl.kt
ExecuteRenameUseCase.kt
UndoRenameUseCase.kt
RenameResult.kt
RenameMatchingViewModel.kt
HomeViewModel.kt
```

ただし、コンパイル上必要な場合のみ最小限の変更を許可する。

---

# 禁止事項

* singleUri fast pathを削除しない
* singleUri fast pathの挙動を変更しない
* singleUri失敗時のfallback条件を変更しない
* treeUri探索範囲を変更しない
* treeUri探索を選択ディレクトリ外へ広げない
* directory.listFiles() のキャッシュ化を今回実装しない
* 対象ファイルMap化を今回実装しない
* Provider別fast pathスキップを今回実装しない
* FragmentやViewModelから直接 `DocumentFile.renameTo()` を呼ばない
* リネーム仕様を変えない
* UNDO仕様を変えない
* 自動連番仕様を変えない
* CSV仕様を変えない
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
5. Prefixモードでファイルを1件リネームする
6. EasyRenameSafResolveログを確認する
7. treeUri compareで一致した後、それ以降のcompareログが出ないことを確認する
8. treeUri candidate matched に matchedIndex が出ることを確認する
9. treeUri found に scannedCount / matchedIndex が出ることを確認する
10. treeUri end に elapsedMs / scannedCount / matchedIndex が出ることを確認する
11. リネーム自体が成功することを確認する
12. UNDOを1件実行する
13. UNDOでも既存挙動が壊れていないことを確認する
14. 自動連番ON/OFFの既存挙動が壊れていないことを確認する
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

## 3. SAF探索ログ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

## 4. 履歴ログ確認

UNDO確認時に必要なら見る。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. クラッシュ確認

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 6. 確認するログ観点

以下を確認する。

```text
- singleUri start が出る
- singleUri failed reason=UnsupportedOperation が出ても、treeUri fallbackへ進む
- treeUri fallback start が出る
- treeUri listFiles source=selectedDirectory が維持されている
- treeUri compare[N] で matches=true になった後、compare[N+1] 以降が出ない
- treeUri candidate matched name=... matchedIndex=... が出る
- treeUri found expected=... actual=... scannedCount=... matchedIndex=... が出る
- treeUri end success=true elapsedMs=... scannedCount=... matchedIndex=... が出る
- renameTo end success=true が出る
- AndroidRuntime のクラッシュが出ていない
```

---

# 実機ログを見た後の判断基準

## 1. 早期終了後、treeUri elapsedMs が大きく短縮した場合

次STEP候補:

```text
STEP4-23: CSVプレビュー
```

この場合、treeUri側の無駄ループ削減で十分な効果があったと判断できる。

## 2. 早期終了後も treeUri elapsedMs が大きく変わらない場合

原因は `directory.listFiles()` 自体のコストである可能性が高い。

次STEP候補:

```text
STEP4-23: directory.listFiles() のキャッシュ化、または対象ファイルMap化
```

## 3. singleUriが毎回UnsupportedOperationになる場合

singleUri解決自体は正しくても、`renameTo` に使えない可能性がある。

次STEP候補:

```text
STEP4-23: Provider別fast pathスキップ、またはsingleUri renameTo失敗後の扱い整理
```

ただし、今回のSTEPではそこまで変更しない。

## 4. treeUri matchedIndex が常に小さい場合

早期終了の効果が出やすい。

## 5. treeUri matchedIndex が常に末尾に近い場合

早期終了の効果は限定的。

この場合は、キャッシュ化や対象ファイルMap化の方が効く可能性がある。

---

# 単体テスト方針

今回の変更は `DocumentFile` とSAF依存が強いため、無理にローカルJVMテストを追加しなくてよい。

ただし、treeUri探索を小さな純粋関数に分けられる場合のみ、以下をテストしてよい。

```text
- 一致した時点で探索を終了する
- scannedCount が一致位置になる
- 見つからない場合は全件走査する
```

大規模リファクタになる場合はテスト追加を見送ること。

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何を変更したか
* treeUri探索をどのように早期終了したか
* `matchedIndex` / `scannedCount` をどうログ出力したか
* 既存のsingleUri挙動を変更していないこと
* 既存のtreeUri探索範囲を変更していないこと
* リネーム / UNDO / 自動連番を変更していないこと

## 2. 変更ファイル一覧

例:

```text
- SafDocumentDataSource.kt
- doc/STEP4-22_codex.md
```

## 3. 動作確認方法

* ビルド方法
* 単体テスト方法
* 実機ログ確認方法
* 通常リネーム確認方法
* UNDO確認方法

## 4. 実機で見るべきログ

ユーザがそのままPowerShellで実行できる形で提示すること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

必要に応じて以下も提示すること。

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. Git操作結果

* STEP4-21のcommit hash
* STEP4-21のpush結果
* STEP4-22の作業ブランチ名
* STEP4-22の分岐元ブランチ
* STEP4-22のcommit message
* STEP4-22のcommit hash
* STEP4-22のpush先
* 未コミット差分の有無

## 6. 保存ファイル

Codexの回答は以下へ保存すること。

```text
doc/STEP4-22_codex.md
```

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text
- singleUri fast pathの仕様見直し
- singleUri結果の検証強化
- Provider別fast pathスキップ
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- treeUri探索時間のさらなる削減
- CSVプレビュー
- CSVプレビューボタン
- CSV選択後のプレビューボタン有効化・色変更
- CSVプレビュー専用画面
- CSVプレビューの表形式表示
- CSV文字コード自動判定
- Spinner横幅調整
- リネームモード選択式ボタンの最長項目に合わせた幅調整
- 前回のセット再押下で選択解除し、マッチング画面に進むボタンをdisableにする
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- UNDO成功時の自動連番カウンタ巻き戻し
- UNDO失敗時の詳細ダイアログ表示
- 自動連番開始番号ダイアログのインライン分解表示改善
- 画面下部リネーム履歴エリアのさらなるUI改善
- HomeViewModelの読み込み処理の本格的なDispatchers.IO対応
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
- STEP4-22ではtreeUri探索の早期終了のみ対応
- directory.listFiles() 自体のProvider側コストは残る可能性がある
- singleUriは名前解決できてもrenameToでUnsupportedOperationになるケースがある
- singleUri fast pathの扱いは今回変更していない
- Provider別fast pathスキップは未実装
- キャッシュ化や対象ファイルMap化は未実装
- ファイル名やUriログはリリース前に抑制方針を検討する必要がある
```

---

# 完了条件

このSTEP4-22の完了条件は以下。

```text
- STEP4-21がcommit / pushされている
- assembleDebug が成功する
- testDebugUnitTest が成功する
- treeUri探索で一致後にループが終了する
- 一致後に不要なcompareログが出ない
- treeUri candidate matched に matchedIndex が出る
- treeUri found に scannedCount / matchedIndex が出る
- treeUri end に elapsedMs / scannedCount / matchedIndex が出る
- treeUri listFiles source=selectedDirectory が維持されている
- singleUriの既存挙動を変更していない
- 通常リネームが壊れていない
- UNDOが壊れていない
- 自動連番が壊れていない
- 実機確認後にSTEP4-22がcommit / pushされている
- Codex回答が doc/STEP4-22_codex.md に保存されている
```

```

**ベスト案**  
STEP4-22は、`treeUri fallback` の早期終了に絞るのがよいです。実機ログ上、7件目で一致しているのに25件目まで比較が続いているため、まずここを小さく直す根拠があります。

**代替案**  
`singleUri fast path` の扱いを先に見直す案もあります。ただし、今回のログではsingleUriの名前解決自体は正しく、失敗点は `renameTo` の `UnsupportedOperation` です。Provider別スキップは設計判断が少し重いので、先にtreeUri側の明確な無駄を潰す方が安全です。

**注意点**  
`directory.listFiles()` は呼び出し時点で配列化されるため、早期終了してもProvider側の一覧取得コストは残る可能性があります。実機ログで `treeUri elapsedMs` があまり下がらない場合は、次にキャッシュ化・対象ファイルMap化・Provider別fast pathスキップを検討する流れが妥当です。
```
