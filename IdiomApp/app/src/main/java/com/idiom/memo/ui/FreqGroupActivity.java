package com.idiom.memo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Locale;

public class FreqGroupActivity extends AppCompatActivity {
    private EntryDao dao;
    private EntryDao selfDao;
    private SettingsDao settings;
    private int frequency;
    private final List<Entry> items = new ArrayList<>();
    private ItemAdapter adapter;
    private TextView tvStats;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freq_group);

        Module module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id, Entry.SOURCE_FREQ);
        selfDao = new EntryDao(this, module.id, Entry.SOURCE_SELF);
        settings = new SettingsDao(this, module.id);
        frequency = getIntent().getIntExtra("frequency", 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(frequency + " 次组");
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStats = findViewById(R.id.tvFreqGroupStats);
        tvEmpty = findViewById(R.id.tvFreqGroupEmpty);

        RecyclerView recycler = findViewById(R.id.freqGroupRecycler);
        adapter = new ItemAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnGroupCard).setOnClickListener(v -> startStudy(false));
        findViewById(R.id.btnGroupQuiz).setOnClickListener(v -> startStudy(true));

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        CategorySync.applyToFreqLibrary(dao, CategorySync.selfCategories(selfDao));
        items.clear();
        items.addAll(dao.byFrequency(frequency));
        if (!isUnlocked()) {
            Toast.makeText(this, "该频次组已锁定，请先完成更高频次组",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        adapter.notifyDataSetChanged();

        int unknown = 0;
        int fuzzy = 0;
        int known = 0;
        for (Entry item : items) {
            if (Entry.KNOWN.equals(item.category)) known++;
            else if (Entry.FUZZY.equals(item.category)) fuzzy++;
            else unknown++;
        }
        tvStats.setText("共 " + items.size() + " 个 · 不会 " + unknown
                + " · 模糊 " + fuzzy + " · 会了 " + known);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isUnlocked() {
        FrequencyUnlock.Group target = null;
        for (FrequencyUnlock.Group group
                : FrequencyUnlock.plan(dao.all(), settings.getFreqThreshold())) {
            if (group.frequency == frequency) {
                target = group;
                break;
            }
        }
        return target != null && target.unlocked;
    }

    private void startStudy(boolean choice) {
        if (items.isEmpty()) {
            Toast.makeText(this, "该频次组暂无成语", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this,
                choice ? ChoiceQuizActivity.class : CardStudyActivity.class);
        intent.putExtra("source", Entry.SOURCE_FREQ);
        intent.putExtra("frequency", frequency);
        intent.putExtra("size", items.size());
        intent.putExtra("ratioU", 50);
        intent.putExtra("ratioF", 30);
        intent.putExtra("ratioK", 20);
        startActivity(intent);
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
