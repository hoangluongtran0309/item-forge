package com.hoangluongtran0309.domain.balance;

/**
 * One thing worth telling the admin about one item, armor piece, or the config as a whole.
 *
 * @param targetId   the id the finding is about, or {@code "*"} when it concerns the whole config
 * @param severity   how much it matters
 * @param rule       stable identifier of what produced it, e.g. {@code PERMANENT_EFFECT}
 * @param issue      what is wrong, quoting the actual numbers
 * @param suggestion what to change; may be blank when there is nothing concrete to propose
 * @param source     whether a rule or the AI produced it
 */
public record BalanceFinding(
        String targetId,
        BalanceSeverity severity,
        String rule,
        String issue,
        String suggestion,
        FindingSource source) {

    /** Used as the target of a finding about the configuration as a whole. */
    public static final String WHOLE_CONFIG = "*";

    public BalanceFinding {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Finding targetId cannot be blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Finding severity cannot be null");
        }
        if (issue == null || issue.isBlank()) {
            throw new IllegalArgumentException("Finding issue cannot be blank");
        }
        rule = rule == null ? "" : rule;
        suggestion = suggestion == null ? "" : suggestion;
        source = source == null ? FindingSource.RULE : source;
    }

    public static BalanceFinding rule(String targetId, BalanceSeverity severity, String rule, String issue,
            String suggestion) {
        return new BalanceFinding(targetId, severity, rule, issue, suggestion, FindingSource.RULE);
    }
}
