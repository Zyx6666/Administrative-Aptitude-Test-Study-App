package com.idiom.memo.core;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownParser {
    private MarkdownParser() {
    }

    public static List<ImportService.Row> parse(String markdown) {
        List<ImportService.Row> rows = new ArrayList<>();
        if (markdown == null || markdown.trim().isEmpty()) return rows;

        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

        // 1) 表格写法
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains("|")) continue;
            String[] header = splitRow(lines[i]);
            int termIdx = -1;
            int contentIdx = -1;
            for (int c = 0; c < header.length; c++) {
                String h = header[c].trim().toLowerCase();
                if (termIdx < 0 && isTermHeader(h)) termIdx = c;
                if (contentIdx < 0 && isContentHeader(h)) contentIdx = c;
            }
            if (termIdx < 0 || contentIdx < 0 || termIdx == contentIdx) continue;

            int j = i + 1;
            if (j < lines.length && isSeparator(lines[j])) j++;
            for (; j < lines.length && lines[j].contains("|"); j++) {
                if (isSeparator(lines[j])) continue;
                String[] cells = splitRow(lines[j]);
                if (termIdx >= cells.length || contentIdx >= cells.length) continue;
                String term = MarkdownNormalizer.normalize(cells[termIdx]);
                String content = MarkdownNormalizer.normalize(cells[contentIdx]);
                if (!term.isEmpty() && !content.isEmpty()) {
                    rows.add(new ImportService.Row(term, content));
                }
            }
            if (!rows.isEmpty()) return rows;
        }

        // 2) 标题作为词条、下面的段落作为解释
        String currentTerm = null;
        StringBuilder currentContent = new StringBuilder();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.matches("^#{1,6}\\s+.*")) {
                flush(rows, currentTerm, currentContent);
                currentTerm = MarkdownNormalizer.normalize(line.replaceFirst("^#{1,6}\\s+", ""));
                currentContent.setLength(0);
            } else if (currentTerm != null) {
                if (line.isEmpty()) {
                    if (currentContent.length() > 0) currentContent.append('\n');
                } else if (isSeparator(line)) {
                    continue;
                } else {
                    String normalized = MarkdownNormalizer.normalize(line);
                    if (!normalized.isEmpty()) currentContent.append(normalized).append('\n');
                }
            }
        }
        flush(rows, currentTerm, currentContent);
        return rows;
    }

    private static void flush(List<ImportService.Row> rows, String term, StringBuilder content) {
        if (term == null || term.trim().isEmpty()) return;
        String body = content.toString().trim();
        if (!body.isEmpty()) rows.add(new ImportService.Row(term.trim(), body));
    }

    private static boolean isTermHeader(String h) {
        return h.contains("成语") || h.contains("知识点") || h.contains("词条")
                || h.contains("标题") || h.contains("名称") || h.equals("term") || h.equals("title");
    }

    private static boolean isContentHeader(String h) {
        return h.contains("解释") || h.contains("内容") || h.contains("定义")
                || h.contains("说明") || h.contains("答案解析")
                || h.equals("content") || h.equals("explanation");
    }

    private static boolean isSeparator(String line) {
        String s = line.trim();
        if (!s.contains("-") || !s.contains("|")) return false;
        String[] cells = splitRow(s);
        if (cells.length == 0) return false;
        for (String c : cells) {
            if (!c.trim().matches(":?-{3,}:?")) return false;
        }
        return true;
    }

    private static String[] splitRow(String line) {
        String s = line.trim();
        if (s.startsWith("|")) s = s.substring(1);
        if (s.endsWith("|")) s = s.substring(0, s.length() - 1);
        String[] parts = s.split("(?<!\\\\)\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("\\|", "|").trim();
        }
        return parts;
    }
}
