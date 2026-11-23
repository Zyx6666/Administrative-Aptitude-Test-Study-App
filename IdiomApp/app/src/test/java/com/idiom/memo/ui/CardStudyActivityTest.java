package com.idiom.memo.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.idiom.memo.R;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CardStudyActivityTest {
    private static long seed(Context context, String term, String content) {
        long now = System.currentTimeMillis();
        Entry item = new Entry();
        item.term = term;
        item.content = content;
        item.category = Entry.UNKNOWN;
        item.createdAt = now;
        item.updatedAt = now;
        return new EntryDao(context, Module.VERBAL.id).insert(item);
    }

    @Test
    public void meaningHiddenUntilRevealedThenFeedbackUpdatesDb() {
        Context context = RuntimeEnvironment.getApplication();
        EntryDao dao = new EntryDao(context, Module.VERBAL.id);
        long id = seed(context, "画蛇添足", "多此一举");

        Intent intent = new Intent(context, CardStudyActivity.class);
        intent.putExtra("size", 1);
        intent.putExtra("ratioU", 100);
        intent.putExtra("ratioF", 0);
        intent.putExtra("ratioK", 0);

        ActivityController<CardStudyActivity> controller =
                Robolectric.buildActivity(CardStudyActivity.class, intent).setup();
        CardStudyActivity activity = controller.get();

        View hiddenBox = activity.findViewById(R.id.hiddenBox);
        View meaningScroll = activity.findViewById(R.id.meaningScroll);
        assertEquals(View.VISIBLE, hiddenBox.getVisibility());
        assertEquals(View.GONE, meaningScroll.getVisibility());

        activity.findViewById(R.id.btnShowMeaning).performClick();
        assertEquals(View.GONE, hiddenBox.getVisibility());
        assertEquals(View.VISIBLE, meaningScroll.getVisibility());
        assertEquals("多此一举",
                ((TextView) activity.findViewById(R.id.tvMeaning)).getText().toString());

        activity.findViewById(R.id.btnKnown).performClick();

        Entry loaded = dao.findById(id);
        assertEquals(Entry.KNOWN, loaded.category);
        assertEquals(1, loaded.knownCount);
        assertNotNull(loaded.nextReviewAt);
    }
}
