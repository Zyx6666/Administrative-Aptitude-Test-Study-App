package com.idiom.memo.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.idiom.memo.R;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;

public class MainActivity extends AppCompatActivity {
    private EntryDao dao;
    private Module module;
    private TextView tvUnknownCount;
    private TextView tvFuzzyCount;
    private TextView tvKnownCount;
    private TextView tvDue;
    private TextView tvTotalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id);

        tvUnknownCount = findViewById(R.id.tvUnknownCount);
        tvFuzzyCount = findViewById(R.id.tvFuzzyCount);
        tvKnownCount = findViewById(R.id.tvKnownCount);
        tvDue = findViewById(R.id.tvDue);
        tvTotalCount = findViewById(R.id.tvTotalCount);

        TextView tvModule = findViewById(R.id.tvModule);
        tvModule.setText(module.displayName + " ▾");
        tvModule.setOnClickListener(v -> showModuleDialog());

        ((TextView) findViewById(R.id.tvAllSectionTitle)).setText(module.allTitle());
        ((TextView) findViewById(R.id.tvAllItemsLabel)).setText(module.allItemsLabel());
        ((TextView) findViewById(R.id.tvStatsTitle)).setText(module.statsTitle());
        ((TextView) findViewById(R.id.tvCardModeTitle)).setText(module.cardModeTitle());
        ((TextView) findViewById(R.id.tvQuizModeTitle)).setText(module.quizModeTitle());
        ((TextView) findViewById(R.id.tvUploadLabel)).setText(module.uploadEntryLabel());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        findViewById(R.id.btnCardMode).setOnClickListener(v -> startConfig("card"));
        findViewById(R.id.btnQuizMode).setOnClickListener(v -> startConfig("choice"));
        findViewById(R.id.btnUpload).setOnClickListener(v ->
                startActivity(new Intent(this, UploadActivity.class)));
        findViewById(R.id.btnAllIdioms).setOnClickListener(v -> openAllList());

        findViewById(R.id.cardUnknown).setOnClickListener(v -> openList(0));
        findViewById(R.id.cardFuzzy).setOnClickListener(v -> openList(1));
        findViewById(R.id.cardKnown).setOnClickListener(v -> openList(2));

        findViewById(R.id.btnFreqLibrary).setVisibility(
                module == Module.VERBAL ? android.view.View.VISIBLE : android.view.View.GONE);
        findViewById(R.id.btnFreqLibrary).setOnClickListener(v ->
                startActivity(new Intent(this, FreqLibraryActivity.class)));
    }

    private void showModuleDialog() {
        Module[] values = Module.values();
        String[] items = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            items[i] = values[i].displayName + (values[i].enabled ? "" : "（建设中）");
        }
        new AlertDialog.Builder(this)
                .setTitle("选择模块")
                .setItems(items, (d, which) -> {
                    Module selected = values[which];
                    if (!selected.enabled) {
                        Toast.makeText(this, selected.displayName + " 建设中", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selected != module) {
                        ModuleManager.setCurrent(this, selected);
                        recreate();
                    }
                })
                .show();
    }

    private void startConfig(String mode) {
        Intent intent = new Intent(this, ConfigActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    private void openList(int tab) {
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra("tab", tab);
        startActivity(intent);
    }

    private void openAllList() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra("all", true);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvUnknownCount.setText(String.valueOf(dao.countByCategory(Entry.UNKNOWN)));
        tvFuzzyCount.setText(String.valueOf(dao.countByCategory(Entry.FUZZY)));
        tvKnownCount.setText(String.valueOf(dao.countByCategory(Entry.KNOWN)));
        tvTotalCount.setText("共 " + dao.countAll() + " 条 · 未学 "
                + dao.countByCategory(Entry.UNLEARNED) + " 条 · 查看与搜索");

        int due = dao.countDue(System.currentTimeMillis());
        String text = "今日待复习：" + due + " 个";
        SpannableString sp = new SpannableString(text);
        int start = text.indexOf('：') + 1;
        int end = start + String.valueOf(due).length();
        sp.setSpan(new ForegroundColorSpan(getColor(R.color.due)), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvDue.setText(sp);
    }
}
