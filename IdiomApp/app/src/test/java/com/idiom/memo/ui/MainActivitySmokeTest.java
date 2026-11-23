package com.idiom.memo.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import com.idiom.memo.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivitySmokeTest {
    @Test
    public void launchesAndShowsCounts() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();

        assertNotNull(activity.findViewById(R.id.tvDue));
        assertNotNull(activity.findViewById(R.id.tvModule));
        assertTrue(activity.findViewById(R.id.btnCardMode).hasOnClickListeners());
        assertTrue(activity.findViewById(R.id.btnQuizMode).hasOnClickListeners());
        assertTrue(activity.findViewById(R.id.btnFreqLibrary).hasOnClickListeners());
    }
}
