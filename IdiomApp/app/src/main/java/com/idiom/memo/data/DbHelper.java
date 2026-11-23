package com.idiom.memo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "idiom.db";
    public static final int DB_VERSION = 3;

    private static final String CREATE_ENTRIES =
            "CREATE TABLE entries(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "module_id TEXT NOT NULL," +
                    "source_type TEXT NOT NULL DEFAULT 'self'," +
                    "term TEXT NOT NULL," +
                    "content TEXT NOT NULL," +
                    "frequency INTEGER NOT NULL DEFAULT 0," +
                    "category TEXT NOT NULL," +
                    "unknown_count INTEGER NOT NULL DEFAULT 0," +
                    "fuzzy_count INTEGER NOT NULL DEFAULT 0," +
                    "known_count INTEGER NOT NULL DEFAULT 0," +
                    "choice_correct_count INTEGER NOT NULL DEFAULT 0," +
                    "choice_wrong_count INTEGER NOT NULL DEFAULT 0," +
                    "last_choice_result TEXT," +
                    "last_choice_at INTEGER," +
                    "known_without_answer INTEGER NOT NULL DEFAULT 0," +
                    "last_review_at INTEGER," +
                    "next_review_at INTEGER," +
                    "created_at INTEGER NOT NULL," +
                    "updated_at INTEGER NOT NULL)";

    public DbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ENTRIES);
        createIndexes(db);
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    }

    private static void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE UNIQUE INDEX idx_module_source_term " +
                "ON entries(module_id, source_type, term)");
        db.execSQL("CREATE INDEX idx_category ON entries(module_id, source_type, category)");
        db.execSQL("CREATE INDEX idx_next_review ON entries(module_id, source_type, next_review_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_ENTRIES);
            db.execSQL("INSERT INTO entries(id, module_id, source_type, term, content, frequency, " +
                    "category, unknown_count, fuzzy_count, known_count, " +
                    "choice_correct_count, choice_wrong_count, last_choice_result, " +
                    "last_choice_at, known_without_answer, last_review_at, next_review_at, " +
                    "created_at, updated_at) " +
                    "SELECT id, 'verbal', 'self', idiom, meaning, 0, category, " +
                    "unknown_count, fuzzy_count, known_count, choice_correct_count, " +
                    "choice_wrong_count, last_choice_result, last_choice_at, " +
                    "known_without_answer, last_review_at, next_review_at, " +
                    "created_at, updated_at FROM idioms");
            db.execSQL("DROP TABLE idioms");
            createIndexes(db);
            db.execSQL("UPDATE settings SET key = 'verbal:' || key");
        } else if (oldVersion < 3) {
            db.execSQL("ALTER TABLE entries ADD COLUMN source_type TEXT NOT NULL DEFAULT 'self'");
            db.execSQL("ALTER TABLE entries ADD COLUMN frequency INTEGER NOT NULL DEFAULT 0");
            db.execSQL("DROP INDEX IF EXISTS idx_module_term");
            db.execSQL("DROP INDEX IF EXISTS idx_category");
            db.execSQL("DROP INDEX IF EXISTS idx_next_review");
            createIndexes(db);
        }
    }
}
