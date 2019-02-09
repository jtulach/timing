package org.apidesign.gate.timing.shared;

public final class Time {
    private Time() {
    }

    public static int[] fromMillis(long millis) {
        int sec = (int) (millis / 1000);
        int remMs = (int) (millis % 1000);

        int hundreds = remMs / 10;

        return new int[] { sec, hundreds };
    }

    public static String toString(Long millis) {
        if (millis == null) {
            return " --:--";
        } else if (millis == 0) {
            return " DNF  ";
        } else {
            int[] secHun = fromMillis(millis);
            String secStr = "  " + String.valueOf(secHun[0]);
            String hun = "0" + String.valueOf(secHun[1]);
            return secStr.substring(secStr.length() - 3) + ":" + hun.substring(hun.length() - 2);
        }
    }
}
