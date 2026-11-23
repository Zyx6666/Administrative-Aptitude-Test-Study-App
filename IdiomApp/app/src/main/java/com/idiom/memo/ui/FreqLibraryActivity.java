package com.idiom.memo.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.idiom.memo.core.FrequencyImportService;
import com.idiom.memo.core.FrequencyMarkdownParser;
import com.idiom.memo.core.FrequencyUnlock;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;
import com.idiom.memo.data.SettingsDao;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreqLibraryActivity extends AppCompatActivity {
    private static final int REQ_PICK = 4001;
    private static final int REQ_TEMPLATE = 4002;
    private static final String TEMPLATE_ASSET = "templates/verbal_freq.md";
    private static final String TEMPLATE_NAME = "下载言语理解导入模板（带频次）.md";

    private EntryDao dao;
    private EntryDao selfDao;
    private SettingsDao settings;
    private Module module;
    private TextView tvSummary;
    private TextView tvEmpty;
    private TextView btnThreshold;
    private TextView tvUnknownCount;
    private TextView tvFuzzyCount;
    private TextView tvKnownCount;
    private TextView tvAllSubtitle;
    private GroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freq_library);

        module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id, Entry.SOURCE_FREQ);
        selfDao = new EntryDao(this, module.id, Entry.SOURCE_SELF);
        settings = new SettingsDao(this, module.id);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvSummary = findViewById(R.id.tvFreqSummary);
        tvEmpty = findViewById(R.id.tvFreqEmpty);
        btnThreshold = findViewById(R.id.btnFreqThreshold);
        tvUnknownCount = findViewById(R.id.tvFreqUnknownCount);
        tvFuzzyCount = findViewById(R.id.tvFreqFuzzyCount);
        tvKnownCount = findViewById(R.id.tvFreqKnownCount);
        tvAllSubtitle = findViewById(R.id.tvFreqAllSubtitle);

        RecyclerView recycler = findViewById(R.id.freqRecycler);
        adapter = new GroupAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnThreshold.setOnClickListener(v -> showThresholdDialog());
        findViewById(R.id.btnImportFreq).setOnClickListener(v -> pickFile());
        findViewById(R.id.btnDownloadFreqTemplate)
                .setOnClickListener(v -> saveTemplate());
        findViewById(R.id.btnFreqAll).setOnClickListener(v -> openList(null));
        findViewById(R.id.cardFreqUnknown).setOnClickListener(v -> openList(Entry.UNKNOWN));
        findViewById(R.id.cardFreqFuzzy).setOnClickListener(v -> openList(Entry.FUZZY));
        findViewById(R.id.cardFreqKnown).setOnClickListener(v -> openList(Entry.KNOWN));

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
        int threshold = settings.getFreqThreshold();
        List<FrequencyUnlock.Group> groups = FrequencyUnlock.plan(all, threshold);
        adapter.setGroups(groups);

        int unknown = 0;
        int fuzzy = 0;
        int known = 0;
        int unlockedCount = 0;
        for (FrequencyUnlock.Group group : groups) {
            if (!group.unlocked) continue;
            unknown += group.unknown;
            fuzzy += group.fuzzy;
            known += group.known;
            unlockedCount += group.total;
        }
        tvUnknownCount.setText(String.valueOf(unknown));
        tvFuzzyCount.setText(String.valueOf(fuzzy));
        tvKnownCount.setText(String.valueOf(known));

        tvSummary.setText("共 " + all.size() + " 个成语 · " + groups.size() + " 个频次组");
        tvAllSubtitle.setText("共 " + all.size() + " 个 · 已解锁 " + unlockedCount
                + " 个 · 查看与搜索");
        tvEmpty.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
        btnThreshold.setText("达标线：模糊 < 会了 × " + threshold + "%　点击调整");
    }

    private void openList(String category) {
        Intent intent = new Intent(this, FreqListActivity.class);
        if (category != null) intent.putExtra("category", category);
        startActivity(intent);
    }

    private void showThresholdDialog() {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.bg_search);
        et.setPadding(dp(12), dp(12), dp(12), dp(12));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(settings.getFreqThreshold()));
        et.setSelection(et.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("考频库达标阈值")
                .setMessage("达标条件：组内没有「不会」，且「模糊」数量 < 「会了」数量 × 阈值。")
                .setView(et)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int value;
            try {
                value = Integer.parseInt(et.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入 0～100 的数字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (value < 0 || value > 100) {
                Toast.makeText(this, "请输入 0～100 的数字", Toast.LENGTH_SHORT).show();
                return;
            }
            settings.setFreqThreshold(value);
            dialog.dismiss();
            load();
        });
    }

    private void showGroupDialog(FrequencyUnlock.Group group) {
        if (!group.unlocked) {
            new AlertDialog.Builder(this)
                    .setTitle(group.frequency + " 次组")
                    .setMessage("该频次组尚未解锁。请先让更高频次组全部达标（没有「不会」，且「模糊」数量 < 「会了」数量 × 阈值）。")
                    .setPositiveButton("知道了", null)
                    .show();
            return;
        }
        String[] options = {"内容卡片", "选择题练习", "查看成语列表"};
        new AlertDialog.Builder(this)
                .setTitle(group.frequency + " 次组 · 共 " + group.total + " 个成语")
                .setItems(options, (d, which) -> {
                    if (which == 2) {
                        Intent intent = new Intent(this, FreqGroupActivity.class);
                        intent.putExtra("frequency", group.frequency);
                        startActivity(intent);
                    } else {
                        startStudy(group, which == 1);
                    }
                })
                .show();
    }

    private void startStudy(FrequencyUnlock.Group group, boolean choice) {
        Intent intent = new Intent(this,
                choice ? ChoiceQuizActivity.class : CardStudyActivity.class);
        intent.putExtra("source", Entry.SOURCE_FREQ);
        intent.putExtra("frequency", group.frequency);
        intent.putExtra("size", Math.max(1, group.total));
        intent.putExtra("ratioU", 50);
        intent.putExtra("ratioF", 30);
        intent.putExtra("ratioK", 20);
        startActivity(intent);
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

    private void saveTemplate() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/markdown");
        intent.putExtra(Intent.EXTRA_TITLE, TEMPLATE_NAME);
        startActivityForResult(intent, REQ_TEMPLATE);
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
            Toast.makeText(this, "读取失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copyTemplate(Uri uri) {
        try (InputStream in = getAssets().open(TEMPLATE_ASSET);
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

        FrequencyMarkdownParser.Result result =
                FrequencyMarkdownParser.parse(new String(bytes, StandardCharsets.UTF_8));
        if (result.rows.isEmpty()) {
            throw new IllegalArgumentException("没有解析到有效内容，请确认为「成语 | 解释 | 频次」三列");
        }

        Map<String, FrequencyImportService.Existing> existing = new HashMap<>();
        for (Entry item : dao.all()) {
            existing.put(item.term,
                    new FrequencyImportService.Existing(item.id, item.content, item.frequency));
        }
        FrequencyImportService.Plan plan =
                FrequencyImportService.plan(result.rows, existing, result.invalid);

        Map<String, String> selfCategories = CategorySync.selfCategories(selfDao);
        int overlap = 0;
        for (FrequencyMarkdownParser.Row row : result.rows) {
            if (selfCategories.containsKey(row.term)) overlap++;
        }
        String message = "解析出：" + result.rows.size() + " 条（同文件重复已按成语合并）\n"
                + "新增：" + plan.fresh.size() + " 条\n"
                + "更新：" + plan.updates.size() + " 条（只更新解释与频次）\n"
                + "无变化：" + plan.unchanged + " 条\n"
                + "无效：" + plan.invalid + " 条\n"
                + (overlap > 0
                ? "与错题本重叠：" + overlap + " 条（未刷过的学习状态以错题本为准）" : "");
        new AlertDialog.Builder(this)
                .setTitle(name.isEmpty() ? "导入考频库" : name)
                .setMessage(message)
                .setNegativeButton("取消", null)
                .setPositiveButton("开始导入", (d, w) -> apply(plan))
                .show();
    }

    private void apply(FrequencyImportService.Plan plan) {
        long now = System.currentTimeMillis();
        Map<String, String> selfCategories = CategorySync.selfCategories(selfDao);
        for (FrequencyMarkdownParser.Row row : plan.fresh) {
            Entry item = new Entry();
            item.sourceType = Entry.SOURCE_FREQ;
            item.term = row.term;
            item.content = row.content;
            item.frequency = row.frequency;
            String category = selfCategories.get(row.term);
            item.category = category == null ? Entry.UNKNOWN : category;
            item.createdAt = now;
            item.updatedAt = now;
            dao.insert(item);
        }
        for (FrequencyImportService.Update update : plan.updates) {
            dao.updateContentAndFrequency(update.id, update.content, update.frequency, now);
        }
        load();
        Toast.makeText(this, "导入完成：新增 " + plan.fresh.size()
                + " 条，更新 " + plan.updates.size() + " 条", Toast.LENGTH_LONG).show();
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

    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.Holder> {
        private final List<FrequencyUnlock.Group> groups = new ArrayList<>();
        private int threshold = FrequencyUnlock.DEFAULT_THRESHOLD;

        void setGroups(List<FrequencyUnlock.Group> list) {
            groups.clear();
            groups.addAll(list);
            threshold = settings.getFreqThreshold();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_freq_group, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            FrequencyUnlock.Group group = groups.get(position);
            holder.frequency.setText(group.frequency + " 次");
            holder.stats.setText("共 " + group.total + " 个 · 不会 " + group.unknown
                    + " · 模糊 " + group.fuzzy + " · 会了 " + group.known);
            holder.rule.setText("达标线：模糊 < 会了 × " + threshold + "%");

            if (group.achieved) {
                holder.status.setText("已达标");
                holder.status.setTextColor(getColor(R.color.known));
            } else if (group.unlocked) {
                holder.status.setText("已解锁");
                holder.status.setTextColor(getColor(R.color.brand));
            } else {
                holder.status.setText("未解锁");
                holder.status.setTextColor(getColor(R.color.text_secondary));
            }
            holder.root.setAlpha(group.unlocked ? 1f : 0.45f);
            holder.root.setOnClickListener(v -> showGroupDialog(group));
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final View root;
            final TextView frequency;
            final TextView stats;
            final TextView rule;
            final TextView status;

            Holder(@NonNull View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.freqGroupRoot);
                frequency = itemView.findViewById(R.id.tvGroupFrequency);
                stats = itemView.findViewById(R.id.tvGroupStats);
                rule = itemView.findViewById(R.id.tvGroupRule);
                status = itemView.findViewById(R.id.tvGroupStatus);
            }
        }
    }
}
