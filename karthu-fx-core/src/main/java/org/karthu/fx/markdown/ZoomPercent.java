package org.karthu.fx.markdown;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Markdown view's zoom in whole percent: the one source of its range, its rounding and its presets, for the zoom
 * property, the zoom bar, the wheel and the shortcuts. It gives the zoom a factor stands for, the step to the next
 * preset in either direction, the text the percent field shows and the rules by which it reads a typed one.
 */
final class ZoomPercent {

    /**
     * The smallest zoom, 50 %.
     */
    static final int MIN = 50;

    /**
     * The largest zoom, 200 %.
     */
    static final int MAX = 200;

    /**
     * The normal size, 100 %.
     */
    static final int NORMAL = 100;

    /**
     * The presets, smallest first: where minus and plus, Up and Down, the wheel, the shortcuts and the list of zoom
     * levels go.
     */
    static final List<Integer> PRESETS = List.of(50, 75, 100, 125, 150, 200);

    /**
     * The widest text the percent field shows.
     */
    static final String WIDEST = format(MAX);

    /**
     * A typed zoom: a decimal number with a dot or a comma, then perhaps a percent sign after spaces.
     */
    private static final Pattern TYPED = Pattern.compile("([+-]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+))\\s*%?");

    private ZoomPercent() {
    }

    /**
     * The zoom a factor stands for, in whole percent: limited to 50 to 200 and rounded half up, not a number as 100.
     *
     * @param factor any factor, 1.0 for 100 %
     * @return the percent the view shows
     */
    static int of(double factor) {
        if (Double.isNaN(factor)) {
            return NORMAL;
        }
        // Limited first: an infinite factor has no decimal. Then through the decimal the factor is written as, so
        // 0.995 gives 100 and not the 99 of its binary fraction.
        double limited = Math.clamp(factor, factor(MIN), factor(MAX));
        return wholePercent(BigDecimal.valueOf(limited).movePointRight(2));
    }

    /**
     * The factor of a percent.
     *
     * @param percent the zoom in percent
     * @return the factor, 1.0 for 100
     */
    static double factor(int percent) {
        return percent / 100.0;
    }

    /**
     * The text the percent field shows: the whole percent followed by {@code %} without a space, such as
     * {@code 125%}.
     *
     * @param percent the zoom in percent
     * @return the text
     */
    static String format(int percent) {
        return percent + "%";
    }

    /**
     * Reads a typed zoom: the text without surrounding spaces, without a trailing {@code %} and the spaces before it,
     * as a decimal number with a dot or a comma, rounded half up to whole percent and limited to 50 to 200.
     *
     * @param text the typed text
     * @return the zoom in percent, empty when the text is no number (the empty text included)
     */
    static OptionalInt parse(String text) {
        Matcher matcher = TYPED.matcher(text.strip());
        if (!matcher.matches()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(wholePercent(new BigDecimal(matcher.group(1).replace(',', '.'))));
    }

    /**
     * The step rule of the zoom: the preset a number of presets away in their direction, also from a value in
     * between, so a step always lands on a preset (from 130, one step up gives 150 and one down 125). The steps stop
     * at the ends of the range: from 200 a step up stays at 200.
     *
     * @param percent the zoom in percent
     * @param presets how many presets to go: positive for larger, negative for smaller, 0 for none
     * @return the zoom in percent after the steps
     */
    static int step(int percent, int presets) {
        int stepped = percent;
        for (int i = 0; i < Math.abs(presets); i++) {
            stepped = presets > 0 ? larger(stepped) : smaller(stepped);
        }
        return stepped;
    }

    /**
     * The one rounding and the one range of the zoom: half up to whole percent, then limited to 50 to 200.
     */
    private static int wholePercent(BigDecimal percent) {
        BigDecimal rounded = percent.setScale(0, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.valueOf(MIN)) < 0) {
            return MIN;
        }
        if (rounded.compareTo(BigDecimal.valueOf(MAX)) > 0) {
            return MAX;
        }
        return rounded.intValueExact();
    }

    /**
     * The next preset larger than a zoom, or the zoom itself at the largest preset.
     */
    private static int larger(int percent) {
        for (int preset : PRESETS) {
            if (preset > percent) {
                return preset;
            }
        }
        return percent;
    }

    /**
     * The next preset smaller than a zoom, or the zoom itself at the smallest preset.
     */
    private static int smaller(int percent) {
        for (int i = PRESETS.size() - 1; i >= 0; i--) {
            if (PRESETS.get(i) < percent) {
                return PRESETS.get(i);
            }
        }
        return percent;
    }
}
