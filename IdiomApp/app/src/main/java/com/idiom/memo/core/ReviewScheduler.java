package com.idiom.memo.core;

import com.idiom.memo.data.Entry;

public class ReviewScheduler {
    public static final long DAY_MS = 86_400_000L;

    private static int daysFor(String category, int[] days) {
        int d;
        if (Entry.FUZZY.equals(category)) d = days[1];
        else if (Entry.KNOWN.equals(category)) d = days[2];
        else d = days[0];
        if (d < 1) d = 1;
        if (d > 365) d = 365;
        return d;
    }

    public static long next(long now, String category, int[] days) {
        return now + daysFor(category, days) * DAY_MS;
    }
}
