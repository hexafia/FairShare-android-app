package com.example.fairshare.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for converting timestamps to relative time strings
 * Provides human-readable time formats like "2h ago", "Yesterday", etc.
 */
public class TimeUtils {

    private static final long SECOND_IN_MILLIS = 1000;
    private static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    private static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    private static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    private static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
    private static final long MONTH_IN_MILLIS = DAY_IN_MILLIS * 30;
    private static final long YEAR_IN_MILLIS = DAY_IN_MILLIS * 365;

    /**
     * Convert a timestamp to a relative time string
     * @param timestamp The timestamp to convert
     * @return Relative time string (e.g., "2h ago", "Yesterday", "3 days ago")
     */
    public static String getRelativeTime(Date timestamp) {
        if (timestamp == null) {
            return "Unknown time";
        }

        long now = System.currentTimeMillis();
        long time = timestamp.getTime();
        long diff = now - time;

        if (diff < MINUTE_IN_MILLIS) {
            return "Just now";
        } else if (diff < HOUR_IN_MILLIS) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes == 1 ? "1m ago" : minutes + "m ago";
        } else if (diff < DAY_IN_MILLIS) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours == 1 ? "1h ago" : hours + "h ago";
        } else if (diff < DAY_IN_MILLIS * 2) {
            return "Yesterday";
        } else if (diff < WEEK_IN_MILLIS) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days == 1 ? "1 day ago" : days + " days ago";
        } else if (diff < WEEK_IN_MILLIS * 2) {
            long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        } else if (diff < MONTH_IN_MILLIS) {
            long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
            return weeks + " weeks ago";
        } else if (diff < YEAR_IN_MILLIS) {
            long months = TimeUnit.MILLISECONDS.toDays(diff) / 30;
            return months == 1 ? "1 month ago" : months + " months ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(timestamp);
        }
    }

    /**
     * Get a more detailed relative time string for notifications
     * @param timestamp The timestamp to convert
     * @return Detailed relative time string
     */
    public static String getDetailedRelativeTime(Date timestamp) {
        if (timestamp == null) {
            return "Unknown time";
        }

        long now = System.currentTimeMillis();
        long time = timestamp.getTime();
        long diff = now - time;

        if (diff < MINUTE_IN_MILLIS) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            return seconds <= 1 ? "Just now" : seconds + " seconds ago";
        } else if (diff < HOUR_IN_MILLIS) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        } else if (diff < DAY_IN_MILLIS) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        } else if (diff < DAY_IN_MILLIS * 2) {
            return "Yesterday";
        } else if (diff < WEEK_IN_MILLIS) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days == 1 ? "1 day ago" : days + " days ago";
        } else if (diff < WEEK_IN_MILLIS * 2) {
            long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        } else if (diff < MONTH_IN_MILLIS) {
            long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
            return weeks + " weeks ago";
        } else if (diff < YEAR_IN_MILLIS) {
            long months = TimeUnit.MILLISECONDS.toDays(diff) / 30;
            return months == 1 ? "1 month ago" : months + " months ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(timestamp);
        }
    }

    /**
     * Check if a timestamp is from today
     * @param timestamp The timestamp to check
     * @return true if the timestamp is from today
     */
    public static boolean isToday(Date timestamp) {
        if (timestamp == null) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String today = sdf.format(new Date());
        String timestampDay = sdf.format(timestamp);
        
        return today.equals(timestampDay);
    }

    /**
     * Check if a timestamp is from yesterday
     * @param timestamp The timestamp to check
     * @return true if the timestamp is from yesterday
     */
    public static boolean isYesterday(Date timestamp) {
        if (timestamp == null) {
            return false;
        }

        long yesterday = System.currentTimeMillis() - DAY_IN_MILLIS;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String yesterdayStr = sdf.format(new Date(yesterday));
        String timestampDay = sdf.format(timestamp);
        
        return yesterdayStr.equals(timestampDay);
    }
}
