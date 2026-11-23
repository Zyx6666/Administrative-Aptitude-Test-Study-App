package com.idiom.memo.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.idiom.memo.R;
import com.idiom.memo.core.CategorySync;
import com.idiom.memo.core.FrequencyUnlock;
import com.idiom.memo.core.MarkdownRenderer;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;
import com.idiom.memo.data.SettingsDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FreqListActivity extends AppCompatActivity {
    private EntryDao dao;
    private EntryDao selfDao;
    private SettingsDao settings;
    private String category;
    private String keyword = "";
    private final List<Entry> items = new ArrayList<>();
    private ItemAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freq_list);

        Module module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id, Entry.SOURCE_FREQ);
        selfDao = new EntryDao(this, module.id, Entry.SOURCE_SELF);
        settings = new SettingsDao(this, module.id);
        category = getIntent().getStringExtra("category");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(category == null ? "全部成语" : Entry.label(category));
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvFreqListEmpty);
        RecyclerView recycler = findViewById(R.id.freqListRecycler);
        adapter = new ItemAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etFreqSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                keyword = s.toString();
                load();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        CategorySync.applyToFreqLibrary(dao, CategorySync.selfCategories(selfDao));
        List<Entry> all = dao.all();
        Set<Integer> unlocked = new HashSet<>();
        for (FrequencyUnlock.Group group
                : FrequencyUnlock.plan(all, settings.getFreqThreshold())) {
            if (group.unlocked) unlocked.add(group.frequency);
        }

        String kw = keyword == null ? "" : keyword.trim();
        items.clear();
        for (Entry entry : all) {
            if (!unlocked.contains(entry.frequency)) continue;
            if (category != null && !category.equals(entry.category)) continue;
            if (!kw.isEmpty() && !entry.term.contains(kw) && !entry.content.contains(kw)) {
                continue;
            }
            items.add(entry);
        }
        items.sort((a, b) -> {
            if (a.frequency != b.frequency) return Integer.compare(b.frequency, a.frequency);
            return Long.compare(b.updatedAt, a.updatedAt);
        });

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDetail(Entry item) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(4));

        TextView content = new TextView(this);
        content.setTextColor(getColor(R.color.text_primary));
        content.setTextSize(15f);
        content.setLineSpacing(dp(5), 1f);
        box.addView(content);

        TextView meta = new TextView(this);
        meta.setTextColor(getColor(R.color.text_secondary));
        meta.setTextSize(12f);
        meta.setPadding(0, dp(12), 0, 0);
        meta.setText("频次：" + item.frequency + " 次"
                + "\n当前分类：" + Entry.label(item.category)
                + "\n不会 " + item.unknownCount + " · 模糊 " + item.fuzzyCount
                + " · 会了 " + item.knownCount
                + "\n选择题：对 " + item.choiceCorrectCount + " / 错 " + item.choiceWrongCount
                + "\n下次复习：" + formatTime(item.nextReviewAt));
        box.addView(meta);

        new AlertDialog.Builder(this)
                .setTitle(item.term)
                .setView(box)
                .setPositiveButton("关闭", null)
                .show();
        MarkdownRenderer.set(content, item.content);
    }

    private static String formatTime(Long time) {
        if (time == null) return "未安排";
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(time));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_idiom, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Entry item = items.get(position);
            MarkdownRenderer.set(holder.term, item.term);
            MarkdownRenderer.set(holder.content, item.content);
            holder.meta.setText("当前：" + Entry.label(item.category)
                    + " · 不会 " + item.unknownCount
                    + " · 模糊 " + item.fuzzyCount
                    + " · 会了 " + item.knownCount
                    + "\n频次 " + item.frequency + " 次 · 下次复习 "
                    + formatTime(item.nextReviewAt));
            View.OnClickListener click = v -> showDetail(item);
            holder.itemView.setOnClickListener(click);
            holder.explain.setOnClickListener(click);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView term;
            final TextView content;
            final TextView meta;
            final TextView explain;

            Holder(@NonNull View itemView) {
                super(itemView);
                term = itemView.findViewById(R.id.tvItemIdiom);
                content = itemView.findViewById(R.id.tvItemMeaning);
                meta = itemView.findViewById(R.id.tvItemMeta);
                explain = itemView.findViewById(R.id.btnItemExplain);
            }
        }
    }
}
