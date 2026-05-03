package com.example.easyrename.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
    private lateinit var directoryNameText: TextView
    private lateinit var csvNameText: TextView
    private lateinit var targetFileCountText: TextView
    private lateinit var candidateCountText: TextView
    private lateinit var startMatchingButton: Button
    private lateinit var loadingView: LoadingView

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
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32 + resolveActionBarHeight(), 32, 32)
        }

        val directoryButton = Button(context).apply {
            text = "ディレクトリを選択"
            textSize = BODY_TEXT_SIZE_SP
            setOnClickListener { directoryPicker.launch(null) }
        }
        val csvButton = Button(context).apply {
            text = "CSVを選択"
            textSize = BODY_TEXT_SIZE_SP
            setOnClickListener { csvPicker.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel")) }
        }
        directoryNameText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        csvNameText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        targetFileCountText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        candidateCountText = TextView(context).apply { textSize = BODY_TEXT_SIZE_SP }
        startMatchingButton = Button(context).apply {
            text = "マッチング画面へ進む"
            textSize = BODY_TEXT_SIZE_SP
            isEnabled = false
            backgroundTintList = primaryButtonTint()
            setTextColor(resolveColor(MaterialR.attr.colorOnPrimary))
            setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(MainActivity.FRAGMENT_CONTAINER_ID, RenameMatchingFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        loadingView = LoadingView(context)

        root.addView(directoryButton)
        root.addView(csvButton)
        root.addView(directoryNameText)
        root.addView(csvNameText)
        root.addView(targetFileCountText)
        root.addView(candidateCountText)
        root.addView(startMatchingButton)
        root.addView(loadingView)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(
            requireActivity(),
            AppViewModelFactory(requireContext()),
        )[HomeViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    directoryNameText.text = "選択中ディレクトリ: ${state.selectedDirectoryName ?: "未選択"}"
                    csvNameText.text = "選択中CSV: ${state.selectedCsvFileName ?: "未選択"}"
                    targetFileCountText.text = "読み込み済みファイル数: ${state.targetFileCount}"
                    candidateCountText.text = "読み込み済み候補数: ${state.renameCandidateCount}"
                    startMatchingButton.isEnabled = state.isReadyToStartMatching
                    loadingView.setLoading(state.isLoading)

                    state.error?.let { error ->
                        ErrorDialog.show(requireContext(), error)
                        viewModel.clearError()
                    }
                }
            }
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

    private fun resolveColor(attribute: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attribute, typedValue, true)
        return typedValue.data
    }

    private companion object {
        const val BODY_TEXT_SIZE_SP = 16f
    }
}
