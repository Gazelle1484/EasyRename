````md id="step4-12-prompt"
# STEP 4-12: 実装（機能単位）用 Codex 指示

STEP4-11では、UseCase側の事前同名チェックを削除し、`SafDocumentDataSource.renameFile()` 内の1回の `directory.listFiles()` 結果で対象ファイル探索と同名チェックを行うようにしました。これにより、UseCase全体時間はSTEP4-10の約2122〜2164msから、STEP4-11後は約1246msまで短縮されています。:contentReference[oaicite:0]{index=0}

一方で、STEP4-11後のログでは `saf resolve target` が約812msかかっており、次の主なボトルネックは `treeUri` からの `directory.listFiles()` による対象ファイル探索です。:contentReference[oaicite:1]{index=1}

このSTEPでは、**singleUri直接リネームを先に試し、失敗時のみtreeUri探索へfallbackする高速化** を対象にしてください。
回答はdec/STEP4-12_codex.mdに保存すること。

---

## 目的

- 1機能ずつ確実に完成させる
- `treeUri` 探索による `listFiles()` 時間を可能な範囲で削減する
- `singleUri` 直接リネームが成功する場合は、`directory.listFiles()` を省略する
- `singleUri` 直接リネームが失敗する場合は、既存の `treeUri` 探索方式へfallbackする
- `UnsupportedOperationException` でクラッシュしない挙動を維持する
- `FileAlreadyExists` 保護を維持する
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
git checkout -b feature/step4-12-single-uri-fast-path
````

### 注意

STEP4-12はSTEP4-11の高速化を前提にします。
`main` にSTEP4-11までの変更が入っていない場合は、作業に必要な最新ブランチを確認し、どのブランチから分岐するべきかを報告してください。

必要であれば、以下のようにSTEP4-11ブランチから分岐してください。

```powershell id="git-alt-start"
git checkout feature/step4-11-saf-scan-optimization
git pull
git checkout -b feature/step4-12-single-uri-fast-path
```

### 作業後に実行

ビルドとテスト成功後、以下を実行してください。

```powershell id="git-finish"
git status
git add .
git commit -m "Add single URI rename fast path"
git push -u origin feature/step4-12-single-uri-fast-path
```

### Git注意事項

* `main` に直接コミットしない
* 作業前に `git status` で未コミット差分を確認する
* 未コミット差分がある場合は、内容を報告してから作業する
* 作業後はcommitとpushまで行う
* pushに失敗した場合は、エラー内容を報告する

---

## 実機ログ分析結果

STEP4-11後の成功ケースでは、以下が確認されています。

```text id="perf-result"
rename total: 1310ms
useCase: 1246ms
repository + SAF: 1246ms
saf resolve target: 812ms
renameTo: 404ms
matching state update: 35ms
home applyRenameResult: 1ms
```

STEP4-10との比較では、UseCase側の事前同名チェック削除により、約0.8秒程度の削減が確認されています。
次の削減候補は `saf resolve target` の約812msです。

---

## 現状の問題

現在のリネーム方式は、安定性を優先して `treeUri` 探索方式になっています。

```text id="current-flow"
directoryUri から DocumentFile.fromTreeUri
→ directory.listFiles()
→ 対象ファイルを探す
→ 同名ファイルを探す
→ targetFile.renameTo(newName)
```

この方式は安定していますが、毎回 `directory.listFiles()` が必要であり、対象探索だけで約812msかかっています。

以前の `singleUri` 直接方式では `UnsupportedOperationException` が発生したことがあります。
そのため、単純に `singleUri` 方式へ戻すのではなく、**fast path + fallback** にしてください。

---

## 今回やること / やらないこと

### 今回やること

```text id="do-this-step"
1. singleUri直接renameを先に試すfast pathを追加する
2. singleUri方式が成功した場合はtreeUri探索を省略する
3. singleUri方式が失敗した場合のみ既存のtreeUri探索方式へfallbackする
4. UnsupportedOperationException / SecurityException / IllegalArgumentException / false戻り値を安全に扱う
5. FileAlreadyExists保護を維持する
6. EasyRenamePerfログでfast path成功 / fallback発生 / fallback成功を確認できるようにする
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
11. コピー作成 + 元ファイル削除の擬似リネーム
12. SAF処理全体の大規模再設計
```

---

## 対象範囲

今回触ってよい主なファイルは以下です。

```text id="scope-files"
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
app/src/main/java/com/example/easyrename/model/RenameResult.kt
app/src/main/java/com/example/easyrename/model/RenameErrorType.kt
```

必要に応じて以下も変更して構いません。

```text id="optional-files"
app/src/main/java/com/example/easyrename/data/repository/StorageRepository.kt
app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt
app/src/main/java/com/example/easyrename/domain/usecase/ExecuteRenameUseCase.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
```

---

## 禁止事項

* `singleUri` 方式だけに戻さない
* fallbackなしで `treeUri` 探索方式を削除しない
* `UnsupportedOperationException` をクラッシュさせない
* `FileAlreadyExists` の検出を削除しない
* 上書きリネームしない
* 自動連番を今回実装しない
* 一括リネームを追加しない
* Prefix / Suffix / Replace の仕様を変更しない
* 既存CSV形式 `A1-1_*` の挙動を変えない
* リネーム後の状態更新方式を全件再読み込みへ戻さない
* UIを大規模に変更しない
* DIライブラリを追加しない

---

# STEP 4-12-1: singleUri直接renameのfast pathを追加する

## 目的

`singleUri` 直接リネームが成功するProviderでは、`directory.listFiles()` を省略して高速化する。

## 対象

* `SafDocumentDataSource.renameFile`

## 実装内容

`renameFile(directoryUri, fileUri, newName)` の先頭で、まず `DocumentFile.fromSingleUri(context, fileUri)` を使った直接リネームを試してください。

流れ:

```text id="single-uri-flow"
1. singleFile = DocumentFile.fromSingleUri(context, fileUri)
2. singleFile が null ならfast path失敗としてfallback
3. singleFile.name を beforeName として取得
4. singleFile.renameTo(newName) をtry-catchで実行
5. renameToがtrueならRenameResult.success = trueで返す
6. renameToがfalseまたは例外ならtreeUri探索方式へfallback
```

## 注意

* `UnsupportedOperationException` を必ず捕捉する
* `SecurityException` を必ず捕捉する
* `IllegalArgumentException` を必ず捕捉する
* その他 `Exception` も捕捉する
* 捕捉した例外はログに出すが、クラッシュさせない
* 失敗時は既存のtreeUri探索方式へ進む

---

# STEP 4-12-2: FileAlreadyExists保護の扱いを安全側で設計する

## 背景

`singleUri` fast pathでは `directory.listFiles()` を省略するため、同名ファイル存在チェックができません。
一方、同名チェックのために毎回 `listFiles()` すると、fast pathの高速化効果がなくなります。

## 方針

このSTEPでは以下の安全側設計にしてください。

```text id="duplicate-policy"
1. singleUri fast pathを試す
2. renameToが成功した場合は、そのProviderがリネームを受理したものとして成功扱いする
3. renameToがfalse / 例外の場合はtreeUri探索方式へfallbackする
4. fallback側では従来どおりlistFiles結果でFileAlreadyExistsを検出する
```

## 注意

* Android Providerによって、同名がある場合に `renameTo` がfalseや例外になる場合がある
* `singleUri` 成功時に同名があってもProviderがどう扱うかはProvider依存
* このリスクをログと未解決事項に明記する
* 上書きの危険があるProviderが確認された場合は、次STEPでfast path前の軽量同名チェックまたは設定化を検討する

---

# STEP 4-12-3: fallback付きtreeUri探索方式を維持する

## 目的

`singleUri` 直接方式が失敗するProviderでも、STEP4-11までの安定動作を維持する。

## 対象

* `SafDocumentDataSource.renameFile`

## 実装内容

`singleUri` fast pathが失敗した場合は、既存のtreeUri探索方式をそのまま使ってください。

既存fallback側の要件:

```text id="fallback-requirements"
- directory = DocumentFile.fromTreeUri(context, directoryUri)
- children = directory.listFiles().filter { it.isFile }
- URI一致で対象ファイルを探す
- 見つからなければbeforeName一致でfallbackする
- 同名ファイルをchildrenから探す
- targetFile自身でない同名ファイルがあればFileAlreadyExistsを返す
- targetFile.renameTo(newName)を実行する
- 結果をRenameResultで返す
```

---

# STEP 4-12-4: RenameResultにrename path情報を追加する

## 目的

実機ログとUI状態から、どの経路でリネームされたか分かるようにする。

## 対象

* `RenameResult`

## 実装内容

必要に応じて、`RenameResult` に以下のような情報を追加してください。

```kotlin id="rename-path"
enum class RenamePath {
    SingleUri,
    TreeUriFallback,
}
```

```kotlin id="rename-result-path"
data class RenameResult(
    ...
    val renamePath: RenamePath? = null,
)
```

## 注意

* 変更範囲が大きくなる場合は、`RenameResult` には追加せずログだけでもよい
* 既存呼び出しが壊れないようデフォルト値を設定する
* UI表示に必須ではない
* 主目的はログ確認

---

# STEP 4-12-5: EasyRenamePerfログを追加・整理する

## 目的

fast pathが効いているか、fallbackへ進んでいるかを確認できるようにする。

## 対象

* `SafDocumentDataSource.renameFile`

## 必須ログ

```text id="required-logs"
EasyRenamePerf: singleUri fast path start
EasyRenamePerf: singleUri fast path success elapsedMs=...
EasyRenamePerf: singleUri fast path failed reason=... elapsedMs=...
EasyRenamePerf: treeUri fallback start
EasyRenamePerf: treeUri fallback success elapsedMs=...
EasyRenamePerf: treeUri fallback failed reason=... elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=... path=...
```

## 維持するログ

```text id="keep-logs"
EasyRenamePerf: rename total elapsedMs=...
EasyRenamePerf: useCase end elapsedMs=...
EasyRenamePerf: repository rename end elapsedMs=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
```

## 注意

* ログ量は多くしすぎない
* URIのフル出力が多すぎる場合は最小限にする
* 失敗時は例外クラス名を出す

---

# STEP 4-12-6: fallback時のエラー種別を維持する

## 目的

fast path導入後も、ユーザー向け失敗表示を壊さない。

## 対象

* `SafDocumentDataSource.renameFile`
* `RenameResult`
* `RenameErrorType`
* 必要に応じて `ErrorDialog`

## 実装内容

以下を維持してください。

```text id="error-requirements"
- 同名ファイルあり: RenameErrorType.FileAlreadyExists
- 対象ファイルなし: RenameErrorType.FileNotFound
- 権限不足: RenameErrorType.PermissionDenied
- UnsupportedOperationException: RenameErrorType.UnsupportedOperation
- その他: RenameErrorType.Unknown または RenameFailed
```

## 注意

* singleUri fast path失敗時にすぐユーザーエラーにせず、まずfallbackを試す
* fallbackも失敗した場合に最終エラーとして返す
* fallback成功時はfast path失敗をユーザーに出さない

---

## 実装ルール

* 今回は「singleUri fast path + treeUri fallback」だけに集中する
* 自動連番は実装しない
* 成功後1件更新方式を維持する
* Prefix / Suffix / Replaceの仕様を変更しない
* 既存CSV互換を壊さない
* `FileAlreadyExists` 保護をfallback側で維持する
* `UnsupportedOperationException` でクラッシュしない
* UI変更は原則しない
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
- singleUri fast pathのON/OFF設定
- fast path前の軽量同名チェック
- Provider別のrename方式選択
- SAF処理のDispatchers.IO対応
- リネーム中のローディング表示
- リネームボタンの二重押下防止強化
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
# STEP 4-12: singleUri fast pathによるリネーム高速化 Codex回答

## 作業ブランチ

## 実装内容

## 実装コード

### SafDocumentDataSource.kt

### RenameResult.kt

### RenameErrorType.kt

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
12. UnsupportedOperationException が発生してもアプリが落ちないことを確認する
13. singleUri fast pathが成功した場合、treeUri fallbackへ進んでいないことをログで確認する
14. singleUri fast pathが失敗した場合、treeUri fallbackで成功することをログで確認する
15. 同名ファイルが既にある名前でリネームし、FileAlreadyExistsとして失敗表示されることを確認する
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

今回の主目的は、`singleUri` fast pathが有効か、fallbackが正しく動くか、総時間が短くなるかの確認です。

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
EasyRenamePerf: singleUri fast path start
EasyRenamePerf: singleUri fast path success elapsedMs=...
EasyRenamePerf: singleUri fast path failed reason=... elapsedMs=...
EasyRenamePerf: treeUri fallback start
EasyRenamePerf: treeUri fallback success elapsedMs=...
EasyRenamePerf: saf resolve target end elapsedMs=...
EasyRenamePerf: saf renameTo end success=true elapsedMs=...
EasyRenamePerf: saf renameFile end elapsedMs=... path=...
```

### 6. 比較観点

```text id="compare-points"
STEP4-11:
- rename total: 約1310ms
- useCase: 約1246ms
- repository + SAF: 約1246ms
- saf resolve target: 約812ms
- renameTo: 約404ms

STEP4-12後:
- singleUri fast path success が出ているか
- fast path成功時に treeUri fallback が出ていないか
- saf resolve target が省略または大幅短縮されているか
- rename total がSTEP4-11より短くなっているか
- fallback時でもSTEP4-11相当の安定性が維持されているか
- FileAlreadyExists が維持されているか
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

* 作業ブランチ `feature/step4-12-single-uri-fast-path` で作業している
* 作業後にcommitしてpushしている
* `singleUri` 直接renameを先に試している
* `singleUri` 成功時は `treeUri` 探索を省略している
* `singleUri` 失敗時は `treeUri` 探索へfallbackしている
* `UnsupportedOperationException` でクラッシュしない
* `FileAlreadyExists` の保護がfallback側で維持されている
* 成功後1件更新方式が維持されている
* Prefix / Suffix / Replace の既存動作が壊れていない
* 既存CSV形式 `A1-1_*` が従来どおり動く
* Prefix / Suffix / Replace の連続リネームで `FileNotFound` が再発しない
* STEP4-11より `rename total` または `saf resolve target` が短くなっている
* ビルドが通る

```
```
