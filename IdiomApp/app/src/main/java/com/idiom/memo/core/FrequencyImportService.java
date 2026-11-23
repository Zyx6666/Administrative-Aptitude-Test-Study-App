package com.idiom.memo.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考频成语库导入计划：新词新增，旧词只更新解释与频次，保留学习状态。
 */
public final class FrequencyImportService {
    public static class Existing {
        public final long id;
        public final String content;
        public final int frequency;

        public Existing(long id, String content, int frequency) {
            this.id = id;
            this.content = content;
            this.frequency = frequency;
        }
    }

    public static class Update {
        public final long id;
        public final String term;
        public final String content;
        public final int frequency;

        public Update(long id, String term, String content, int frequency) {
            this.id = id;
            this.term = term;
            this.content = content;
            this.frequency = frequency;
        }
    }

    public static class Plan {
        public final List<FrequencyMarkdownParser.Row> fresh = new ArrayList<>();
        public final List<Update> updates = new ArrayList<>();
        public int unchanged;
        public int invalid;
    }

    private FrequencyImportService() {
    }

    public static Plan plan(List<FrequencyMarkdownParser.Row> rows,
                            Map<String, Existing> existing, int invalid) {
        Plan plan = new Plan();
        plan.invalid = invalid;
        Map<String, Existing> working = new HashMap<>(existing);
        for (FrequencyMarkdownParser.Row row : rows) {
            Existing old = working.get(row.term);
            if (old == null) {
                working.put(row.term, new Existing(0, row.content, row.frequency));
                plan.fresh.add(row);
            } else if (same(old.content, row.content) && old.frequency == row.frequency) {
                plan.unchanged++;
            } else {
                plan.updates.add(new Update(old.id, row.term, row.content, row.frequency));
                working.put(row.term, new Existing(old.id, row.content, row.frequency));
            }
        }
        return plan;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.trim().equals(b == null ? null : b.trim());
    }
}
