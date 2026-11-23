package com.idiom.memo.core;

import static org.junit.Assert.assertTrue;

import com.idiom.memo.data.Entry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MarkdownExporterTest {
    @Test
    public void exportsTableWithStatsAndEscapes() {
        Entry item = new Entry();
        item.term = "基期量";
        item.content = "$基期量=\\frac{a|b}{c}$\n第二行";
        item.category = Entry.KNOWN;
        item.knownCount = 3;
        item.choiceCorrectCount = 2;
        List<Entry> items = new ArrayList<>();
        items.add(item);

        String md = MarkdownExporter.toMarkdown(items, "言语理解 · 全部知识点", "知识点", "内容");

        assertTrue(md.startsWith("# 言语理解 · 全部知识点"));
        assertTrue(md.contains("| 知识点 | 内容 | 分类 | 不会 |"));
        assertTrue(md.contains("| 基期量 |"));
        assertTrue(md.contains("\\|"));
        assertTrue(md.contains("<br>"));
        assertTrue(md.contains("| 会了 | 0 | 0 | 3 | 2 | 0 | 未安排 |"));
    }

    @Test
    public void emptyListHasHint() {
        String md = MarkdownExporter.toMarkdown(new ArrayList<>(), "空列表", "知识点", "内容");
        assertTrue(md.contains("（暂无数据）"));
    }
}
