package com.idiom.memo.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 考频成语库的三列 Markdown 解析：成语 | 解释 | 频次。
 */
public final class FrequencyMarkdownParser {
    public static class Row {
        public final String term;
        public final String content;
        public final int frequency;

        public Row(String term, String content, int frequency) {
            this.term = term;
            this.content = content;
            this.frequency = frequency;
        }
    }

    public static class Result {
        public final List<Row> rows = new ArrayList<>();
        public int invalid;
    }

    private FrequencyMarkdownParser() {
    }

    public static Result parse(String markdown) {
        Result result = new Result();
        if (markdown == null || markdown.trim().isEmpty()) return result;

        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        Map<String, Row> merged = new LinkedHashMap<>();
        int i = 0;
        while (i < lines.length) {
            if (!lines[i].contains("|")) {
                i++;
                continue;
            }
            int end = i;
            while (end < lines.length && lines[end].contains("|")) end++;
            parseBlock(lines, i, end, merged, result);
            i = end;
        }
        result.rows.addAll(merged.values());
        return result;
    }

    private static void parseBlock(String[] lines, int start, int end,
                                   Map<String, Row> merged, Result result) {
        String[] header = splitRow(lines[start]);
        int termIdx = -1;
        int contentIdx = -1;
        int freqIdx = -1;
        for (int c = 0; c < header.length; c++) {
            String h = header[c].trim().toLowerCase();
            if (termIdx < 0 && isTermHeader(h)) termIdx = c;
            if (contentIdx < 0 && isContentHeader(h)) contentIdx = c;
            if (freqIdx < 0 && isFrequencyHeader(h)) freqIdx = c;
        }

        int dataStart;
        if (termIdx >= 0 && contentIdx >= 0 && freqIdx >= 0
                && termIdx != contentIdx && termIdx != freqIdx && contentIdx != freqIdx) {
            dataStart = start + 1;
        } else if (header.length == 3) {
            // 没有可识别的表头时，按固定三列处理
            termIdx = 0;
            contentIdx = 1;
            freqIdx = 2;
            dataStart = isSeparator(lines[start]) ? start + 1 : start;
        } else {
            return;
        }

        for (int j = dataStart; j < end; j++) {
            String line = lines[j];
            if (isSeparator(line)) continue;
            String[] cells = splitRow(line);
            if (termIdx >= cells.length || contentIdx >= cells.length || freqIdx >= cells.length) {
                result.invalid++;
                continue;
            }
            String term = MarkdownNormalizer.normalize(cells[termIdx]);
            String content = MarkdownNormalizer.normalize(cells[contentIdx]);
            int frequency = parseFrequency(cells[freqIdx]);
            if (term.isEmpty() || content.isEmpty() || frequency <= 0) {
                result.invalid++;
                continue;
            }
            Row old = merged.get(term);
            if (old == null) {
                merged.put(term, new Row(term, content, frequency));
            } else {
                // 同一文件内按成语去重：解释用最新值，频次取最大值
                merged.put(term, new Row(term, content, Math.max(old.frequency, frequency)));
            }
        }
    }

    private static int parseFrequency(String cell) {
        Matcher matcher = Pattern.compile("\\d+").matcher(cell == null ? "" : cell);
        if (!matcher.find()) return -1;
        try {
            int value = Integer.parseInt(matcher.group());
            return value > 0 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean isTermHeader(String h) {
        return h.contains("成语") || h.contains("词条") || h.contains("知识点")
                || h.contains("名称") || h.contains("标题")
                || h.equals("term") || h.equals("word");
    }

    private static boolean isContentHeader(String h) {
        return h.contains("解释") || h.contains("释义") || h.contains("含义")
                || h.contains("意思") || h.contains("内容") || h.contains("定义")
                || h.contains("说明") || h.equals("content") || h.equals("meaning")
                || h.equals("explanation");
    }

    private static boolean isFrequencyHeader(String h) {
        return h.contains("频次") || h.contains("考频") || h.contains("频度")
                || h.contains("次数") || h.contains("频率") || h.contains("考查次数")
                || h.equals("frequency") || h.equals("count");
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
