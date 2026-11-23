package com.idiom.memo.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.idiom.memo.R;
import com.idiom.memo.data.Entry;
import com.idiom.memo.data.EntryDao;
import com.idiom.memo.data.Module;
import com.idiom.memo.data.ModuleManager;
import com.idiom.memo.data.SettingsDao;

public class ConfigActivity extends AppCompatActivity {
    private String mode;
    private String category;
    private int defaultSize;
    private final int[] ratios = new int[3];

    private EditText etSize;
    private TextView tvRatioU;
    private TextView tvRatioF;
    private TextView tvRatioK;
    private MaterialButton btnModeCard;
    private MaterialButton btnModeChoice;
    private EntryDao dao;
    private Module module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        module = ModuleManager.current(this);
        dao = new EntryDao(this, module.id);

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "card";
        category = getIntent().getStringExtra("category");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        SettingsDao settings = new SettingsDao(this, module.id);
        int[] saved = settings.getRatios();
        ratios[0] = saved[0];
        ratios[1] = saved[1];
        ratios[2] = saved[2];
        defaultSize = settings.getSessionSize();

        etSize = findViewById(R.id.etSize);
        tvRatioU = findViewById(R.id.tvRatioU);
        tvRatioF = findViewById(R.id.tvRatioF);
        tvRatioK = findViewById(R.id.tvRatioK);
        btnModeCard = findViewById(R.id.btnModeCard);
        btnModeChoice = findViewById(R.id.btnModeChoice);

        etSize.setText(String.valueOf(defaultSize));
        etSize.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) restoreIfInvalid();
        });

        SeekBar sbU = findViewById(R.id.sbU);
        SeekBar sbF = findViewById(R.id.sbF);
        SeekBar sbK = findViewById(R.id.sbK);
        sbU.setProgress(ratios[0]);
        sbF.setProgress(ratios[1]);
        sbK.setProgress(ratios[2]);

        sbU.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ratios[0] = progress;
                updateRatioLabels();
            }
        });
        sbF.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ratios[1] = progress;
                updateRatioLabels();
            }
        });
        sbK.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ratios[2] = progress;
                updateRatioLabels();
            }
        });

        findViewById(R.id.btnMinus).setOnClickListener(v -> changeSize(-1));
        findViewById(R.id.btnPlus).setOnClickListener(v -> changeSize(1));

        btnModeCard.setOnClickListener(v -> {
            mode = "card";
            updateModeButtons();
        });
        btnModeChoice.setOnClickListener(v -> {
            mode = "choice";
            updateModeButtons();
        });

        if (category != null) {
            findViewById(R.id.groupRatios).setVisibility(View.GONE);
            toolbar.setTitle("专项复习 · " + Entry.label(category));
        }

        findViewById(R.id.btnStart).setOnClickListener(v -> startSession());

        updateRatioLabels();
        updateModeButtons();
    }

    private void startSession() {
        int size = parseInt(etSize.getText().toString(), -1);
        if (size < 1 || size > 999) {
            size = defaultSize;
            etSize.setText(String.valueOf(size));
            Toast.makeText(this, "数量已恢复默认值", Toast.LENGTH_SHORT).show();
        }

        int available = category == null ? dao.countAll() : dao.countByCategory(category);
        if (available <= 0) {
            Toast.makeText(this, "没有可复习的" + module.termLabel, Toast.LENGTH_SHORT).show();
            return;
        }
        int effective = Math.min(size, available);
        if (effective < size) {
            Toast.makeText(this, "当前只有 " + available + " 条，本次按 " + effective + " 条开始",
                    Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this,
                "choice".equals(mode) ? ChoiceQuizActivity.class : CardStudyActivity.class);
        intent.putExtra("category", category);
        intent.putExtra("size", effective);
        intent.putExtra("ratioU", ratios[0]);
        intent.putExtra("ratioF", ratios[1]);
        intent.putExtra("ratioK", ratios[2]);
        startActivity(intent);
        finish();
    }

    private void restoreIfInvalid() {
        int size = parseInt(etSize.getText().toString(), -1);
        if (size < 1 || size > 999) {
            etSize.setText(String.valueOf(defaultSize));
            Toast.makeText(this, "数量需为 1～999，已恢复默认值", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeSize(int delta) {
        int current = parseInt(etSize.getText().toString(), defaultSize);
        int next = Math.max(1, Math.min(999, current + delta));
        etSize.setText(String.valueOf(next));
        etSize.setSelection(etSize.getText().length());
    }

    private void updateRatioLabels() {
        tvRatioU.setText("不会：" + ratios[0] + "%");
        tvRatioF.setText("模糊：" + ratios[1] + "%");
        tvRatioK.setText("会了：" + ratios[2] + "%");
    }

    private void updateModeButtons() {
        boolean card = "card".equals(mode);
        styleMode(btnModeCard, card);
        styleMode(btnModeChoice, !card);
    }

    private void styleMode(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.brand)));
            button.setTextColor(getColor(R.color.white));
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.brand_light)));
            button.setTextColor(getColor(R.color.brand_dark));
        }
    }

    private static int parseInt(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
