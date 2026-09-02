package com.hoangluongtran0309.domain.balance;

public enum BalanceSeverity {

    /** Worth knowing about, but not a balance problem on its own. */
    INFO,

    /** Likely to distort gameplay; the admin should look at it. */
    WARNING,

    /** Breaks the intended progression outright, for example a permanently active effect. */
    CRITICAL
}
