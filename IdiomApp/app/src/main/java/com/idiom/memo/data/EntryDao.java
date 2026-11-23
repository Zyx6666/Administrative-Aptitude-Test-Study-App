package com.idiom.memo.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class EntryDao {
    private final DbHelper helper;
    private final String moduleId;
    private final String sourceType;

    public EntryDao(Context context, String moduleId) {
        this(context, moduleId, Entry.SOURCE_SELF);
    }

    public EntryDao(Context context, String moduleId, String sourceType) {
        this.helper = new DbHelper(context);
        this.moduleId = moduleId;
        this.sourceType = sourceType == null ? Entry.SOURCE_SELF : sourceType;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getSourceType() {
        return sourceType;
    }

    private static Entry map(Cursor c) {
        Entry e = new Entry();
        e.id = c.getLong(c.getColumnIndexOrThrow("id"));
        e.moduleId = c.getString(c.getColumnIndexOrThrow("module_id"));
        e.sourceType = c.getString(c.getColumnIndexOrThrow("source_type"));
        e.term = c.getString(c.getColumnIndexOrThrow("term"));
        e.content = c.getString(c.getColumnIndexOrThrow("content"));
        e.frequency = c.getInt(c.getColumnIndexOrThrow("frequency"));
        e.category = c.getString(c.getColumnIndexOrThrow("category"));
        e.unknownCount = c.getInt(c.getColumnIndexOrThrow("unknown_count"));
        e.fuzzyCount = c.getInt(c.getColumnIndexOrThrow("fuzzy_count"));
        e.knownCount = c.getInt(c.getColumnIndexOrThrow("known_count"));
        e.choiceCorrectCount = c.getInt(c.getColumnIndexOrThrow("choice_correct_count"));
        e.choiceWrongCount = c.getInt(c.getColumnIndexOrThrow("choice_wrong_count"));
        e.lastChoiceResult = c.isNull(c.getColumnIndexOrThrow("last_choice_result")) ? null
                : c.getString(c.getColumnIndexOrThrow("last_choice_result"));
        e.lastChoiceAt = c.isNull(c.getColumnIndexOrThrow("last_choice_at")) ? null
                : c.getLong(c.getColumnIndexOrThrow("last_choice_at"));
        e.knownWithoutAnswer = c.getInt(c.getColumnIndexOrThrow("known_without_answer")) == 1;
        e.lastReviewAt = c.isNull(c.getColumnIndexOrThrow("last_review_at")) ? null
                : c.getLong(c.getColumnIndexOrThrow("last_review_at"));
        e.nextReviewAt = c.isNull(c.getColumnIndexOrThrow("next_review_at")) ? null
                : c.getLong(c.getColumnIndexOrThrow("next_review_at"));
        e.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        e.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return e;
    }

    public long insert(Entry e) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("module_id", moduleId);
        v.put("source_type", sourceType);
        v.put("term", e.term);
        v.put("content", e.content);
        v.put("frequency", e.frequency);
        v.put("category", e.category);
        v.put("unknown_count", e.unknownCount);
        v.put("fuzzy_count", e.fuzzyCount);
        v.put("known_count", e.knownCount);
        v.put("choice_correct_count", e.choiceCorrectCount);
        v.put("choice_wrong_count", e.choiceWrongCount);
        v.put("last_choice_result", e.lastChoiceResult);
        v.put("last_choice_at", e.lastChoiceAt);
        v.put("known_without_answer", e.knownWithoutAnswer ? 1 : 0);
        v.put("last_review_at", e.lastReviewAt);
        v.put("next_review_at", e.nextReviewAt);
        v.put("created_at", e.createdAt);
        v.put("updated_at", e.updatedAt);
        return db.insertOrThrow("entries", null, v);
    }

    public Entry findById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query("entries", null, "id=? AND module_id=? AND source_type=?",
                new String[]{String.valueOf(id), moduleId, sourceType}, null, null, null)) {
            return c.moveToFirst() ? map(c) : null;
        }
    }

    public Entry findByTerm(String term) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query("entries", null, "term=? AND module_id=? AND source_type=?",
                new String[]{term, moduleId, sourceType}, null, null, null)) {
            return c.moveToFirst() ? map(c) : null;
        }
    }

    public List<Entry> all() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Entry> out = new ArrayList<>();
        try (Cursor c = db.query("entries", null, "module_id=? AND source_type=?",
                new String[]{moduleId, sourceType}, null, null, "updated_at DESC")) {
            while (c.moveToNext()) out.add(map(c));
        }
        return out;
    }

    public List<Entry> allFiltered(String keyword, boolean sortByCount) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String order = sortByCount
                ? "(unknown_count + fuzzy_count + known_count) DESC, updated_at DESC"
                : "updated_at DESC";
        String kw = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        List<Entry> out = new ArrayList<>();
        try (Cursor c = db.query("entries", null,
                "module_id=? AND source_type=? AND (term LIKE ? OR content LIKE ?)",
                new String[]{moduleId, sourceType, kw, kw}, null, null, order)) {
            while (c.moveToNext()) out.add(map(c));
        }
        return out;
    }

    public List<Entry> byCategory(String category, String keyword, boolean sortByCount) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String countCol = Entry.FUZZY.equals(category) ? "fuzzy_count"
                : Entry.KNOWN.equals(category) ? "known_count" : "unknown_count";
        String order = sortByCount ? countCol + " DESC, updated_at DESC" : "updated_at DESC";
        String kw = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        List<Entry> out = new ArrayList<>();
        try (Cursor c = db.query("entries", null,
                "module_id=? AND source_type=? AND category=? AND (term LIKE ? OR content LIKE ?)",
                new String[]{moduleId, sourceType, category, kw, kw}, null, null, order)) {
            while (c.moveToNext()) out.add(map(c));
        }
        return out;
    }

    public List<Entry> byFrequency(int frequency) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Entry> out = new ArrayList<>();
        try (Cursor c = db.query("entries", null,
                "module_id=? AND source_type=? AND frequency=?",
                new String[]{moduleId, sourceType, String.valueOf(frequency)},
                null, null, "updated_at DESC")) {
            while (c.moveToNext()) out.add(map(c));
        }
        return out;
    }

    public int countByCategory(String category) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM entries WHERE module_id=? AND source_type=? AND category=?",
                new String[]{moduleId, sourceType, category})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int countAll() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM entries WHERE module_id=? AND source_type=?",
                new String[]{moduleId, sourceType})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int countDue(long now) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM entries WHERE module_id=? AND source_type=? " +
                        "AND (next_review_at IS NULL OR next_review_at <= ?)",
                new String[]{moduleId, sourceType, String.valueOf(now)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void updateTerm(long id, String term, long now) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("term", term);
        v.put("updated_at", now);
        db.update("entries", v, "id=? AND module_id=? AND source_type=?",
                new String[]{String.valueOf(id), moduleId, sourceType});
    }

    public void updateContent(long id, String content, long now) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("content", content);
        v.put("updated_at", now);
        db.update("entries", v, "id=? AND module_id=? AND source_type=?",
                new String[]{String.valueOf(id), moduleId, sourceType});
    }

    public void updateContentAndFrequency(long id, String content, int frequency, long now) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("content", content);
        v.put("frequency", frequency);
        v.put("updated_at", now);
        db.update("entries", v, "id=? AND module_id=? AND source_type=?",
                new String[]{String.valueOf(id), moduleId, sourceType});
    }

    public void syncCategory(long id, String category) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("category", category);
        db.update("entries", v, "id=? AND module_id=? AND source_type=?",
                new String[]{String.valueOf(id), moduleId, sourceType});
    }

    public void applyFeedback(long id, String category, long nextAt,
                              boolean knownWithoutAnswer, long now) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String col = Entry.FUZZY.equals(category) ? "fuzzy_count"
                : Entry.KNOWN.equals(category) ? "known_count" : "unknown_count";
        db.execSQL("UPDATE entries SET category=?, " + col + "=" + col + "+1, " +
                        "known_without_answer=?, last_review_at=?, next_review_at=?, updated_at=? " +
                        "WHERE id=? AND module_id=? AND source_type=?",
                new Object[]{category, knownWithoutAnswer ? 1 : 0, now, nextAt, now,
                        id, moduleId, sourceType});
    }

    public void applyChoiceResult(long id, boolean correct, String newCategory,
                                  long nextAt, long now) {
        SQLiteDatabase db = helper.getWritableDatabase();
        StringBuilder sql = new StringBuilder("UPDATE entries SET ");
        List<Object> args = new ArrayList<>();
        sql.append("choice_correct_count=choice_correct_count+?, ");
        args.add(correct ? 1 : 0);
        sql.append("choice_wrong_count=choice_wrong_count+?, ");
        args.add(correct ? 0 : 1);
        sql.append("last_choice_result=?, last_choice_at=?, last_review_at=?, next_review_at=?, updated_at=?");
        args.add(correct ? "correct" : "wrong");
        args.add(now);
        args.add(now);
        args.add(nextAt);
        args.add(now);
        if (newCategory != null) {
            String col = Entry.FUZZY.equals(newCategory) ? "fuzzy_count"
                    : Entry.KNOWN.equals(newCategory) ? "known_count" : "unknown_count";
            sql.append(", category=?, ").append(col).append("=").append(col)
                    .append("+1, known_without_answer=0");
            args.add(newCategory);
        }
        sql.append(" WHERE id=? AND module_id=? AND source_type=?");
        args.add(id);
        args.add(moduleId);
        args.add(sourceType);
        db.execSQL(sql.toString(), args.toArray());
    }

    public void moveCategory(long id, String category, long now) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("category", category);
        v.put("updated_at", now);
        db.update("entries", v, "id=? AND module_id=? AND source_type=?",
                new String[]{String.valueOf(id), moduleId, sourceType});
    }

    public void delete(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("entries", "id=? AND module_id=? AND source_type=?",
                new String[]{String.valueOf(id), moduleId, sourceType});
    }

    public void deleteAll() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("entries", "module_id=? AND source_type=?",
                new String[]{moduleId, sourceType});
    }
}
