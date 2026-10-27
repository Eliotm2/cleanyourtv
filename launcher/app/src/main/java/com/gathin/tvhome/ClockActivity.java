package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * The clock screensaver shown by Home itself after home_idle_clock_minutes without input. Some TVs' own
 * screens (CVTE's live TV) do not keep the screen on, so the system screensaver would cut into programmes;
 * this one only ever starts from Home. Any key returns to Home.
 *
 * Fork (Thomas): with EXTRA_NIGHT the same screen is the night mode: plain black, no glow, the clock dimmed,
 * the backlight asked for its lowest level, and tomorrow's weather from the last reading under the date.
 */
public class ClockActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }


    static final String EXTRA_NIGHT = "night";

    private ClockScene scene;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dream_clock);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        scene = new ClockScene(getWindow().getDecorView());
        if (getIntent().getBooleanExtra(EXTRA_NIGHT, false)) {
            setUpNight();
        } else {
            scene.useHomeWallpaper(this);
        }
    }

    private void setUpNight() {
        scene.plainBlack();
        View content = findViewById(R.id.dreamContent);
        content.setAlpha(0.45f);
        scene.setDimmed(0.45f);
        Weather.Data w = Weather.cached(this);
        TextView tomorrow = findViewById(R.id.dreamTomorrow);
        if (w != null && w.tomorrowMax != null && w.tomorrowMin != null && w.tomorrowCode != null) {
            String desc = getResources().getStringArray(R.array.weather_desc)[Weather.descIndex(w.tomorrowCode)];
            Locale locale = getResources().getConfiguration().getLocales().get(0);
            tomorrow.setText(getString(R.string.weather_tomorrow, desc.toLowerCase(locale), w.tomorrowMax, w.tomorrowMin));
            tomorrow.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        scene.start();
    }

    @Override
    protected void onPause() {
        scene.stop();
        super.onPause();
        finish();   // never left behind in the back stack
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // The key that wakes the clock should not also act on Home, so it is consumed here.
        if (event.getAction() == KeyEvent.ACTION_UP) finish();
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        finish();
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
