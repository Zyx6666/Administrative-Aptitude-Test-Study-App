package com.idiom.memo.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportService {
    public static class Row {
        public final String term;
        public final String content;

        public Row(String term, String content) {
            this.term = term;
            this.content = content;
        }
    }

    public static class Existing {
        public final long id;
        public final String content;

        public Existing(long id, String content) {
            this.id = id;
            this.content = content;
        }
    }

    public static class Conflict {
        public final long id;
        public final String term;
        public final String oldContent;
        public final String newContent;

        public Conflict(long id, String term, String oldContent, String newContent) {
            this.id = id;
            this.term = term;
            this.oldContent = oldContent;
            this.newContent = newContent;
        }
    }

    public static class Plan {
        public final List<Row> fresh = new ArrayList<>();
        public int sameCount;
        public final List<Conflict> conflicts = new ArrayList<>();
        public int invalid;
    }

    public static Plan plan(List<Row> rows, Map<String, Existing> existing) {
        Plan p = new Plan();
        Map<String, Existing> working = new HashMap<>(existing);
        for (Row row : rows) {
            String term = row.term == null ? "" : row.term.trim();
            String content = row.content == null ? "" : row.content.trim();
            if (term.isEmpty() || content.isEmpty()) {
                p.invalid++;
                continue;
            }
            Existing old = working.get(term);
            if (old == null) {
                working.put(term, new Existing(0, content));
                p.fresh.add(new Row(term, content));
            } else if (old.content != null && old.content.trim().equals(content)) {
                p.sameCount++;
            } else {
                p.conflicts.add(new Conflict(old.id, term, old.content, content));
            }
        }
        return p;
    }
}
