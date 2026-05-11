package medichine.mediacationalert.mytherapy.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;

public class DateTimeComparator implements Comparator<DateTimeSorter> {
    DateFormat f = new SimpleDateFormat("d/M/yyyy H:mm", Locale.US);

    public int compare(DateTimeSorter a, DateTimeSorter b) {
        String o1 = a.getDateTime();
        String o2 = b.getDateTime();

        try {
            return f.parse(o1).compareTo(f.parse(o2));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
