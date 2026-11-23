package com.idiom.memo.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.idiom.memo.R;
import com.idiom.memo.core.MarkdownExporter;
import com.idiom.memo.core.MarkdownRenderer;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListActivity extends AppCompatActivity {
    private static final int REQ_EXPORT = 1001;
    private static final String[] CATEGORIES = {Entry.UNKNOWN, Entry.FUZZY, Entry.KNOWN};
    private static final String[] CATEGORY_LABELS = {"未学", "不会", "模糊", "会了"};
    private static final String[] CATEGORY_VALUES = {
            Entry.UNLEARNED, Entry.UNKNOWN, Entry.FUZZY, Entry.KNOWN
    };

    private EntryDao dao;
    private Module module;
    private ItemAdapter adapter;
    private int tab;
    private boolean allMode;
    private boolean sortByCount;
    private String keyword = "";

    private RecyclerView recycler;
    private TextView tvEmpty;
    private TextView btnSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id);
        tab = Math.max(0, Math.min(2, getIntent().getIntExtra("tab", 0)));
        allMode = getIntent().getBooleanExtra("all", false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(allMode ? module.allTitle() : module.listTitle());
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_list);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                showAddDialog();
                return true;
            }
            return false;
        });

        recycler = findViewById(R.id.recycler);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnSort = findViewById(R.id.btnSort);

        adapter = new ItemAdapter(this::showDetail, this::showItemMenu);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        if (allMode) {
            tabLayout.setVisibility(View.GONE);
        } else {
            tabLayout.addTab(tabLayout.newTab().setText("不会"));
            tabLayout.addTab(tabLayout.newTab().setText("模糊"));
            tabLayout.addTab(tabLayout.newTab().setText("会了"));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab t) {
                    tab = t.getPosition();
                    if (tab == 0) sortByCount = true;
                    load();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab t) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab t) {
                }
            });
            TabLayout.Tab initial = tabLayout.getTabAt(tab);
            if (initial != null) initial.select();
        }

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setHint(module.searchHint());
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

        btnSort.setOnClickListener(v -> {
            sortByCount = !sortByCount;
            load();
        });

        findViewById(R.id.btnExport).setOnClickListener(v -> exportCurrent());
        findViewById(R.id.btnReviewList).setOnClickListener(v -> {
            String category = allMode ? null : CATEGORIES[tab];
            new AlertDialog.Builder(this)
                    .setTitle(category == null ? "复习全部" + module.termLabel
                            : "复习 " + Entry.label(category))
                    .setItems(new String[]{"内容卡片", "选择题"}, (d, which) -> {
                        Intent intent = new Intent(this, ConfigActivity.class);
                        intent.putExtra("category", category);
                        intent.putExtra("mode", which == 1 ? "choice" : "card");
                        startActivity(intent);
                    })
                    .show();
        });
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        List<Entry> items;
        if (allMode) {
            items = dao.allFiltered(keyword, sortByCount);
            btnSort.setText(sortByCount ? "按总次数降序 ▾" : "按更新时间降序 ▾");
        } else {
            items = dao.byCategory(CATEGORIES[tab], keyword, sortByCount);
            btnSort.setText(sortByCount ? "按次数降序 ▾" : "按更新时间降序 ▾");
        }
        adapter.setItems(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDetail(Entry item) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_detail, null);
        TextView tvTerm = content.findViewById(R.id.tvDialogIdiom);
        TextView tvContent = content.findViewById(R.id.tvDialogMeaning);
        TextView tvMeta = content.findViewById(R.id.tvDialogMeta);

        tvTerm.setText(item.term);
        tvContent.setText("");
        tvMeta.setText("当前分类：" + Entry.label(item.category)
                + "\n不会 " + item.unknownCount + " · 模糊 " + item.fuzzyCount
                + " · 会了 " + item.knownCount
                + "\n选择题：对 " + item.choiceCorrectCount + " / 错 " + item.choiceWrongCount
                + "\n最后反馈：" + formatTime(item.lastReviewAt)
                + "\n下次复习：" + formatTime(item.nextReviewAt)
                + (item.knownWithoutAnswer ? "\n最近一次“会了”来自未看答案自评" : ""));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setNegativeButton("关闭", null)
                .create();
        dialog.show();

        MarkdownRenderer.set(tvTerm, item.term);
        MarkdownRenderer.set(tvContent, item.content);

        content.findViewById(R.id.btnMoveUnknown).setOnClickListener(v -> {
            dao.moveCategory(item.id, Entry.UNKNOWN, System.currentTimeMillis());
            dialog.dismiss();
            load();
        });
        content.findViewById(R.id.btnMoveFuzzy).setOnClickListener(v -> {
            dao.moveCategory(item.id, Entry.FUZZY, System.currentTimeMillis());
            dialog.dismiss();
            load();
        });
        content.findViewById(R.id.btnMoveKnown).setOnClickListener(v -> {
            dao.moveCategory(item.id, Entry.KNOWN, System.currentTimeMillis());
            dialog.dismiss();
            load();
        });
    }

    private void showItemMenu(Entry item) {
        String[] items = {
                "修改" + module.termLabel,
                "修改" + module.contentLabel,
                "移动分类",
                "删除" + module.termLabel
        };
        new AlertDialog.Builder(this)
                .setTitle(item.term)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            showEditTermDialog(item);
                            break;
                        case 1:
                            showEditContentDialog(item);
                            break;
                        case 2:
                            showMoveDialog(item);
                            break;
                        case 3:
                            confirmDelete(item);
                            break;
                    }
                })
                .show();
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_entry, null);
        TextView tvTermLabel = view.findViewById(R.id.tvTermLabel);
        TextView tvContentLabel = view.findViewById(R.id.tvContentLabel);
        EditText etTerm = view.findViewById(R.id.etTerm);
        EditText etContent = view.findViewById(R.id.etContent);
        Spinner spCategory = view.findViewById(R.id.spCategory);

        tvTermLabel.setText(module.termLabel);
        tvContentLabel.setText(module.contentLabel);
        spCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CATEGORY_LABELS));
        spCategory.setSelection(0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(module.addTitle())
                .setView(view)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String term = etTerm.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (term.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "请填写" + module.termLabel + "和" + module.contentLabel,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (dao.findByTerm(term) != null) {
                Toast.makeText(this, "该" + module.termLabel + "已存在", Toast.LENGTH_SHORT).show();
                return;
            }
            long now = System.currentTimeMillis();
            Entry entry = new Entry();
            entry.term = term;
            entry.content = content;
            entry.category = CATEGORY_VALUES[spCategory.getSelectedItemPosition()];
            entry.createdAt = now;
            entry.updatedAt = now;
            dao.insert(entry);
            dialog.dismiss();
            load();
        });
    }

    private void showEditTermDialog(Entry item) {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.bg_search);
        et.setPadding(dp(12), dp(12), dp(12), dp(12));
        et.setText(item.term);
        et.setSelection(item.term.length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改" + module.termLabel)
                .setView(et)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String term = et.getText().toString().trim();
            if (term.isEmpty()) {
                Toast.makeText(this, module.termLabel + "不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            Entry duplicate = dao.findByTerm(term);
            if (duplicate != null && duplicate.id != item.id) {
                Toast.makeText(this, "该" + module.termLabel + "已存在", Toast.LENGTH_SHORT).show();
                return;
            }
            dao.updateTerm(item.id, term, System.currentTimeMillis());
            dialog.dismiss();
            load();
        });
    }

    private void showEditContentDialog(Entry item) {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.bg_search);
        et.setPadding(dp(12), dp(12), dp(12), dp(12));
        et.setMinLines(4);
        et.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        et.setText(item.content);
        et.setSelection(item.content.length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改" + module.contentLabel)
                .setView(et)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String content = et.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, module.contentLabel + "不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            dao.updateContent(item.id, content, System.currentTimeMillis());
            dialog.dismiss();
            load();
        });
    }

    private void showMoveDialog(Entry item) {
        String[] labels = {"不会", "模糊", "会了"};
        String[] values = {Entry.UNKNOWN, Entry.FUZZY, Entry.KNOWN};
        new AlertDialog.Builder(this)
                .setTitle("移动分类")
                .setItems(labels, (d, which) -> {
                    dao.moveCategory(item.id, values[which], System.currentTimeMillis());
                    load();
                })
                .show();
    }

    private void confirmDelete(Entry item) {
        new AlertDialog.Builder(this)
                .setTitle("删除" + module.termLabel)
                .setMessage("确定删除「" + item.term + "」？删除后无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    dao.delete(item.id);
                    load();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void exportCurrent() {
        String scope = allMode ? "全部" + module.termLabel
                : Entry.label(CATEGORIES[tab]) + "列表";
        String name = module.displayName + "_" + scope + "_"
                + new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date()) + ".md";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/markdown");
        intent.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, REQ_EXPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXPORT && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("无法写入");
                List<Entry> dataList = allMode
                        ? dao.allFiltered("", sortByCount)
                        : dao.byCategory(CATEGORIES[tab], "", false);
                String title = module.displayName + " · "
                        + (allMode ? "全部" + module.termLabel
                        : Entry.label(CATEGORIES[tab]) + "列表");
                String markdown = MarkdownExporter.toMarkdown(
                        dataList, title, module.termLabel, module.contentLabel);
                out.write(markdown.getBytes(StandardCharsets.UTF_8));
                out.flush();
                Toast.makeText(this, "已导出", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private static String formatTime(Long time) {
        if (time == null) return "未安排";
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(time));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.Holder> {
        private final List<Entry> items = new ArrayList<>();
        private final OnItemClick onClick;
        private final OnItemClick onLongClick;

        ItemAdapter(OnItemClick onClick, OnItemClick onLongClick) {
            this.onClick = onClick;
            this.onLongClick = onLongClick;
        }

        void setItems(List<Entry> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_idiom, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Entry item = items.get(position);
            MarkdownRenderer.set(holder.idiom, item.term);
            MarkdownRenderer.set(holder.meaning, item.content);
            holder.meta.setText("当前：" + Entry.label(item.category)
                    + " · 不会 " + item.unknownCount
                    + " · 模糊 " + item.fuzzyCount
                    + " · 会了 " + item.knownCount
                    + "\n下次复习 " + formatTime(item.nextReviewAt));
            View.OnClickListener click = v -> onClick.onClick(item);
            holder.btnExplain.setOnClickListener(click);
            holder.itemView.setOnClickListener(click);
            holder.itemView.setOnLongClickListener(v -> {
                onLongClick.onClick(item);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView idiom;
            final TextView meaning;
            final TextView meta;
            final TextView btnExplain;

            Holder(@NonNull View itemView) {
                super(itemView);
                idiom = itemView.findViewById(R.id.tvItemIdiom);
                meaning = itemView.findViewById(R.id.tvItemMeaning);
                meta = itemView.findViewById(R.id.tvItemMeta);
                btnExplain = itemView.findViewById(R.id.btnItemExplain);
            }
        }
    }

    private interface OnItemClick {
        void onClick(Entry item);
    }
}
