package com.example.easyrename.data.csv

import com.example.easyrename.model.RenameRule

class CsvRuleParser {

    fun parse(csvText: String): List<RenameRule> {
        return csvText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, rawPattern ->
                val wildcardIndex = rawPattern.indexOf('*')
                RenameRule(
                    id = index.toString(),
                    rawPattern = rawPattern,
                    prefix = if (wildcardIndex >= 0) rawPattern.substring(0, wildcardIndex) else rawPattern,
                    suffix = if (wildcardIndex >= 0) rawPattern.substring(wildcardIndex + 1) else "",
                    hasWildcard = wildcardIndex >= 0,
                )
            }
            .toList()
    }
}
