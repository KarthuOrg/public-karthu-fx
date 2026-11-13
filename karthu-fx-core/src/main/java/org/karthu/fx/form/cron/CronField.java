package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.List;

/**
 * One field of a valid cron expression: which field it is, its text as written and its entries, parsed.
 *
 * @param kind  which field
 * @param text  the field's text as written (case kept); for a macro the text of the expression it stands for
 * @param parts the entries of its comma-separated list, in the order written, never empty: {@code *} is a single
 *              {@link CronPart.All}, {@code ?} a single {@link CronPart.NoSpecificValue}
 */
public record CronField(CronFieldKind kind, String text, List<CronPart> parts) {

    /**
     * Creates the field, keeping an unmodifiable copy of its entries.
     *
     * @param kind  which field
     * @param text  the field's text as written
     * @param parts the entries, never empty
     */
    public CronField {
        parts = List.copyOf(parts);
    }

    /**
     * Whether the field is {@code *} alone: it restricts nothing.
     *
     * @return true for {@code *}
     */
    public boolean isAll() {
        return parts.size() == 1 && parts.get(0) instanceof CronPart.All;
    }

    /**
     * Whether the field is {@code ?}, Quartz's no specific value.
     *
     * @return true for {@code ?}
     */
    public boolean isNoSpecificValue() {
        return parts.size() == 1 && parts.get(0) instanceof CronPart.NoSpecificValue;
    }

    /**
     * Whether the field restricts nothing: {@code *}, Quartz's {@code ?}, a list with {@code *} in it, which takes
     * every value whatever else the list holds, or a step of 1 through the whole field ({@code *}{@code /1},
     * {@code 0/1} in the minute). The one reading of "free" for the plain text, which leaves such a field out, and the
     * conversion, which lets such a day field become {@code ?} and drops such a year.
     *
     * @param format the format of the expression, which decides the weekday's lowest value
     * @return true when the field takes every value
     */
    public boolean isFree(CronFormat format) {
        for (CronPart part : parts) {
            if (part instanceof CronPart.All || part instanceof CronPart.NoSpecificValue) {
                return true;
            }
            boolean wholeFieldStep = part instanceof CronPart.Step step && step.step() == 1
                    && kind.isWholeFieldStart(step.start(), format);
            if (wholeFieldStep) {
                return true;
            }
        }
        return false;
    }
}
