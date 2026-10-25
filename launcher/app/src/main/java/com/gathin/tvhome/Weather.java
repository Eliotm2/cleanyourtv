package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Fork addition (Thomas): the weather for one place from Open-Meteo, preferring MeteoSwiss' ICON-CH models
 * (ICON-CH1 at 1 km for the next 33 h, then ICON-CH2), which cover the French Alps. No key, no account.
 * Everything here blocks: call it from a background thread. The last reading is cached in SharedPreferences
 * so Home shows it at once after a restart and the night screen can show tomorrow offline.
 */
final class Weather {

    private static final String PREFS = "weather";
    private static final String[] MODELS = {"meteoswiss_icon_seamless", "meteoswiss_icon_ch2", ""};
    private static final int TIMEOUT_MS = 10_000;

    /** One reading. Temperatures in °C, rounded; null fields were not in the model's answer. */
    static final class Data {
        String city = "";
        int temp, code, max, min;
        boolean isDay = true;
        /** Rain-snow line in metres at this hour, rounded to 100 m; null if the model has none. */
        Integer snowLine;
        /** "HH:mm" local, or null. */
        String sunrise, sunset;
        Integer tomorrowMax, tomorrowMin, tomorrowCode;
        String source = "Open-Meteo";
        /** When this was fetched, System.currentTimeMillis(). */
        long fetchedAt;

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("city", city).put("temp", temp).put("code", code).put("isDay", isDay).put("max", max).put("min", min)
                        .put("source", source).put("fetchedAt", fetchedAt);
                if (snowLine != null) o.put("snow", snowLine);
                if (sunrise != null) o.put("sunrise", sunrise);
                if (sunset != null) o.put("sunset", sunset);
                if (tomorrowMax != null) o.put("tMax", tomorrowMax);
                if (tomorrowMin != null) o.put("tMin", tomorrowMin);
                if (tomorrowCode != null) o.put("tCode", tomorrowCode);
            } catch (JSONException ignored) {
            }
            return o;
        }

        static Data fromJson(JSONObject o) {
            Data d = new Data();
            d.city = o.optString("city", "");
            d.temp = o.optInt("temp");
            d.code = o.optInt("code");
            d.isDay = o.optBoolean("isDay", true);
            d.max = o.optInt("max");
            d.min = o.optInt("min");
            d.source = o.optString("source", "Open-Meteo");
            d.fetchedAt = o.optLong("fetchedAt");
            if (o.has("snow")) d.snowLine = o.optInt("snow");
            if (o.has("sunrise")) d.sunrise = o.optString("sunrise");
            if (o.has("sunset")) d.sunset = o.optString("sunset");
            if (o.has("tMax")) d.tomorrowMax = o.optInt("tMax");
            if (o.has("tMin")) d.tomorrowMin = o.optInt("tMin");
            if (o.has("tCode")) d.tomorrowCode = o.optInt("tCode");
            return d;
        }
    }

    private Weather() {
    }

    /** The last reading saved by {@link #fetch}, or null. Main thread is fine: one small preferences read. */
    static Data cached(Context ctx) {
        String s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last", null);
        if (s == null) return null;
        try {
            return Data.fromJson(new JSONObject(s));
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Fetches the weather for the configured place and caches it. lat/lon empty: the city name is looked up once
     * through Open-Meteo's geocoder and the coordinates are kept. Throws on any failure (no network, bad answer).
     */
    static Data fetch(Context ctx, String city, String lat, String lon) throws IOException, JSONException {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String name = city.trim();
        if (lat.isEmpty() || lon.isEmpty()) {
            if (name.equals(prefs.getString("geo_city", null))) {
                lat = prefs.getString("geo_lat", "");
                lon = prefs.getString("geo_lon", "");
                name = prefs.getString("geo_name", name);
            } else {
                String q = URLEncoder.encode(name, "UTF-8");
                JSONObject g = new JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=" + q
                        + "&count=1&language=fr&format=json"));
                JSONArray results = g.optJSONArray("results");
                if (results == null || results.length() == 0) throw new IOException("Unknown place: " + name);
                JSONObject r = results.getJSONObject(0);
                lat = String.valueOf(r.getDouble("latitude"));
                lon = String.valueOf(r.getDouble("longitude"));
                name = r.optString("name", name);
                prefs.edit().putString("geo_city", city.trim()).putString("geo_lat", lat).putString("geo_lon", lon)
                        .putString("geo_name", name).apply();
            }
        }
        Exception last = null;
        for (String model : MODELS) {
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,is_day"
                        + "&hourly=snowfall_height"
                        + "&daily=temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset"
                        + "&timezone=auto&forecast_days=2" + (model.isEmpty() ? "" : "&models=" + model);
                Data d = parse(name, get(url));
                d.source = model.startsWith("meteoswiss") ? "MétéoSuisse" : "Open-Meteo";
                d.fetchedAt = System.currentTimeMillis();
                prefs.edit().putString("last", d.toJson().toString()).apply();
                return d;
            } catch (IOException | JSONException e) {
                last = e;
            }
        }
        if (last instanceof IOException) throw (IOException) last;
        throw (JSONException) last;
    }

    /** Parses one forecast answer. Package-private for tests. */
    static Data parse(String city, String body) throws JSONException {
        JSONObject j = new JSONObject(body);
        JSONObject cur = j.getJSONObject("current");
        JSONObject daily = j.getJSONObject("daily");
        if (cur.isNull("temperature_2m") || cur.isNull("weather_code")) throw new JSONException("no current data");
        JSONArray maxArr = daily.getJSONArray("temperature_2m_max");
        JSONArray minArr = daily.getJSONArray("temperature_2m_min");
        if (maxArr.isNull(0) || minArr.isNull(0)) throw new JSONException("no daily data");
        Data d = new Data();
        d.city = city;
        d.temp = (int) Math.round(cur.getDouble("temperature_2m"));
        d.code = cur.getInt("weather_code");
        d.isDay = cur.optInt("is_day", 1) == 1;
        d.max = (int) Math.round(maxArr.getDouble(0));
        d.min = (int) Math.round(minArr.getDouble(0));
        // Rain-snow line at the current hour: "2025-10-25T17:00" is the hourly key of current "2025-10-25T17:15".
        JSONObject hourly = j.optJSONObject("hourly");
        JSONArray snow = hourly == null ? null : hourly.optJSONArray("snowfall_height");
        JSONArray times = hourly == null ? null : hourly.optJSONArray("time");
        String now = cur.optString("time", "");
        if (snow != null && times != null && now.length() >= 13) {
            String key = now.substring(0, 13);
            for (int i = 0; i < times.length() && i < snow.length(); i++) {
                if (times.optString(i, "").startsWith(key)) {
                    if (!snow.isNull(i)) d.snowLine = (int) (Math.round(snow.getDouble(i) / 100.0) * 100);
                    break;
                }
            }
        }
        d.sunrise = hhmm(daily.optJSONArray("sunrise"), 0);
        d.sunset = hhmm(daily.optJSONArray("sunset"), 0);
        d.tomorrowMax = dayInt(daily.optJSONArray("temperature_2m_max"), 1);
        d.tomorrowMin = dayInt(daily.optJSONArray("temperature_2m_min"), 1);
        d.tomorrowCode = dayInt(daily.optJSONArray("weather_code"), 1);
        return d;
    }

    private static String hhmm(JSONArray a, int i) {
        if (a == null || i >= a.length() || a.isNull(i)) return null;
        String s = a.optString(i, "");
        int t = s.indexOf('T');
        return t < 0 || s.length() < t + 6 ? null : s.substring(t + 1, t + 6);
    }

    private static Integer dayInt(JSONArray a, int i) {
        if (a == null || i >= a.length() || a.isNull(i)) return null;
        return (int) Math.round(a.optDouble(i));
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("User-Agent", "cleanyourtv-launcher-fork");
        try {
            int code = c.getResponseCode();
            if (code / 100 != 2) throw new IOException("HTTP " + code);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[8192];
                int n;
                while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            }
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    /** The index into the weather_desc string array for a WMO weather code. */
    static int descIndex(int code) {
        switch (code) {
            case 0: return 0;
            case 1: return 1;
            case 2: return 2;
            case 3: return 3;
            case 45: case 48: return 4;
            case 51: case 53: case 55: return 5;
            case 56: case 57: return 6;
            case 61: return 7;
            case 63: return 8;
            case 65: return 9;
            case 66: case 67: return 10;
            case 71: return 11;
            case 73: return 12;
            case 75: return 13;
            case 77: return 14;
            case 80: case 81: case 82: return 15;
            case 85: case 86: return 16;
            case 95: return 17;
            case 96: case 99: return 18;
            default: return 19;
        }
    }

    /** "1 800 m" in the TV's locale. */
    static String metres(Locale locale, int m) {
        return String.format(locale, "%,d m", m).replace(',', ' ').replace(' ', ' ');
    }
}
