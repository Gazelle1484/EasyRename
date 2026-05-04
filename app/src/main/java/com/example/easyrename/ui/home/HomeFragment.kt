package com.example.easyrename.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.easyrename.model.RenameMode
import com.example.easyrename.ui.AppViewModelFactory
import com.example.easyrename.ui.MainActivity
import com.example.easyrename.ui.common.ErrorDialog
import com.example.easyrename.ui.common.LoadingView
import com.example.easyrename.ui.matching.RenameMatchingFragment
import com.example.easyrename.viewmodel.HomeViewModel
import com.google.android.material.R as MaterialR
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var lastUsedSetButton: Button
    private lateinit var directoryNameText: TextView
    private lateinit var csvNameText: TextView
    private lateinit var targetFileCountText: TextView
    private lateinit var candidateCountText: TextView
    private lateinit var renameModeSpinner: Spinner
    private lateinit var startMatchingButton: Button
    private lateinit var loadingView: LoadingView
    private lateinit var startMatchingDisabledTextColors: ColorStateList

    private val directoryPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            viewModel.onDirectorySelected(uri)
        }
    }

    private val csvPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onCsvSelected(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val context = requireContext()
        val scrollView = ScrollView(context).apply {
            isFillViewport = true
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32 + resolveActionBarHeight(), 32, 32)
        }
        val initialPaddingLeft = root.paddingLeft
        val initialPaddingTop = root.paddingTop
        val initialPaddingRight = root.paddingRight
        val initialPaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                initialPaddingLeft,
                initialPaddingTop,
                initialPaddingRight,
                initialPaddingBottom + bottomInset,
            )
            insets
        }

        lastUsedSetButton = Button(context).apply {
            text = "前回のセット: なし"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            isEnabled = false
            setOnClickListener { viewModel.selectLastUsedSet() }
        }
        val directoryButton = Button(context).apply {
            text = "ディレクトリを選択"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            setOnClickListener { directoryPicker.launch(null) }
        }
        val csvButton = Button(context).apply {
            text = "CSVを選択"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            backgroundTintList = defaultButtonTint()
            setOnClickListener { csvPicker.launch(CSV_MIME_TYPES) }
        }
        directoryNameText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        csvNameText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        targetFileCountText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        candidateCountText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        renameModeSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                RenameMode.entries.map { it.displayName },
            )
        }
        startMatchingButton = Button(context).apply {
            text = "マッチング画面へ進む"
            isAllCaps = false
            textSize = BODY_TEXT_SIZE_SP
            isEnabled = false
            backgroundTintList = defaultButtonTint()
            setOnClickListener {
                viewModel.prepareMatchingData {
                    parentFragmentManager.beginTransaction()
                        .replace(MainActivity.FRAGMENT_CONTAINER_ID, RenameMatchingFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
        loadingView = LoadingView(context)
        startMatchingDisabledTextColors = startMatchingButton.textColors

        root.addView(lastUsedSetButton)
        root.addView(directoryButton)
        root.addView(csvButton)
        root.addView(directoryNameText)
        root.addView(csvNameText)
        root.addView(targetFileCountText)
        root.addView(candidateCountText)
        root.addView(TextView(context).apply {
            text = "リネームモード:"
            textSize = BODY_TEXT_SIZE_SP
        })
        root.addView(renameModeSpinner)
        root.addView(startMatchingButton)
        root.addView(loadingView)

        scrollView.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(
            requireActivity(),
            AppViewModelFactory(requireContext()),
        )[HomeViewModel::class.java]

        renameModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.onRenameModeSelected(RenameMode.entries[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    lastUsedSetButton.text = state.lastUsedSet?.let { lastUsedSet ->
                        "前回のセット: ${lastUsedSet.directoryDisplayName} / ${lastUsedSet.csvDisplayName}"
                    } ?: "前回のセット: なし"
                    lastUsedSetButton.isEnabled = state.isLastUsedSetAvailable && !state.isLoading
                    directoryNameText.text = "選択中ディレクトリ: ${state.selectedDirectoryName ?: "未選択"}"
                    csvNameText.text = "選択中CSV: ${state.selectedCsvFileName ?: "未選択"}"
                    targetFileCountText.text = "読み込み済みファイル数: ${state.targetFileCount}"
                    candidateCountText.text = "読み込み済み候補数: ${state.renameCandidateCount}"
                    val modePosition = RenameMode.entries.indexOf(state.renameMode)
                    if (modePosition >= 0 && renameModeSpinner.selectedItemPosition != modePosition) {
                        renameModeSpinner.setSelection(modePosition)
                    }
                    val hasSelectedInputs = state.selectedDirectoryUri != null && state.selectedCsvUri != null
                    val shouldShowStartReady = hasSelectedInputs &&
                        state.isStartMatchingActionArmed &&
                        !state.hasMatchingPreparationFailed
                    val canStartMatching = shouldShowStartReady && !state.isLoading
                    Log.d(
                        LOG_TAG,
                        "HomeFragment.startMatchingState selectedDirectoryUri=${state.selectedDirectoryUri} selectedCsvUri=${state.selectedCsvUri} selectedDirectoryName=${state.selectedDirectoryName} selectedCsvFileName=${state.selectedCsvFileName} isReadyToStartMatching=${state.isReadyToStartMatching} isLoading=${state.isLoading} hasMatchingPreparationFailed=${state.hasMatchingPreparationFailed} isStartMatchingActionArmed=${state.isStartMatchingActionArmed} shouldShowStartReady=$shouldShowStartReady canStartMatching=$canStartMatching targetFileCount=${state.targetFileCount} renameCandidateCount=${state.renameCandidateCount} selectionRevision=${state.selectionRevision}",
                    )
                    startMatchingButton.isEnabled = canStartMatching
                    if (shouldShowStartReady) {
                        startMatchingButton.visibility = View.VISIBLE
                        startMatchingButton.backgroundTintList = readyStartButtonTint()
                        startMatchingButton.setTextColor(Color.WHITE)
                    } else {
                        startMatchingButton.visibility = View.VISIBLE
                        startMatchingButton.backgroundTintList = defaultButtonTint()
                        startMatchingButton.setTextColor(startMatchingDisabledTextColors)
                    }
                    startMatchingButton.post {
                        val location = IntArray(2)
                        startMatchingButton.getLocationOnScreen(location)
                        Log.d(
                            LOG_TAG,
                            "HomeFragment.startMatchingButtonView visibility=${startMatchingButton.visibility} isShown=${startMatchingButton.isShown} isEnabled=${startMatchingButton.isEnabled} width=${startMatchingButton.width} height=${startMatchingButton.height} screenX=${location[0]} screenY=${location[1]} text=${startMatchingButton.text} backgroundTint=${startMatchingButton.backgroundTintList} textColors=${startMatchingButton.textColors}",
                        )
                    }
                    loadingView.setLoading(state.isLoading)

                    state.error?.let { error ->
                        ErrorDialog.show(requireContext(), error)
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.resetStartMatchingAction()
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

    private fun readyStartButtonTint(): ColorStateList {
        return ColorStateList.valueOf(resolveColor(MaterialR.attr.colorPrimary))
    }

    private fun defaultButtonTint(): ColorStateList {
        return ColorStateList.valueOf(resolveColor(android.R.attr.colorButtonNormal))
    }

    private fun resolveColor(attribute: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attribute, typedValue, true)
        return typedValue.data
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val BODY_TEXT_SIZE_SP = 16f
        val CSV_MIME_TYPES = arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "text/plain",
            "application/csv",
            "application/vnd.ms-excel",
        )
    }
}
