````md id="step4-11-prompt"
# STEP 4-11: 実装（機能単位）用 Codex 指示

STEP4-10では、`EasyRenamePerf` タグでリネーム1件の段階別処理時間ログが追加されました。実機ログ分析では、Prefix / Suffix / Replace の成功ケースで1件あたり約2.2秒、UseCase全体が約2.1秒、Repository + SAF が約1.35〜1.40秒、SAF対象探索が約0.56〜0.60秒、`DocumentFile.renameTo` は約0.34〜0.39秒でした。:contentReference[oaicite:0]{index=0}

ログ上、主因は `renameTo()` そのものではなく、UseCase側の事前同名チェックとDataSource側の対象探索・同名確認で、SAFディレクトリ走査が重複していることです。:contentReference[oaicite:1]{index=1}

このSTEPでは、**SAF走査回数削減によるリネーム高速化のみ**を対象にしてください。
回答はdec/STEP4-11_codex.mdに保存すること。
---

## 目的

- 1機能ずつ確実に完成させる
- 1リネームあたりの待ち時間を短縮する
- UseCase側とDataSource側で重複している同名チェック / ディレクトリ走査を整理する
- `FileAlreadyExists` の検出は維持する
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
git checkout -b feature/step4-11-saf-scan-optimization
````

### 注意

STEP4-11はSTEP4-10の性能ログ分析結果に基づく修正です。
`main` にSTEP4-10までの変更が入っていない場合は、作業に必要な最新ブランチを確認し、どのブランチから分岐するべきかを報告してください。

必要であれば、以下のようにSTEP4-10ブランチから分岐してください。

```powershell id="git-alt-start"
git checkout feature/step4-10-rename-performance-log
git pull
git checkout -b feature/step4-11-saf-scan-optimization
```

### 作業後に実行

ビルドとテスト成功後、以下を実行してください。

```powershell id="git-finish"
git status
git add .
git commit -m "Reduce SAF scans during rename"
git push -u origin feature/step4-11-saf-scan-optimization
```

### Git注意事項

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分を確認する
* 未コミット差分がある場合は、内容を報告してから作業する
* 作業後はcommitとpushまで行う
* pushに失敗した場合は、エラー内容を報告する

---

## 実機ログ分析結果

```text id="perf-summary"
リネーム1件の総時間:
- Prefix: 約2186ms
- Suffix: 約2213ms
- Replace: 約2208ms

UseCase全体:
- 約2122〜2164ms

Repository + SAF:
- 約1352〜1405ms

SAF対象探索:
- 約557〜603ms

DocumentFile.renameTo:
- 約343〜391ms

Matching側状態更新:
- 約24〜38ms

Home側状態更新:
- 約0〜1ms
```

---

## 現状の問題

現在は、1件リネームの中でSAFディレクトリ走査が重複している可能性が高いです。

```text id="current-flow"
ExecuteRenameUseCase
→ storageRepository.existsInSameDirectory(...)
   → ディレクトリ走査

SafDocumentDataSource.renameFile(...)
→ directory.listFiles()
   → 対象ファイル探索
   → 同名ファイル確認
   → renameTo()
```

つまり、リネーム1件につき少なくとも以下が発生しています。

```text id="duplicated-scan"
1. UseCase側の同名チェック
2. DataSource側の対象ファイル探索
3. DataSource側のリネーム直前同名チェック
```

このSTEPでは、同名チェックをDataSource側に一本化し、`listFiles()` の回数を減らしてください。

---

## 今回やること / やらないこと

### 今回やること

```text id="do-this-step"
1. UseCase側の事前同名チェックを削除する
2. 同名チェックをSafDocumentDataSource.renameFile内に一本化する
3. DataSource内で1回取得したlistFiles結果を、対象探索と同名チェックの両方に使う
4. FileAlreadyExistsの失敗結果は維持する
5. 性能ログで改善前後を比較できるようにする
6. 成功ケースで不要な重いDocumentFile詳細確認ログを減らす
```

### 今回やらないこと

```text id="not-this-step"
1. 自動連番機能
2. 一括リネーム
3. RenameMode仕様変更
4. CSV仕様変更
5. 独自ファイルピッカー
6. RecyclerView化
7. XMLレイアウト化
8. Material Componentsへの本格移行
9. Edge-to-Edge / WindowInsets正式対応
10. 画面回転時の完全な状態復元
11. SAF処理全体の大規模再設計
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text id="scope-files"
app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt
app/src/main/java/com/example/easyrename/model/RenameResult.kt
app/src/main/java/com/example/easyrename/model/RenameErrorType.kt
```

必要に応じて以下も変更して構いません。

```text id="optional-files"
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt
```

---

## 禁止事項

* 自動連番を今回実装しない
* 一括リネームを追加しない
* Prefix / Suffix / Replace の仕様を変更しない
* 既存CSV形式 `A1-1_*` の挙動を変えない
* リネーム後の状態更新方式を全件再読み込みへ戻さない
* UIを大規模に変更しない
* RecyclerView化しない
* Composeへ移行しない
* DIライブラリを追加しない
* `FileAlreadyExists` の検出を削除しない
* エラー時にアプリをクラッシュさせない

---

# STEP 4-11-1: UseCase側の事前同名チェックを削除する

## 目的

UseCase側で走っている `existsInSameDirectory` によるSAFディレクトリ走査を削減する。

## 対象

* `ExecuteRenameUseCase`

## 実装内容

現在、`ExecuteRenameUseCase` に以下のような処理がある場合は削除してください。

```kotlin id="remove-usecase-check"
if (storageRepository.existsInSameDirectory(renamePair.directoryUri, renamePair.resolvedNewName)) {
    return RenameResult(
        beforeName = renamePair.sourceFile.displayName,
        afterName = renamePair.resolvedNewName,
        success = false,
        errorMessage = "A file with the same name already exists.",
        errorType = RenameErrorType.FileAlreadyExists,
    )
}
```

削除後のUseCaseの責務は以下にしてください。

```text id="usecase-new-flow"
1. ValidateRenameUseCaseでファイル名検証
2. storageRepository.renameFile(...) を呼ぶ
3. Repository / DataSourceから返ったRenameResultを返す
```

## 注意

* ファイル名検証は残す
* 同名チェック自体は削除せず、DataSource側へ移す
* `FileAlreadyExists` の失敗表示が維持されるようにする

---

# STEP 4-11-2: DataSource内でlistFiles結果を1回だけ使う

## 目的

対象ファイル探索と同名チェックを、1回の `directory.listFiles()` 結果で処理する。

## 対象

* `SafDocumentDataSource.renameFile`

## 実装内容

以下のような流れにしてください。

```text id="datasource-flow"
1. directory = DocumentFile.fromTreeUri(context, directoryUri)
2. children = directory.listFiles()
3. targetFile = childrenから対象ファイルを探す
4. duplicateFile = childrenから同名ファイルを探す
5. duplicateFileが存在し、targetFileと同一でなければFileAlreadyExistsを返す
6. targetFile.renameTo(newName)を実行
7. RenameResultを返す
```

## 対象ファイル探索ルール

```text id="target-find-rule"
1. URI一致を最優先
2. URI一致で見つからない場合、beforeName一致をfallbackとして使う
3. それでも見つからなければFileNotFound
```

## 同名チェックルール

```text id="duplicate-rule"
1. childrenの中に name == newName のファイルがあるか確認する
2. そのファイルがtargetFile自身でない場合はFileAlreadyExists
3. targetFile自身と判断できる場合は許可
```

## 注意

* `children = directory.listFiles()` は原則1回だけにする
* `existsInSameDirectory()` は他で使っていなければ残してもよいが、このリネームフローでは呼ばない
* `FileAlreadyExists` は必ず `RenameResult.errorType = RenameErrorType.FileAlreadyExists` で返す
* `FileNotFound` は `RenameErrorType.FileNotFound` で返す

---

# STEP 4-11-3: 成功ケースの重い詳細ログを減らす

## 目的

STEP4-6 / STEP4-10で追加した診断ログのうち、成功ケースで毎回呼ぶ必要がない重い確認を減らす。

## 対象

* `SafDocumentDataSource.renameFile`

## 見直し候補

以下はSAF Provider越しにコストが出る可能性があるため、成功ケースで必ず呼ぶ必要があるか見直してください。

```kotlin id="heavy-checks"
targetFile.exists()
targetFile.canWrite()
targetFile.isFile
targetFile.name
```

## 実装方針

* 成功ケースでは必要最小限の情報だけログ出力する
* 失敗ケースでは原因調査に必要な情報を出す
* 既存の `EasyRenamePerf` 時間ログは残す
* `EasyRename` の詳細ログは過剰なら減らす

## 注意

* ログ削減によってエラー調査不能にならないようにする
* `EasyRenamePerf` の性能ログは消さない

---

# STEP 4-11-4: 性能ログを維持して改善効果を測れるようにする

## 目的

STEP4-10ログと比較できるようにする。

## 対象

* `ExecuteRenameUseCase`
* `StorageRepositoryImpl`
* `SafDocumentDataSource`

## 実装内容

以下のログは維持してください。

```text id="keep-perf-logs"
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=...
EasyRenamePerf: repository rename end elapsedMs=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=...
```

可能であれば、UseCase側の同名チェック削除を確認しやすいログを追加してください。

```text id="new-perf-log"
EasyRenamePerf: useCase duplicate precheck skipped
```

---

# STEP 4-11-5: FileAlreadyExists失敗ケースを維持する

## 目的

高速化しても、同名ファイル保護を壊さない。

## 対象

* `SafDocumentDataSource.renameFile`
* `RenameMatchingViewModel`
* `RenameMatchingFragment`
* `ErrorDialog`

## 実装内容

以下を維持してください。

```text id="file-exists-requirement"
- 同じディレクトリ内にnewNameと同名ファイルがある場合はリネームしない
- RenameResult.success = false
- RenameResult.errorType = RenameErrorType.FileAlreadyExists
- UI上で「同じ名前のファイルが既に存在します」相当の表示を出す
```

## 注意

* 同名時に自動連番しない
* 上書きしない
* 失敗時に候補を使用済みにしない
* 失敗時に元ファイルをリネーム済みにしない

---

## 実装ルール

* 今回は「SAF走査回数削減」だけに集中する
* 自動連番は実装しない
* 成功後1件更新方式を維持する
* Prefix / Suffix / Replaceの仕様を変更しない
* 既存CSV互換を壊さない
* UI変更は必要最小限にする
* `FileAlreadyExists` の保護は必ず維持する
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
- SAF処理のDispatchers.IO対応
- リネーム中のローディング表示
- リネームボタンの二重押下防止強化
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
# STEP 4-11: SAF走査回数削減によるリネーム高速化 Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### ExecuteRenameUseCase.kt

### SafDocumentDataSource.kt

### StorageRepository.kt

### StorageRepositoryImpl.kt

### RenameMatchingViewModel.kt

### RenameMatchingFragment.kt

### ErrorDialog.kt

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
7. リネーム実行する
8. リネーム成功することを確認する
9. Suffixモードで1件リネームする
10. Replaceモードで1件リネームする
11. Prefix / Suffix / Replaceの連続リネームでFileNotFoundが再発しないことを確認する
12. 同名ファイルが既にある名前でリネームし、FileAlreadyExistsとして失敗表示されることを確認する
13. 失敗時に元ファイルがリネーム済みにならないことを確認する
14. 失敗時に候補が使用済みにならないことを確認する
15. EasyRenamePerfログでSTEP4-10よりUseCase全体時間が短くなっていることを確認する
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

今回の主目的は、SAF走査回数削減によりUseCase全体時間が短くなったか確認することです。

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
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=...
EasyRenamePerf: repository rename end elapsedMs=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=...
```

### 6. 比較観点

```text id="compare-points"
- STEP4-10では総時間が約2186〜2213msだった
- STEP4-10ではUseCase全体が約2122〜2164msだった
- STEP4-10ではRepository + SAFが約1352〜1405msだった
- STEP4-10ではSAF対象探索が約557〜603msだった
- STEP4-10ではrenameToが約343〜391msだった

STEP4-11後:
- UseCase全体時間が短くなっているか
- repository rename start前の待ち時間が減っているか
- saf resolve targetが大きく増えていないか
- renameToは従来と同程度か
- FileAlreadyExists時にRepository / SAFへ進みすぎていないか
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

* 作業ブランチ `feature/step4-11-saf-scan-optimization` で作業している
* 作業後にcommitしてpushしている
* UseCase側の事前同名チェックが削除されている
* DataSource側で1回の `listFiles()` 結果を対象探索と同名チェックに使っている
* `FileAlreadyExists` の保護が維持されている
* 成功後1件更新方式が維持されている
* Prefix / Suffix / Replace の既存動作が壊れていない
* 既存CSV形式 `A1-1_*` が従来どおり動く
* Prefix / Suffix / Replace の連続リネームで `FileNotFound` が再発しない
* STEP4-10よりUseCase全体時間が短くなっている
* ビルドが通る

```
```
