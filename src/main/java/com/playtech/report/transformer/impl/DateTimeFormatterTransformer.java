package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;

public class DateTimeFormatterTransformer implements Transformer {
    public static final String NAME = "DateTimeFormatter";

    private final String inputColumnName;
    private final String outputColumnName;
    private final DateTimeFormatter formatter;

    public DateTimeFormatterTransformer(Column input, String format, Column output) {
        this.inputColumnName = input.getName();
        this.outputColumnName = output.getName();
        try {
            this.formatter = DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid date/time format pattern provided to DateTimeFormatterTransformer: " + format, e);
        }
    }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (outputColumnName == null || outputColumnName.isEmpty()) {
            System.err.println("Error: DateTimeFormatterTransformer has no output column defined. Skipping.");
            return;
        }
        if (inputColumnName == null || inputColumnName.isEmpty()) {
            System.err.println("Error: DateTimeFormatterTransformer has no input column defined. Skipping.");
            return;
        }

        for (Map<String, Object> row : rows) {
            Object value = row.get(inputColumnName);

            if (value == null) {
                row.put(outputColumnName, null);
                continue;
            }

            if (value instanceof TemporalAccessor) {
                try {
                    String formattedDate = formatter.format((TemporalAccessor) value);
                    row.put(outputColumnName, formattedDate);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to format date/time for column '" + inputColumnName + "' in row: " + row + ". Error: " + e.getMessage());
                    row.put(outputColumnName, null);
                }
            } else {
                System.err.println("Warning: DateTimeFormatterTransformer expected DATE or DATETIME for input '" + inputColumnName + "', but found type " + value.getClass().getName() + ". Setting output to null.");
                row.put(outputColumnName, null);
            }
        }
    }
}