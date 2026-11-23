package com.idiom.memo.core;

import com.idiom.memo.data.Entry;

public class ChoiceRules {
    /** 只有原分类为“会了”且答错时，才需要用户重新选择分类。 */
    public static boolean askRecategory(String category, boolean correct) {
        return !correct && Entry.KNOWN.equals(category);
    }
}
