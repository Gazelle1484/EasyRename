このSTEPでは、STEPで確定し3た空実装に基づいて、**コンパイル可能な実装コードを生成したいです。codexの回答をもとにフォーマットに従ってSTEP4-3のプロンプトを生成してください。
## フォーマット　STEP 4-2: 実装（機能単位）
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

# STEP 4-2: Repository / SAF実装 Codex回答

今回は指示どおり、**Data層とUtil層のみ**を対象に実装しました。ViewModel、UI、リネーム統合UseCaseには触れていません。

## 全体アーキテクチャ

本アプリは引き続き **MVVM + Repository** 構成です。

```text
UI層
↓
ViewModel層
↓
Domain / UseCase層
↓
Repository層
↓
SafDocumentDataSource
↓
Android Storage Access Framework
```

今回のSTEP4-2では、Repository層から下だけを実装しました。Android APIである `ContentResolver` や `DocumentFile` は `SafDocumentDataSource` とUtilに閉じ込めています。

この設計にした理由は、UseCaseやViewModelがSAFの詳細に依存しないようにするためです。たとえば将来、MediaStore対応や別のDocumentProvider対応が必要になっても、主な変更範囲をData層に限定できます。

## 実装コード

### StorageRepositoryImpl.kt

```kotlin
package com.example.easyrename.data.repository

import android.net.Uri
import com.example.easyrename.data.saf.SafDocumentDataSource
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

class StorageRepositoryImpl(
    private val safDocumentDataSource: SafDocumentDataSource,
) : StorageRepository {

    override fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile> {
        return safDocumentDataSource.loadFilesInDirectory(directoryUri)
    }

    override fun readTextFromUri(uri: Uri): String {
        return safDocumentDataSource.readTextFromUri(uri)
    }

    override fun renameFile(fileUri: Uri, newName: String): RenameResult {
        return safDocumentDataSource.renameFile(fileUri, newName)
    }

    override fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean {
        return safDocumentDataSource.existsInSameDirectory(directoryUri, fileName)
    }

    override fun takePersistablePermission(uri: Uri) {
        safDocumentDataSource.takePersistablePermission(uri)
    }
}
```

### SafDocumentDataSource.kt

```kotlin
package com.example.easyrename.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class SafDocumentDataSource(
    private val context: Context,
) {

    fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile> {
        val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return emptyList()

        return directory
            .listFiles()
            .filter { it.isFile }
            .map { file ->
                RenameTargetFile(
                    id = file.uri.toString(),
                    displayName = file.name ?: file.uri.lastPathSegment.orEmpty(),
                    uri = file.uri,
                    size = file.length(),
                    lastModified = file.lastModified(),
                )
            }
    }

    fun readTextFromUri(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IllegalArgumentException("Unable to open input stream: $uri")

        return decodeText(bytes)
    }

    fun renameFile(fileUri: Uri, newName: String): RenameResult {
        return try {
            val documentFile = DocumentFile.fromSingleUri(context, fileUri)
            val beforeName = documentFile?.name ?: fileUri.lastPathSegment.orEmpty()

            if (documentFile == null) {
                return RenameResult(
                    beforeName = beforeName,
                    afterName = newName,
                    success = false,
                    errorMessage = "DocumentFile could not be resolved.",
                )
            }

            val success = documentFile.renameTo(newName)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = success,
                errorMessage = if (success) null else "DocumentFile.renameTo returned false.",
            )
        } catch (exception: Exception) {
            RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = exception.message ?: exception::class.java.simpleName,
            )
        }
    }

    fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean {
        val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return false

        return directory
            .listFiles()
            .any { it.isFile && it.name == fileName }
    }

    fun takePersistablePermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
    }

    private fun decodeText(bytes: ByteArray): String {
        return try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (exception: CharacterCodingException) {
            String(bytes, Charset.forName(SHIFT_JIS))
        }
    }

    private companion object {
        const val SHIFT_JIS = "Shift_JIS"
    }
}
```

### UriDisplayNameUtil.kt

```kotlin
package com.example.easyrename.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object UriDisplayNameUtil {

    fun getDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        val displayName = cursor.getString(columnIndex)
                        if (!displayName.isNullOrBlank()) {
                            return displayName
                        }
                    }
                }
            }

        return uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
    }
}
```

### DateFormatUtil.kt

```kotlin
package com.example.easyrename.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatUtil {

    fun format(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) {
            return ""
        }

        return SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date(timestamp))
    }

    private const val DATE_PATTERN = "yyyy/MM/dd HH:mm"
}
```

## 変更ファイル一覧

- `app/src/main/java/com/example/easyrename/data/repository/StorageRepositoryImpl.kt`
- `app/src/main/java/com/example/easyrename/data/saf/SafDocumentDataSource.kt`
- `app/src/main/java/com/example/easyrename/util/UriDisplayNameUtil.kt`
- `app/src/main/java/com/example/easyrename/util/DateFormatUtil.kt`
- `doc/STEP4-2_codex.md`

## 変更理由

`StorageRepositoryImpl` は、Repositoryインターフェースの実装として `SafDocumentDataSource` に委譲するだけの薄いクラスにしました。ここにSAFの詳細ロジックを書かないことで、Repositoryは「UseCase向けの入口」、DataSourceは「Android API操作」という責務分離を維持できます。

`SafDocumentDataSource` では `DocumentFile.fromTreeUri`、`DocumentFile.fromSingleUri`、`ContentResolver.openInputStream`、`takePersistableUriPermission` を使いました。これはAndroid 33以降のストレージ制約に対して、ユーザーが明示的に許可したURIだけを扱う設計と整合します。

CSV読み込みはUTF-8を基本にし、UTF-8として不正なバイト列だった場合のみShift_JISへフォールバックします。常にShift_JISも試す代替案はありますが、UTF-8として正しく読めるファイルを誤って別文字コード扱いするリスクがあるため、まずUTF-8を厳密に判定する実装にしました。

サブディレクトリは `it.isFile` のみを対象にすることで除外しています。これはSTEP4-2の範囲と、現時点では直下ファイルのみという仕様に合わせた判断です。

## 動作確認方法

1. SAFでディレクトリを選択する
2. `StorageRepositoryImpl.loadFilesInDirectory(directoryUri)` を呼び、直下ファイルだけが `RenameTargetFile` として返ることを確認する
3. SAFでCSVファイルを選択する
4. `StorageRepositoryImpl.readTextFromUri(csvUri)` を呼び、UTF-8またはShift_JISのテキストが返ることを確認する
5. `StorageRepositoryImpl.existsInSameDirectory(directoryUri, fileName)` を呼び、完全一致の同名ファイルだけが `true` になることを確認する
6. `StorageRepositoryImpl.renameFile(fileUri, newName)` を呼び、成功時は `success = true`、失敗時は `success = false` と `errorMessage` が返ることを確認する
7. 必要に応じて `StorageRepositoryImpl.takePersistablePermission(uri)` を呼び、READ / WRITE権限保持を確認する

## ビルド確認結果

以下を実行し、成功しました。

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

結果:

```text
BUILD SUCCESSFUL
```

## 未解決事項・リスク

- `DocumentFile.renameTo` の挙動は端末やDocumentProviderによって差があります。失敗時は `RenameResult.success = false` として扱います。
- `DocumentFile.fromSingleUri` で得たURIがプロバイダによってはリネーム不可の場合があります。
- `takePersistableUriPermission` は、呼び出し元が永続化可能なURI権限を受け取っていない場合に例外になる可能性があります。
- CSVの文字コード判定はUTF-8優先、失敗時Shift_JISフォールバックです。UTF-8として文法上は成立するが実際には別文字コード、というケースは完全には判定できません。
- UIとViewModelはまだ未実装のため、実機での一連操作確認はSTEP4-3以降で行います。
