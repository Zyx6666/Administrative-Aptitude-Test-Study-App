package com.idiom.memo.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.idiom.memo.R;
import com.idiom.memo.core.CategorySync;
import com.idiom.memo.core.ChoiceRules;
import com.idiom.memo.core.DrawPlanner;
import com.idiom.memo.core.MarkdownRenderer;
import com.idiom.memo.core.QuizBuilder;
import com.idiom.memo.core.ReviewScheduler;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;
import com.idiom.memo.data.SettingsDao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class ChoiceQuizActivity extends AppCompatActivity {
    private EntryDao dao;
    private EntryDao selfDao;
    private SettingsDao settings;
    private Module module;
    private boolean freqMode;
    private final Random random = new Random();
    private final List<Entry> queue = new ArrayList<>();
    private final List<MaterialButton> optionButtons = new ArrayList<>();
    private List<Entry> pool = new ArrayList<>();
    private List<Entry> optionPool = new ArrayList<>();
    private Entry current;
    private QuizBuilder.Quiz quiz;
    private boolean answered;
    private boolean waitingRecategory;
    private int total;

    private TextView tvProgress;
    private TextView tvIdiom;
    private TextView tvResult;
    private TextView tvExplain;
    private LinearLayout optionGroup;
    private View explainCard;
    private View groupRecategory;
    private View btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        module = ModuleManager.current(this);
        freqMode = Entry.SOURCE_FREQ.equals(getIntent().getStringExtra("source"));
        int frequency = getIntent().getIntExtra("frequency", 0);
        dao = new EntryDao(this, module.id,
                freqMode ? Entry.SOURCE_FREQ : Entry.SOURCE_SELF);
        selfDao = new EntryDao(this, module.id, Entry.SOURCE_SELF);
        settings = new SettingsDao(this, module.id);
        if (freqMode) toolbar.setTitle(frequency + " 次组 · 选择题");

        tvProgress = findViewById(R.id.tvProgress);
        tvIdiom = findViewById(R.id.tvIdiom);
        tvResult = findViewById(R.id.tvResult);
        tvExplain = findViewById(R.id.tvExplain);
        optionGroup = findViewById(R.id.optionGroup);
        explainCard = findViewById(R.id.explainCard);
        groupRecategory = findViewById(R.id.groupRecategory);
        btnNext = findViewById(R.id.btnNext);

        String category = getIntent().getStringExtra("category");
        int size = getIntent().getIntExtra("size", 20);
        int ru = getIntent().getIntExtra("ratioU", 50);
        int rf = getIntent().getIntExtra("ratioF", 30);
        int rk = getIntent().getIntExtra("ratioK", 20);

        if (freqMode) {
            pool = frequency > 0 ? dao.byFrequency(frequency) : dao.all();
            optionPool = dao.all();
        } else {
            pool = category == null ? dao.all() : dao.byCategory(category, "", false);
            optionPool = pool;
        }

        if (!canBuildQuiz(optionPool)) {
            new AlertDialog.Builder(this)
                    .setTitle("无法开始")
                    .setMessage("知识条目太少，无法生成选择题。\n至少需要 4 条，且内容互不相同。")
                    .setCancelable(false)
                    .setPositiveButton("知道了", (d, w) -> finish())
                    .show();
            return;
        }

        queue.addAll(DrawPlanner.plan(pool, size, ru, rf, rk, random));
        total = queue.size();

        findViewById(R.id.btnRecatUnknown).setOnClickListener(v -> recategorize(Entry.UNKNOWN));
        findViewById(R.id.btnRecatFuzzy).setOnClickListener(v -> recategorize(Entry.FUZZY));
        findViewById(R.id.btnRecatKnown).setOnClickListener(v -> recategorize(Entry.KNOWN));
        btnNext.setOnClickListener(v -> showNext());

        showNext();
    }

    private static boolean canBuildQuiz(List<Entry> pool) {
        if (pool.size() < 4) return false;
        HashSet<String> contents = new HashSet<>();
        for (Entry i : pool) {
            String m = i.content == null ? "" : i.content.trim();
            if (!m.isEmpty()) contents.add(m);
        }
        return contents.size() >= 4;
    }

    private void showNext() {
        if (queue.isEmpty()) {
            Toast.makeText(this, "选择题完成", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        current = queue.remove(0);
        quiz = QuizBuilder.build(current, optionPool, random);
        answered = false;
        waitingRecategory = false;

        tvProgress.setText("进度：" + (total - queue.size()) + " / " + total);
        MarkdownRenderer.set(tvIdiom, module.termLabel + "：" + current.term);
        tvResult.setVisibility(View.GONE);
        explainCard.setVisibility(View.GONE);
        groupRecategory.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        optionGroup.removeAllViews();
        optionButtons.clear();
        for (int i = 0; i < 4; i++) {
            MaterialButton button = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setAllCaps(false);
            button.setTextSize(16f);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            button.setStrokeColor(ColorStateList.valueOf(getColor(R.color.card_stroke)));
            button.setStrokeWidth(dp(1));
            button.setCornerRadius(dp(14));
            button.setInsetTop(0);
            button.setInsetBottom(0);
            button.setTextColor(getColor(R.color.text_primary));
            button.setPadding(dp(16), dp(14), dp(16), dp(14));
            button.setMinHeight(dp(62));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            int index = i;
            button.setOnClickListener(v -> answer(index));
            optionGroup.addView(button, lp);
            MarkdownRenderer.set(button, ("ABCD".charAt(i)) + ". " + quiz.options.get(i));
            optionButtons.add(button);
        }
    }

    private void answer(int selected) {
        if (answered || waitingRecategory) return;
        answered = true;
        boolean correct = selected == quiz.correctIndex;

        for (MaterialButton b : optionButtons) {
            b.setEnabled(false);
            b.setAlpha(0.55f);
        }
        MaterialButton correctButton = optionButtons.get(quiz.correctIndex);
        correctButton.setAlpha(1f);
        correctButton.setStrokeWidth(dp(2));
        correctButton.setStrokeColor(ColorStateList.valueOf(getColor(R.color.brand)));
        correctButton.setCompoundDrawablePadding(dp(8));
        correctButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, R.drawable.ic_check, 0);

        if (!correct) {
            MaterialButton wrongButton = optionButtons.get(selected);
            wrongButton.setAlpha(1f);
            wrongButton.setStrokeWidth(dp(2));
            wrongButton.setStrokeColor(ColorStateList.valueOf(getColor(R.color.wrong)));
            wrongButton.setTextColor(getColor(R.color.wrong));
        }

        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText(correct ? "回答正确" : "回答错误");
        tvResult.setTextColor(getColor(correct ? R.color.correct : R.color.wrong));

        String explanation;
        if (correct) {
            explanation = "正确。内容：\n" + current.content;
        } else {
            explanation = "正确答案：" + ("ABCD".charAt(quiz.correctIndex)) + ". "
                    + quiz.options.get(quiz.correctIndex)
                    + "\n\n内容：\n" + current.content;
        }
        tvExplain.setText("");
        explainCard.setVisibility(View.VISIBLE);
        MarkdownRenderer.set(tvExplain, explanation);

        long now = System.currentTimeMillis();
        int[] days = settings.getIntervals();
        long next = ReviewScheduler.next(now, current.category, days);

        if (ChoiceRules.askRecategory(current.category, correct)) {
            waitingRecategory = true;
            groupRecategory.setVisibility(View.VISIBLE);
        } else {
            dao.applyChoiceResult(current.id, correct, null, next, now);
            btnNext.setVisibility(View.VISIBLE);
        }
    }

    private void recategorize(String category) {
        if (!waitingRecategory) return;
        waitingRecategory = false;
        long now = System.currentTimeMillis();
        long next = ReviewScheduler.next(now, category, settings.getIntervals());
        dao.applyChoiceResult(current.id, false, category, next, now);
        if (freqMode) CategorySync.applyToSelfLibrary(selfDao, current.term, category);
        groupRecategory.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
