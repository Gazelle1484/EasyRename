以下をそのまま `doc/STEP4-24.md` として保存して、Codexに渡してください。
STEP4-23の実機ログでは `DocumentsContract.renameDocument()` が成功し、`afterUri` もリネーム後Uriとして返っています。一方で、Codex回答上は「通常リネーム / UNDO操作確認: 未実施」のままなので、STEP4-24では **DocumentsContract成功経路でのafterUri連続利用・UNDO確認ログ強化** に絞るのが安全です。

````md id="step4-24-prompt"
# STEP 4-24: 実装（機能単位）

## 目的

- 1機能ずつ確実に完成させる
- STEP4-23で導入した `DocumentsContract.renameDocument()` 成功経路について、`afterUri` が次回リネーム・UNDO・Home/Matching表示更新に正しく使われているか確認できるようにする
- 通常リネーム成功後、返却された `afterUri` が履歴・Matching画面・Home画面へ反映されていることをログで確認する
- UNDO実行時に、履歴に保存された `afterUri` を使って戻せることを確認する
- 今回は確認ログと必要最小限の状態同期修正だけを行う
- CSVプレビューなど別機能は今回実装しない

## 指示方法

- 「機能単位」で分割して指示する
- 今回は以下の1機能だけを実装すること

## 今回実装する機能

- `DocumentsContract.renameDocument()` 成功時に返った `afterUri` が、`RenameResult.afterUri` に入っていることをログで確認できるようにする
- リネーム成功後、Matching側の対象ファイル一覧が `afterUri` に更新されていることをログで確認できるようにする
- リネーム成功後、Home側の対象ファイル一覧が `afterUri` に更新されていることをログで確認できるようにする
- リネーム成功履歴に保存される `afterUri` が `DocumentsContract` の戻りUriになっていることをログで確認できるようにする
- UNDO押下時に、履歴の `afterUri` を使って逆リネームしていることをログで確認できるようにする
- UNDO成功後、Matching側・Home側が元ファイル名とUNDO後Uriへ更新されていることをログで確認できるようにする
- `renamePath=SingleUri` または `DocumentsContract` 経路で成功したことが、履歴ログ・SAFログから分かるようにする
- 既存のリネーム処理、UNDO、自動連番、下部Insets対応を壊さない

## 今回実装しない機能

- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- リネームモード選択式ボタンの幅調整
- 前回のセット再押下で選択解除する処理
- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- UNDO成功時の自動連番カウンタ巻き戻し
- DocumentsContract失敗Providerの記録
- Provider別fast pathスキップ
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- RecyclerView化
- XMLレイアウト大規模変更
- Material Componentsへの本格移行
- リリース向けログ抑制

---

# 重要: STEP4-23のcommit / pushを先に完了すること

STEP4-23のCodex回答では以下の状態だった。

```text
STEP4-23 commit hash: 未作成
STEP4-23 push先: origin/feature/step4-23-documents-contract-rename 予定
未コミット差分: あり
````

そのため、STEP4-24の作業を始める前に、必ずSTEP4-23をcommit / pushすること。

## 1. 現在状態確認

```powershell id="krcf0c"
git status
git branch --show-current
```

現在ブランチが以下であることを確認する。

```text id="zu4sjo"
feature/step4-23-documents-contract-rename
```

## 2. STEP4-23のビルド確認

```powershell id="plg5c7"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text id="o57tjr"
BUILD SUCCESSFUL
```

## 3. STEP4-23の単体テスト確認

```powershell id="d40vzc"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text id="fvpg0w"
BUILD SUCCESSFUL
```

## 4. STEP4-23をcommit / push

```powershell id="yabc52"
git status
git add .
git commit -m "Use DocumentsContract rename fast path"
git push -u origin feature/step4-23-documents-contract-rename
```

commit hashを控えること。

---

# STEP4-24 作業ブランチ

STEP4-23のcommit / push完了後、STEP4-24用ブランチを作成する。

```powershell id="eq0kpi"
git checkout feature/step4-23-documents-contract-rename
git pull
git checkout -b feature/step4-24-verify-documents-contract-uri
```

ブランチ名は以下とする。

```text id="z0pn7f"
feature/step4-24-verify-documents-contract-uri
```

作業完了後、実機確認OKのあとに以下を実行すること。

```powershell id="yypg8s"
git status
git add .
git commit -m "Verify DocumentsContract rename URI propagation"
git push -u origin feature/step4-24-verify-documents-contract-uri
```

Codexの最終回答では以下を報告すること。

* STEP4-23のcommit hash
* STEP4-23のpush結果
* STEP4-24の作業ブランチ名
* STEP4-24の分岐元ブランチ
* 変更ファイル一覧
* 実装内容
* ビルド結果
* テスト結果
* 実機確認結果
* STEP4-24のcommit hash
* STEP4-24のpush結果
* 未コミット差分の有無
* 回答を保存した `doc/STEP4-24_codex.md`

---

# 背景

STEP4-23では、singleUri由来の `DocumentFile.renameTo()` をfast pathとして使うのをやめ、代わりに `DocumentsContract.renameDocument()` をfast pathとして試すようにした。

実機ログでは以下が確認できた。

```text id="n0n2jx"
documentsContract rename start sourceName=log (24).zip targetName=A1-1-1_log (24).zip
documentsContract rename success elapsedMs=549 afterUri=content://.../A1-1-1_log%20(24).zip
singleUri end success=true elapsedMs=607
```

つまり、今回の実機Providerでは `DocumentsContract.renameDocument()` が成功している。

また、成功時の `afterUri` はリネーム後ファイル名を含むUriになっている。

```text id="h0s9la"
afterUri=content://com.android.externalstorage.documents/tree/.../document/.../A1-1-1_log%20(24).zip
```

このため、次に確認すべきことは以下。

```text id="bcjkm8"
- RenameResult.afterUri にこのUriが入っているか
- Matching側の targetFiles の uri / id がこのUriに更新されているか
- Home側の targetFiles の uri / id がこのUriに更新されているか
- RenameHistoryRecord.afterUri にこのUriが保存されているか
- UNDO実行時にこの afterUri を使って戻せるか
```

STEP4-24では、これらを確認するログを追加し、必要なら最小限の状態同期修正を行う。

---

# 実装方針

## 1. RenameResult.afterUri確認ログ

`SafDocumentDataSource` で `DocumentsContract.renameDocument()` 成功後、`RenameResult` を返す直前に以下を出す。

```text id="b01u8r"
EasyRenameSafResolve: result path=DocumentsContract success=true beforeName=... afterName=... afterUri=...
```

既に `renamePath=RenamePath.SingleUri` のような情報がある場合は、それも出す。

```text id="a88fkv"
EasyRenameSafResolve: result renamePath=SingleUri afterUri=...
```

注意:

```text id="qv6jz5"
ここでの SingleUri は「singleUri DocumentFile.renameTo」ではなく、DocumentsContract.renameDocument 成功経路を意味する。
命名が紛らわしい場合は DocumentsContract などへ変更を検討する。
```

ただし、今回のSTEPでenum名を変えると影響範囲が広がる場合は、ログだけで補足する。

---

## 2. RenameHistoryRecord.afterUri確認ログ

`RenameMatchingViewModel` で履歴追加するときに、以下を出す。

```text id="yx5a0r"
EasyRenameHistory: history add afterUri source=RenameResult.afterUri afterUri=...
EasyRenameHistory: history add beforeUri=... afterUri=... renamePath=...
```

確認すること。

```text id="mi6nxp"
RenameHistoryRecord.afterUri == RenameResult.afterUri
```

もし `RenameResult.afterUri` がnullの場合は、既存どおり旧Uri fallbackになる可能性がある。
ただし、DocumentsContract成功時はnullでないことを期待する。

---

## 3. Matching側状態更新ログ

リネーム成功後、Matching側の対象ファイル一覧を更新する箇所でログを出す。

例:

```text id="g9gzqt"
EasyRenameStateSync: matching applyRenameResult sourceFileId=...
EasyRenameStateSync: matching before displayName=... uri=... id=...
EasyRenameStateSync: matching after displayName=... uri=... id=... isRenamed=true
```

確認すること。

```text id="xkfdtm"
- after displayName がリネーム後名
- after uri が RenameResult.afterUri
- after id が afterUri.toString()
```

タグは新規で以下を推奨する。

```kotlin id="m5dpvh"
private const val TAG_STATE_SYNC = "EasyRenameStateSync"
```

---

## 4. Home側状態更新ログ

`HomeViewModel.applyRenameResult(result)` など、Home側更新処理でログを出す。

例:

```text id="gpv8x4"
EasyRenameStateSync: home applyRenameResult sourceFileId=...
EasyRenameStateSync: home before displayName=... uri=... id=...
EasyRenameStateSync: home after displayName=... uri=... id=... isRenamed=true
```

確認すること。

```text id="et862o"
- Home側もMatching側と同じ afterUri に更新されている
- Homeへ戻った後も古いUriを保持していない
```

---

## 5. UNDO開始時のafterUri使用ログ

`RenameMatchingViewModel.onUndoClicked()` または `UndoRenameUseCase` 実行前に、以下を出す。

```text id="x6zsjf"
EasyRenameHistory: undo start record.beforeName=... record.afterName=...
EasyRenameHistory: undo sourceUriFromHistoryAfterUri=...
```

`UndoRenameUseCase` 側では以下を出す。

```text id="mo6ftz"
EasyRenameHistory: undo usecase sourceUri=record.afterUri targetName=record.beforeName
```

確認すること。

```text id="t1n8y5"
UNDOで使っているsourceUriがRenameHistoryRecord.afterUriであること
```

---

## 6. UNDO成功後の状態更新ログ

UNDO成功後、Matching側とHome側で元ファイル名に戻す処理のログを出す。

Matching側:

```text id="e4d2vq"
EasyRenameStateSync: matching applyUndo before displayName=... uri=... id=...
EasyRenameStateSync: matching applyUndo after displayName=... uri=... id=... isRenamed=false
```

Home側:

```text id="hpuo0p"
EasyRenameStateSync: home applyUndo before displayName=... uri=... id=...
EasyRenameStateSync: home applyUndo after displayName=... uri=... id=... isRenamed=false
```

確認すること。

```text id="ijhh06"
- UNDO後displayNameが元ファイル名に戻る
- UNDO後uriがUNDO結果のafterUriになる
- isRenamed=false に戻る
```

---

# 必要な最小修正

今回の主目的はログ確認だが、ログ追加中に以下の不整合が見つかった場合は、必要最小限で修正すること。

## 修正してよいもの

```text id="o7ag8v"
- DocumentsContract成功時のRenameResult.afterUriがnullになっている
- Matching側更新でRenameResult.afterUriを使っていない
- Home側更新でRenameResult.afterUriを使っていない
- 履歴追加でRenameResult.afterUriではなく旧Uriを保存している
- UNDOがrecord.afterUriではなくrecord.beforeUriを使っている
```

## 修正してはいけないもの

```text id="nz3flz"
- リネーム仕様そのもの
- UNDO仕様そのもの
- treeUri探索範囲
- DocumentsContract失敗時のfallback方針
- 自動連番カウンタ挙動
- CSV処理
```

---

# ログ仕様

## 1. SAF結果ログ

タグ:

```text id="an8l7r"
EasyRenameSafResolve
```

ログ例:

```text id="hhfhgo"
EasyRenameSafResolve: documentsContract rename success elapsedMs=... afterUri=...
EasyRenameSafResolve: result path=DocumentsContract success=true beforeName=... afterName=... afterUri=...
```

## 2. 履歴ログ

タグ:

```text id="mvk8q5"
EasyRenameHistory
```

ログ例:

```text id="qbmckk"
EasyRenameHistory: history add beforeUri=... afterUri=... renamePath=...
EasyRenameHistory: undo sourceUriFromHistoryAfterUri=...
```

## 3. 状態同期ログ

タグ:

```text id="7bh6sm"
EasyRenameStateSync
```

ログ例:

```text id="4vi9t1"
EasyRenameStateSync: matching applyRenameResult ...
EasyRenameStateSync: home applyRenameResult ...
EasyRenameStateSync: matching applyUndo ...
EasyRenameStateSync: home applyUndo ...
```

---

# 実装対象ファイル候補

実際の構成に合わせて確認すること。

想定される変更ファイル:

```text id="y2vkph"
app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt
app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt
app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt
app/src/main/java/com/example/easyrename/domain/usecase/UndoRenameUseCase.kt
```

必要に応じて変更:

```text id="o89445"
app/src/main/java/com/example/easyrename/model/RenameResult.kt
app/src/main/java/com/example/easyrename/domain/history/RenameHistoryRecord.kt
```

ただし、データ構造変更は最小限にすること。

ドキュメント出力:

```text id="n0vzju"
doc/STEP4-24_codex.md
```

---

# 禁止事項

* CSVプレビューを今回実装しない
* Spinner横幅調整を今回実装しない
* 複数件UNDOを今回実装しない
* 作業履歴一覧表示を今回実装しない
* 履歴の永続化を今回実装しない
* UNDO成功時の自動連番カウンタ巻き戻しを実装しない
* DocumentsContract失敗Providerの記録を今回実装しない
* Provider別fast pathスキップを今回実装しない
* directory.listFiles() のキャッシュ化を今回実装しない
* 対象ファイル情報のMap化を今回実装しない
* treeUri探索範囲を変更しない
* treeUri早期終了を壊さない
* リネーム仕様を変えない
* 自動連番仕様を変えない
* FragmentやViewModelから直接DocumentsContractやDocumentFile.renameToを呼ばない
* 大規模リファクタをしない

---

# 動作確認方法

## 1. ビルド確認

以下を実行すること。

```powershell id="c7n030"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

期待結果:

```text id="mk6e8z"
BUILD SUCCESSFUL
```

## 2. 単体テスト

以下を実行すること。

```powershell id="zwgjme"
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

期待結果:

```text id="r5ri3j"
BUILD SUCCESSFUL
```

## 3. 実機確認

以下を確認する。

```text id="q5ocld"
1. アプリを起動する
2. リネーム対象ディレクトリを選択する
3. CSVファイルを選択する
4. マッチング画面へ進む
5. 1件リネームする
6. documentsContract rename success が出ることを確認する
7. result path=DocumentsContract の afterUri がリネーム後Uriであることを確認する
8. EasyRenameHistory の履歴追加ログで afterUri が同じUriであることを確認する
9. EasyRenameStateSync のMatching更新ログで after uri が同じUriであることを確認する
10. EasyRenameStateSync のHome更新ログで after uri が同じUriであることを確認する
11. Homeへ戻り、リネーム後ファイル名で表示されることを確認する
12. 再度Matching画面へ進み、古いUriではなくリネーム後Uri相当で扱われることを確認する
13. UNDOを実行する
14. UNDO開始ログで record.afterUri をsourceUriとして使っていることを確認する
15. UNDO成功後、Matching側表示が元ファイル名に戻ることを確認する
16. UNDO成功後、Home側表示が元ファイル名に戻ることを確認する
17. AndroidRuntime のクラッシュが出ていないことを確認する
```

---

# 実機で確認するべきログ

## 1. 端末確認

```powershell id="qt08em"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

## 2. ログクリア

```powershell id="kvg506"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c
```

## 3. SAF探索ログ確認

```powershell id="wh4suq"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

## 4. 履歴ログ確認

```powershell id="to6093"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

## 5. 状態同期ログ確認

```powershell id="ov2kmv"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameStateSync
```

## 6. クラッシュ確認

```powershell id="gsh0cw"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr "AndroidRuntime EasyRename Exception"
```

## 7. 確認するログ観点

以下を確認する。

```text id="wom32p"
- documentsContract rename success の afterUri がリネーム後Uriである
- result path=DocumentsContract の afterUri が同じである
- history add の afterUri が同じである
- matching applyRenameResult after uri が同じである
- home applyRenameResult after uri が同じである
- UNDO start で sourceUriFromHistoryAfterUri が同じである
- UNDO usecase sourceUri が record.afterUri である
- UNDO成功後の matching applyUndo after uri がUNDO結果Uriである
- UNDO成功後の home applyUndo after uri がUNDO結果Uriである
- AndroidRuntime のクラッシュが出ていない
```

---

# 実機ログを見た後の判断基準

## 1. afterUriがすべて一致し、UNDOも成功する場合

DocumentsContract経路は安定していると判断できる。

次STEP候補:

```text id="gn3uwv"
STEP4-25: CSVプレビュー
```

## 2. RenameResult.afterUriは正しいが履歴afterUriが旧Uriの場合

次STEPで履歴追加処理の修正が必要。

ただし今回のSTEP内で小さく直せるなら修正してよい。

## 3. Matching側またはHome側だけ旧Uriを持っている場合

次STEPで該当ViewModelの状態更新修正が必要。

ただし今回のSTEP内で小さく直せるなら修正してよい。

## 4. UNDOがrecord.beforeUriを使っている場合

これはバグなので、今回のSTEP内で `record.afterUri` を使うよう修正すること。

## 5. DocumentsContract成功後、次回リネームまたはUNDOでFileNotFoundになる場合

`afterUri` 反映漏れがある可能性が高い。

確認対象:

```text id="cf90ms"
- RenameResult.afterUri
- RenameHistoryRecord.afterUri
- Matching targetFiles uri/id
- Home targetFiles uri/id
```

---

# 単体テスト方針

今回の変更はログ追加と状態同期確認が中心であり、Android SAF依存が強いため、無理にローカルJVMテストを追加しなくてよい。

ただし、URI反映処理を純粋関数として切り出している場合のみ、以下をテストしてよい。

```text id="evfdvn"
- result.afterUri がある場合、更新後uri/idにafterUriを使う
- result.afterUri がnullの場合、既存uriを維持する
- UNDO時はrecord.afterUriをsourceとして使う
```

大規模リファクタになる場合はテスト追加を見送ること。

---

# 出力させるもの

Codexの最終回答には、以下を必ず含めること。

## 1. 実装コード概要

* どのファイルに何を変更したか
* DocumentsContract成功時のafterUri確認ログ
* 履歴追加時のafterUri確認ログ
* Matching側状態更新ログ
* Home側状態更新ログ
* UNDO時にrecord.afterUriを使っていること
* 必要な修正を行った場合はその内容
* リネーム / UNDO / 自動連番を変更していないこと

## 2. 変更ファイル一覧

例:

```text id="bgd5hx"
- SafDocumentDataSource.kt
- RenameMatchingViewModel.kt
- HomeViewModel.kt
- UndoRenameUseCase.kt
- doc/STEP4-24_codex.md
```

## 3. 動作確認方法

* ビルド方法
* 単体テスト方法
* 実機ログ確認方法
* 通常リネーム確認方法
* UNDO確認方法

## 4. 実機で見るべきログ

ユーザがそのままPowerShellで実行できる形で提示すること。

```powershell id="u29jj0"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameSafResolve
```

```powershell id="va475w"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

```powershell id="n28t1a"
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameStateSync
```

## 5. Git操作結果

* STEP4-23のcommit hash
* STEP4-23のpush結果
* STEP4-24の作業ブランチ名
* STEP4-24の分岐元ブランチ
* STEP4-24のcommit message
* STEP4-24のcommit hash
* STEP4-24のpush先
* 未コミット差分の有無

## 6. 保存ファイル

Codexの回答は以下へ保存すること。

```text id="cvwuma"
doc/STEP4-24_codex.md
```

---

# 後に回す機能メモ

以下は今回実装しない。メモとして残すこと。

```text id="h47m7r"
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
- DocumentsContract失敗Providerの記録
- Provider別fast pathスキップ
- directory.listFiles() のキャッシュ化
- 対象ファイル情報のMap化
- treeUri探索時間のさらなる削減
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
- リリースビルド向けログ抑制
```

---

# 未解決事項・リスクとして明記すること

Codexの最終回答では、以下を未解決事項として明記すること。

```text id="lp8k5r"
- STEP4-24ではDocumentsContract成功経路のafterUri伝播確認を行う
- CSVプレビューは未実装
- DocumentsContractが失敗するProviderではtreeUri fallbackに依存する
- DocumentsContract成功時のafterUriがProviderによって元Uriと同じ場合も変わる場合もある
- ファイル名やUriログはリリース前に抑制方針を検討する必要がある
- UNDO成功時に自動連番カウンタは巻き戻していない
```

---

# 完了条件

このSTEP4-24の完了条件は以下。

```text id="y3rzyk"
- STEP4-23がcommit / pushされている
- assembleDebug が成功する
- testDebugUnitTest が成功する
- documentsContract rename success の afterUri がログで確認できる
- RenameResult.afterUri がログで確認できる
- RenameHistoryRecord.afterUri が RenameResult.afterUri と一致する
- Matching側更新後uri/idが afterUri に更新されている
- Home側更新後uri/idが afterUri に更新されている
- UNDOが record.afterUri をsourceUriとして使っている
- UNDO成功後、Matching側表示が元ファイル名に戻る
- UNDO成功後、Home側表示が元ファイル名に戻る
- 通常リネームが壊れていない
- UNDOが壊れていない
- 自動連番が壊れていない
- 実機確認後にSTEP4-24がcommit / pushされている
- Codex回答が doc/STEP4-24_codex.md に保存されている
```

```

**ベスト案**  
STEP4-24では、CSVプレビューに進む前に `DocumentsContract` 成功経路の `afterUri` 伝播確認を入れるのが安全です。今回の実機ログではfast pathが成功したため、履歴・UNDO・Home/Matching状態がすべて新Uriを使えているか確認する価値があります。

**代替案**  
CSVプレビューへ進む案もあります。ただし、`DocumentsContract.renameDocument()` を入れた直後なので、次回リネームやUNDOで古いUri問題が出ないことを先に固めた方が、後続機能を安心して積めます。

**注意点**  
今回のSTEPではログ追加が中心です。ただし、`RenameResult.afterUri` があるのに履歴・Home・Matchingのどこかで旧Uriを保存している場合は、次回の `FileNotFound` に直結するため、その範囲だけは同STEP内で修正してよいです。
```
