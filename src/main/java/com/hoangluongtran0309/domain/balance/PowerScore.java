package com.hoangluongtran0309.domain.balance;

/**
 * How strong one item is, broken down so a finding can quote the part that caused it rather
 * than only a total.
 *
 * @param total           materialTier + abilityScore, on the same 1-5-per-tier scale
 * @param materialTier    the vanilla progression tier of the base material, 0 when unrecognized
 * @param abilityScore    what the abilities add on top
 * @param maxUptimeRatio  the highest {@code duration / cooldown} of any potion-effect ability;
 *                        1.0 or more means the effect is always available
 */
public record PowerScore(double total, double materialTier, double abilityScore, double maxUptimeRatio) {

    public boolean hasKnownMaterial() {
        return materialTier != MaterialTier.UNKNOWN_TIER;
    }
}
