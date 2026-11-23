package com.idiom.memo.core;

import android.content.Context;
import android.widget.TextView;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

public final class MarkdownRenderer {
    private static Markwon markwon;

    private MarkdownRenderer() {
    }

    public static void set(TextView view, String text) {
        String normalized = MarkdownNormalizer.normalize(text);
        if (normalized.isEmpty()) {
            view.setText("");
            return;
        }
        if (!normalized.contains("$")) {
            view.setText(normalized);
            return;
        }
        getMarkwon(view.getContext()).setMarkdown(view, normalized);
    }

    private static Markwon getMarkwon(Context context) {
        if (markwon == null) {
            Context app = context.getApplicationContext();
            float texSize = 18f * app.getResources().getDisplayMetrics().scaledDensity;
            markwon = Markwon.builder(app)
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(JLatexMathPlugin.create(texSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.blocksEnabled(true);
                        builder.theme()
                                .textColor(0xFF1B1B1F)
                                .inlineTextColor(0xFF1B1B1F)
                                .blockTextColor(0xFF1B1B1F);
                        builder.errorHandler((latex, error) -> {
                            android.util.Log.e("JLatex", "render error: " + latex, error);
                            return new android.graphics.drawable.ColorDrawable(0x22000000);
                        });
                    }))
                    .build();
        }
        return markwon;
    }
}
