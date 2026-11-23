package com.idiom.memo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.idiom.memo.data.Entry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DrawPlannerTest {
    private static Entry item(long id, String category) {
        Entry e = new Entry();
        e.id = id;
        e.term = "成语" + id;
        e.content = "解释" + id;
        e.category = category;
        return e;
    }

    @Test
    public void shortageIsFilledFromOtherCategories() {
        List<Entry> pool = new ArrayList<>();
        for (int i = 1; i <= 3; i++) pool.add(item(i, Entry.UNKNOWN));
        for (int i = 4; i <= 13; i++) pool.add(item(i, Entry.FUZZY));

        List<Entry> picked = DrawPlanner.plan(pool, 20, 50, 30, 20, new Random(1));

        assertEquals(13, picked.size());
        Set<Long> ids = new HashSet<>();
        for (Entry e : picked) ids.add(e.id);
        assertEquals(13, ids.size());
    }

    @Test
    public void respectsSizeWhenPoolIsLargeEnough() {
        List<Entry> pool = new ArrayList<>();
        for (int i = 1; i <= 100; i++) pool.add(item(i, Entry.UNKNOWN));

        List<Entry> picked = DrawPlanner.plan(pool, 20, 50, 30, 20, new Random(2));

        assertEquals(20, picked.size());
        assertEquals(20, new HashSet<>(picked).size());
    }

    @Test
    public void emptyPoolReturnsEmpty() {
        assertTrue(DrawPlanner.plan(new ArrayList<>(), 20, 50, 30, 20, new Random(3)).isEmpty());
    }

    @Test
    public void unlearnedParticipatesAsUnknownBucket() {
        List<Entry> pool = new ArrayList<>();
        pool.add(item(1, Entry.UNLEARNED));
        pool.add(item(2, Entry.UNLEARNED));

        List<Entry> picked = DrawPlanner.plan(pool, 10, 100, 0, 0, new Random(4));

        assertEquals(2, picked.size());
    }
}
