package com.hoangluongtran0309.domain.balance;

/**
 * The broad kind of gear a material represents. Items are only ever compared against others in
 * the same category, so a strong pickaxe is not reported as an outlier next to a weak helmet.
 */
public enum MaterialCategory {
    SWORD,
    TOOL,
    ARMOR,
    OTHER
}
