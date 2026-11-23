package com.idiom.memo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.idiom.memo.data.Entry;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class FrequencyUnlockTest {
    private static Entry entry(int frequency, String category) {
        Entry e = new Entry();
        e.frequency = frequency;
        e.category = category;
        return e;
    }

    @Test
    public void groupsAreSortedByFrequencyDescending() {
        List<FrequencyUnlock.Group> groups = FrequencyUnlock.plan(Arrays.asList(
                entry(18, Entry.UNKNOWN),
                entry(20, Entry.UNKNOWN),
                entry(19, Entry.UNKNOWN)
        ), 30);

        assertEquals(3, groups.size());
        assertEquals(20, groups.get(0).frequency);
        assertEquals(19, groups.get(1).frequency);
        assertEquals(18, groups.get(2).frequency);
        assertTrue(groups.get(0).unlocked);
        assertFalse(groups.get(1).unlocked);
        assertFalse(groups.get(2).unlocked);
    }

    @Test
    public void achievedRequiresNoUnknownAndFuzzyBelowKnownRatio() {
        assertFalse(FrequencyUnlock.achieved(1, 0, 9, 30));
        assertFalse(FrequencyUnlock.achieved(0, 2, 5, 30));
        assertTrue(FrequencyUnlock.achieved(0, 1, 4, 30));
        assertTrue(FrequencyUnlock.achieved(0, 0, 1, 30));
        assertTrue(FrequencyUnlock.achieved(0, 0, 2, 0));
        assertFalse(FrequencyUnlock.achieved(0, 1, 9, 0));
    }

    @Test
    public void lowerGroupUnlocksAfterHigherGroupAchieves() {
        List<FrequencyUnlock.Group> groups = FrequencyUnlock.plan(Arrays.asList(
                entry(20, Entry.KNOWN),
                entry(20, Entry.KNOWN),
                entry(20, Entry.KNOWN),
                entry(20, Entry.KNOWN),
                entry(20, Entry.FUZZY),
                entry(19, Entry.UNKNOWN),
                entry(18, Entry.UNKNOWN)
        ), 30);

        assertTrue(groups.get(0).achieved);
        assertTrue(groups.get(1).unlocked);
        assertFalse(groups.get(1).achieved);
        assertFalse(groups.get(2).unlocked);
    }

    @Test
    public void revertingHigherGroupRelocksAllLowerGroups() {
        List<Entry> entries = Arrays.asList(
                entry(20, Entry.KNOWN),
                entry(19, Entry.KNOWN),
                entry(18, Entry.UNKNOWN)
        );
        List<FrequencyUnlock.Group> before = FrequencyUnlock.plan(entries, 30);
        assertTrue(before.get(1).unlocked);
        assertTrue(before.get(2).unlocked);

        entries.get(0).category = Entry.UNKNOWN;

        List<FrequencyUnlock.Group> after = FrequencyUnlock.plan(entries, 30);
        assertFalse(after.get(0).achieved);
        assertFalse(after.get(1).unlocked);
        assertFalse(after.get(2).unlocked);
    }
}
