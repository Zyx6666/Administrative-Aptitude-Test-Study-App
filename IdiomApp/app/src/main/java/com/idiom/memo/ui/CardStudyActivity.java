package com.idiom.memo.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.idiom.memo.R;
import com.idiom.memo.core.CategorySync;
import com.idiom.memo.core.DrawPlanner;
import com.idiom.memo.core.MarkdownRenderer;
import com.idiom.memo.core.ReviewScheduler;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;
import com.idiom.memo.data.SettingsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardStudyActivity extends AppCompatActivity {
    private EntryDao dao;
    private EntryDao selfDao;
    private SettingsDao settings;
    private Module module;
    private boolean freqMode;
    private final List<Entry> queue = new ArrayList<>();
    private Entry current;
    private int total;

    private TextView tvProgress;
    private TextView tvIdiom;
    private TextView tvMeaning;
    private View hiddenBox;
    private View meaningScroll;
    private View idleButtons;
    private View feedbackButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_study);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        module = ModuleManager.current(this);
        freqMode = Entry.SOURCE_FREQ.equals(getIntent().getStringExtra("source"));
        int frequency = getIntent().getIntExtra("frequency", 0);
        dao = new EntryDao(this, module.id,
                freqMode ? Entry.SOURCE_FREQ : Entry.SOURCE_SELF);
        selfDao = new EntryDao(this, module.id, Entry.SOURCE_SELF);
        settings = new SettingsDao(this, module.id);
        if (freqMode) toolbar.setTitle(frequency + " 次组 · 内容卡片");

        tvProgress = findViewById(R.id.tvProgress);
        tvIdiom = findViewById(R.id.tvIdiom);
        tvMeaning = findViewById(R.id.tvMeaning);
        hiddenBox = findViewById(R.id.hiddenBox);
        meaningScroll = findViewById(R.id.meaningScroll);
        idleButtons = findViewById(R.id.idleButtons);
        feedbackButtons = findViewById(R.id.feedbackButtons);

        String category = getIntent().getStringExtra("category");
        int size = getIntent().getIntExtra("size", 20);
        int ru = getIntent().getIntExtra("ratioU", 50);
        int rf = getIntent().getIntExtra("ratioF", 30);
        int rk = getIntent().getIntExtra("ratioK", 20);

        List<Entry> pool;
        if (freqMode) {
            pool = frequency > 0 ? dao.byFrequency(frequency) : dao.all();
        } else {
            pool = category == null ? dao.all() : dao.byCategory(category, "", false);
        }
        queue.addAll(DrawPlanner.plan(pool, size, ru, rf, rk, new Random()));
        total = queue.size();

        if (queue.isEmpty()) {
            Toast.makeText(this, "没有可复习的" + module.termLabel, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnShowMeaning).setOnClickListener(v -> reveal());
        findViewById(R.id.btnKnowWithout).setOnClickListener(v -> feedback(Entry.KNOWN, true));
        findViewById(R.id.btnSkip).setOnClickListener(v -> skip());
        findViewById(R.id.btnUnknown).setOnClickListener(v -> feedback(Entry.UNKNOWN, false));
        findViewById(R.id.btnFuzzy).setOnClickListener(v -> feedback(Entry.FUZZY, false));
        findViewById(R.id.btnKnown).setOnClickListener(v -> feedback(Entry.KNOWN, false));

        showNext();
    }

    private void showNext() {
        if (queue.isEmpty()) {
            Toast.makeText(this, "本轮完成，共 " + total + " 张", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        current = queue.remove(0);
        tvProgress.setText("进度：" + (total - queue.size()) + " / " + total);
        MarkdownRenderer.set(tvIdiom, current.term);
        tvMeaning.setText("");
        meaningScroll.setVisibility(View.GONE);
        hiddenBox.setVisibility(View.VISIBLE);
        idleButtons.setVisibility(View.VISIBLE);
        feedbackButtons.setVisibility(View.GONE);
    }

    private void reveal() {
        hiddenBox.setVisibility(View.GONE);
        meaningScroll.setVisibility(View.VISIBLE);
        MarkdownRenderer.set(tvMeaning, current.content);
        idleButtons.setVisibility(View.GONE);
        feedbackButtons.setVisibility(View.VISIBLE);
    }

    private void skip() {
        queue.add(current);
        showNext();
    }

    private void feedback(String category, boolean withoutAnswer) {
        if (current == null) return;
        long now = System.currentTimeMillis();
        long next = ReviewScheduler.next(now, category, settings.getIntervals());
        dao.applyFeedback(current.id, category, next, withoutAnswer, now);
        if (freqMode) CategorySync.applyToSelfLibrary(selfDao, current.term, category);
        showNext();
    }
}
