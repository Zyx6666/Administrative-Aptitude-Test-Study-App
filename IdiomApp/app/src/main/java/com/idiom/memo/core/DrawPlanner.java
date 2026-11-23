package com.idiom.memo.core;

import com.idiom.memo.data.Entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DrawPlanner {
    public static List<Entry> plan(List<Entry> pool, int size,
                                   int ratioUnknown, int ratioFuzzy, int ratioKnown, Random rnd) {
        List<Entry> result = new ArrayList<>();
        if (pool == null || pool.isEmpty() || size <= 0) return result;

        List<Entry> u = new ArrayList<>();
        List<Entry> f = new ArrayList<>();
        List<Entry> k = new ArrayList<>();
        for (Entry i : pool) {
            if (Entry.KNOWN.equals(i.category)) k.add(i);
            else if (Entry.FUZZY.equals(i.category)) f.add(i);
            else u.add(i); // unknown 与 unlearned 都进入“不会”抽取池
        }
        Collections.shuffle(u, rnd);
        Collections.shuffle(f, rnd);
        Collections.shuffle(k, rnd);

        int sum = ratioUnknown + ratioFuzzy + ratioKnown;
        if (sum <= 0) {
            ratioUnknown = 50;
            ratioFuzzy = 30;
            ratioKnown = 20;
            sum = 100;
        }

        List<List<Entry>> groups = Arrays.asList(u, f, k);
        int[] used = new int[3];
        int[] desired = {
                Math.round(size * (float) ratioUnknown / sum),
                Math.round(size * (float) ratioFuzzy / sum),
                Math.round(size * (float) ratioKnown / sum)
        };
        for (int g = 0; g < 3; g++) {
            used[g] = Math.min(desired[g], groups.get(g).size());
            result.addAll(groups.get(g).subList(0, used[g]));
        }

        while (result.size() < size) {
            boolean added = false;
            for (int g = 0; g < 3 && result.size() < size; g++) {
                if (used[g] < groups.get(g).size()) {
                    result.add(groups.get(g).get(used[g]++));
                    added = true;
                }
            }
            if (!added) break;
        }

        Collections.shuffle(result, rnd);
        return result;
    }
}
