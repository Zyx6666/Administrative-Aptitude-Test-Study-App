package com.idiom.memo.core;

import com.idiom.memo.data.Entry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MarkdownExporter {
    private MarkdownExporter() {
    }

    public static String toMarkdown(List<Entry> items, String title,
                                    String termLabel, String contentLabel) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        sb.append("导出时间：").append(fmt.format(new Date())).append("\n\n");
        sb.append("| ").append(termLabel)
                .append(" | ").append(contentLabel)
                .append(" | 分类 | 不会 | 模糊 | 会了 | 选择题对 | 选择题错 | 下次复习 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");

        if (items == null || items.isEmpty()) {
            sb.append("\n（暂无数据）\n");
            return sb.toString();
        }

        for (Entry item : items) {
            sb.append("| ").append(cell(item.term))
                    .append(" | ").append(cell(item.content))
                    .append(" | ").append(Entry.label(item.category))
                    .append(" | ").append(item.unknownCount)
                    .append(" | ").append(item.fuzzyCount)
                    .append(" | ").append(item.knownCount)
                    .append(" | ").append(item.choiceCorrectCount)
                    .append(" | ").append(item.choiceWrongCount)
                    .append(" | ").append(item.nextReviewAt == null ? "未安排"
                            : fmt.format(new Date(item.nextReviewAt)))
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String cell(String text) {
        if (text == null) return "";
        return text.replace("|", "\\|")
                .replace("\r\n", "\n")
                .replace("\n", "<br>")
                .trim();
    }
}
