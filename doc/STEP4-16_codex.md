実機検証結果と、codexの回答から、STEP4-17のプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-16: 実装（機能単位）
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
-
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
# STEP 4-16: 表示改善と選択中プレビュー Codex回答

## 作業ブランチ

`feature/step4-16-matching-ui-display`

分岐元は `feature/step4-15-last-used-set` です。

作業開始時点で以下の未コミット差分がありました。

```text
M  doc/STEP4-14_codex.md
A  doc/STEP4-15.md
MM doc/STEP4-15_codex.md
AM doc/STEP4-16.md
```

これらは既存ドキュメント差分として扱い、今回の実装では上書きしない方針で進めました。

## 実装内容

- Home画面とMatching画面の `Button` に `isAllCaps = false` を設定し、小文字が大文字化されないようにしました。
- Matching画面の「[選択中]」表記を削除しました。
- 選択中ファイル・選択中候補を `colorPrimaryContainer` 相当の背景色でハイライトするようにしました。
- リネーム済みファイル、使用済み候補は選択中とは別の控えめな色にしました。
- Matching画面下部に、両方選択時のプレビューを表示する `selectedPreviewText` を追加しました。
- プレビューは `RenameMatchingViewModel` 側で生成し、Fragmentにリネーム名生成ロジックを持たせないようにしました。
- 自動連番ON時は、次に使う予定番号をプレビューに反映します。ただしプレビュー生成ではカウンタを進めません。
- Matching画面とHome画面のルートViewに navigation bar inset 分のbottom paddingを追加しました。
- 実機確認用に、選択プレビューとbottom inset適用ログを追加しました。
- Home画面の「マッチング画面へ進む」ボタンは、Home画面へ遷移した時点でdisabled状態へ初期化するようにしました。
- 「前回のセット: ...」押下やディレクトリ/CSV選択で開始準備が整った場合は、即座にprimary色で有効化するようにしました。
- 「マッチング画面へ進む」押下後、読み込み中はprimary色を維持し、準備失敗時だけdisabled表示へ戻すようにしました。
- disabled時の背景色は `CSVを選択` と同じ `colorButtonNormal` を明示的に適用しました。

## 実装コード

### RenameMatchingUiState.kt

```kotlin
val selectedPreviewText: String? = null
```

両方選択時のリネーム予定表示をUI状態として持たせました。

### RenameMatchingViewModel.kt

選択状態変更時と自動連番ON/OFF切替時に `buildSelectedPreviewText()` を呼びます。

```kotlin
val previewAutoNumber = if (state.isAutoNumberingEnabled) {
    autoNumberCounters[selectedCandidate.rawPattern] ?: INITIAL_AUTO_NUMBER
} else {
    null
}
```

プレビューは既存の `ResolveRenameNameUseCase` を使って生成します。これにより Prefix / Suffix / Replace、自動連番、`*` 付きCSV候補の既存仕様と同じルールで表示できます。

失敗結果がプレビューに隠れないよう、リネーム失敗時は `selectedPreviewText = null` にしています。

### RenameMatchingFragment.kt

`Button` の全大文字化を無効化しました。

```kotlin
isAllCaps = false
```

選択状態は文字ではなく背景色で示します。

```kotlin
private fun itemButtonTint(isSelected: Boolean, isCompleted: Boolean): ColorStateList
```

「[選択中]」は削除し、「[リネーム済み]」「[使用済み]」は維持しました。

下部表示は以下の優先順です。

```text
1. リネーム中
2. 選択中プレビュー
3. 成功/失敗結果
```

navigation bar inset は `ViewCompat.setOnApplyWindowInsetsListener` でbottom paddingに加算しています。

### HomeFragment.kt

Home画面のボタンにも `isAllCaps = false` を設定しました。

対象:

- 前回セット
- ディレクトリを選択
- CSVを選択
- マッチング画面へ進む

Home画面にも最小限のbottom inset対応を入れました。

「マッチング画面へ進む」は、単にURIが入っているかだけではなく、Home画面上で開始操作が有効化されたかを示す `isStartMatchingActionArmed` を見て制御します。

```kotlin
val shouldShowStartReady = hasSelectedInputs &&
    state.isStartMatchingActionArmed &&
    !state.hasMatchingPreparationFailed
```

Home画面が表示状態に入った時点では、以下で開始ボタンだけをdisabledへ戻します。

```kotlin
override fun onStart() {
    super.onStart()
    viewModel.resetStartMatchingAction()
}
```

この処理では、選択済みディレクトリ名やCSV名は消しません。戻る操作後に古い選択内容を見せながら、誤ってそのまま開始できる状態だけを避けるためです。

### HomeUiState.kt

```kotlin
val hasMatchingPreparationFailed: Boolean = false
val isStartMatchingActionArmed: Boolean = false
val selectionRevision: Long = 0L
```

`isStartMatchingActionArmed` は「このHome表示中に開始ボタンを有効化してよい状態になったか」を表します。

`hasMatchingPreparationFailed` は、「マッチング画面へ進む」押下後に実際の読み込みやCSV解析が失敗した場合だけdisabled表示へ戻すために使います。

`selectionRevision` は、同じ前回セットを連続で押してもStateFlowの更新がUIへ流れるようにするための更新カウンタです。

### HomeViewModel.kt

`selectLastUsedSet()` では、保存済みのdirectoryUri/csvUriが復元できた時点で `isStartMatchingActionArmed = true` にします。これにより、ファイル件数や候補数がまだ0でも、「マッチング画面へ進む」を即座にprimary色へ変えられます。

```kotlin
isReadyToStartMatching = true
hasMatchingPreparationFailed = false
isStartMatchingActionArmed = true
```

`prepareMatchingData()` 開始時はprimary表示を維持するため、`hasMatchingPreparationFailed` はfalseにします。成功時は画面遷移するためprimary表示を崩さず、失敗時だけ `hasMatchingPreparationFailed = true` にしてdisabled表示へ戻します。

Home画面へ戻った時の初期化は `resetStartMatchingAction()` に集約しました。

```kotlin
fun resetStartMatchingAction() {
    _uiState.update { state ->
        state.copy(
            isStartMatchingActionArmed = false,
            hasMatchingPreparationFailed = false,
        )
    }
}
```

### ResolveRenameNameUseCase.kt

変更なし。

既存の名前解決ロジックを、ViewModelのプレビュー生成から再利用しました。

### その他変更ファイル

なし。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/home/HomeUiState.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `doc/STEP4-16_codex.md`

## 変更理由

## 全体アーキテクチャ

既存の MVVM + UseCase 構成を維持しました。

```text
UI層 Fragment
→ ViewModel層
→ UseCase層
```

今回の改善は表示改善が中心ですが、リネーム予定名の生成はビジネスルールを含みます。そのため、FragmentではなくViewModelでプレビュー文字列を作り、名前生成そのものは既存の `ResolveRenameNameUseCase` に委譲しました。

## 概要

小文字が大文字化される問題は、AndroidのButton表示側の `textAllCaps` 由来です。ファイル名データやソート処理ではなく表示コンポーネントの問題なので、表示用Buttonで `isAllCaps = false` を明示しました。

## 設計詳細

- `RenameMatchingUiState`
  - 責務: Matching画面の表示状態。
  - `selectedPreviewText` に選択中プレビューを保持します。

- `RenameMatchingViewModel`
  - 責務: 選択状態、自動連番状態、プレビュー生成。
  - プレビュー生成ではファイルI/Oを行わず、既存UseCaseで文字列だけを解決します。
  - 自動連番ON時は現在のカウンタ値を読むだけで、カウンタ更新はリネーム成功時だけです。

- `RenameMatchingFragment`
  - 責務: 表示反映、色ハイライト、insets対応。
  - 選択状態は `isSelected` を見て背景色で表現します。

- `HomeFragment`
  - 責務: Home画面の表示。
  - Buttonの全大文字化抑止、bottom inset対応、開始ボタンのdisabled/primary表示制御を担当します。

- `HomeViewModel`
  - 責務: Home画面の選択状態、読み込み状態、開始ボタン有効化状態を管理します。
  - `resetStartMatchingAction()` により、Home画面への遷移時に開始ボタンだけを初期化します。

## 採用理由・根拠

プレビュー生成をViewModelに置いた理由は、FragmentにPrefix / Suffix / Replaceや自動連番の判断を持たせると、実行時の名前解決ロジックと表示用ロジックが二重化するためです。二重化すると、将来ゼロ埋めや開始番号指定を追加したときに、実行結果とプレビューがズレるリスクが高くなります。

`ResolveRenameNameUseCase` を再利用した理由は、既に通常リネーム・自動連番・`*` 付きCSV互換のルールが集約されているためです。DRYの観点でも、同じ名前解決ルールを別実装しない方が保守しやすいです。

`Button` を維持した理由は、今回のSTEPが大規模UI刷新を禁止しているためです。TextViewやRecyclerViewへ置き換える案もありますが、変更範囲が広がります。今回は `isAllCaps = false` と背景色だけで表示問題を解決する方が、KISSとYAGNIに合います。

Home画面の開始ボタン制御では、既存の `isReadyToStartMatching` だけに依存しない設計にしました。`isReadyToStartMatching` は「directoryUri/csvUriが揃っている」という入力状態を表し、`isStartMatchingActionArmed` は「このHome表示中にユーザー操作で開始可能になった」というUI操作状態を表します。両者を分けることで、戻る遷移後にURIだけが残って開始ボタンが勝手に有効化される問題を避けられます。

## 代替案

- ファイル・候補の一覧をTextView + click listenerへ置き換える
  - 有効な条件: Buttonの見た目や状態制御を全面的に見直す場合。
  - 今回採用しない理由: UI構造の変更範囲が広がり、STEP4-16の「小さな表示改善」を超えるため。

- プレビューをFragment側で算出する
  - 有効な条件: 単純な文字列結合だけで済む場合。
  - 今回採用しない理由: Prefix / Suffix / Replace、自動連番、`*` 互換を反映する必要があり、ViewModel + UseCaseで扱う方が実行ロジックと整合しやすいため。

- Home遷移時にselectedDirectoryUri/selectedCsvUri自体をnullへ戻す
  - 有効な条件: Homeへ戻るたびに完全な初期状態へ戻したい場合。
  - 今回採用しない理由: ユーザーは前回選択内容を確認できた方がよく、要件は「開始ボタンをdisableに初期化する」ことなので、入力状態まで消すのは変更範囲が大きすぎるため。

## 動作確認方法

```text
1. アプリを起動する
2. Home画面で小文字ファイル名・CSV名が大文字化されていないことを確認する
3. リネーム対象ディレクトリを選択する
4. CSVファイルを選択する
5. マッチング画面へ進む
6. リネーム前ファイル名が小文字・大文字を維持して表示されることを確認する
7. リネーム候補名が小文字・大文字を維持して表示されることを確認する
8. ファイルを1件選択する
9. 選択中ファイルのカード色が変わることを確認する
10. 候補を1件選択する
11. 選択中候補のカード色が変わることを確認する
12. 「[選択中]」表記が表示されないことを確認する
13. 両方選択後、画面下部に「選択中：<元ファイル名> -> <リネーム後名>」が表示されることを確認する
14. 自動連番ON時、プレビューに次番号が反映されることを確認する
15. プレビュー表示だけでは連番カウンタが進まないことを確認する
16. リネーム実行後、対象ファイルがリネーム後名になり、リネーム済み表示になることを確認する
17. 3ボタンナビゲーションで画面下部表示が隠れないことを確認する
18. ジェスチャーナビゲーションでも画面下部表示が不自然に隠れないことを確認する
19. Prefix / Suffix / Replaceで既存のリネーム結果が壊れていないことを確認する
20. マッチング画面から戻った直後、「マッチング画面へ進む」がdisabled表示になっていることを確認する
21. 戻った後に「前回のセット: ...」を押すと、「マッチング画面へ進む」がprimary色で有効化されることを確認する
22. 「マッチング画面へ進む」押下後、読み込み中はprimary色を維持することを確認する
23. 読み込みまたはCSV解析が失敗した場合だけ、disabled表示へ戻ることを確認する
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

以下も実行しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

## 実機確認結果

ユーザー確認により、以下の挙動調整を追加しました。

```text
- 小文字ファイル名が大文字化されていない
- 選択中カードが色で分かる
- 「[選択中]」表記が消えている
- 画面下部の選択中プレビューが表示される
- 3ボタンナビゲーションで下部表示が隠れない
- 既存のリネーム処理が成功する
- 自動連番ON時のプレビューが期待どおり
- 「前回のセット: ...」押下後に開始ボタンがprimary色になる
- Homeへ戻った時に開始ボタンがdisabledへ初期化される
```

## Git操作結果

- 作業開始時のgit status:

```text
## feature/step4-15-last-used-set...origin/feature/step4-15-last-used-set
M  doc/STEP4-14_codex.md
A  doc/STEP4-15.md
MM doc/STEP4-15_codex.md
AM doc/STEP4-16.md
```

- 作成したブランチ名: `feature/step4-16-matching-ui-display`
- 分岐元ブランチ: `feature/step4-15-last-used-set`
- 最終ビルド結果: `assembleDebug` / `testDebugUnitTest` ともに `BUILD SUCCESSFUL`
- 実機確認結果: ユーザー確認OK
- commit hash: コミット後に確認
- push先ブランチ: 未push
- 未コミット差分: コミット対象外の既存ドキュメント差分は残す

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
- selectedTargetFile.displayName が大文字化されていないこと
- selectedCandidate.displayName が大文字化されていないこと
- selectedPreviewText が期待どおりであること
- isAutoNumberingEnabled=true 時に previewAutoNumber が次番号であること
- bottomInsetApplied が出ていること
- プレビュー表示だけでは autoNumberCounters が進まないこと
- リネーム実行時だけ counterAfter が進むこと
- AndroidRuntime のクラッシュログが出ていないこと
```

## 後に回す機能メモ

- 自動連番デフォルトON
- 自動連番開始番号指定
- 自動連番開始番号変更ダイアログ
- UNDOボタン
- 作業履歴30件保持
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索範囲の追加確認
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

- 選択中ハイライト色はprimary系の色を使っています。テーマ差によって見え方が薄い場合は、後続STEPで色定義をさらに明示する余地があります。
- 一覧は引き続きButtonベースです。大量件数時の性能改善やRecyclerView化は今回対象外です。
- Home画面の開始ボタン初期化は `onStart()` で行っています。ファイルピッカー復帰など別ライフサイクル経路で意図しない初期化が起きる場合は、遷移元を識別するフラグを追加する余地があります。

## 次に進めるべきSTEP

今回の指示ではcommitまで実行します。pushは未実行です。

```powershell
git status
git commit -m "Improve matching screen display states"
```

次の候補は、今回後回しにした以下です。

```text
1. 自動連番開始番号指定
2. Undo用の作業履歴設計
3. CSVプレビュー
4. Spinner横幅調整
```
