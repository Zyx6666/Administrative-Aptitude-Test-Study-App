package com.idiom.memo.core;

import com.idiom.memo.data.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuizBuilder {
    public static class Quiz {
        public final Entry target;
        public final List<String> options;
        public final int correctIndex;

        public Quiz(Entry target, List<String> options, int correctIndex) {
            this.target = target;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    public static Quiz build(Entry target, List<Entry> pool, Random rnd) {
        if (pool == null || pool.size() < 4) {
            throw new IllegalStateException("知识条目太少，无法生成选择题");
        }
        String correct = target.content == null ? "" : target.content.trim();
        List<String> cand = new ArrayList<>();
        for (Entry i : pool) {
            if (i.id == target.id) continue;
            String m = i.content == null ? "" : i.content.trim();
            if (m.isEmpty() || m.equals(correct) || cand.contains(m)) continue;
            cand.add(m);
        }
        if (correct.isEmpty() || cand.size() < 3) {
            throw new IllegalStateException("知识条目太少，无法生成选择题");
        }
        Collections.shuffle(cand, rnd);
        List<String> options = new ArrayList<>();
        options.add(correct);
        options.addAll(cand.subList(0, 3));
        Collections.shuffle(options, rnd);
        int correctIndex = options.indexOf(correct);
        return new Quiz(target, options, correctIndex);
    }
}
