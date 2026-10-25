package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Fork (Thomas): draws Home with fake apps, a cached weather reading and stats into app/screenshots/, to look at.
 * Two looks: the dock at the bottom with the grid below it (home.png), and every app in one row (home-single-row.png).
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "fr-rFR-w960dp-h540dp-land-xhdpi")
public class RenderTest {

    private static final String[][] APPS = {
            {"com.netflix.ninja", "Netflix", "E50914"}, {"com.amazon.amazonvideo.livingroom", "Prime Video", "1A98FF"},
            {"com.canal.android.canal", "Canal+", "222222"}, {"com.spotify.tv.android", "Spotify", "1DB954"},
            {"tv.arte.plus7", "Arte", "FF4D00"}, {"fr.francetv.apps.tv", "France TV", "1F2A44"},
            {"com.disney.disneyplus", "Disney+", "0E2A6B"}, {"com.teamsmart.videomanager.tv", "SmartTube", "FFFFFF"},
            {"com.android.gallery3d", "Gallery", "3E7BFA"}, {"com.google.android.videos", "Films", "6A4CFF"},
    };

    private void fakeApps(Context ctx) {
        ShadowPackageManager spm = Shadows.shadowOf(ctx.getPackageManager());
        for (String[] a : APPS) {
            PackageInfo pi = new PackageInfo();
            pi.packageName = a[0];
            pi.applicationInfo = new ApplicationInfo();
            pi.applicationInfo.packageName = a[0];
            pi.applicationInfo.name = a[1];
            spm.installPackage(pi);
            ActivityInfo ai = new ActivityInfo();
            ai.packageName = a[0];
            ai.name = a[0] + ".Main";
            ai.exported = true;
            ai.applicationInfo = pi.applicationInfo;
            ai.nonLocalizedLabel = a[1];
            ResolveInfo ri = new ResolveInfo();
            ri.activityInfo = ai;
            ri.nonLocalizedLabel = a[1];
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            spm.addResolveInfoForIntent(i, ri);
            spm.addActivityIfNotPresent(new ComponentName(a[0], ai.name));
            spm.setApplicationIcon(a[0], new ColorDrawable((int) Long.parseLong("FF" + a[2], 16)));
        }
    }

    private void overlay(Context ctx, String json) throws Exception {
        File dir = ctx.getExternalFilesDir(null);
        Files.write(new File(dir, "config.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    private void weather(Context ctx) {
        Weather.Data d = new Weather.Data();
        d.city = "Saint-Gervais-les-Bains"; d.temp = 10; d.code = 3; d.max = 11; d.min = 4; d.snowLine = 2100;
        d.sunrise = "07:03"; d.sunset = "17:30"; d.tomorrowMax = 9; d.tomorrowMin = 2; d.tomorrowCode = 71;
        d.fetchedAt = System.currentTimeMillis();
        ctx.getSharedPreferences("weather", Context.MODE_PRIVATE).edit()
                .putString("last", d.toJson().toString()).putString("last_query", "Saint-Gervais-les-Bains").apply();
    }

    private void draw(String name) throws Exception {
        org.robolectric.android.controller.ActivityController<MainActivity> ctl = Robolectric.buildActivity(MainActivity.class).setup().visible();
        MainActivity a = ctl.get();
        // The app list loads off the main thread; let it land, then run every main-thread task (layout, animations).
        for (int i = 0; i < 6; i++) {
            Thread.sleep(300);
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        java.lang.reflect.Method stop = MainActivity.class.getDeclaredMethod("stopStats");
        stop.setAccessible(true);
        stop.invoke(a);   // the real reader (this build box's /proc) must not overwrite the figures below
        java.lang.reflect.Method m = MainActivity.class.getDeclaredMethod("showStats", int[].class);
        m.setAccessible(true);
        m.invoke(a, (Object) new int[] {-1, 58, 16});
        View root = a.getWindow().getDecorView();
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        int w = root.getWidth(), h = root.getHeight();
        if (w == 0 || h == 0) throw new AssertionError("decor not laid out: " + w + "x" + h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(0xFF101010);
        root.draw(c);
        File out = new File(System.getProperty("user.dir"), "screenshots/" + name);
        out.getParentFile().mkdirs();
        try (FileOutputStream f = new FileOutputStream(out)) { b.compress(Bitmap.CompressFormat.PNG, 100, f); }
        ctl.pause().stop().destroy();
    }

    private static final String DOCK = "\"dock_apps\": [\"com.netflix.ninja\", \"com.amazon.amazonvideo.livingroom\", \"com.canal.android.canal\", \"com.spotify.tv.android\"], \"greeting_name\": \"Thomas\"";

    @Test
    public void dockAtTheBottom() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        fakeApps(ctx); weather(ctx);
        overlay(ctx, "{" + DOCK + ", \"rows_below_dock\": 0}");
        draw("home.png");
    }

    @Test
    public void oneRowPeeking() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        fakeApps(ctx); weather(ctx);
        overlay(ctx, "{" + DOCK + ", \"rows_below_dock\": 1}");
        draw("home-row-below.png");
    }

    @Test
    public void singleRow() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        fakeApps(ctx); weather(ctx);
        overlay(ctx, "{" + DOCK + ", \"single_row\": true}");
        draw("home-single-row.png");
    }
}
