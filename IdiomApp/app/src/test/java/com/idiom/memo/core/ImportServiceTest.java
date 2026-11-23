package com.idiom.memo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportServiceTest {
    @Test
    public void classifiesRows() {
        Map<String, ImportService.Existing> existing = new HashMap<>();
        existing.put("差强人意", new ImportService.Existing(1, "旧解释"));
        existing.put("不刊之论", new ImportService.Existing(2, "相同解释"));

        List<ImportService.Row> rows = Arrays.asList(
                new ImportService.Row("空穴来风", "新解释"),
                new ImportService.Row("不刊之论", "相同解释"),
                new ImportService.Row("差强人意", "新解释"),
                new ImportService.Row("  ", "解释"),
                new ImportService.Row("有解释", "  ")
        );

        ImportService.Plan plan = ImportService.plan(rows, existing);

        assertEquals(1, plan.fresh.size());
        assertEquals("空穴来风", plan.fresh.get(0).term);
        assertEquals(1, plan.sameCount);
        assertEquals(1, plan.conflicts.size());
        assertEquals(1L, plan.conflicts.get(0).id);
        assertEquals("旧解释", plan.conflicts.get(0).oldContent);
        assertEquals(2, plan.invalid);
    }

    @Test
    public void trimsTermAndContent() {
        ImportService.Plan plan = ImportService.plan(
                Arrays.asList(new ImportService.Row("  画蛇添足  ", "  多此一举  ")),
                new HashMap<>());

        assertEquals(1, plan.fresh.size());
        assertEquals("画蛇添足", plan.fresh.get(0).term);
        assertEquals("多此一举", plan.fresh.get(0).content);
    }

    @Test
    public void sameTermTwiceInFileBecomesConflict() {
        List<ImportService.Row> rows = Arrays.asList(
                new ImportService.Row("一举两得", "解释A"),
                new ImportService.Row("一举两得", "解释A"),
                new ImportService.Row("一举两得", "解释B")
        );

        ImportService.Plan plan = ImportService.plan(rows, new HashMap<>());

        assertEquals(1, plan.fresh.size());
        assertEquals(1, plan.sameCount);
        assertEquals(1, plan.conflicts.size());
        assertTrue(plan.conflicts.get(0).id == 0);
    }
}
