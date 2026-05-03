package com.example.easyrename.ui.common

import android.content.Context
import com.example.easyrename.model.AppError
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ErrorDialog {

    fun show(context: Context, error: AppError) {
        MaterialAlertDialogBuilder(context)
            .setTitle("エラー")
            .setMessage(toMessage(error))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toMessage(error: AppError): String {
        return when (error) {
            AppError.CsvReadFailed -> "CSVファイルの読み込みに失敗しました。"
            AppError.DirectoryReadFailed -> "ディレクトリ内のファイル取得に失敗しました。"
            AppError.PermissionDenied -> "ファイルまたはディレクトリへのアクセス権限がありません。"
            AppError.InvalidFileName -> "リネーム後のファイル名が不正です。"
            AppError.FileAlreadyExists -> "同じ名前のファイルが既に存在します。"
            AppError.UnsupportedOperation -> "この保存場所ではファイル名変更がサポートされていません。\n別のフォルダを選択するか、端末内ストレージのDocuments/Download配下で試してください。"
            AppError.RenameFailed -> "ファイル名の変更に失敗しました。"
            is AppError.Unknown -> error.detailMessage.ifBlank { "不明なエラーが発生しました。" }
        }
    }
}
