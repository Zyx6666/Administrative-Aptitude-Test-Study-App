package com.idiom.memo.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ModuleTest {
    @Test
    public void allSixModulesAreEnabled() {
        assertEquals(6, Module.values().length);
        for (Module module : Module.values()) {
            assertTrue(module.displayName, module.enabled);
        }
    }

    @Test
    public void moduleSwitchPersists() {
        Context context = RuntimeEnvironment.getApplication();
        assertEquals(Module.VERBAL, ModuleManager.current(context));

        ModuleManager.setCurrent(context, Module.QUANTITY);
        assertEquals(Module.QUANTITY, ModuleManager.current(context));

        ModuleManager.setCurrent(context, Module.CURRENT_AFFAIRS);
        assertEquals(Module.CURRENT_AFFAIRS, ModuleManager.current(context));
    }

    @Test
    public void moduleLabelsAndHintsAreConfigured() {
        for (Module module : Module.values()) {
            assertTrue(!module.termLabel.isEmpty());
            assertTrue(!module.contentLabel.isEmpty());
            assertTrue(module.uploadHint.startsWith("注：建议通过豆包"));
        }
        assertEquals("知识点", Module.VERBAL.termLabel);
        assertEquals("内容", Module.VERBAL.contentLabel);
        assertEquals("我的错题本", Module.VERBAL.allTitle());
        assertEquals("全部错题", Module.VERBAL.allItemsLabel());
        assertEquals("错题状态", Module.VERBAL.statsTitle());
        assertEquals("刷内容卡片（错题）", Module.VERBAL.cardModeTitle());
        assertEquals("做选择题练习（错题）", Module.VERBAL.quizModeTitle());
        assertEquals("上传 MD 导入知识点（错题模块）", Module.VERBAL.uploadEntryLabel());
        assertEquals("点击选择 .md 文件（只针对错题）", Module.VERBAL.pickFileTitle());
        assertEquals("下载言语理解导入模板（错题）.md", Module.VERBAL.templateFileName());
        assertEquals("我的知识库", Module.QUANTITY.allTitle());
        assertEquals("全部知识点", Module.QUANTITY.allItemsLabel());
        assertEquals("学习状态", Module.QUANTITY.statsTitle());
        assertEquals("刷内容卡片", Module.QUANTITY.cardModeTitle());
        assertEquals("做选择题练习", Module.QUANTITY.quizModeTitle());
        assertEquals("上传 MD 导入知识点", Module.QUANTITY.uploadEntryLabel());
        assertEquals("点击选择 .md 文件", Module.QUANTITY.pickFileTitle());
        assertEquals("下载数量关系导入模板.md", Module.QUANTITY.templateFileName());
        assertEquals("知识点", Module.QUANTITY.termLabel);
        assertEquals("内容", Module.QUANTITY.contentLabel);
        for (Module module : Module.values()) {
            assertTrue(module.uploadHint.contains("建议上传md文档"));
        }
    }
}
