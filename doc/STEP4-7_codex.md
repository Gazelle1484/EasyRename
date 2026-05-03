実機検証結果と、codexの回答から、STEP4-8のプロンプトを生成してください。
プロンプトはフォーマットに従ってください。
コードの修正を依頼する場合は、必要な部分から段階を踏んで単機能で依頼すること。
後に回す機能はメモとして残しておいてください。
実機で確認するべきログがあれば別途ユーザに指示してください。
今はgitのmainにpushしているが、機能追加に合わせてブランチをcheckout, commit, pushするよう、codexに指示してください。
## フォーマット　STEP 4-8: 実装（機能単位）
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
# 実機確認結果
1. アプリを起動する：ok
2. Home画面でボタンがTopAppBarに隠れていないことを確認する：ok
3. 「マッチング画面へ進む」ボタンの色が分かりやすいことを確認する：ok
4. 全体の文字サイズが以前より少し読みやすくなっていることを確認する：ok
5. リネーム対象ディレクトリを選択する：ok
6. CSVファイルを選択する：ok
7. マッチング画面へ進む：ok
8. マッチング画面上部がTopAppBarに隠れていないことを確認する：ok
9. 左側に元ファイル一覧、右側にリネーム候補一覧が表示されることを確認する：ok
10. 左右の列が独立してスクロールできることを確認する：：ok
11. 元ファイルを1件選択する：ok
12. 候補を1件選択する：ok
13. 「リネーム実行」ボタンが有効になり、色が分かりやすいことを確認する：ok
14. リネームを実行し、成功することを確認する：ok
15. マッチング画面の「戻る」ボタンでHome画面へ戻れることを確認する：ok

###### 追加の要望
- （可能であれば）ディレクトリを選択する際、変更日時が新しい順にあらかじめソートし、ディレクトリがトップに来るようにしてほしい
- （可能であれば）csvを選択する際、変更日時が新しい順にあらかじめソートし、csvがトップに来るようにしてほしい。または、csvのみが表示されるようにしてほしい
- マッチング画面では、リネーム対象/リネーム候補それぞれが辞書順にソートされてほしい。
- リネームにモードをつけたい。ホーム画面で選択する。リネームモードを選択する欄を作って、選択式リストから選択する方式にしてはどうか。モードはまず3つ作る。一つ目は先頭に文字列を追加する。二つ目は拡張子の直前、ファイル名の最後につける。三つめはファイル名を置き換える。デフォルトは先頭に文字列をつけるモードにする。つまり、先頭モードの場合は、csvから読み込んだリネーム候補にすべて「*_」をつけて扱う。ファイル名の末尾モードも同様である。
- リネーム前ファイルがリネームするとリネーム後になるのは維持してほしい。
- 自動連番機能をつけたい。これは実装例です。「画面上部に自動連番ボタンを追加し、自動連番ボタンを押下すると自動連番モードになる。自動連番モードでリネーム候補とリネーム前ファイルを押下すると、リネーム候補の末尾に「-1」のようにつけて扱う。つまり、logs1223.txtとlogs1226.txtをそれぞれ「A1-1」の先頭モードで、この順にリネームすると「A1-1-1_logs1223.txt」,「A1-1-2_logs1226.txt」になる。末尾モードであれば「logs1223_A1-1-1.txt」, 「logs1226_A1-1-2.txt」となる。

# STEP 4-7: マッチング画面UI改善 Codex回答

## 作業ブランチ

`feature/step4-7-matching-ui`

作業開始前の `git status` では、既に以下の未コミット差分があった。

- `doc/STEP4-6_codex.md`: STEP4-7プロンプト生成用の追記
- `doc/STEP4-7.md`: STEP4-7プロンプト本文の追加途中

コード差分はなかったため、内容を確認したうえで差分を保持したまま作業ブランチを作成した。

## 実装内容

- Home画面の「マッチング画面へ進む」ボタンをprimary相当の色に変更した。
- Home画面のTextView / Buttonの文字サイズを本文相当の `16sp` にした。
- Matching画面の上部paddingにActionBar高さを加え、TopAppBar被りを避けた。
- Matching画面の上部に「戻る」ボタンと「リネーム実行」ボタンを横並びで配置した。
- Matching画面を左右2列に分割し、左にリネーム前ファイル、右にリネーム候補を表示した。
- 左右それぞれを独立した `ScrollView` にした。
- Matching画面のTextView / Buttonの文字サイズを本文 `16sp`、見出し `18sp` にした。
- 「リネーム実行」ボタンをprimary相当、「戻る」ボタンをsecondary相当の色にした。
- リネーム処理、SAF、Repository、Domain、CSV仕様には触れていない。

## 実装コード

### HomeFragment.kt

`Button` / `TextView` に `16sp` を指定し、実機での視認性を少し上げた。

「マッチング画面へ進む」は主要アクションなので、Materialテーマの `colorPrimary` / `colorOnPrimary` を使うようにした。

```kotlin
startMatchingButton = Button(context).apply {
    text = "マッチング画面へ進む"
    textSize = BODY_TEXT_SIZE_SP
    isEnabled = false
    backgroundTintList = primaryButtonTint()
    setTextColor(resolveColor(MaterialR.attr.colorOnPrimary))
}
```

無効時は `colorControlNormal`、有効時は `colorPrimary` を使う `ColorStateList` にして、既存の `isEnabled` 制御を維持した。

### RenameMatchingFragment.kt

Rootは縦方向のまま維持し、上部にActionBar高さ分のpaddingを追加した。

```kotlin
setPadding(32, 32 + resolveActionBarHeight(), 32, 32)
```

画面上部には「戻る」と「リネーム実行」を配置した。

```kotlin
backButton.setOnClickListener {
    parentFragmentManager.popBackStack()
}
```

左右2分割は `LinearLayout.HORIZONTAL` と `layout_weight` で実装した。

```kotlin
LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    addView(createFileColumn("リネーム前ファイル", filesContainer))
    addView(createFileColumn("リネーム候補", candidatesContainer))
}
```

各列の内部に見出しと `ScrollView` を持たせ、左右の一覧が独立してスクロールできるようにした。

### MainActivity.kt

変更なし。

既存の `FragmentContainerView` 相当の役割は `FrameLayout` が担っており、今回の目的はMatching画面内のレイアウト改善なので、Activity全体の構造変更は行っていない。

### その他変更ファイル

新規作成。

- `doc/STEP4-7_codex.md`

作業前から存在していた未コミット差分。

- `doc/STEP4-6_codex.md`
- `doc/STEP4-7.md`

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/ui/home/HomeFragment.kt`
- `app/src/main/java/com/example/easyrename/ui/matching/RenameMatchingFragment.kt`
- `doc/STEP4-7_codex.md`

## 変更理由

## 全体アーキテクチャ

本アプリは、現状のMVVM + Repository構成を維持している。

```text
UI層
→ ViewModel層
→ UseCase層
→ Repository層
→ SAF DataSource層
```

今回のSTEP4-7はUI改善のみが目的のため、変更はUI層の `HomeFragment` と `RenameMatchingFragment` に閉じ込めた。これにより、成功済みのリネーム実行フロー、SAFリネーム処理、CSV解析、`*` 解釈ルールへの影響を避けている。

## 概要

目的は、実機で操作しやすいマッチング画面にすることである。元ファイルと候補を上下に並べると比較しづらいため、左右2列にして、選択対象を見比べながら操作できる構成にした。

## 設計詳細

- `HomeFragment`
  - 責務: Home画面の表示、ディレクトリ選択、CSV選択、マッチング画面への遷移。
  - 変更: 主要ボタンの色と文字サイズのみ。
  - 依存: 既存どおり `HomeViewModel` と `ErrorDialog` に依存する。

- `RenameMatchingFragment`
  - 責務: 対象ファイル一覧、候補一覧、選択状態、実行ボタン、結果表示。
  - 変更: レイアウトを左右2列化し、戻るボタンを追加。
  - 依存: 既存どおり `RenameMatchingViewModel` にユーザー操作を渡す。

## 採用理由・根拠

プログラムmatic Viewを維持した理由は、既存UIがXMLではなくKotlinコードで構築されており、今回のSTEPが「最小UI改善」に限定されているためである。XMLレイアウト化やMaterial Componentsへの本格移行は、UI全体の整理としては有効だが、このSTEPでは変更範囲が広がり、成功済みフローを壊すリスクが増える。

左右2分割は `LinearLayout` と `layout_weight` で実装した。理由は、現在の単純な画面構成に対して十分であり、RecyclerViewや複雑なConstraintLayoutを導入しなくても要件を満たせるためである。KISSを優先し、今必要な比較しやすさと独立スクロールだけを実装した。

色は可能な範囲でテーマ属性を使った。`colorPrimary` は主要アクション、`colorSecondary` は戻る操作に使い、Android / Materialの役割分けに寄せている。これにより、将来テーマ色を変更した場合もボタン色が追従しやすい。

## 代替案

- XMLレイアウト化
  - 有効になる条件: 画面数が増え、デザイン調整やプレビュー確認の頻度が上がる場合。
  - 今回採用しなかった理由: STEP4-7の範囲を超える構造変更になり、既存のプログラムmatic Viewとの混在コストが増えるため。

- RecyclerView化
  - 有効になる条件: ファイル数や候補数が多くなり、表示性能やView再利用が問題になる場合。
  - 今回採用しなかった理由: 禁止事項に含まれており、現在は1件ずつ確実に操作できることが優先だからである。

## 動作確認方法

```text
1. アプリを起動する
2. Home画面でボタンがTopAppBarに隠れていないことを確認する
3. 「マッチング画面へ進む」ボタンの色が分かりやすいことを確認する
4. 全体の文字サイズが以前より少し読みやすくなっていることを確認する
5. リネーム対象ディレクトリを選択する
6. CSVファイルを選択する
7. マッチング画面へ進む
8. マッチング画面上部がTopAppBarに隠れていないことを確認する
9. 左側に元ファイル一覧、右側にリネーム候補一覧が表示されることを確認する
10. 左右の列が独立してスクロールできることを確認する
11. 元ファイルを1件選択する
12. 候補を1件選択する
13. 「リネーム実行」ボタンが有効になり、色が分かりやすいことを確認する
14. リネームを実行し、成功することを確認する
15. マッチング画面の「戻る」ボタンでHome画面へ戻れることを確認する
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

## 実機ログ確認手順

今回の主目的はUI改善のため、常時ログ確認は不要である。

リネーム実行や戻る操作でクラッシュ・失敗が出た場合のみ、以下を確認する。

### 1. 端末確認

```powershell
adb devices
```

### 2. ログクリア

```powershell
adb logcat -c
```

### 3. EasyRenameログ確認

```powershell
adb logcat | findstr EasyRename
```

### 4. クラッシュ確認

```powershell
adb logcat | findstr "AndroidRuntime EasyRename Exception"
```

確認する内容。

```text
- リネーム実行時に例外が出ていないか
- 戻るボタン押下時にFragment遷移エラーが出ていないか
- TopAppBar余白調整でレイアウト例外が出ていないか
- renameTo result=true が維持されているか
```

## 後に回す機能メモ

- RecyclerView化
- XMLレイアウト化
- Material Componentsへの本格移行
- Edge-to-Edge / WindowInsetsの正式対応
- 画面回転時の完全な状態復元
- リネーム成功後のファイル一覧再読み込み
- リネーム履歴
- 取り消し機能
- 一括リネーム
- CSVプレビュー
- リネーム前後の差分プレビュー
- 同名時の自動連番
- 失敗時テストケースの自動化

## 未解決事項・リスク

- 横幅が狭い端末では左右2列の各ボタン文字が詰まる可能性がある。今回の禁止事項にRecyclerView化や大規模UI刷新があるため、まずは `layout_weight` による最小対応に留めた。
- `Button` をプログラム生成しているため、MaterialButtonの細かなoutlined表現までは使っていない。後続STEPでMaterial Componentsへ寄せる場合に整理するとよい。
- `popBackStack()` による戻る操作は、HomeからMatchingへ遷移するときにBackStackへ追加されている現状に依存している。直接Matchingを表示する導線を追加する場合はfallbackを検討する必要がある。
- 失敗時表示はSTEP4-6の既存実装を維持した。今回、失敗処理の再設計や自動テスト追加は行っていない。

## 次に進めるべきSTEP

次は実機でSTEP4-7のUI確認を行う。

優先確認は以下。

1. Matching画面の上部がTopAppBarに隠れないこと。
2. 左右2列がそれぞれ独立してスクロールできること。
3. 「戻る」ボタンでHomeへ戻れること。
4. 既存の1件リネーム成功フローが壊れていないこと。

実機で狭い画面幅による読みにくさが残る場合は、次STEPで横画面・縦画面別レイアウト、またはXMLレイアウト化を検討する。
