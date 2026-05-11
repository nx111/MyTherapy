package medichine.mediacationalert.mytherapy.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReminderSchedule {
    private static final long MIL_DAY = 86400000L;
    private static final long MIL_WEEK = 604800000L;
    private static final long MIL_MONTH = 2592000000L;

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("d/M/yyyy H:mm", Locale.US);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d/M/yyyy", Locale.US);

    public static Calendar parse(Reminder reminder) {
        return parseDateTime(reminder.getDate(), firstDoseTime(reminder));
    }

    public static Calendar parseDate(String date) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(DATE_FORMAT.parse(date));
        } catch (ParseException e) {
            return startOfDay(calendar);
        }
        return startOfDay(calendar);
    }

    public static Calendar parseScheduledAt(String scheduledAt) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(DATE_TIME_FORMAT.parse(scheduledAt));
        } catch (ParseException e) {
            return calendar;
        }
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static Calendar parseDateTime(String date, String time) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(DATE_TIME_FORMAT.parse(date + " " + time));
        } catch (ParseException e) {
            return calendar;
        }
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static String format(Calendar calendar) {
        return DATE_TIME_FORMAT.format(calendar.getTime());
    }

    public static String formatDate(Calendar calendar) {
        return DATE_FORMAT.format(calendar.getTime());
    }

    public static long repeatMillis(Reminder reminder) {
        if ("Week".equals(reminder.getRepeatType())) {
            return MIL_WEEK;
        } else if ("Month".equals(reminder.getRepeatType())) {
            return MIL_MONTH;
        }
        return MIL_DAY;
    }

    public static List<String> doseTimes(Reminder reminder) {
        ArrayList<String> times = new ArrayList<>();
        String raw = reminder.getDoseTimes();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String time = normalizeTime(part);
                if (time.length() > 0) {
                    times.add(time);
                }
            }
        }
        if (times.isEmpty()) {
            times.add(normalizeTime(reminder.getTime()));
        }
        return times;
    }

    public static String joinDoseTimes(List<String> times) {
        StringBuilder builder = new StringBuilder();
        for (String time : times) {
            String normalized = normalizeTime(time);
            if (normalized.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(normalized);
        }
        return builder.toString();
    }

    public static String firstDoseTime(Reminder reminder) {
        List<String> times = doseTimes(reminder);
        return times.isEmpty() ? reminder.getTime() : times.get(0);
    }

    public static List<Calendar> occurrencesBetween(Reminder reminder, long startMillis, long endMillis) {
        ArrayList<Calendar> occurrences = new ArrayList<>();
        Calendar planDate = parseDate(reminder.getDate());
        Calendar endDate = "true".equals(reminder.getRepeat())
                ? parseDate(reminder.getEndDate())
                : parseDate(reminder.getDate());
        List<String> times = doseTimes(reminder);

        while (planDate.getTimeInMillis() <= endDate.getTimeInMillis()) {
            for (String time : times) {
                Calendar occurrence = parseDateTime(formatDate(planDate), time);
                long occurrenceMillis = occurrence.getTimeInMillis();
                if (occurrenceMillis >= startMillis && occurrenceMillis <= endMillis) {
                    occurrences.add(occurrence);
                }
            }
            if (!"true".equals(reminder.getRepeat())) {
                break;
            }
            advance(planDate, reminder.getRepeatType());
        }
        return occurrences;
    }

    public static Calendar nextOccurrenceAfter(Reminder reminder, long afterMillis) {
        List<Calendar> occurrences = occurrencesBetween(reminder, afterMillis, endOfDay(parseDate(reminder.getEndDate())).getTimeInMillis());
        Calendar next = null;
        for (Calendar occurrence : occurrences) {
            if (occurrence.getTimeInMillis() >= afterMillis
                    && (next == null || occurrence.getTimeInMillis() < next.getTimeInMillis())) {
                next = occurrence;
            }
        }
        return next;
    }

    public static Calendar currentOccurrence(Reminder reminder, ReminderDatabase database) {
        Calendar next = nextOccurrenceAfter(reminder, System.currentTimeMillis());
        return next == null ? parse(reminder) : next;
    }

    public static boolean hasOccurrenceAt(Reminder reminder, String scheduledAt) {
        Calendar scheduled = parseScheduledAt(scheduledAt);
        long scheduledMillis = scheduled.getTimeInMillis();
        Calendar startDate = parseDate(reminder.getDate());
        Calendar endDate = "true".equals(reminder.getRepeat())
                ? parseDate(reminder.getEndDate())
                : parseDate(reminder.getDate());

        if (scheduledMillis < startDate.getTimeInMillis()
                || scheduledMillis > endOfDay(endDate).getTimeInMillis()) {
            return false;
        }

        String scheduledTime = formatTime(scheduled);
        if (!doseTimes(reminder).contains(scheduledTime)) {
            return false;
        }
        if (!"true".equals(reminder.getRepeat())) {
            return sameDate(startDate, scheduled);
        }
        if ("Week".equals(reminder.getRepeatType())) {
            long diffDays = (startOfDay(scheduled).getTimeInMillis() - startDate.getTimeInMillis()) / MIL_DAY;
            return diffDays >= 0 && diffDays % 7 == 0;
        } else if ("Month".equals(reminder.getRepeatType())) {
            Calendar cursor = (Calendar) startDate.clone();
            while (cursor.getTimeInMillis() <= scheduled.getTimeInMillis()) {
                if (sameDate(cursor, scheduled)) {
                    return true;
                }
                cursor.add(Calendar.MONTH, 1);
            }
            return false;
        }
        return true;
    }

    private static void advance(Calendar calendar, String repeatType) {
        if ("Week".equals(repeatType)) {
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        } else if ("Month".equals(repeatType)) {
            calendar.add(Calendar.MONTH, 1);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static Calendar startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static Calendar endOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar;
    }

    private static boolean sameDate(Calendar left, Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private static String normalizeTime(String time) {
        if (time == null) {
            return "";
        }
        String[] parts = time.trim().split(":");
        if (parts.length < 2) {
            return "";
        }
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return "";
            }
            return formatTime(hour, minute);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static String formatTime(Calendar calendar) {
        return formatTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    public static String formatTime(int hour, int minute) {
        if (minute < 10) {
            return hour + ":0" + minute;
        }
        return hour + ":" + minute;
    }
}
