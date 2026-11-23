package com.idiom.memo.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.idiom.memo.R;
import com.idiom.memo.core.MarkdownExporter;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;
import com.idiom.memo.data.SettingsDao;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static final int REQ_EXPORT_ALL = 3001;

    private EntryDao dao;
    private SettingsDao settingsDao;
    private Module module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id);
        settingsDao = new SettingsDao(this, module.id);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(module.displayName + "设置");
        toolbar.setNavigationOnClickListener(v -> finish());

        int[] intervals = settingsDao.getIntervals();
        int[] ratios = settingsDao.getRatios();
        int sessionSize = settingsDao.getSessionSize();

        EditText etUnknownDays = findViewById(R.id.etUnknownDays);
        EditText etFuzzyDays = findViewById(R.id.etFuzzyDays);
        EditText etKnownDays = findViewById(R.id.etKnownDays);
        EditText etRatioU = findViewById(R.id.etRatioU);
        EditText etRatioF = findViewById(R.id.etRatioF);
        EditText etRatioK = findViewById(R.id.etRatioK);
        EditText etSessionSize = findViewById(R.id.etSessionSize);

        etUnknownDays.setText(String.valueOf(intervals[0]));
        etFuzzyDays.setText(String.valueOf(intervals[1]));
        etKnownDays.setText(String.valueOf(intervals[2]));
        etRatioU.setText(String.valueOf(ratios[0]));
        etRatioF.setText(String.valueOf(ratios[1]));
        etRatioK.setText(String.valueOf(ratios[2]));
        etSessionSize.setText(String.valueOf(sessionSize));

        ((TextView) findViewById(R.id.tvClearLabel))
                .setText("清除" + module.displayName + "数据");

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            int u = parseInt(etUnknownDays.getText().toString(), -1);
            int f = parseInt(etFuzzyDays.getText().toString(), -1);
            int k = parseInt(etKnownDays.getText().toString(), -1);
            int ru = parseInt(etRatioU.getText().toString(), -1);
            int rf = parseInt(etRatioF.getText().toString(), -1);
            int rk = parseInt(etRatioK.getText().toString(), -1);
            int size = parseInt(etSessionSize.getText().toString(), -1);

            if (u < 1 || u > 365 || f < 1 || f > 365 || k < 1 || k > 365) {
                Toast.makeText(this, "复习间隔请填 1～365 天", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ru < 0 || rf < 0 || rk < 0 || ru > 100 || rf > 100 || rk > 100) {
                Toast.makeText(this, "抽取比例请填 0～100", Toast.LENGTH_SHORT).show();
                return;
            }
            if (size < 1 || size > 999) {
                Toast.makeText(this, "每次数量请填 1～999", Toast.LENGTH_SHORT).show();
                return;
            }

            settingsDao.save(new int[]{u, f, k}, new int[]{ru, rf, rk}, size);
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnExportAll).setOnClickListener(v -> {
            String name = module.displayName + "_全部" + module.termLabel + "_"
                    + new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date()) + ".md";
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/markdown");
            intent.putExtra(Intent.EXTRA_TITLE, name);
            startActivityForResult(intent, REQ_EXPORT_ALL);
        });

        findViewById(R.id.btnClearAll).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("清除" + module.displayName + "数据")
                        .setMessage("将删除当前模块的全部条目和学习记录，无法恢复。建议先导出备份。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("清除", (d, w) -> {
                            dao.deleteAll();
                            settingsDao.save(new int[]{1, 3, 7}, new int[]{50, 30, 20}, 20);
                            Toast.makeText(this, "已清除当前模块数据", Toast.LENGTH_SHORT).show();
                        })
                        .show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXPORT_ALL && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("无法写入");
                String markdown = MarkdownExporter.toMarkdown(dao.all(),
                        module.displayName + " · 全部" + module.termLabel,
                        module.termLabel, module.contentLabel);
                out.write(markdown.getBytes(StandardCharsets.UTF_8));
                out.flush();
                Toast.makeText(this, "已导出", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private static int parseInt(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
