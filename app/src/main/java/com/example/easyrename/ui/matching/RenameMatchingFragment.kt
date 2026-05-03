package com.example.easyrename.ui.matching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.easyrename.model.AppError
import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameErrorType
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile
import com.example.easyrename.ui.AppViewModelFactory
import com.example.easyrename.ui.common.ErrorDialog
import com.example.easyrename.ui.common.LoadingView
import com.example.easyrename.viewmodel.HomeViewModel
import com.example.easyrename.viewmodel.RenameMatchingViewModel
import kotlinx.coroutines.launch

class RenameMatchingFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var viewModel: RenameMatchingViewModel
    private lateinit var filesContainer: LinearLayout
    private lateinit var candidatesContainer: LinearLayout
    private lateinit var executeButton: Button
    private lateinit var resultText: TextView
    private lateinit var loadingView: LoadingView
    private var lastShownErrorMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        filesContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        candidatesContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        executeButton = Button(context).apply {
            text = "リネーム実行"
            isEnabled = false
        }
        resultText = TextView(context)
        loadingView = LoadingView(context)

        root.addView(TextView(context).apply { text = "元ファイル一覧" })
        root.addView(ScrollView(context).apply { addView(filesContainer) })
        root.addView(TextView(context).apply { text = "リネーム候補一覧" })
        root.addView(ScrollView(context).apply { addView(candidatesContainer) })
        root.addView(executeButton)
        root.addView(resultText)
        root.addView(loadingView)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeFactory = AppViewModelFactory(requireContext())
        homeViewModel = ViewModelProvider(requireActivity(), homeFactory)[HomeViewModel::class.java]
        viewModel = ViewModelProvider(
            this,
            AppViewModelFactory(requireContext()) { homeViewModel },
        )[RenameMatchingViewModel::class.java]

        executeButton.setOnClickListener {
            viewModel.executeSelectedRename()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderFiles(state.targetFiles)
                    renderCandidates(state.renameCandidates)
                    executeButton.isEnabled = state.canExecuteRename && !state.isExecuting
                    loadingView.setLoading(state.isExecuting)
                    resultText.text = state.lastResult?.let { result ->
                        if (result.success) {
                            "成功: ${result.beforeName} -> ${result.afterName} に変更しました。"
                        } else {
                            "失敗: ${toResultErrorMessage(result)}"
                        }
                    }.orEmpty()

                    state.error?.let { error ->
                        val messageKey = error.toString()
                        if (messageKey != lastShownErrorMessage) {
                            lastShownErrorMessage = messageKey
                            ErrorDialog.show(requireContext(), error)
                        }
                    }
                }
            }
        }
    }

    private fun renderFiles(files: List<RenameTargetFile>) {
        filesContainer.removeAllViews()
        files.forEach { file ->
            filesContainer.addView(
                Button(requireContext()).apply {
                    text = buildString {
                        if (file.isSelected) append("[選択中] ")
                        if (file.isRenamed) append("[リネーム済み] ")
                        append(file.displayName)
                    }
                    isEnabled = !file.isRenamed
                    setOnClickListener { viewModel.selectTargetFile(file.id) }
                },
            )
        }
    }

    private fun renderCandidates(candidates: List<RenameCandidate>) {
        candidatesContainer.removeAllViews()
        candidates.forEach { candidate ->
            candidatesContainer.addView(
                Button(requireContext()).apply {
                    text = buildString {
                        if (candidate.isSelected) append("[選択中] ")
                        if (candidate.isUsed) append("[使用済み] ")
                        append(candidate.displayName)
                    }
                    isEnabled = !candidate.isUsed
                    setOnClickListener { viewModel.selectRenameCandidate(candidate.id) }
                },
            )
        }
    }

    private fun toResultErrorMessage(result: RenameResult): String {
        val errorMessage = result.errorMessage
        return when (result.errorType) {
            RenameErrorType.UnsupportedOperation -> "この保存場所ではファイル名変更がサポートされていません。\n別のフォルダを選択するか、端末内ストレージのDocuments/Download配下で試してください。"
            RenameErrorType.InvalidFileName -> "リネーム後のファイル名が不正です。"
            RenameErrorType.FileAlreadyExists -> "同名ファイルが既に存在します。"
            RenameErrorType.PermissionDenied -> "ファイルまたはディレクトリへのアクセス権限がありません。"
            else -> when {
                errorMessage?.contains("UnsupportedOperationException", ignoreCase = true) == true -> "この保存場所ではファイル名変更がサポートされていません。\n別のフォルダを選択するか、端末内ストレージのDocuments/Download配下で試してください。"
            errorMessage?.contains("Invalid file name", ignoreCase = true) == true -> "リネーム後のファイル名が不正です。"
            errorMessage?.contains("same name", ignoreCase = true) == true -> "同名ファイルが既に存在します。"
            errorMessage?.contains("already exists", ignoreCase = true) == true -> "同名ファイルが既に存在します。"
            errorMessage.isNullOrBlank() -> "理由不明"
            else -> errorMessage
            }
        }
    }
}
