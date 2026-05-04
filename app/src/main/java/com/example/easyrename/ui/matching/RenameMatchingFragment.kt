package com.example.easyrename.ui.matching

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.google.android.material.R as MaterialR
import kotlinx.coroutines.launch

class RenameMatchingFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var viewModel: RenameMatchingViewModel
    private lateinit var filesContainer: LinearLayout
    private lateinit var candidatesContainer: LinearLayout
    private lateinit var executeButton: Button
    private lateinit var backButton: Button
    private lateinit var autoNumberButton: Button
    private lateinit var undoButton: Button
    private lateinit var resultText: TextView
    private lateinit var changeAutoNumberButton: Button
    private lateinit var bottomContentContainer: LinearLayout
    private lateinit var loadingView: LoadingView
    private var lastShownErrorMessage: String? = null
    private var lastSyncedSuccessResult: RenameResult? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32 + resolveActionBarHeight(), 32, 32)
        }
        filesContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        candidatesContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        executeButton = Button(context).apply {
            text = "リネーム実行"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            isEnabled = false
            backgroundTintList = primaryButtonTint()
            setTextColor(resolveColor(MaterialR.attr.colorOnPrimary))
        }
        backButton = Button(context).apply {
            text = "戻る"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            backgroundTintList = secondaryButtonTint()
            setTextColor(resolveColor(MaterialR.attr.colorOnSecondary))
        }
        autoNumberButton = Button(context).apply {
            text = "自動連番: ON"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            backgroundTintList = primaryButtonTint()
            setTextColor(resolveColor(MaterialR.attr.colorOnPrimary))
        }
        undoButton = Button(context).apply {
            text = "UNDO"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            isEnabled = false
            backgroundTintList = secondaryButtonTint()
            setTextColor(resolveColor(MaterialR.attr.colorOnSecondary))
        }
        resultText = TextView(context).apply {
            textSize = BODY_TEXT_SIZE_SP
        }
        changeAutoNumberButton = Button(context).apply {
            text = "変更"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            visibility = View.GONE
            backgroundTintList = secondaryButtonTint()
            setTextColor(resolveColor(MaterialR.attr.colorOnSecondary))
        }
        loadingView = LoadingView(context)
        bottomContentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    backButton,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = 8
                    },
                )
                addView(
                    autoNumberButton,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f).apply {
                        marginStart = 8
                        marginEnd = 8
                    },
                )
                addView(
                    executeButton,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f).apply {
                        marginStart = 8
                    },
                )
            },
        )
        root.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(createFileColumn("リネーム前ファイル", filesContainer))
                addView(createFileColumn("リネーム候補", candidatesContainer))
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        bottomContentContainer.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    resultText,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = 8
                    },
                )
                addView(
                    changeAutoNumberButton,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
            },
        )
        bottomContentContainer.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    undoButton,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = 8
                    },
                )
                addView(
                    loadingView,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
            },
        )
        root.addView(bottomContentContainer)
        applyNavigationBarBottomMargin(root, bottomContentContainer)

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
            val state = viewModel.uiState.value
            val selectedTargetName = state.targetFiles.firstOrNull { file ->
                file.id == state.selectedTargetFileId
            }?.displayName
            val selectedCandidateName = state.renameCandidates.firstOrNull { candidate ->
                candidate.id == state.selectedCandidateId
            }?.displayName
            Log.d(
                TAG_PERF,
                "rename click start selectedTargetName=$selectedTargetName selectedCandidateName=$selectedCandidateName",
            )
            viewModel.executeSelectedRename()
        }
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        autoNumberButton.setOnClickListener {
            viewModel.toggleAutoNumbering()
        }
        changeAutoNumberButton.setOnClickListener {
            showAutoNumberDialog()
        }
        undoButton.setOnClickListener {
            viewModel.onUndoClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderFiles(state.targetFiles, state.isExecuting)
                    renderCandidates(state.renameCandidates, state.isExecuting)
                    executeButton.isEnabled = state.canExecuteRename && !state.isExecuting
                    executeButton.text = if (state.isExecuting) {
                        "リネーム中..."
                    } else {
                        "リネーム実行"
                    }
                    autoNumberButton.isEnabled = !state.isExecuting
                    autoNumberButton.text = if (state.isAutoNumberingEnabled) {
                        "自動連番: ON"
                    } else {
                        "自動連番: OFF"
                    }
                    autoNumberButton.backgroundTintList = if (state.isAutoNumberingEnabled) {
                        primaryButtonTint()
                    } else {
                        secondaryButtonTint()
                    }
                    autoNumberButton.setTextColor(
                        resolveColor(
                            if (state.isAutoNumberingEnabled) {
                                MaterialR.attr.colorOnPrimary
                            } else {
                                MaterialR.attr.colorOnSecondary
                            },
                        ),
                    )
                    loadingView.setLoading(state.isExecuting)
                    undoButton.isEnabled = state.canUndo && !state.isExecuting
                    undoButton.text = if (state.renameHistoryCount > 0) {
                        "UNDO (${state.renameHistoryCount})"
                    } else {
                        "UNDO"
                    }
                    val shouldShowChangeButton = state.isAutoNumberingEnabled &&
                        state.selectedTargetFileId != null &&
                        state.selectedCandidateId != null &&
                        state.selectedPreviewText != null
                    changeAutoNumberButton.visibility = if (shouldShowChangeButton) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    changeAutoNumberButton.isEnabled = shouldShowChangeButton && !state.isExecuting
                    resultText.text = if (state.isExecuting) {
                        "リネーム中..."
                    } else {
                        state.selectedPreviewText ?: state.lastResult?.let { result ->
                            if (result.success) {
                                if (result != lastSyncedSuccessResult) {
                                    lastSyncedSuccessResult = result
                                    homeViewModel.applyRenameResult(result)
                                }
                                "成功: ${result.beforeName} -> ${result.afterName} に変更しました。"
                            } else {
                                "失敗: ${toResultErrorMessage(result)}"
                            }
                        }.orEmpty()
                    }

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

    private fun renderFiles(files: List<RenameTargetFile>, isExecuting: Boolean) {
        filesContainer.removeAllViews()
        files.forEach { file ->
            filesContainer.addView(
                Button(requireContext()).apply {
                    text = buildString {
                        if (file.isRenamed) append("[リネーム済み] ")
                        append(file.displayName)
                    }
                    isAllCaps = false
                    isEnabled = !file.isRenamed && !isExecuting
                    textSize = BODY_TEXT_SIZE_SP
                    backgroundTintList = itemButtonTint(
                        isSelected = file.isSelected,
                        isCompleted = file.isRenamed,
                    )
                    if (file.isSelected) {
                        setTextColor(Color.WHITE)
                    }
                    setOnClickListener { viewModel.selectTargetFile(file.id) }
                },
            )
        }
    }

    private fun renderCandidates(candidates: List<RenameCandidate>, isExecuting: Boolean) {
        candidatesContainer.removeAllViews()
        candidates.forEach { candidate ->
            candidatesContainer.addView(
                Button(requireContext()).apply {
                    text = buildString {
                        if (candidate.isUsed) append("[使用済み] ")
                        append(candidate.displayName)
                    }
                    isAllCaps = false
                    isEnabled = !candidate.isUsed && !isExecuting
                    textSize = BODY_TEXT_SIZE_SP
                    backgroundTintList = itemButtonTint(
                        isSelected = candidate.isSelected,
                        isCompleted = candidate.isUsed,
                    )
                    if (candidate.isSelected) {
                        setTextColor(Color.WHITE)
                    }
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

    private fun createFileColumn(title: String, content: LinearLayout): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            addView(
                TextView(requireContext()).apply {
                    text = title
                    textSize = HEADING_TEXT_SIZE_SP
                },
            )
            addView(
                ScrollView(requireContext()).apply {
                    addView(content)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f,
            )
        }
    }

    private fun applyNavigationBarBottomMargin(insetsSource: View, target: View) {
        val initialBottomMargin = (target.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

        fun applyBottomMarginFromRootInsets() {
            val rootInsets = ViewCompat.getRootWindowInsets(insetsSource)
                ?: ViewCompat.getRootWindowInsets(requireActivity().window.decorView)
                ?: return

            val navigationBottomInset = rootInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val systemBottomInset = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val bottomInset = maxOf(
                navigationBottomInset,
                systemBottomInset,
            )
            val layoutParams = target.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = initialBottomMargin + bottomInset
            target.layoutParams = layoutParams
        }

        ViewCompat.setOnApplyWindowInsetsListener(insetsSource) { _, insets ->
            val navigationBottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val systemBottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val bottomInset = maxOf(navigationBottomInset, systemBottomInset)
            val layoutParams = target.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = initialBottomMargin + bottomInset
            target.layoutParams = layoutParams
            insets
        }

        insetsSource.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    ViewCompat.requestApplyInsets(view)
                    ViewCompat.requestApplyInsets(requireActivity().window.decorView)
                    view.post {
                        applyBottomMarginFromRootInsets()
                    }
                }

                override fun onViewDetachedFromWindow(view: View) = Unit
            },
        )
        insetsSource.post {
            ViewCompat.requestApplyInsets(insetsSource)
            applyBottomMarginFromRootInsets()
        }
    }

    private fun resolveActionBarHeight(): Int {
        val typedValue = TypedValue()
        return if (requireContext().theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        } else {
            0
        }
    }

    private fun primaryButtonTint(): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(),
            ),
            intArrayOf(
                resolveColor(android.R.attr.colorControlNormal),
                resolveColor(MaterialR.attr.colorPrimary),
            ),
        )
    }

    private fun secondaryButtonTint(): ColorStateList {
        return ColorStateList.valueOf(resolveColor(MaterialR.attr.colorSecondary))
    }

    private fun itemButtonTint(isSelected: Boolean, isCompleted: Boolean): ColorStateList {
        return when {
            isSelected -> primaryButtonTint()
            isCompleted -> ColorStateList.valueOf(resolveColor(android.R.attr.colorControlNormal))
            else -> ColorStateList.valueOf(resolveColor(android.R.attr.colorButtonNormal))
        }
    }

    private fun showAutoNumberDialog() {
        val state = viewModel.uiState.value
        val currentAutoNumber = viewModel.getNextAutoNumberForSelectedCandidate() ?: return
        val currentPreview = state.selectedPreviewText.orEmpty()
        Log.d(
            LOG_TAG,
            "RenameMatchingFragment.showAutoNumberDialog isAutoNumberingEnabled=${state.isAutoNumberingEnabled} currentAutoNumberBeforeDialog=$currentAutoNumber selectedPreviewText=$currentPreview",
        )

        val numberInput = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentAutoNumber.toString())
            selectAll()
            textSize = BODY_TEXT_SIZE_SP
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(
                TextView(requireContext()).apply {
                    text = "現在の予定名:\n$currentPreview"
                    textSize = BODY_TEXT_SIZE_SP
                },
            )
            addView(
                TextView(requireContext()).apply {
                    text = "次に使う番号:"
                    textSize = BODY_TEXT_SIZE_SP
                },
            )
            addView(numberInput)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("連番番号を変更")
            .setView(content)
            .setPositiveButton("OK", null)
            .setNegativeButton("キャンセル", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val requestedAutoNumber = numberInput.text.toString().toIntOrNull()
                if (requestedAutoNumber == null || requestedAutoNumber < MIN_AUTO_NUMBER) {
                    numberInput.error = "1以上の数字を入力してください"
                    return@setOnClickListener
                }

                Log.d(
                    LOG_TAG,
                    "RenameMatchingFragment.submitAutoNumber requestedAutoNumber=$requestedAutoNumber",
                )
                viewModel.setNextAutoNumberForSelectedCandidate(requestedAutoNumber)
                dialog.dismiss()
            }
            numberInput.requestFocus()
            numberInput.post {
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(numberInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        dialog.show()
    }

    private fun resolveColor(attribute: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attribute, typedValue, true)
        return typedValue.data
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val TAG_PERF = "EasyRenamePerf"
        const val BODY_TEXT_SIZE_SP = 16f
        const val HEADING_TEXT_SIZE_SP = 18f
        const val MIN_AUTO_NUMBER = 1
    }
}
