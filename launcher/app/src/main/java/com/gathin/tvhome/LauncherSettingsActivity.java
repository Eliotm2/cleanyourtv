package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private TextView weatherCityChoice;   // fork (Thomas)
    private TextView remoteKeysChoice;    // fork (Thomas)
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        remoteKeysChoice = findViewById(R.id.remoteKeysChoice);
        remoteKeysChoice.setOnClickListener(v -> showRemoteKeys());
        weatherCityChoice = findViewById(R.id.weatherCityChoice);
        weatherCityChoice.setOnClickListener(v -> showWeatherCityDialog());
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY).remove(Config.WEATHER_CITY_KEY).commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        weatherCityChoice.setText(getString(R.string.weather_city_state, Config.load(this).weatherCity));
        remoteKeysChoice.setText(getString(R.string.remote_keys_state, Config.learntAppKeys(choices).size()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(OpenOnStart.KEY) || choices.contains(Config.WEATHER_CITY_KEY);
    }

    // ---- fork (Thomas): remote buttons learnt on the TV ---------------------------------------------

    private final android.os.Handler learnHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    /** The list: every learnt button (tap to remove) and "Add a button…". */
    private void showRemoteKeys() {
        if (!HomeKeyService.isRunning()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.remote_keys_title)
                    .setMessage(R.string.remote_keys_helper_off)
                    .setPositiveButton(R.string.remote_keys_open_accessibility, (d, w) -> open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }
        List<String> entries = Config.learntAppKeys(choices);
        List<String> labels = new ArrayList<>();
        for (String e : entries) {
            String[] parts = e.split(":");
            String key = parts[0].equals("scan") && parts.length >= 3 ? "scan " + parts[1] : keyName(parts[0]);
            labels.add(getString(R.string.remote_keys_entry, key, appLabel(parts[parts.length - 1])));
        }
        labels.add(getString(R.string.remote_keys_add));
        new AlertDialog.Builder(this)
                .setTitle(R.string.remote_keys_title)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    if (which == entries.size()) learnRemoteKey();
                    else confirmRemove(entries, which);
                })
                .show();
    }

    private void confirmRemove(List<String> entries, int index) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.remote_keys_remove))
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    List<String> kept = new ArrayList<>(entries);
                    kept.remove(index);
                    saveRemoteKeys(kept);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Waits 20 s for the helper to hand over the next non-navigation key, then asks which app it opens. */
    private void learnRemoteKey() {
        AlertDialog waiting = new AlertDialog.Builder(this)
                .setTitle(R.string.remote_keys_add)
                .setMessage(R.string.remote_keys_press)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        Runnable timeout = () -> {
            HomeKeyService.learner = null;
            if (waiting.isShowing()) {
                waiting.dismiss();
                Toast.makeText(this, R.string.remote_keys_timeout, Toast.LENGTH_LONG).show();
            }
        };
        waiting.setOnDismissListener(d -> { HomeKeyService.learner = null; learnHandler.removeCallbacks(timeout); });
        learnHandler.postDelayed(timeout, 20_000);
        HomeKeyService.learner = (keyCode, scanCode) -> learnHandler.post(() -> {
            HomeKeyService.learner = null;
            learnHandler.removeCallbacks(timeout);
            if (waiting.isShowing()) waiting.dismiss();
            // A keycode Android names is matched by keycode; an unmapped button (KEYCODE_UNKNOWN) by its scancode.
            String key = keyCode > 0 ? String.valueOf(keyCode) : scanCode > 0 ? "scan:" + scanCode : null;
            if (key == null) {
                Toast.makeText(this, R.string.remote_keys_timeout, Toast.LENGTH_LONG).show();
                return;
            }
            pickAppFor(key, keyCode > 0 ? keyName(String.valueOf(keyCode)) : "scan " + scanCode);
        });
    }

    private void pickAppFor(String key, String keyLabel) {
        PackageManager pm = getPackageManager();
        java.util.Map<String, String> apps = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                if (ri.activityInfo == null || ri.activityInfo.packageName.equals(getPackageName())) continue;
                apps.putIfAbsent(String.valueOf(ri.loadLabel(pm)), ri.activityInfo.packageName);
            }
        }
        List<String> names = new ArrayList<>(apps.keySet());
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_keys_pick_app, keyLabel))
                .setItems(names.toArray(new String[0]), (d, which) -> {
                    String pkg = apps.get(names.get(which));
                    List<String> kept = new ArrayList<>();
                    // One app per button: an earlier entry for the same key goes.
                    for (String e : Config.learntAppKeys(choices)) if (!e.startsWith(key + ":")) kept.add(e);
                    kept.add(key + ":" + pkg);
                    saveRemoteKeys(kept);
                    Toast.makeText(this, getString(R.string.remote_keys_saved, keyLabel, names.get(which)), Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveRemoteKeys(List<String> entries) {
        if (entries.isEmpty()) choices.edit().remove(Config.APP_KEYS_KEY).apply();
        else choices.edit().putString(Config.APP_KEYS_KEY, String.join(";", entries)).apply();
        HomeKeyService.reloadConfig();   // the helper reads its config once at bind
        changed();
        refresh();
    }

    private String keyName(String code) {
        try {
            String name = android.view.KeyEvent.keyCodeToString(Integer.parseInt(code));
            return name.startsWith("KEYCODE_") ? name.substring(8).replace('_', ' ') : name;
        } catch (NumberFormatException e) {
            return code;
        }
    }

    private String appLabel(String pkg) {
        try {
            return String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /** Fork (Thomas): the weather city, typed with the remote's keyboard; empty puts the theme's or config.xml's back. */
    private void showWeatherCityDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.weather_city_hint);
        input.setText(choices.getString(Config.WEATHER_CITY_KEY, ""));
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(R.string.weather_city_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String city = input.getText().toString().trim();
                    if (city.isEmpty()) choices.edit().remove(Config.WEATHER_CITY_KEY).apply();
                    else choices.edit().putString(Config.WEATHER_CITY_KEY, city).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
