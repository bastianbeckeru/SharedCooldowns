package com.sharedcooldowns.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Formats remaining time for the %time_compact% placeholder. */
public final class TimeFormatter {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private TimeFormatter() {}

    /**
     * Fills in %days%, %hours%, %minutes% and %seconds%, dropping units that are
     * zero. A word without a placeholder is a label of the unit before it, so
     * "%hours% hours" drops as a whole; words before the first unit are always kept.
     */
    public static String compact(String format, long totalSeconds) {
        String[] names = {"%days%", "%hours%", "%minutes%", "%seconds%"};
        long[] values = {
            totalSeconds / 86400,
            (totalSeconds % 86400) / 3600,
            (totalSeconds % 3600) / 60,
            totalSeconds % 60
        };

        List<String> kept = new ArrayList<>();
        boolean previousDropped = false;
        boolean unitShown = false;
        for (String token : WHITESPACE_PATTERN.split(format)) {
            boolean hasUnit = false;
            boolean nonZero = false;
            String replaced = token;
            for (int i = 0; i < names.length; i++) {
                if (token.contains(names[i])) {
                    hasUnit = true;
                    nonZero |= values[i] > 0;
                    replaced = replaced.replace(names[i], String.valueOf(values[i]));
                }
            }
            if (!hasUnit) {
                if (!previousDropped) {
                    kept.add(token);
                }
                continue;
            }
            // Show seconds even at zero when nothing else was shown, so the result is never blank.
            boolean keep = nonZero || (token.contains("%seconds%") && !unitShown);
            if (keep) {
                kept.add(replaced);
                unitShown = true;
            }
            previousDropped = !keep;
        }
        return String.join(" ", kept);
    }
}
