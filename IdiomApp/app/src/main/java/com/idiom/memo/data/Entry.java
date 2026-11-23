package com.idiom.memo.data;

public class Entry {
    public static final String UNLEARNED = "unlearned";
    public static final String UNKNOWN = "unknown";
    public static final String FUZZY = "fuzzy";
    public static final String KNOWN = "known";

    public static final String SOURCE_SELF = "self";
    public static final String SOURCE_FREQ = "freq";

    public long id;
    public String moduleId;
    public String sourceType = SOURCE_SELF;
    public String term;
    public String content;
    public int frequency;
    public String category = UNKNOWN;
    public int unknownCount;
    public int fuzzyCount;
    public int knownCount;
    public int choiceCorrectCount;
    public int choiceWrongCount;
    public String lastChoiceResult;
    public Long lastChoiceAt;
    public boolean knownWithoutAnswer;
    public Long lastReviewAt;
    public Long nextReviewAt;
    public long createdAt;
    public long updatedAt;

    public static String label(String category) {
        if (UNLEARNED.equals(category)) return "未学";
        if (FUZZY.equals(category)) return "模糊";
        if (KNOWN.equals(category)) return "会了";
        return "不会";
    }

    public int countOf(String category) {
        if (FUZZY.equals(category)) return fuzzyCount;
        if (KNOWN.equals(category)) return knownCount;
        return unknownCount;
    }
}
