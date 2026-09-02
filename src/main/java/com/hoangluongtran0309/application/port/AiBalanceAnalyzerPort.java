package com.hoangluongtran0309.application.port;

import java.util.List;

import com.hoangluongtran0309.domain.balance.BalanceAnalysisRequest;
import com.hoangluongtran0309.domain.balance.BalanceFinding;
import com.hoangluongtran0309.domain.balance.BalanceReport;

/**
 * Asks an AI provider for the judgement calls the rules cannot make -- whether a set of items
 * fits together, whether a theme is coherent, whether a number is right for this server.
 */
public interface AiBalanceAnalyzerPort {

    /**
     * @param ruleFindings what the deterministic rules already found, passed along as context so
     *        the model comments on the real numbers instead of re-deriving them and repeats less
     * @return findings the rules did not produce, plus a narrative summary
     * @throws com.hoangluongtran0309.application.exception.AiBalanceAnalysisException if the
     *         provider call fails
     */
    BalanceReport analyze(BalanceAnalysisRequest request, List<BalanceFinding> ruleFindings);
}
