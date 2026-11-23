package com.idiom.memo.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;
import com.idiom.memo.R;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ChoiceQuizActivityTest {
    private static void seed(Context context, String term, String content, String category) {
        long now = System.currentTimeMillis();
        Entry item = new Entry();
        item.term = term;
        item.content = content;
        item.category = category;
        item.createdAt = now;
        item.updatedAt = now;
        new EntryDao(context, Module.VERBAL.id).insert(item);
    }

    @Test
    public void knownWrongAnswerAsksRecategoryAndUpdatesDb() {
        Context context = RuntimeEnvironment.getApplication();
        seed(context, "差强人意", "大体上还能使人满意", Entry.KNOWN);
        seed(context, "不刊之论", "不能改动或不可磨灭的言论", Entry.KNOWN);
        seed(context, "空穴来风", "消息并非完全没有根据", Entry.KNOWN);
        seed(context, "首鼠两端", "迟疑不决或动摇不定", Entry.KNOWN);

        Intent intent = new Intent(context, ChoiceQuizActivity.class);
        intent.putExtra("size", 1);
        intent.putExtra("ratioU", 0);
        intent.putExtra("ratioF", 0);
        intent.putExtra("ratioK", 100);

        ChoiceQuizActivity activity =
                Robolectric.buildActivity(ChoiceQuizActivity.class, intent).setup().get();

        String term = ((android.widget.TextView) activity.findViewById(R.id.tvIdiom))
                .getText().toString().replace("知识点：", "");
        EntryDao dao = new EntryDao(context, Module.VERBAL.id);
        Entry target = dao.findByTerm(term);
        LinearLayout options = activity.findViewById(R.id.optionGroup);
        assertEquals(4, options.getChildCount());

        int correct = -1;
        for (int i = 0; i < options.getChildCount(); i++) {
            String text = ((MaterialButton) options.getChildAt(i)).getText().toString();
            if (text.endsWith(target.content)) correct = i;
        }
        assertTrue(correct >= 0);

        int wrong = (correct + 1) % 4;
        options.getChildAt(wrong).performClick();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.tvExplain).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.groupRecategory).getVisibility());

        activity.findViewById(R.id.btnRecatUnknown).performClick();

        Entry loaded = dao.findById(target.id);
        assertEquals(Entry.UNKNOWN, loaded.category);
        assertEquals(1, loaded.choiceWrongCount);
        assertEquals(1, loaded.unknownCount);
    }
}
