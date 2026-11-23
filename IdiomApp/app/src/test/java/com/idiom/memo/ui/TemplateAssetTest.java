package com.idiom.memo.ui;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import com.idiom.memo.core.ImportService;
import com.idiom.memo.core.MarkdownParser;
import com.idiom.memo.core.FrequencyMarkdownParser;
import com.idiom.memo.data.Module;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TemplateAssetTest {
    private static byte[] read(Context context, String name) throws Exception {
        try (InputStream in = context.getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    @Test
    public void everyModuleHasReadableMdTemplate() throws Exception {
        Context context = RuntimeEnvironment.getApplication();

        for (Module module : Module.values()) {
            List<ImportService.Row> rows = MarkdownParser.parse(new String(
                    read(context, "templates/" + module.id + ".md"), StandardCharsets.UTF_8));
            assertEquals(module.displayName, 2, rows.size());
            assertEquals(module.displayName, "知识点", module.termLabel);
            assertEquals(module.displayName, "内容", module.contentLabel);
        }
    }

    @Test
    public void frequencyTemplateParsesThreeColumns() throws Exception {
        Context context = RuntimeEnvironment.getApplication();

        FrequencyMarkdownParser.Result result = FrequencyMarkdownParser.parse(new String(
                read(context, "templates/verbal_freq.md"), StandardCharsets.UTF_8));

        assertEquals(2, result.rows.size());
        assertEquals("差强人意", result.rows.get(0).term);
        assertEquals(20, result.rows.get(0).frequency);
        assertEquals(19, result.rows.get(1).frequency);
    }
}
