package com.idiom.memo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class MarkdownParserTest {
    @Test
    public void parsesTableAndStripsBold() {
        String md = "| **成语** | **解释** |\n| --- | --- |\n| **差强人意** | 大体上还能使人满意 |\n| 不刊之论 | 指不能改动或不可磨灭的言论 |\n";

        List<ImportService.Row> rows = MarkdownParser.parse(md);

        assertEquals(2, rows.size());
        assertEquals("差强人意", rows.get(0).term);
        assertEquals("大体上还能使人满意", rows.get(0).content);
        assertFalse(rows.get(0).term.contains("**"));
    }

    @Test
    public void parsesHeadingStyle() {
        String md = "# 差强人意\n大体上还能使人满意，常被误用为“不满意”。\n\n## 不刊之论\n**指不能改动或不可磨灭的言论。**\n";

        List<ImportService.Row> rows = MarkdownParser.parse(md);

        assertEquals(2, rows.size());
        assertEquals("差强人意", rows.get(0).term);
        assertEquals("不刊之论", rows.get(1).term);
        assertFalse(rows.get(1).content.contains("**"));
    }

    @Test
    public void normalizesLatexDdelimitersWithoutDoubleEscaping() {
        assertEquals("$$a+b$$", MarkdownNormalizer.normalize("\\(a+b\\)"));
        assertEquals("$$x=\\frac{a}{b}$$",
                MarkdownNormalizer.normalize("\\[x=\\frac{a}{b}\\]"));
        assertEquals("$$基期量=\\frac{现期量}{1+增长率}$$",
                MarkdownNormalizer.normalize("$基期量=\\frac{现期量}{1+增长率}$"));
    }

    @Test
    public void stripsHeadingsListsAndLinks() {
        String normalized = MarkdownNormalizer.normalize("## 标题\n- 第一项\n1. 第二项\n[链接](https://example.com)");
        assertFalse(normalized.contains("#"));
        assertFalse(normalized.contains("- "));
        assertFalse(normalized.contains("](http"));
        assertTrue(normalized.contains("链接"));
    }
}
