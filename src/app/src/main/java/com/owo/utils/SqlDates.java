package com.owo.utils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Reads date and date-time columns without assuming the writer was this application.
 *
 * <p>The DAOs write ISO-8601, but SQLite's own {@code CURRENT_TIMESTAMP} uses a space
 * separator instead of {@code T}. A single row inserted by any other tool used to throw
 * {@link DateTimeParseException} out of the middle of a query and fail the whole result.
 */
public final class SqlDates {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SqlDates() {
    }

    public static String format(LocalDate value) {
        return value.format(DATE_FORMATTER);
    }

    public static String format(LocalDateTime value) {
        return value.format(DATETIME_FORMATTER);
    }

    /**
     * @param column the column name, used only to make the failure message actionable
     * @throws SQLException if the value is absent or in no recognised format
     */
    public static LocalDateTime parseDateTime(String value, String column) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException("Column '" + column + "' holds no timestamp");
        }
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // fall through to the SQLite native format
        }
        try {
            return LocalDateTime.parse(value, SQLITE_DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new SQLException(
                    "Column '" + column + "' holds an unparseable timestamp: " + value, e);
        }
    }

    /**
     * @param column the column name, used only to make the failure message actionable
     * @throws SQLException if the value is absent or in no recognised format
     */
    public static LocalDate parseDate(String value, String column) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException("Column '" + column + "' holds no date");
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // A full timestamp in a date column is still meaningful.
            try {
                return parseDateTime(value, column).toLocalDate();
            } catch (SQLException nested) {
                throw new SQLException(
                        "Column '" + column + "' holds an unparseable date: " + value, e);
            }
        }
    }
}
