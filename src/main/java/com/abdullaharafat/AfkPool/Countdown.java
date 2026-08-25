package com.abdullaharafat.AfkPool;

public final class Countdown {

    private Countdown() {
    }

    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, (millis + 999) / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }
}
