package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StringFormatterTransformer implements Transformer {
    public final static String NAME = "StringFormatter";

    private final List<String> inputColumnNames;
    private final String formatString;
    private final String outputColumnName;

    public StringFormatterTransformer(List<Column> inputs, String format, Column output) {
        this.inputColumnNames = inputs.stream().map(Column::getName).collect(Collectors.toList());
        this.formatString = format;
        this.outputColumnName = output.getName();

        if (this.inputColumnNames.isEmpty()) {
            System.err.println("Warning: StringFormatterTransformer created with no input columns for output '" + outputColumnName + "'.");
        }
        if (this.formatString == null || this.formatString.isEmpty()) {
            System.err.println("Warning: StringFormatterTransformer created with empty format string for output '" + outputColumnName + "'.");
        }
    }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (outputColumnName == null || outputColumnName.isEmpty()) {
            System.err.println("Error: StringFormatterTransformer has no output column defined. Skipping.");
            return;
        }

        for (Map<String, Object> row : rows) {
            try {
                Object[] args = inputColumnNames.stream()
                        .map(row::get) // gets value, lets String.format handle nulls/types
                        .toArray();
                String formattedString = String.format(formatString, args);
                row.put(outputColumnName, formattedString);
            } catch (Exception e) {
                System.err.println("Warning: Failed to format string for output '" + outputColumnName + "' in row: " + row + ". Error: " + e.getMessage());
                row.put(outputColumnName, null);
            }
        }
    }
}