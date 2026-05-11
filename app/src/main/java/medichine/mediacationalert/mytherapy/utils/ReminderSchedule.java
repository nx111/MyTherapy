package medichine.mediacationalert.mytherapy.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderSchedule {
    private static final long MIL_MINUTE = 60000L;
    private static final long MIL_HOUR = 3600000L;
    private static final long MIL_DAY = 86400000L;
    private static final long MIL_WEEK = 604800000L;
    private static final long MIL_MONTH = 2592000000L;

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("d/M/yyyy H:mm", Locale.US);

    public static Calendar parse(Reminder reminder) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(FORMAT.parse(reminder.getDate() + " " + reminder.getTime()));
        } catch (ParseException e) {
            return calendar;
        }
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static String format(Calendar calendar) {
        return FORMAT.format(calendar.getTime());
    }

    public static long repeatMillis(Reminder reminder) {
        int repeatNo;
        try {
            repeatNo = Integer.parseInt(reminder.getRepeatNo());
        } catch (NumberFormatException e) {
            repeatNo = 1;
        }
        if (repeatNo <= 0) {
            repeatNo = 1;
        }

        if ("Minute".equals(reminder.getRepeatType())) {
            return repeatNo * MIL_MINUTE;
        } else if ("Hour".equals(reminder.getRepeatType())) {
            return repeatNo * MIL_HOUR;
        } else if ("Day".equals(reminder.getRepeatType())) {
            return repeatNo * MIL_DAY;
        } else if ("Week".equals(reminder.getRepeatType())) {
            return repeatNo * MIL_WEEK;
        } else if ("Month".equals(reminder.getRepeatType())) {
            return repeatNo * MIL_MONTH;
        }
        return MIL_DAY;
    }

    public static Calendar currentOccurrence(Reminder reminder, ReminderDatabase database) {
        Calendar occurrence = parse(reminder);
        if (!"true".equals(reminder.getRepeat())) {
            return occurrence;
        }

        long repeatMillis = repeatMillis(reminder);
        long now = System.currentTimeMillis();
        while (occurrence.getTimeInMillis() + repeatMillis <= now) {
            occurrence.setTimeInMillis(occurrence.getTimeInMillis() + repeatMillis);
        }

        if (database != null && database.isReminderTaken(reminder.getID(), format(occurrence))) {
            occurrence.setTimeInMillis(occurrence.getTimeInMillis() + repeatMillis);
        }
        return occurrence;
    }
}
