package com.idiom.memo.ui;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;

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
public class ScreenSmokeTest {
    private static void seed(Context context, String term, String content) {
        seed(context, term, content, Entry.UNKNOWN);
    }

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
    public void allScreensInflate() {
        Context context = RuntimeEnvironment.getApplication();
        seed(context, "差强人意", "大体上还能使人满意");
        seed(context, "不刊之论", "不能改动或不可磨灭的言论");

        Robolectric.buildActivity(UploadActivity.class).setup().get();
        Robolectric.buildActivity(FreqLibraryActivity.class).setup().get();

        Intent group = new Intent(context, FreqGroupActivity.class);
        group.putExtra("frequency", 20);
        Robolectric.buildActivity(FreqGroupActivity.class, group).setup().get();

        Intent freqList = new Intent(context, FreqListActivity.class);
        freqList.putExtra("category", Entry.UNKNOWN);
        Robolectric.buildActivity(FreqListActivity.class, freqList).setup().get();

        Intent config = new Intent(context, ConfigActivity.class);
        config.putExtra("mode", "card");
        Robolectric.buildActivity(ConfigActivity.class, config).setup().get();

        Intent list = new Intent(context, ListActivity.class);
        list.putExtra("tab", 0);
        ListActivity listActivity = Robolectric.buildActivity(ListActivity.class, list).setup().get();
        RecyclerView recycler = listActivity.findViewById(R.id.recycler);
        assertEquals(2, recycler.getAdapter().getItemCount());

        Robolectric.buildActivity(SettingsActivity.class).setup().get();
    }

    @Test
    public void allModeListsEveryCategory() {
        Context context = RuntimeEnvironment.getApplication();
        seed(context, "差强人意", "大体上还能使人满意", Entry.UNKNOWN);
        seed(context, "万人空巷", "形容庆祝、欢迎的盛况", Entry.FUZZY);
        seed(context, "首鼠两端", "迟疑不决或动摇不定", Entry.KNOWN);
        seed(context, "画蛇添足", "多此一举", Entry.UNLEARNED);

        Intent all = new Intent(context, ListActivity.class);
        all.putExtra("all", true);
        ListActivity activity = Robolectric.buildActivity(ListActivity.class, all).setup().get();
        RecyclerView recycler = activity.findViewById(R.id.recycler);
        assertEquals(4, recycler.getAdapter().getItemCount());
    }
}
