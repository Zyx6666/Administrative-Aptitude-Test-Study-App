package com.idiom.memo.core;

import com.idiom.memo.data.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 考频库分组与解锁规则：
 * 1. 按频次从高到低分组，同一频次为一组；
 * 2. 最高频组永远可刷，后续组需前面的组全部达标才解锁；
 * 3. 达标条件：组内没有「不会」，且「模糊」数量 < 「会了」数量 × 阈值；
 * 4. 解锁状态实时推导，因此高频组回退为「不会」时，所有低频组自动一并锁回。
 */
public final class FrequencyUnlock {
    public static final int DEFAULT_THRESHOLD = 30;

    public static class Group {
        public final int frequency;
        public final int total;
        public final int unknown;
        public final int fuzzy;
        public final int known;
        public final boolean achieved;
        public final boolean unlocked;

        public Group(int frequency, int total, int unknown, int fuzzy, int known,
                     boolean achieved, boolean unlocked) {
            this.frequency = frequency;
            this.total = total;
            this.unknown = unknown;
            this.fuzzy = fuzzy;
            this.known = known;
            this.achieved = achieved;
            this.unlocked = unlocked;
        }
    }

    private FrequencyUnlock() {
    }

    public static boolean achieved(int unknown, int fuzzy, int known, int thresholdPercent) {
        if (unknown > 0) return false;
        if (thresholdPercent <= 0) return fuzzy == 0;
        return (long) fuzzy * 100 < (long) known * thresholdPercent;
    }

    public static List<Group> plan(List<Entry> entries, int thresholdPercent) {
        Map<Integer, int[]> counts = new TreeMap<>(Collections.reverseOrder());
        if (entries != null) {
            for (Entry entry : entries) {
                int frequency = entry.frequency > 0 ? entry.frequency : 0;
                int[] c = counts.get(frequency);
                if (c == null) {
                    c = new int[3];
                    counts.put(frequency, c);
                }
                if (Entry.KNOWN.equals(entry.category)) c[2]++;
                else if (Entry.FUZZY.equals(entry.category)) c[1]++;
                else c[0]++; // unknown / unlearned 都算未掌握
            }
        }

        List<Group> groups = new ArrayList<>();
        boolean higherAllAchieved = true;
        for (Map.Entry<Integer, int[]> item : counts.entrySet()) {
            int[] c = item.getValue();
            int total = c[0] + c[1] + c[2];
            boolean achieved = achieved(c[0], c[1], c[2], thresholdPercent);
            boolean unlocked = groups.isEmpty() || higherAllAchieved;
            groups.add(new Group(item.getKey(), total, c[0], c[1], c[2], achieved, unlocked));
            higherAllAchieved = higherAllAchieved && achieved;
        }
        return groups;
    }
}
