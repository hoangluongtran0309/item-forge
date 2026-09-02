package com.hoangluongtran0309.domain.balance;

/**
 * Where a finding came from. Kept on every finding so a reader can tell a reproducible rule
 * result from an AI opinion, and so the two can be shown differently.
 */
public enum FindingSource {
    RULE,
    AI
}
