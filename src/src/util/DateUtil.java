package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateUtil() {
    }

    /** Tra ve null neu chuoi rong, nem DateTimeParseException neu sai dinh dang. */
    public static LocalDate parse(String ddMMyyyy) {
        if (ddMMyyyy == null || ddMMyyyy.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(ddMMyyyy.trim(), DISPLAY_FORMAT);
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_FORMAT);
    }

    public static java.sql.Date toSqlDate(LocalDate date) {
        return date == null ? null : java.sql.Date.valueOf(date);
    }

    public static LocalDate fromSqlDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
