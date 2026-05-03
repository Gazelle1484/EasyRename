package com.example.easyrename.domain.usecase

import android.net.Uri
import com.example.easyrename.data.csv.CsvRuleParser
import com.example.easyrename.data.repository.StorageRepository
import com.example.easyrename.model.RenameRule

class LoadRenameRulesFromCsvUseCase(
    private val storageRepository: StorageRepository,
    private val csvRuleParser: CsvRuleParser,
) {

    operator fun invoke(csvUri: Uri): List<RenameRule> {
        val csvText = storageRepository.readTextFromUri(csvUri)
        return csvRuleParser.parse(csvText)
    }
}
