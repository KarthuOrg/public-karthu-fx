package org.karthu.fx.form;

/**
 * The syntax a cron expression is written in: the Linux crontab's (Unix) or Quartz's. The two look alike but differ in
 * the number of fields, the numbering of the weekdays and the special characters they allow.
 *
 * <p>A format is shown by its name, {@link #displayName()}, which {@link #toString()} returns too, so that a list or a
 * combo box shows it without a converter.
 */
public enum CronFormat {

    /**
     * The Linux crontab, as cronie and Vixie cron take it: five fields (minute, hour, day of month, month, weekday),
     * Sunday as 0 or 7, the macros such as {@code @daily}. Shown as <em>Unix</em>.
     */
    UNIX("Unix"),

    /**
     * Quartz's cron expressions: six or seven fields (second first, an optional year last), Sunday as 1, exactly one of
     * day of month and weekday {@code ?}, and the special forms {@code L}, {@code W} and {@code #}. Shown as
     * <em>Quartz</em>.
     */
    QUARTZ("Quartz");

    private final String displayName;

    CronFormat(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the name a user reads for this format: <em>Unix</em> or <em>Quartz</em>.
     *
     * @return the format's name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the format's name, {@link #displayName()}.
     *
     * @return the format's name
     */
    @Override
    public String toString() {
        return displayName;
    }
}
