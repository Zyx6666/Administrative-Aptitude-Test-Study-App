package com.idiom.memo.core;

import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;

import java.util.HashMap;
import java.util.Map;

/**
 * 错题本与考频库的学习状态双向同步：同名成语在两侧保持一致。
 */
public final class CategorySync {
    private CategorySync() {
    }

    public static Map<String, String> selfCategories(EntryDao selfDao) {
        Map<String, String> categories = new HashMap<>();
        for (Entry item : selfDao.all()) {
            if (item.category != null) categories.put(item.term, normalize(item.category));
        }
        return categories;
    }

    public static int applyToFreqLibrary(EntryDao freqDao, Map<String, String> selfCategories) {
        if (selfCategories.isEmpty()) return 0;
        int changed = 0;
        for (Entry item : freqDao.all()) {
            String category = selfCategories.get(item.term);
            if (category != null && !category.equals(item.category)) {
                freqDao.syncCategory(item.id, category);
                changed++;
            }
        }
        return changed;
    }

    public static void applyToSelfLibrary(EntryDao selfDao, String term, String category) {
        if (category == null) return;
        Entry item = selfDao.findByTerm(term);
        if (item != null && !category.equals(item.category)) {
            selfDao.syncCategory(item.id, category);
        }
    }

    private static String normalize(String category) {
        return Entry.UNLEARNED.equals(category) ? Entry.UNKNOWN : category;
    }
}
