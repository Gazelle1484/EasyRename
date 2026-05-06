実機検証結果と、codexの回答から、のプロンプトを次に与えるプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-21: 実装（機能単位）
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
- 回答は本ファイル名+*_codexした.mdファイルに保存
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

# STEP 4-20: 最新1件UNDO実行 Codex回答

## 全体アーキテクチャ

EasyRenameは、既存どおり MVVM + UseCase + Repository 構成を維持しています。

```text
UI層 RenameMatchingFragment
-> ViewModel層 RenameMatchingViewModel / HomeViewModel
-> UseCase層 UndoRenameUseCase
-> Repository層 StorageRepository
-> SAF DataSource層 SafDocumentDataSource
```

UI層はボタン表示とクリックイベントだけを担当し、逆リネームの判断はViewModel、実際のリネーム処理はUseCase経由でRepositoryへ委譲します。FragmentやViewModelから直接 `DocumentFile.renameTo()` を呼ばない構成にした理由は、通常リネームと同じSAF処理、失敗分類、`afterUri` 取得を再利用し、責務の混在を避けるためです。

## 概要

STEP4-20では、STEP4-19で追加した `UNDO` ボタンから最新履歴1件だけを逆リネームできるようにしました。UNDOでは履歴の `afterUri` を使い、`afterName -> beforeName` の方向でリネームします。

## 設計詳細

- `UndoRenameUseCase`
  - 責務: `RenameHistoryRecord` を受け取り、`record.afterUri` のファイルを `record.beforeName` へ戻す。
  - `StorageRepository.renameFile(directoryUri, afterUri, beforeName)` を呼びます。
  - UNDO先の `beforeName` が同一ディレクトリに存在する場合は `FileAlreadyExists` として失敗扱いにします。

- `RenameHistoryManager`
  - `removeLatest()` を追加しました。
  - 最新履歴を1件だけ削除して返します。
  - UNDO成功時だけ呼び、失敗時は履歴を残します。

- `RenameMatchingViewModel`
  - `onUndoClicked()` をログのみから実行処理へ変更しました。
  - UNDO中は `isUndoExecuting=true` にしてボタン操作を抑止します。
  - 成功時はMatching画面の対象ファイル名を `beforeName` へ戻し、`isRenamed=false` に戻します。
  - 失敗時は画面表示を戻さず、履歴も削除しません。

- `HomeViewModel`
  - `applyUndoRenameResult()` を追加しました。
  - Home側の対象ファイル一覧も `afterName` から `beforeName` へ戻し、`isRenamed=false` に戻します。

- `RenameMatchingFragment`
  - `state.isUndoExecuting` を見て、UNDO中は `UNDO` / 通常リネーム / 選択操作をdisabledにします。
  - `lastUndoResult` をHome側へ同期し、成功時は `UNDO成功: ...` を表示します。
  - STEP4-17/19の `bottomContentContainer` bottomMargin Insets対応は維持しました。

## 採用理由・根拠

UNDO専用UseCaseを追加した理由は、通常リネームとは入力が異なるためです。通常リネームは選択中ファイルと候補から新名を解決しますが、UNDOは履歴レコードが唯一の入力で、必ず `afterUri` と `beforeName` を使います。この違いをViewModel内に埋め込むと責務が重くなるため、UseCaseに分けました。

Repository/DataSourceは再利用しました。SAF Providerによってはrename後にUriが変わるため、既存の `RenameResult.afterUri` を使う流れを維持する方が保守しやすく、通常リネームとUNDOで失敗分類が分岐しません。

自動連番カウンタは巻き戻していません。複数UNDOや候補ごとの履歴と絡むため、STEP4-20の「最新1件のファイル名UNDO」からは外しました。これはKISSとYAGNIに沿った分割です。

## 代替案

- `ExecuteRenameUseCase` をそのまま流用する案
  - 有効な条件: UNDOも `RenamePair` として自然に表現できる場合。
  - 今回採用しない理由: UNDOでは候補やリネームモードを使わず、`afterUri -> beforeName` が本質なので、通常リネームの入力モデルに無理に合わせると意味が混乱します。

- ViewModelから直接 `StorageRepository.renameFile()` を呼ぶ案
  - 有効な条件: 小さな試作でUseCase層を置かない場合。
  - 今回採用しない理由: 既存アーキテクチャではViewModelの下にUseCaseがあり、逆リネームの業務ルールをUseCaseに閉じた方がテストと保守がしやすいためです。

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/domain/history/RenameHistoryManager.kt`
- `app/src/main/java/com/example/easyrename/domain/usecase/UndoRenameUseCase.kt`
- `app/src/main/java/com/example/easyrename/ui/AppViewModelFactory.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingUiState.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/easyrename/viewmodel/RenameMatchingViewModel.kt`
- `app/src/test/java/com/example/easyrename/domain/history/RenameHistoryManagerTest.kt`
- `doc/STEP4-20.md`
- `doc/STEP4-20_codex.md`

## ビルド結果

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

## テスト結果

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```

結果:

```text
BUILD SUCCESSFUL
```

追加確認:

```text
RenameHistoryManager.removeLatest の単体テストを追加
git diff --check 成功
```

## 実機確認手順

```text
1. アプリを起動する
2. リネーム対象ディレクトリとCSVを選択する
3. マッチング画面へ進む
4. 履歴0件でUNDOボタンがdisabledであることを確認する
5. 1件リネームする
6. UNDOボタンがenabledになり、履歴件数が表示されることを確認する
7. UNDOを押す
8. ファイル名がリネーム前の名前に戻ることを確認する
9. Matching画面の表示がリネーム前の名前に戻ることを確認する
10. Homeへ戻り、対象ファイル一覧もリネーム前の名前に戻ることを確認する
11. 履歴0件になった場合、UNDOボタンがdisabledになることを確認する
12. 3ボタンナビゲーションでUNDOボタン・メッセージ行・LoadingViewが隠れないことを確認する
```

ログ確認:

```powershell
C:\Users\gazel\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat | findstr EasyRenameHistory
```

見るべきログ:

```text
undo start
undo sourceName=<afterName> targetName=<beforeName>
undo sourceUri=<afterUri>
undo result success=true
remove latest
undo success restoredName=<beforeName>
undo failed ... errorType=...
```

## Git操作結果

- STEP4-19 commit hash: `bcf214e Add undo button UI`
- STEP4-19 push先: `origin/feature/step4-19-undo-button-ui`
- STEP4-20作業ブランチ名: `feature/step4-20-undo-rename`
- STEP4-20分岐元ブランチ: `feature/step4-19-undo-button-ui`
- STEP4-20 commit message: `Implement single undo rename`
- STEP4-20 commit hash: 実機確認OK後に作成
- STEP4-20 push先: 実機確認OK後に `origin/feature/step4-20-undo-rename`
- 未コミット差分: あり

## リスク・今後の検討点

- STEP4-20では最新1件のUNDOのみ対応しています。
- 複数件UNDOは未実装です。
- UNDO成功時に自動連番カウンタは巻き戻していません。
- 履歴はアプリ起動中のみ保持し、アプリキルで消えます。
- SAF Providerによっては履歴の `afterUri` が無効になる可能性があります。
- UNDO先の `beforeName` が既に存在する場合は失敗扱いです。
- UNDO失敗時の詳細なリカバリUIは未実装です。

## 後に回す機能メモ

- 複数件UNDO
- 作業履歴一覧表示
- 履歴の永続化
- UNDO成功時の自動連番カウンタ巻き戻し
- UNDO失敗時の詳細ダイアログ表示
- CSVプレビュー
- CSVプレビューボタン
- Spinner横幅調整
- singleUri探索名とtreeUri探索名の比較ログ
- treeUri探索時間削減
- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
