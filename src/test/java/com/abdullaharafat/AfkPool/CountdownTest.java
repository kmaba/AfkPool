package com.abdullaharafat.AfkPool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountdownTest {

    @Test
    void formatsZeroAsZeroMinutes() {
        assertEquals("0:00", Countdown.formatDuration(0));
    }

    @Test
    void formatsNegativeDurationClampedToZero() {
        assertEquals("0:00", Countdown.formatDuration(-5000));
    }

    @Test
    void formatsSubSecondRoundedUpToOneSecond() {
        assertEquals("0:01", Countdown.formatDuration(1));
        assertEquals("0:01", Countdown.formatDuration(999));
    }

    @Test
    void roundsUpToNextSecond() {
        assertEquals("0:05", Countdown.formatDuration(4001));
    }

    @Test
    void formatsExactlyOneMinute() {
        assertEquals("1:00", Countdown.formatDuration(60_000));
    }

    @Test
    void formatsFiveMinutesLikeDefaultConfigInterval() {
        // 6000 ticks * 50ms per tick = 5 minutes
        assertEquals("5:00", Countdown.formatDuration(6000 * 50L));
    }

    @Test
    void formatsTenMinutesLikeDefaultSecondInterval() {
        assertEquals("10:00", Countdown.formatDuration(12000 * 50L));
    }

    @Test
    void formatsMixedMinutesAndSeconds() {
        assertEquals("4:59", Countdown.formatDuration(4 * 60_000 + 59_000));
    }

    @Test
    void formatsHoursWhenOverAnHour() {
        assertEquals("1:00:00", Countdown.formatDuration(3600_000));
        assertEquals("2:30:05", Countdown.formatDuration(2 * 3600_000 + 30 * 60_000 + 5_000));
    }
}
