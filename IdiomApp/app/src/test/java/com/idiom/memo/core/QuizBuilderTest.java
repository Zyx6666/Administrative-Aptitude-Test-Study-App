package com.idiom.memo.core;

import static org.junit.Assert.assertEquals;

import com.idiom.memo.data.Entry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class QuizBuilderTest {
    private static Entry item(long id, String content) {
        Entry e = new Entry();
        e.id = id;
        e.term = "成语" + id;
        e.content = content;
        e.category = Entry.UNKNOWN;
        return e;
    }

    @Test
    public void buildsFourDistinctOptionsWithCorrectOne() {
        List<Entry> pool = new ArrayList<>();
        pool.add(item(1, "解释一"));
        pool.add(item(2, "解释二"));
        pool.add(item(3, "解释三"));
        pool.add(item(4, "解释四"));

        QuizBuilder.Quiz quiz = QuizBuilder.build(pool.get(0), pool, new Random(1));

        assertEquals(4, quiz.options.size());
        assertEquals(4, new HashSet<>(quiz.options).size());
        assertEquals("解释一", quiz.options.get(quiz.correctIndex));
    }

    @Test(expected = IllegalStateException.class)
    public void tooFewIdiomsThrows() {
        List<Entry> pool = new ArrayList<>();
        pool.add(item(1, "解释一"));
        pool.add(item(2, "解释二"));
        pool.add(item(3, "解释三"));
        QuizBuilder.build(pool.get(0), pool, new Random(1));
    }

    @Test(expected = IllegalStateException.class)
    public void duplicatedMeaningsThrow() {
        List<Entry> pool = new ArrayList<>();
        pool.add(item(1, "解释一"));
        pool.add(item(2, "解释一"));
        pool.add(item(3, "解释一"));
        pool.add(item(4, "解释一"));
        QuizBuilder.build(pool.get(0), pool, new Random(1));
    }
}
