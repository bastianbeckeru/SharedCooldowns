package com.sharedcooldowns.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatterTest {

    private static final String DEFAULT = "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s";

    @Test
    void defaultFormatDropsZeroUnits() {
        assertEquals("&b1&7d &b5&7s", TimeFormatter.compact(DEFAULT, 86405));
        assertEquals("&b10&7m", TimeFormatter.compact(DEFAULT, 600));
    }

    @Test
    void labelsSeparatedBySpacesDropWithTheirUnit() {
        String format = "%hours% horas %minutes% minutos %seconds% segundos";
        assertEquals("5 minutos", TimeFormatter.compact(format, 300));
        assertEquals("1 horas 1 segundos", TimeFormatter.compact(format, 3601));
    }

    @Test
    void wordsBeforeTheFirstUnitAreKept() {
        assertEquals("Espera 5 minutos", TimeFormatter.compact("Espera %minutes% minutos %seconds% segundos", 300));
    }

    @Test
    void zeroStillShowsSeconds() {
        assertEquals("&b0&7s", TimeFormatter.compact(DEFAULT, 0));
    }
}
