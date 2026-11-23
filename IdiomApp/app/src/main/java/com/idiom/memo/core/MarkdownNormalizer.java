package com.idiom.memo.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownNormalizer {
    private static final Pattern SINGLE_DOLLAR =
            Pattern.compile("(?<!\\$)\\$(?!\\$)([^$\\n]+?)(?<!\\$)\\$(?!\\$)");

    private MarkdownNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) return "";
        String s = text;

        // LaTeX 定界符统一成 $$...$$（JLatexMath 的行内解析器使用双美元）
        s = s.replace("\\[", "$$").replace("\\]", "$$");
        s = s.replace("\\(", "$$").replace("\\)", "$$");

        // 行内单美元统一成双美元
        Matcher matcher = SINGLE_DOLLAR.matcher(s);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer,
                    Matcher.quoteReplacement("$$" + matcher.group(1) + "$$"));
        }
        matcher.appendTail(buffer);
        s = buffer.toString();

        // 加粗按普通文本展示
        s = s.replace("**", "").replace("__", "");

        // 标题、引用、列表、代码块
        s = s.replaceAll("(?m)^[ \\t]{0,3}#{1,6}[ \\t]*", "");
        s = s.replaceAll("(?m)^[ \\t]*>[ \\t]?", "");
        s = s.replaceAll("(?m)^[ \\t]*[-*+][ \\t]+", "");
        s = s.replaceAll("(?m)^[ \\t]*\\d+\\.[ \\t]+", "");
        s = s.replaceAll("(?m)^[ \\t]*```.*$", "");
        s = s.replace("`", "");

        // 链接、图片
        s = s.replaceAll("!\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
        s = s.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");

        // 常见 HTML
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</?p>", "\n");

        // 表格分隔线
        s = s.replaceAll("(?m)^[ \\t]*\\|?[ \\t]*:?-{3,}:?[ \\t]*(\\|[ \\t]*:?-{3,}:?[ \\t]*)+\\|?[ \\t]*$", "");

        s = s.replaceAll("[ \\t]+\\n", "\n");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }
}
