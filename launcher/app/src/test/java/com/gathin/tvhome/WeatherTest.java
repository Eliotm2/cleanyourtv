package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Fork (Thomas): the Open-Meteo answer is read the way Home shows it. */
public class WeatherTest {

    private static final String BODY = "{\"current\":{\"time\":\"2026-10-25T17:15\",\"temperature_2m\":10.6,\"weather_code\":61,\"is_day\":1},"
            + "\"hourly\":{\"time\":[\"2026-10-25T16:00\",\"2026-10-25T17:00\",\"2026-10-25T18:00\"],\"snowfall_height\":[1900.0,1840.0,1700.0]},"
            + "\"daily\":{\"time\":[\"2026-10-25\",\"2026-10-26\"],\"temperature_2m_max\":[12.4,8.6],\"temperature_2m_min\":[5.5,2.2],"
            + "\"weather_code\":[61,71],\"sunrise\":[\"2026-10-25T08:21\",\"2026-10-26T08:22\"],\"sunset\":[\"2026-10-25T18:32\",\"2026-10-26T18:30\"]}}";

    @Test
    public void readsNowTodayTomorrowAndSnowLineAtThisHour() throws Exception {
        Weather.Data d = Weather.parse("Saint-Gervais-les-Bains", BODY);
        assertEquals(11, d.temp);
        assertEquals(61, d.code);
        assertEquals(12, d.max);
        assertEquals(6, d.min);
        assertEquals(Integer.valueOf(1800), d.snowLine);   // 1840 at 17:00, rounded to 100 m
        assertEquals("08:21", d.sunrise);
        assertEquals("18:32", d.sunset);
        assertEquals(Integer.valueOf(9), d.tomorrowMax);
        assertEquals(Integer.valueOf(2), d.tomorrowMin);
        assertEquals(Integer.valueOf(71), d.tomorrowCode);
        assertEquals(Weather.descIndex(71), 11);
    }

    @Test
    public void missingSnowLineStaysNull() throws Exception {
        Weather.Data d = Weather.parse("x", BODY.replace("\"snowfall_height\":[1900.0,1840.0,1700.0]", "\"snowfall_height\":[null,null,null]"));
        assertNull(d.snowLine);
    }

    @Test
    public void cacheRoundTrips() throws Exception {
        Weather.Data d = Weather.parse("Saint-Gervais-les-Bains", BODY);
        Weather.Data back = Weather.Data.fromJson(d.toJson());
        assertEquals(d.temp, back.temp);
        assertEquals(d.snowLine, back.snowLine);
        assertEquals(d.tomorrowCode, back.tomorrowCode);
        assertEquals(d.sunset, back.sunset);
    }
}
