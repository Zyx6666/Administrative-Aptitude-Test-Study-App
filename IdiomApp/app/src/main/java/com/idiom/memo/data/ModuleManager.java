package com.idiom.memo.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class ModuleManager {
    private static final String PREFS = "app_prefs";
    private static final String KEY_MODULE = "current_module";

    private ModuleManager() {
    }

    public static Module current(Context context) {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Module module = Module.fromId(sp.getString(KEY_MODULE, Module.VERBAL.id));
        return module.enabled ? module : Module.VERBAL;
    }

    public static void setCurrent(Context context, Module module) {
        if (!module.enabled) return;
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MODULE, module.id).apply();
    }
}
