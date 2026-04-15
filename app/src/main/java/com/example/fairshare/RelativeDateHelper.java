package com.example.fairshare;

import android.text.format.DateUtils;

import java.util.Date;

/**
 * Utility class for converting Date objects into relative time strings.
 * Uses Android's built-in DateUtils for localized, human-friendly formatting.
 *
 * Examples: "Just now", "2 min. ago", "3 hrs. ago", "Yesterday", "2 days ago", "1 week ago"
 */
public class RelativeDateHelper {

    private static final long MINUTE_MILLIS = 60 * 1000;
    private static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final long DAY_MILLIS = 24 * HOUR_MILLIS;
    private static final long WEEK_MILLIS = 7 * DAY_MILLIS;

    /**
     * Converts a Date object into a concise relative time string.
     *
     * @param date The Date to convert. If null, returns "Unknown".
     * @return A human-friendly relative time string (e.g., "Just now", "2hrs ago", "1w ago").
     */
    public static String getRelativeTimeString(Date date) {
        if (date == null) {
            return "Unknown";
        }

        long now = System.currentTimeMillis();
        long timestamp = date.getTime();
        long diff = now - timestamp;

        // Guard against future timestamps
        if (diff < 0) {
            return "Just now";
        }

        if (diff < MINUTE_MILLIS) {
            return "Just now";
        } else if (diff < HOUR_MILLIS) {
            long minutes = diff / MINUTE_MILLIS;
            return minutes + "min ago";
        } else if (diff < DAY_MILLIS) {
            long hours = diff / HOUR_MILLIS;
            return hours + "hrs ago";
        } else if (diff < WEEK_MILLIS) {
            long days = diff / DAY_MILLIS;
            return days + "d ago";
        } else {
            long weeks = diff / WEEK_MILLIS;
            return weeks + "w ago";
        }
    }
}
