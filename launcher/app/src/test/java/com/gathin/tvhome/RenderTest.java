package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/** Fork (Thomas): draws Home with a cached weather reading and stats into app/screenshots/home.png, to look at. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "fr-rFR-w960dp-h540dp-land-xhdpi")
public class RenderTest {

    @Test
    public void drawHome() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        Weather.Data d = new Weather.Data();
        d.city = "Saint-Gervais-les-Bains"; d.temp = 10; d.code = 3; d.max = 11; d.min = 4; d.snowLine = 2100;
        d.sunrise = "07:03"; d.sunset = "17:30"; d.tomorrowMax = 9; d.tomorrowMin = 2; d.tomorrowCode = 71;
        d.fetchedAt = System.currentTimeMillis();
        ctx.getSharedPreferences("weather", Context.MODE_PRIVATE).edit()
                .putString("last", d.toJson().toString()).putString("last_query", "Saint-Gervais-les-Bains").apply();
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        a.getClass().getDeclaredMethod("showStats", int[].class);   // exists
        java.lang.reflect.Method m = MainActivity.class.getDeclaredMethod("showStats", int[].class);
        m.setAccessible(true);
        m.invoke(a, (Object) new int[] {-1, 58, 16});
        View root = a.getWindow().getDecorView();
        int w = 1920, h = 1080;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(0xFF101010);
        root.draw(c);
        File out = new File(System.getProperty("user.dir"), "screenshots/home.png");
        out.getParentFile().mkdirs();
        try (FileOutputStream f = new FileOutputStream(out)) { b.compress(Bitmap.CompressFormat.PNG, 100, f); }
    }
}
