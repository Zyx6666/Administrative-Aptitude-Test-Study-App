package com.idiom.memo.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.idiom.memo.R;
import com.idiom.memo.core.ImportService;
import com.idiom.memo.core.MarkdownParser;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadActivity extends AppCompatActivity {
    private static final int REQ_PICK = 2001;
    private static final int REQ_TEMPLATE = 2002;

    private EntryDao dao;
    private Module module;
    private TextView tvStatus;
    private LinearLayout previewTable;
    private TextView tvPreviewEmpty;
    private View summaryCard;
    private View btnStartImport;
    private TextView tvNewCount;
    private TextView tvSameCount;
    private TextView tvConflictCount;
    private TextView tvInvalidCount;

    private ImportService.Plan plan;
    private final Map<String, Long> insertedIds = new HashMap<>();
    private int conflictIndex;
    private int handledConflicts;
    private String pendingTemplateAsset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("导入" + module.termLabel + "库");
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStatus = findViewById(R.id.tvStatus);
        previewTable = findViewById(R.id.previewTable);
        tvPreviewEmpty = findViewById(R.id.tvPreviewEmpty);
        summaryCard = findViewById(R.id.summaryCard);
        btnStartImport = findViewById(R.id.btnStartImport);
        tvNewCount = findViewById(R.id.tvNewCount);
        tvSameCount = findViewById(R.id.tvSameCount);
        tvConflictCount = findViewById(R.id.tvConflictCount);
        tvInvalidCount = findViewById(R.id.tvInvalidCount);

        ((TextView) findViewById(R.id.tvPickTitle)).setText(module.pickFileTitle());
        ((TextView) findViewById(R.id.tvPickSubtitle))
                .setText("两列：" + module.termLabel + " | " + module.contentLabel);

        TextView tvHint = findViewById(R.id.tvHint);
        tvHint.setText(module.uploadHint);

        bindTemplateButton(R.id.btnTplMd, "templates/" + module.id + ".md",
                module.templateFileName(), "text/markdown");

        findViewById(R.id.btnPickFile).setOnClickListener(v -> pickFile());
        btnStartImport.setOnClickListener(v -> startImport());
    }

    private void bindTemplateButton(int id, String asset, String label, String mime) {
        TextView button = findViewById(id);
        button.setText(label);
        button.setOnClickListener(v -> saveTemplate(asset, label, mime));
    }

    private void saveTemplate(String asset, String fileName, String mime) {
        pendingTemplateAsset = asset;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, REQ_TEMPLATE);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/markdown",
                "text/x-markdown",
                "text/plain",
                "application/octet-stream"
        });
        startActivityForResult(intent, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQ_TEMPLATE) {
            copyTemplate(uri);
            return;
        }
        if (requestCode != REQ_PICK) return;
        try {
            preview(uri);
        } catch (Exception e) {
            tvStatus.setText("读取失败：" + e.getMessage());
            Toast.makeText(this, "读取失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copyTemplate(Uri uri) {
        if (pendingTemplateAsset == null) return;
        try (InputStream in = getAssets().open(pendingTemplateAsset);
             OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("无法写入");
            out.write(readAll(in));
            out.flush();
            Toast.makeText(this, "模板已保存到手机", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void preview(Uri uri) throws Exception {
        String name = displayName(uri);
        String lower = name.toLowerCase();
        if (!lower.endsWith(".md") && !lower.endsWith(".markdown")) {
            throw new IllegalArgumentException("目前只支持 .md 文件");
        }

        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("无法读取文件");
            bytes = readAll(in);
        }

        List<ImportService.Row> parsed =
                MarkdownParser.parse(new String(bytes, StandardCharsets.UTF_8));
        if (parsed.isEmpty()) throw new IllegalArgumentException("没有解析到有效内容");

        Map<String, ImportService.Existing> existing = new HashMap<>();
        for (Entry item : dao.all()) {
            existing.put(item.term, new ImportService.Existing(item.id, item.content));
        }
        plan = ImportService.plan(parsed, existing);

        tvStatus.setText((name.isEmpty() ? "已选择文件" : name) + " · 解析出 " + parsed.size() + " 条");
        renderPreview(parsed);
        tvNewCount.setText("新增：" + plan.fresh.size() + " 条");
        tvSameCount.setText("重复内容相同：" + plan.sameCount + " 条（跳过）");
        tvConflictCount.setText("重复内容不同：" + plan.conflicts.size() + " 条待处理");
        tvInvalidCount.setText("空行/失败：" + plan.invalid + " 条");
        summaryCard.setVisibility(View.VISIBLE);
        btnStartImport.setVisibility(View.VISIBLE);
        btnStartImport.setEnabled(true);
    }

    private void renderPreview(List<ImportService.Row> parsed) {
        previewTable.removeAllViews();
        addPreviewRow(module.termLabel, module.contentLabel, true);
        int shown = 0;
        for (ImportService.Row row : parsed) {
            if (shown >= 2) break;
            addPreviewRow(row.term, row.content, false);
            shown++;
        }
        if (shown == 0) {
            previewTable.setVisibility(View.GONE);
            tvPreviewEmpty.setVisibility(View.VISIBLE);
            tvPreviewEmpty.setText("文件中没有数据行");
        } else {
            previewTable.setVisibility(View.VISIBLE);
            tvPreviewEmpty.setVisibility(View.GONE);
            TextView dots = new TextView(this);
            dots.setText("……");
            dots.setGravity(Gravity.CENTER);
            dots.setPadding(0, dp(6), 0, dp(6));
            dots.setTextColor(getColor(R.color.text_secondary));
            previewTable.addView(dots);
        }
    }

    private void addPreviewRow(String left, String right, boolean header) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(cell(left, header), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(cell(right, header), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        previewTable.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.card_stroke));
        previewTable.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
    }

    private TextView cell(String text, boolean header) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(10), dp(8), dp(10));
        tv.setTextSize(14f);
        tv.setMaxLines(1);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setTextColor(getColor(header ? R.color.text_primary : R.color.text_secondary));
        if (header) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private void startImport() {
        if (plan == null) return;
        btnStartImport.setEnabled(false);

        long now = System.currentTimeMillis();
        insertedIds.clear();
        for (ImportService.Row row : plan.fresh) {
            Entry item = new Entry();
            item.term = row.term;
            item.content = row.content;
            item.category = Entry.UNKNOWN;
            item.createdAt = now;
            item.updatedAt = now;
            long id = dao.insert(item);
            insertedIds.put(row.term, id);
        }

        conflictIndex = 0;
        handledConflicts = 0;
        showNextConflict();
    }

    private void showNextConflict() {
        if (plan == null || conflictIndex >= plan.conflicts.size()) {
            showSummary();
            return;
        }
        ImportService.Conflict conflict = plan.conflicts.get(conflictIndex);
        new AlertDialog.Builder(this)
                .setTitle(conflict.term)
                .setMessage("旧" + module.contentLabel + "：\n" + conflict.oldContent
                        + "\n\n新" + module.contentLabel + "：\n" + conflict.newContent)
                .setCancelable(false)
                .setNegativeButton("用旧" + module.contentLabel, (d, w) -> {
                    conflictIndex++;
                    showNextConflict();
                })
                .setPositiveButton("用新" + module.contentLabel, (d, w) -> {
                    long id = conflict.id != 0 ? conflict.id
                            : insertedIds.getOrDefault(conflict.term, 0L);
                    if (id != 0) {
                        dao.updateContent(id, conflict.newContent, System.currentTimeMillis());
                    }
                    handledConflicts++;
                    conflictIndex++;
                    showNextConflict();
                })
                .show();
    }

    private void showSummary() {
        if (plan == null) return;
        tvConflictCount.setText("重复内容不同：" + handledConflicts + " 条已处理");
        String message = "新增：" + plan.fresh.size() + " 条\n"
                + "重复且内容相同：" + plan.sameCount + " 条，已跳过\n"
                + "重复但内容不同：" + handledConflicts + " 条，已处理\n"
                + "空行/失败：" + plan.invalid + " 条";
        new AlertDialog.Builder(this)
                .setTitle("导入结果")
                .setMessage(message)
                .setPositiveButton("完成", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = c.getString(idx);
                    if (name != null) return name;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
