package medichine.mediacationalert.mytherapy.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyTherapyArchiveImporter {
    private static final SimpleDateFormat ARCHIVE_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat ARCHIVE_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat STORAGE_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static class Result {
        public int drugRows;
        public int measurementRows;
        public int rejectedRows;
        public int createdReminders;
        public int reusedReminders;
        public int importedLogs;

        public int totalReminders() {
            return createdReminders + reusedReminders;
        }
    }

    private static class ArchiveRow {
        Calendar scheduledAt;
        Calendar actualAt;
        String type;
        String name;
        double value;
        String unit;
        String status;
        Calendar endAt;
    }

    private static class ScheduledDose {
        ArchiveRow row;
        String date;
        String time;
    }

    private static class Segment {
        String name;
        double dose;
        String unit;
        String time;
        String startDate;
        String endDate;
        String sourceEndDate;
        boolean noEndDate;
        String importedEndDate;
        ArrayList<ScheduledDose> doses = new ArrayList<>();
    }

    public Result importArchive(Context context, InputStream inputStream) throws IOException {
        ArrayList<ArchiveRow> rows = readRows(inputStream);
        Result result = new Result();
        ArrayList<ScheduledDose> drugDoses = new ArrayList<>();

        for (ArchiveRow row : rows) {
            if ("measurement".equals(row.type)) {
                result.measurementRows++;
                continue;
            }
            if (!"drug".equals(row.type) || row.scheduledAt == null || row.name.length() == 0) {
                continue;
            }
            result.drugRows++;
            if ("rejected".equals(row.status)) {
                result.rejectedRows++;
            }
            ScheduledDose dose = new ScheduledDose();
            dose.row = row;
            dose.date = ReminderSchedule.formatDate(row.scheduledAt);
            dose.time = ReminderSchedule.formatTime(
                    row.scheduledAt.get(Calendar.HOUR_OF_DAY),
                    row.scheduledAt.get(Calendar.MINUTE));
            drugDoses.add(dose);
        }

        ReminderDatabase database = new ReminderDatabase(context);
        List<Segment> segments = buildSegments(drugDoses);
        Map<String, Integer> reminderIdByDoseKey = new HashMap<>();

        for (Segment segment : segments) {
            String active = isOngoing(segment.endDate) ? "true" : "false";
            int reminderId = findExistingReminder(database.getAllReminders(), segment);
            if (reminderId == -1) {
                Reminder reminder = new Reminder(
                        segment.name,
                        segment.startDate,
                        segment.time,
                        "true",
                        "1",
                        "Day",
                        active,
                        segment.dose,
                        iconTypeFor(segment),
                        "",
                        segment.endDate,
                        segment.time);
                reminderId = database.addReminder(reminder);
                result.createdReminders++;
            } else {
                result.reusedReminders++;
            }

            for (ScheduledDose dose : segment.doses) {
                reminderIdByDoseKey.put(doseKey(dose), reminderId);
            }
        }

        for (ScheduledDose dose : drugDoses) {
            if (!"confirmed".equals(dose.row.status)) {
                continue;
            }
            Integer reminderId = reminderIdByDoseKey.get(doseKey(dose));
            if (reminderId == null) {
                continue;
            }
            String scheduledAt = ReminderSchedule.format(dose.row.scheduledAt);
            String takenAt = dose.row.actualAt == null ? "" : STORAGE_DATE_TIME.format(dose.row.actualAt.getTime());
            if (database.insertIntakeLog(reminderId, dose.row.name, dose.row.value, scheduledAt, takenAt)) {
                result.importedLogs++;
            }
        }

        AlarmReceiver alarmReceiver = new AlarmReceiver();
        for (Reminder reminder : database.getAllReminders()) {
            if ("true".equals(reminder.getActive())) {
                alarmReceiver.scheduleReminder(context, reminder);
            }
        }
        return result;
    }

    private ArrayList<ArchiveRow> readRows(InputStream inputStream) throws IOException {
        ArrayList<ArchiveRow> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String header = reader.readLine();
        if (header == null || !header.contains("scheduled_date") || !header.contains("actual_date")) {
            throw new IOException("Invalid MyTherapy archive");
        }
        List<String> headerFields = parseCsvLine(header);
        int endDateIndex = findColumnIndex(headerFields, "end_date", "enddate", "end_at",
                "expiration_date", "expiration", "expires_at", "expiry_date", "expirydate",
                "expiry", "valid_until", "until", "到期日期", "结束日期");

        String line;
        while ((line = reader.readLine()) != null) {
            List<String> fields = parseCsvLine(line);
            if (fields.size() < 7) {
                continue;
            }
            ArchiveRow row = new ArchiveRow();
            row.scheduledAt = parseArchiveDate(fields.get(0));
            row.actualAt = parseArchiveDate(fields.get(1));
            row.type = fields.get(2).trim();
            row.name = fields.get(3).trim();
            row.value = parseDouble(fields.get(4));
            row.unit = fields.get(5).trim();
            row.status = fields.get(6).trim();
            if (endDateIndex >= 0 && endDateIndex < fields.size()) {
                row.endAt = parseArchiveEndDate(fields.get(endDateIndex));
            }
            rows.add(row);
        }
        return rows;
    }

    private List<Segment> buildSegments(List<ScheduledDose> doses) {
        LinkedHashMap<String, ArrayList<ScheduledDose>> byPlan = new LinkedHashMap<>();
        for (ScheduledDose dose : doses) {
            String key = dose.row.name + "|" + formatDose(dose.row.value) + "|" + dose.time;
            if (!byPlan.containsKey(key)) {
                byPlan.put(key, new ArrayList<>());
            }
            byPlan.get(key).add(dose);
        }

        ArrayList<Segment> segments = new ArrayList<>();
        for (ArrayList<ScheduledDose> group : byPlan.values()) {
            Collections.sort(group, Comparator.comparingLong(dose -> dose.row.scheduledAt.getTimeInMillis()));
            Segment current = null;
            Calendar previousDate = null;

            for (ScheduledDose dose : group) {
                Calendar currentDate = ReminderSchedule.parseDate(dose.date);
                if (current == null || previousDate == null || !isNextDay(previousDate, currentDate)) {
                    current = new Segment();
                    current.name = dose.row.name;
                    current.dose = dose.row.value;
                    current.unit = dose.row.unit;
                    current.time = dose.time;
                    current.startDate = dose.date;
                    segments.add(current);
                }
                current.endDate = dose.date;
                if (dose.row.endAt != null) {
                    current.importedEndDate = ReminderSchedule.formatDate(dose.row.endAt);
                }
                current.doses.add(dose);
                previousDate = currentDate;
            }
        }
        for (Segment segment : segments) {
            segment.sourceEndDate = segment.endDate;
            if (segment.importedEndDate == null || segment.importedEndDate.length() == 0) {
                segment.noEndDate = true;
                segment.endDate = Reminder.NO_END_DATE;
            } else {
                segment.endDate = segment.importedEndDate;
            }
        }
        return segments;
    }

    private int findExistingReminder(List<Reminder> reminders, Segment segment) {
        for (Reminder reminder : reminders) {
            if (segment.name.equals(reminder.getTitle())
                    && Math.abs(segment.dose - reminder.getDose()) < 0.000001
                    && ReminderSchedule.doseTimes(reminder).contains(segment.time)) {
                return reminder.getID();
            }
        }
        return -1;
    }

    private boolean isOngoing(String endDate) {
        if (Reminder.isNoEndDate(endDate)) {
            return true;
        }
        return ReminderSchedule.parseDate(endDate).getTimeInMillis() >= startOfToday().getTimeInMillis();
    }

    private Calendar startOfToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

    private boolean isNextDay(Calendar previousDate, Calendar currentDate) {
        Calendar next = (Calendar) previousDate.clone();
        next.add(Calendar.DAY_OF_MONTH, 1);
        return next.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR)
                && next.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR);
    }

    private String doseKey(ScheduledDose dose) {
        return dose.row.name + "|" + formatDose(dose.row.value) + "|" + dose.time + "|" + dose.date;
    }

    private String iconTypeFor(Segment segment) {
        if (segment.name.contains("胶囊")) {
            return "capsule";
        }
        if (segment.unit.contains("liquid") || segment.unit.contains("ml")) {
            return "liquid";
        }
        return "pill";
    }

    private Calendar parseArchiveDate(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(ARCHIVE_DATE_TIME.parse(value.trim()));
        } catch (ParseException e) {
            return null;
        }
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Calendar parseArchiveEndDate(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        Calendar calendar = parseArchiveDate(value);
        if (calendar != null) {
            return calendar;
        }
        calendar = Calendar.getInstance();
        try {
            calendar.setTime(ARCHIVE_DATE.parse(value.trim()));
        } catch (ParseException e) {
            return null;
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private int findColumnIndex(List<String> headers, String... names) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).trim().toLowerCase(Locale.US)
                    .replace(" ", "_")
                    .replace("-", "_");
            for (String name : names) {
                if (header.equals(name.toLowerCase(Locale.US))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDose(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private List<String> parseCsvLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }
}
