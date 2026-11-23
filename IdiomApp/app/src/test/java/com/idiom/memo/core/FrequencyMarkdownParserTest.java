package com.idiom.memo.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FrequencyMarkdownParserTest {
    @Test
    public void parsesThreeColumnTable() {
        String md = "| 成语 | 解释 | 频次 |\n"
                + "| --- | --- | --- |\n"
                + "| 差强人意 | 大体上还能使人满意 | 20 |\n"
                + "| 不刊之论 | 不能改动或不可磨灭的言论 | 19 |\n";

        FrequencyMarkdownParser.Result result = FrequencyMarkdownParser.parse(md);
        assertEquals(2, result.rows.size());
        assertEquals(0, result.invalid);
        assertEquals("差强人意", result.rows.get(0).term);
        assertEquals("大体上还能使人满意", result.rows.get(0).content);
        assertEquals(20, result.rows.get(0).frequency);
        assertEquals(19, result.rows.get(1).frequency);
    }

    @Test
    public void parsesHeaderlessThreeColumnTable() {
        String md = "| 首鼠两端 | 迟疑不决或动摇不定 | 18 |\n"
                + "| 万人空巷 | 形容庆祝、欢迎的盛况 | 18 次 |\n";

        FrequencyMarkdownParser.Result result = FrequencyMarkdownParser.parse(md);
        assertEquals(2, result.rows.size());
        assertEquals(18, result.rows.get(0).frequency);
        assertEquals(18, result.rows.get(1).frequency);
    }

    @Test
    public void mergesSameTermByMaxFrequency() {
        String md = "| 成语 | 解释 | 频次 |\n"
                + "| --- | --- | --- |\n"
                + "| 画蛇添足 | 比喻多此一举 | 17 |\n"
                + "| 画蛇添足 | 比喻多此一举，反而不好 | 20 |\n";

        FrequencyMarkdownParser.Result result = FrequencyMarkdownParser.parse(md);
        assertEquals(1, result.rows.size());
        assertEquals(20, result.rows.get(0).frequency);
        assertEquals("比喻多此一举，反而不好", result.rows.get(0).content);
    }

    @Test
    public void skipsRowsWithoutValidFrequency() {
        String md = "| 成语 | 解释 | 频次 |\n"
                + "| --- | --- | --- |\n"
                + "| 空穴来风 | 消息并非完全没有根据 | 无 |\n"
                + "| 首鼠两端 | 迟疑不决或动摇不定 | 0 |\n"
                + "| 万人空巷 | 形容庆祝、欢迎的盛况 | 16 |\n";

        FrequencyMarkdownParser.Result result = FrequencyMarkdownParser.parse(md);
        assertEquals(1, result.rows.size());
        assertEquals(2, result.invalid);
        assertEquals("万人空巷", result.rows.get(0).term);
    }

    @Test
    public void ignoresNonTableText() {
        FrequencyMarkdownParser.Result result =
                FrequencyMarkdownParser.parse("这是一段普通文本\n没有表格");
        assertEquals(0, result.rows.size());
    }
}
