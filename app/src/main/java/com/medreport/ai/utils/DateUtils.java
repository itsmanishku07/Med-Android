package com.medreport.ai.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    public static String timeAgo(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(isoDate);
            if (date == null) return isoDate;
            long diff = System.currentTimeMillis() - date.getTime();
            long mins = diff / 60000;
            if (mins < 1)  return "just now";
            if (mins < 60) return mins + "m ago";
            long hrs = mins / 60;
            if (hrs < 24)  return hrs + "h ago";
            long days = hrs / 24;
            if (days < 7)  return days + "d ago";
            return new SimpleDateFormat("MMM d", Locale.US).format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    public static String formatDate(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = in.parse(isoDate);
            if (date == null) return isoDate;
            return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(date);
        } catch (ParseException e) { return isoDate; }
    }
}
