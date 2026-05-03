package com.example.easyrename.domain.usecase

import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameRule

class GenerateRenameCandidateUseCase {

    operator fun invoke(rules: List<RenameRule>): List<RenameCandidate> {
        return rules.map { rule ->
            RenameCandidate(
                id = rule.id,
                ruleId = rule.id,
                displayName = rule.rawPattern,
                rawPattern = rule.rawPattern,
                isUsed = rule.isUsed,
            )
        }
    }
}
