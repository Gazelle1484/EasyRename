以下は、前回の **Provider単位skip案を更新した版**です。
方針を **「singleUri `DocumentFile.renameTo()` の再試行抑制」ではなく、「`DocumentsContract.renameDocument()` をfast pathとして試し、失敗したらtreeUri fallbackへ進む」** に変更しています。

根拠は、Androidの `DocumentsContract.renameDocument()` が既存documentの表示名変更用APIであり、成功時に新しいdocument Uriを返し得る仕様だからです。([Android Developers][1])
一方、実機ログでは `singleUri` は名前解決・URI一致までは成功しているものの、`DocumentFile.renameTo()` で `UnsupportedOperation` になっており、treeUri fallbackでは成功しています。

````md id="step4-23-updated-documentscontract-prompt"
# STEP 4-23: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- `DocumentFile.fromSingleUri(...).renameTo(...)` をリネーム用fast pathとして使うのをやめる
- 代わりに `DocumentsContract.renameDocument(contentResolver, fileUri, newName)` をfast pathとして試す
- `DocumentsContract.renameDocument()` が成功した場合は、その戻り値Uriを `afterUri` として扱う
- `DocumentsContract.renameDocument()` が失敗、null返却、Unsupported、FileNotFoundになった場合は、既存のtreeUri fallbackへ進む
- STEP4-22で実装したtreeUri早期終了は維持する
- 通常リネーム、UNDO、自動連番、下部Insets対応を壊さない

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

## 今回実装する機能

- singleUri由来の `DocumentFile.renameTo()` をリネーム処理から外す
- `DocumentsContract.renameDocument()` をfast pathとして追加する
- `DocumentsContract.renameDocument()` 成功時は、その戻りUriを `RenameResult.afterUri` に入れる
- `DocumentsContract.renameDocument()` 成功時はtreeUri fallbackへ進まない
- `DocumentsContract.renameDocument()` 失敗時は既存のtreeUri fallbackへ進む
- `DocumentsContract.renameDocument()` の処理時間をログ出力する
- `DocumentsContract.renameDocument()` の成功/失敗理由をログ出力する
- treeUri fallbackの `listFiles source=selectedDirectory` を維持する
- treeUri fallbackの早期終了を維持する
- 通常リネームとUNDOの両方で同じ経路を使えるようにする

## 今回実装しない機能

- singleUri fast pathの完全削除
- Provider別skip状態の保存
- Provider別skip状態の永続化
- SharedPreferences保存
- treeUri探索範囲の変更
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- renameTo自体の高速化
- コピーして削除する疑似リネーム
- ContentResolver.update() によるDISPLAY_NAME変更
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

# 重要: STEP4-22のcommit / pushを先に完了すること

STEP4-22のCodex回答では以下の状態だった。

```text
STEP4-22 commit hash: 未作成
STEP4-22 push先: origin/feature/step4-22-tree-uri-early-exit 予定
未コミット差分: あり
````

そのため、STEP4-23の作業を始める前に、必ずSTEP4-22をcommit / pushすること。

## 1. 現在状態確認

```powershell
git status
git branch --show-current
```

現在ブランチが以下であることを確認する。

```text
feature/step4-22-tree-uri-early-exit
```

## 2. STEP4-22のビルド確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 3. STEP4-22の単体テスト確認

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text
BUILD SUCCESSFUL
```

## 4. STEP4-22をcommit / push

```powershell
git status
git add .
git commit -m "Stop tree URI search after first match"
git push -u origin feature/step4-22-tree-uri-early-exit
```

commit hashを控えること。

---

# STEP4-23 作業ブランチ

STEP4-22のcommit / push完了後、STEP4-23用ブランチを作成する。

```powershell
git checkout feature/step4-22-tree-uri-early-exit
git pull
git checkout -b feature/step4-23-documents-contract-rename
```

ブランチ名は以下とする。

```text
feature/step4-23-documents-contract-rename
```

作業完了後、実機確認OKのあとに以下を実行すること。

```powershell
git status
git add .
git commit -m "Use DocumentsContract rename fast path"
git push -u origin feature/step4-23-documents-contract-rename
```

Codexの最終回答では以下を報告すること。

* STEP4-22のcommit hash
* STEP4-22のpush結果
* STEP4-23の作業ブランチ名
* STEP4-23の分岐元ブランチ
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* 実機確認結果
* STEP4-23のcommit hash
* STEP4-23のpush結果
* 未コミット差分の有無
* 回答を保存した `doc/STEP4-23_codex.md`

---

# 背景

STEP4-21では、`singleUri fast path` と `treeUri fallback` の診断ログを追加した。

STEP4-22では、treeUri fallbackで対象ファイルが見つかった時点で、Kotlin側の比較ループを早期終了するようにした。

STEP4-22後の実機ログでは以下が確認できた。

```text
singleUri document resolved name=log (23).zip exists=true canWrite=true
singleUri name compare expected=log (23).zip actual=log (23).zip matches=true
singleUri accepted expected=log (23).zip actual=log (23).zip
renameTo start sourceName=log (23).zip targetName=A1-1-1_log (23).zip
singleUri failed reason=UnsupportedOperation
singleUri end success=false elapsedMs=61
```

つまり、`singleUri` は対象ファイル名の解決までは正しいが、`DocumentFile.fromSingleUri(...).renameTo(...)` はUnsupportedになっている。

一方、treeUri fallbackでは以下が確認できた。

```text
treeUri listFiles source=selectedDirectory
treeUri compare[7] candidateName=log (23).zip expectedName=log (23).zip matches=true
treeUri candidate matched name=log (23).zip ... matchedIndex=7
treeUri found ... scannedCount=7 matchedIndex=7
renameTo end success=true elapsedMs=430
treeUri end success=true elapsedMs=1025 scannedCount=7 matchedIndex=7
```

また、`singleUri` と `treeUri` で見つけたURIは同じだった。

```text
content://com.android.externalstorage.documents/tree/45FC-18EF%3ADownload%2Fdummy_test/document/45FC-18EF%3ADownload%2Fdummy_test%2Flog%20(23).zip
```

したがって、問題は「singleUriが違うファイルを掴んでいる」ことではなく、`DocumentFile.fromSingleUri(...).renameTo(...)` がリネーム操作として使えないことにある。

`DocumentsContract.renameDocument()` は既存documentの表示名を変更するためのAndroid標準APIで、Providerが新しいdocument IDを作る場合は新Uriを返す。
そのため、STEP4-23では `DocumentFile.fromSingleUri(...).renameTo(...)` の代わりに `DocumentsContract.renameDocument()` をfast pathとして試す。

---

# 実装方針

## 1. singleUri DocumentFile.renameTo を使わない

現在の流れが以下なら、

```text
DocumentFile.fromSingleUri(context, fileUri)
→ name / exists / canWrite確認
→ singleUriDocument.renameTo(newName)
→ UnsupportedOperation
→ treeUri fallback
```

次のように変更する。

```text
DocumentsContract.renameDocument(contentResolver, fileUri, newName)
→ 成功ならRenameResult success
→ null / 例外 / 失敗ならtreeUri fallback
```

`DocumentFile.fromSingleUri(...).renameTo(...)` は呼ばない。

## 2. DocumentsContract rename fast pathを追加する

`SafDocumentDataSource` に小さな関数を追加する。

例:

```kotlin
private fun tryRenameByDocumentsContract(
    fileUri: Uri,
    beforeName: String,
    afterName: String,
): RenameResult?
```

または、既存の戻り値設計に合わせて内部結果型を作ってもよい。

処理イメージ:

```kotlin
private fun tryRenameByDocumentsContract(
    fileUri: Uri,
    beforeName: String,
    afterName: String,
): RenameResult? {
    val start = SystemClock.elapsedRealtime()

    return try {
        Log.d(
            TAG_SAF_RESOLVE,
            "documentsContract rename start sourceName=$beforeName targetName=$afterName sourceUri=$fileUri"
        )

        val renamedUri = DocumentsContract.renameDocument(
            context.contentResolver,
            fileUri,
            afterName
        )

        val elapsedMs = SystemClock.elapsedRealtime() - start

        if (renamedUri != null) {
            Log.d(
                TAG_SAF_RESOLVE,
                "documentsContract rename success elapsedMs=$elapsedMs afterUri=$renamedUri"
            )

            RenameResult(
                beforeName = beforeName,
                afterName = afterName,
                success = true,
                afterUri = renamedUri,
            )
        } else {
            Log.d(
                TAG_SAF_RESOLVE,
                "documentsContract rename returned null elapsedMs=$elapsedMs"
            )
            null
        }
    } catch (e: FileNotFoundException) {
        val elapsedMs = SystemClock.elapsedRealtime() - start
        Log.d(
            TAG_SAF_RESOLVE,
            "documentsContract rename failed reason=FileNotFound elapsedMs=$elapsedMs exception=${e.message}"
        )
        null
    } catch (e: UnsupportedOperationException) {
        val elapsedMs = SystemClock.elapsedRealtime() - start
        Log.d(
            TAG_SAF_RESOLVE,
            "documentsContract rename failed reason=UnsupportedOperation elapsedMs=$elapsedMs exception=${e.message}"
        )
        null
    } catch (e: Exception) {
        val elapsedMs = SystemClock.elapsedRealtime() - start
        Log.d(
            TAG_SAF_RESOLVE,
            "documentsContract rename failed reason=Exception elapsedMs=$elapsedMs exception=${e::class.java.simpleName}:${e.message}"
        )
        null
    }
}
```

実際のコードでは、既存の `RenameErrorType` と整合させること。

## 3. 成功時はtreeUri fallbackへ進まない

`DocumentsContract.renameDocument()` が成功した場合は、その時点でリネーム成功として返す。

```text
documentsContract rename success
→ RenameResult.success=true
→ afterUri=renamedUri
→ treeUri fallbackへ進まない
```

## 4. 失敗時はtreeUri fallbackへ進む

以下の場合は、既存のtreeUri fallbackへ進む。

```text
- returned null
- FileNotFoundException
- UnsupportedOperationException
- SecurityException
- その他Exception
```

このとき、失敗を最終エラーにはしない。
treeUri fallbackも失敗した場合に、既存のエラー分類で最終失敗扱いにする。

## 5. treeUri fallbackは維持する

既存のtreeUri fallback処理は維持する。

必ず以下を維持する。

```text
- treeUri listFiles source=selectedDirectory
- treeUri candidate matched ... matchedIndex=...
- treeUri found ... scannedCount=... matchedIndex=...
- treeUri end ... elapsedMs=... scannedCount=... matchedIndex=...
```

## 6. 通常リネームとUNDOの両方で有効にする

通常リネームもUNDOも最終的に `SafDocumentDataSource.renameFile(...)` を通るなら、DataSource内で実装すれば両方に効く。

確認すること。

```text
- 通常リネームで documentsContract rename start が出る
- UNDOでも documentsContract rename start が出る
- 成功時または失敗時に既存フローが壊れない
```

---

# ログ仕様

タグは既存と同じ。

```kotlin
private const val TAG_SAF_RESOLVE = "EasyRenameSafResolve"
```

## DocumentsContract rename開始

```text
EasyRenameSafResolve: documentsContract rename start sourceName=... targetName=... sourceUri=...
```

## DocumentsContract rename成功

```text
EasyRenameSafResolve: documentsContract rename success elapsedMs=... afterUri=...
```

## DocumentsContract rename null返却

```text
EasyRenameSafResolve: documentsContract rename returned null elapsedMs=...
```

## DocumentsContract rename失敗

```text
EasyRenameSafResolve: documentsContract rename failed reason=FileNotFound elapsedMs=... exception=...
EasyRenameSafResolve: documentsContract rename failed reason=UnsupportedOperation elapsedMs=... exception=...
EasyRenameSafResolve: documentsContract rename failed reason=SecurityException elapsedMs=... exception=...
EasyRenameSafResolve: documentsContract rename failed reason=Exception elapsedMs=... exception=...
```

## fallback移行

```text
EasyRenameSafResolve: documentsContract rename fallback to treeUri reason=...
```

## 廃止する/出なくなることが期待されるログ

今回の変更後、通常リネームで以下は原則出ないことを期待する。

```text
singleUri accepted ...
renameTo start ...  // singleUri DocumentFile.renameTo由来のもの
singleUri failed reason=UnsupportedOperation
```

ただし、treeUri fallback側の `renameTo start` は維持する。

ログ名が紛らわしい場合は、以下のようにsourceを明記する。

```text
renameTo start source=treeUri sourceName=... targetName=...
```

または、

```text
documentsContract rename start ...
```

と、

```text
treeUri renameTo start ...
```

を分ける。

---

# 実装対象ファイル候補

最有力変更ファイル:

```text
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
```

必要に応じて変更:

```text
doc/STEP4-23_codex.md
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

* `DocumentFile.fromSingleUri(...).renameTo(...)` を残さない
* singleUri由来のDocumentFileでrenameToを再試行しない
* DocumentsContract失敗時に処理を即失敗で終わらせない
* DocumentsContract失敗時のtreeUri fallbackを削除しない
* treeUri探索範囲を変更しない
* treeUri早期終了を壊さない
* directory.listFiles() のキャッシュ化を今回実装しない
* 対象ファイルMap化を今回実装しない
* コピーして削除する疑似リネームを実装しない
* ContentResolver.update() によるDISPLAY_NAME変更を実装しない
* FragmentやViewModelから直接リネームAPIを呼ばない
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
5. 1件目をリネームする
6. documentsContract rename start が出ることを確認する
7. documentsContract rename success が出る場合、treeUri fallbackへ進まないことを確認する
8. documentsContract rename failed / returned null が出る場合、treeUri fallbackへ進むことを確認する
9. treeUri fallbackへ進んだ場合も、リネームが成功することを確認する
10. singleUri DocumentFile.renameTo由来の UnsupportedOperation が出なくなることを確認する
11. リネーム成功後、画面上のファイル名がリネーム後になることを確認する
12. UNDOを実行する
13. UNDOでも documentsContract rename start が出ることを確認する
14. UNDO成功時、ファイル名が戻ることを確認する
15. 自動連番ON/OFFの既存挙動が壊れていないことを確認する
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
- documentsContract rename start が出る
- documentsContract rename success または failed / returned null が出る
- documentsContract rename success の場合、afterUri が入っている
- documentsContract rename success の場合、treeUri fallback start が出ない
- documentsContract rename failed / returned null の場合、fallback to treeUri が出る
- fallback時に treeUri listFiles source=selectedDirectory が維持されている
- fallback時に treeUri matchedIndex / scannedCount が維持されている
- fallback時に treeUri renameTo end success=true が出る
- singleUri DocumentFile.renameTo由来の UnsupportedOperation が出ない
- 通常リネームが成功する
- UNDOが成功する
- AndroidRuntime のクラッシュが出ていない
```

---

# 実機ログを見た後の判断基準

## 1. DocumentsContract rename が成功する場合

期待どおり。

次STEP候補:

```text
STEP4-24: CSVプレビュー
```

または、性能確認を続けるなら、

```text
STEP4-24: DocumentsContract成功時のafterUri反映確認とログ整理
```

## 2. DocumentsContract rename がUnsupported / nullでtreeUri fallbackが成功する場合

機能としてはOK。

この場合、`DocumentFile.fromSingleUri(...).renameTo(...)` の無駄は消せたが、根本的にはtreeUri fallback頼み。

次STEP候補:

```text
STEP4-24: CSVプレビュー
```

または、

```text
STEP4-24: directory.listFiles() キャッシュ化 / 対象ファイルMap化
```

## 3. DocumentsContract rename 成功後にafterUriが古い/不正な場合

次STEP候補:

```text
STEP4-24: DocumentsContract成功時のafterUri検証強化
```

確認すること。

```text
- 戻り値Uriがnullでないか
- 戻り値Uriで次回リネーム/UNDOできるか
- Home/Matching側の表示更新が新Uriを使っているか
```

## 4. DocumentsContract rename 成功後にUNDOが失敗する場合

履歴に保存されたafterUriまたはHome/Matching側のUri更新に問題がある可能性が高い。

次STEP候補:

```text
STEP4-24: DocumentsContract経路の履歴afterUri検証
```

---

# 単体テスト方針

今回の変更は `ContentResolver` / `DocumentsContract` / SAF依存が強いため、無理にローカルJVMテストを追加しなくてよい。

ただし、fast path結果を内部sealed classなどに分ける場合のみ、以下のような純粋ロジックをテストしてよい。

```text
- successならtreeUri fallbackしない
- null/exceptionならtreeUri fallbackする
- success時にafterUriを結果へ入れる
```

大規模リファクタになる場合はテスト追加を見送ること。

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何を変更したか
* `DocumentFile.fromSingleUri(...).renameTo(...)` を使わなくしたこと
* `DocumentsContract.renameDocument()` をfast pathとして追加したこと
* 成功時に `afterUri` をどう扱うか
* 失敗時にtreeUri fallbackへ進むこと
* treeUri早期終了を維持していること
* リネーム / UNDO / 自動連番を変更していないこと

## 2. 変更ファイル一覧

例:

```text
- SafDocumentDataSource.kt
- doc/STEP4-23_codex.md
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

* STEP4-22のcommit hash
* STEP4-22のpush結果
* STEP4-23の作業ブランチ名
* STEP4-23の分岐元ブランチ
* STEP4-23のcommit message
* STEP4-23のcommit hash
* STEP4-23のpush先
* 未コミット差分の有無

## 6. 保存ファイル

Codexの回答は以下へ保存すること。

```text
doc/STEP4-23_codex.md
```

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text
- DocumentsContract成功時のafterUri詳細検証
- DocumentsContract失敗Providerの記録
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
- STEP4-23ではDocumentsContract.renameDocumentをfast pathとして試す
- ProviderによってはDocumentsContract.renameDocumentもUnsupported / null / FileNotFoundになる可能性がある
- DocumentsContract失敗時はtreeUri fallbackへ進む設計
- DocumentsContract成功時に返るafterUriはProviderにより元Uriと同じ場合も変わる場合もある
- afterUri更新が正しくないと、次回リネームやUNDOに影響する
- コピー削除方式は未実装
- ContentResolver.updateによるDISPLAY_NAME変更は未実装
- directory.listFiles() 自体のProvider側コストはfallback時に残る
- ファイル名やUriログはリリース前に抑制方針を検討する必要がある
```

---

# 完了条件

このSTEP4-23の完了条件は以下。

```text
- STEP4-22がcommit / pushされている
- assembleDebug が成功する
- testDebugUnitTest が成功する
- DocumentFile.fromSingleUri(...).renameTo(...) を使っていない
- DocumentsContract.renameDocument のfast pathが実装されている
- DocumentsContract rename start ログが出る
- DocumentsContract成功時にafterUriがRenameResultへ反映される
- DocumentsContract成功時はtreeUri fallbackへ進まない
- DocumentsContract失敗時はtreeUri fallbackへ進む
- treeUri fallback時は既存どおりリネーム成功できる
- treeUri listFiles source=selectedDirectory が維持されている
- treeUri早期終了が維持されている
- 通常リネームが壊れていない
- UNDOが壊れていない
- 自動連番が壊れていない
- 実機確認後にSTEP4-23がcommit / pushされている
- Codex回答が doc/STEP4-23_codex.md に保存されている
```

```

**ベスト案**  
STEP4-23は、`DocumentFile.fromSingleUri(...).renameTo(...)` をやめて、`DocumentsContract.renameDocument()` をfast pathに置き換えるのが一番筋が良いです。`singleUri` で対象URIと名前が正しいことはログで確認済みなので、次は「そのURIに対して低レベルAPIでrenameできるか」を確認する価値があります。

**代替案**  
`DocumentsContract.renameDocument()` も失敗するProviderなら、結局treeUri fallback頼みになります。その場合でも、singleUri由来の `DocumentFile.renameTo()` でUnsupportedを出す無駄は消せます。

**注意点**  
`DocumentsContract.renameDocument()` が成功した場合、戻り値Uriを必ず `afterUri` として扱わせてください。Providerによってはリネーム後にdocument IDが変わるため、ここを更新しないと次回リネームやUNDOで古いUri問題が再発します。
::contentReference[oaicite:2]{index=2}
```

[1]: https://developer.android.com/reference/android/provider/DocumentsContract?utm_source=chatgpt.com "DocumentsContract | API reference"
