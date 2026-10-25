package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Fork (Thomas): Home and the night screen start with the fork's defaults (config.xml) without crashing. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w960dp-h540dp-land-xhdpi")
public class StartupTest {

    @Test
    public void homeStartsWithWeatherStatsAndNightOn() {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertNotNull(a.findViewById(R.id.weather));
        assertEquals(View.GONE, a.findViewById(R.id.weather).getVisibility());   // nothing cached yet
        assertNotNull(a.findViewById(R.id.stats));
        Robolectric.flushForegroundThreadScheduler();
        a.onPause();
        a.onResume();
    }

    /** The phone app pushes a theme (config.json and a wallpaper) before the takeover; Home must start on it too. */
    @Test
    public void homeStartsOnAPushedTheme() throws Exception {
        android.content.Context ctx = org.robolectric.RuntimeEnvironment.getApplication();
        java.io.File dir = ctx.getExternalFilesDir(null);
        assertNotNull(dir);
        java.nio.file.Files.write(new java.io.File(dir, "config.json").toPath(), ("{\n  \"dock_apps\": [\"com.netflix.ninja\", \"com.amazon.amazonvideo.livingroom\", \"com.teamsmart.videomanager.tv\"],\n"
                + "  \"columns\": 5,\n  \"rows_below_dock\": 1,\n  \"background_color\": \"#070A14\",\n  \"wallpaper\": true,\n  \"wallpaper_dim\": 30,\n"
                + "  \"greeting_name\": \"Thomas\",\n  \"module_cards\": true,\n  \"weather_city\": \"Saint-Gervais-les-Bains\",\n  \"night_from\": 21\n}\n").getBytes("UTF-8"));
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(64, 36, android.graphics.Bitmap.Config.ARGB_8888);
        b.eraseColor(0xFF203050);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir, "wallpaper.jpg"))) {
            b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out);
        }
        com.gathin.tvhome.Config cfg = com.gathin.tvhome.Config.load(ctx);
        assertEquals("Saint-Gervais-les-Bains", cfg.weatherCity);
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertNotNull(a.findViewById(R.id.weather));
        Robolectric.flushForegroundThreadScheduler();
        a.onPause();
    }

    @Test
    public void nightScreenStartsWithoutAReading() {
        Intent i = new Intent(org.robolectric.RuntimeEnvironment.getApplication(), ClockActivity.class).putExtra(ClockActivity.EXTRA_NIGHT, true);
        ClockActivity c = Robolectric.buildActivity(ClockActivity.class, i).setup().get();
        assertEquals(View.GONE, c.findViewById(R.id.dreamTomorrow).getVisibility());
        c.onPause();
    }

    @Test
    public void plainClockScreenStillStarts() {
        ClockActivity c = Robolectric.buildActivity(ClockActivity.class).setup().get();
        assertNotNull(c.findViewById(R.id.dreamContent));
    }
}
