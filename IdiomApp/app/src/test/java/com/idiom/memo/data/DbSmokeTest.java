package com.idiom.memo.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.idiom.memo.core.CategorySync;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DbSmokeTest {
    private static Entry newEntry(String term, String content, String category) {
        long now = System.currentTimeMillis();
        Entry e = new Entry();
        e.term = term;
        e.content = content;
        e.category = category;
        e.createdAt = now;
        e.updatedAt = now;
        return e;
    }

    @Test
    public void insertFeedbackAndChoiceResult() {
        Context context = RuntimeEnvironment.getApplication();
        EntryDao dao = new EntryDao(context, Module.VERBAL.id);
        long id = dao.insert(newEntry("差强人意", "大体上还能使人满意", Entry.UNKNOWN));

        assertTrue(id > 0);
        Entry loaded = dao.findById(id);
        assertNotNull(loaded);
        assertEquals("差强人意", loaded.term);
        assertEquals(Entry.UNKNOWN, loaded.category);

        long now = 1_700_000_000_000L;
        long next = now + 3 * 86_400_000L;
        dao.applyFeedback(id, Entry.FUZZY, next, false, now);

        loaded = dao.findById(id);
        assertEquals(Entry.FUZZY, loaded.category);
        assertEquals(1, loaded.fuzzyCount);
        assertEquals(next, loaded.nextReviewAt.longValue());

        dao.applyChoiceResult(id, false, Entry.KNOWN, next + 86_400_000L, now + 1000);

        loaded = dao.findById(id);
        assertEquals("wrong", loaded.lastChoiceResult);
        assertEquals(1, loaded.choiceWrongCount);
        assertEquals(1, loaded.knownCount);
        assertEquals(Entry.KNOWN, loaded.category);
        assertEquals(1, dao.countByCategory(Entry.KNOWN));
    }

    @Test
    public void modulesAreIsolated() {
        Context context = RuntimeEnvironment.getApplication();
        EntryDao verbal = new EntryDao(context, Module.VERBAL.id);
        EntryDao quantity = new EntryDao(context, Module.QUANTITY.id);

        verbal.insert(newEntry("同一词条", "言语理解内容", Entry.UNKNOWN));
        quantity.insert(newEntry("同一词条", "数量关系内容", Entry.UNKNOWN));

        assertEquals(1, verbal.countAll());
        assertEquals(1, quantity.countAll());
        assertEquals("言语理解内容", verbal.findByTerm("同一词条").content);
        assertEquals("数量关系内容", quantity.findByTerm("同一词条").content);

        verbal.deleteAll();
        assertEquals(0, verbal.countAll());
        assertEquals(1, quantity.countAll());
    }

    @Test
    public void crudOperationsWork() {
        Context context = RuntimeEnvironment.getApplication();
        EntryDao dao = new EntryDao(context, Module.VERBAL.id);
        long id = dao.insert(newEntry("画蛇添足", "多此一举", Entry.UNLEARNED));

        dao.updateTerm(id, "画龙点睛", System.currentTimeMillis());
        dao.updateContent(id, "比喻在关键处点明要旨", System.currentTimeMillis());
        Entry loaded = dao.findById(id);
        assertEquals("画龙点睛", loaded.term);
        assertEquals("比喻在关键处点明要旨", loaded.content);

        dao.delete(id);
        assertEquals(null, dao.findById(id));
    }

    @Test
    public void settingsHaveDefaultsAndPersistPerModule() {
        Context context = RuntimeEnvironment.getApplication();
        SettingsDao verbal = new SettingsDao(context, Module.VERBAL.id);
        SettingsDao quantity = new SettingsDao(context, Module.QUANTITY.id);

        assertEquals(1, verbal.getIntervals()[0]);
        assertEquals(3, verbal.getIntervals()[1]);
        assertEquals(7, verbal.getIntervals()[2]);
        assertEquals(20, verbal.getSessionSize());

        verbal.save(new int[]{2, 5, 10}, new int[]{60, 30, 10}, 30);
        assertEquals(10, verbal.getIntervals()[2]);
        assertEquals(30, verbal.getSessionSize());

        assertEquals(1, quantity.getIntervals()[0]);
        assertEquals(20, quantity.getSessionSize());
    }

    @Test
    public void frequencyLibraryIsSeparateFromSelfLibrary() {
        Context context = RuntimeEnvironment.getApplication();
        EntryDao self = new EntryDao(context, Module.VERBAL.id);
        EntryDao freq = new EntryDao(context, Module.VERBAL.id, Entry.SOURCE_FREQ);

        Entry mine = newEntry("差强人意", "我的错题本解释", Entry.UNKNOWN);
        mine.frequency = 20;
        self.insert(mine);

        Entry imported = newEntry("差强人意", "考频库解释", Entry.FUZZY);
        imported.frequency = 20;
        long freqId = freq.insert(imported);

        assertEquals(1, self.countAll());
        assertEquals(1, freq.countAll());
        assertEquals("我的错题本解释", self.findByTerm("差强人意").content);
        assertEquals("考频库解释", freq.findByTerm("差强人意").content);
        assertEquals(1, freq.byFrequency(20).size());
        assertEquals("考频库解释", freq.byFrequency(20).get(0).content);
        assertEquals(1, self.byFrequency(20).size());
        assertEquals("我的错题本解释", self.byFrequency(20).get(0).content);

        freq.updateContentAndFrequency(freqId, "考频库新解释", 19,
                System.currentTimeMillis());
        Entry loaded = freq.findById(freqId);
        assertEquals("考频库新解释", loaded.content);
        assertEquals(19, loaded.frequency);
        assertEquals(Entry.FUZZY, loaded.category);
    }

    @Test
    public void categorySyncsBothWays() {
        Context context = RuntimeEnvironment.getApplication();
        EntryDao self = new EntryDao(context, Module.VERBAL.id);
        EntryDao freq = new EntryDao(context, Module.VERBAL.id, Entry.SOURCE_FREQ);

        long selfId = self.insert(newEntry("差强人意", "错题本解释", Entry.FUZZY));
        Entry imported = newEntry("差强人意", "考频库解释", Entry.UNKNOWN);
        imported.frequency = 20;
        freq.insert(imported);

        assertEquals(1, CategorySync.applyToFreqLibrary(freq, CategorySync.selfCategories(self)));
        assertEquals(Entry.FUZZY, freq.findByTerm("差强人意").category);

        self.moveCategory(selfId, Entry.KNOWN, System.currentTimeMillis());
        CategorySync.applyToFreqLibrary(freq, CategorySync.selfCategories(self));
        assertEquals(Entry.KNOWN, freq.findByTerm("差强人意").category);

        CategorySync.applyToSelfLibrary(self, "差强人意", Entry.UNKNOWN);
        assertEquals(Entry.UNKNOWN, self.findById(selfId).category);
        CategorySync.applyToFreqLibrary(freq, CategorySync.selfCategories(self));
        assertEquals(Entry.UNKNOWN, freq.findByTerm("差强人意").category);

        CategorySync.applyToSelfLibrary(self, "不存在", Entry.KNOWN);
        assertEquals(Entry.UNKNOWN, self.findById(selfId).category);
    }
}
