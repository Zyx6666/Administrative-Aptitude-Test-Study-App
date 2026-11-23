package com.idiom.memo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.idiom.memo.data.Entry;

import org.junit.Test;

public class ReviewSchedulerTest {
    private static final long NOW = 1_700_000_000_000L;

    @Test
    public void knownUsesSevenDays() {
        assertEquals(NOW + 7 * ReviewScheduler.DAY_MS,
                ReviewScheduler.next(NOW, Entry.KNOWN, new int[]{1, 3, 7}));
    }

    @Test
    public void zeroDaysClampedToOne() {
        assertEquals(NOW + ReviewScheduler.DAY_MS,
                ReviewScheduler.next(NOW, Entry.UNKNOWN, new int[]{0, 3, 7}));
    }

    @Test
    public void tooLargeClampedTo365() {
        assertEquals(NOW + 365 * ReviewScheduler.DAY_MS,
                ReviewScheduler.next(NOW, Entry.FUZZY, new int[]{1, 500, 7}));
    }

    @Test
    public void choiceRulesOnlyKnownWrongAsksRecategory() {
        assertFalse(ChoiceRules.askRecategory(Entry.UNKNOWN, false));
        assertFalse(ChoiceRules.askRecategory(Entry.UNKNOWN, true));
        assertFalse(ChoiceRules.askRecategory(Entry.FUZZY, false));
        assertFalse(ChoiceRules.askRecategory(Entry.KNOWN, true));
        assertTrue(ChoiceRules.askRecategory(Entry.KNOWN, false));
    }
}
