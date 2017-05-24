package com.example.xyzreader.ui;

import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by hans.dykstra on 4/30/2017.
 */

public class DisplayUtil {
    private static final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private DisplayUtil() {}

    public static Spanned buildBylineHTML(Date date, DateFormat format, String author) {
        if (!date.before(START_OF_EPOCH.getTime())) {
            return Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            date.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <span style=\"font-weight: bold\">"
                            + author
                            + "</span>");

        } else {
            // If date is before 1902, just show the string
            return Html.fromHtml(
                    format.format(date) + " by <font color='#ffffff'>"
                            + author
                            + "</font>");

        }

    }
}
