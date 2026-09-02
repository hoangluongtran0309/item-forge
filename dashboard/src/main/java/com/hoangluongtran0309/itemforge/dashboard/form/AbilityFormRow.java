package com.hoangluongtran0309.itemforge.dashboard.form;

/**
 * Always exposes the fields of BOTH ability kinds (POTION_EFFECT and DAMAGE_BONUS). The
 * form template hides the irrelevant group with a &lt;select onchange&gt; toggle (HTML's
 * `hidden` attribute does not stop a field being submitted), and ItemFormMapper decides
 * which fields still mean anything based on the type.
 */
public class AbilityFormRow {

    private String type = "POTION_EFFECT";
    private String trigger = "RIGHT_CLICK";
    private Integer cooldownSeconds;
    private String effect;
    private Integer durationSeconds;
    private Double bonusPercent;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Integer cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Double getBonusPercent() {
        return bonusPercent;
    }

    public void setBonusPercent(Double bonusPercent) {
        this.bonusPercent = bonusPercent;
    }
}
