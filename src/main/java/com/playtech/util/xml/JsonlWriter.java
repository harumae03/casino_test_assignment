package com.playtech.util.xml;

import com.playtech.report.column.Column;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonlWriter {


    public static void writeData(Path outputDir, String reportName, List<Column> outputColumns, List<Map<String, Object>> data) {
        Path outputFilePath = outputDir.resolve(reportName + ".jsonl");
        List<String> outputColumnNames = outputColumns.stream()
                .map(Column::getName)
                .collect(Collectors.toList());

        try {
            Files.createDirectories(outputDir);

            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
                for (Map<String, Object> row : data) {
                    String jsonLine = formatRowAsJson(row, outputColumnNames);
                    writer.write(jsonLine);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing JSONL output file: " + outputFilePath);
            throw new RuntimeException("Failed to write output file: " + outputFilePath, e);
        }
    }

    private static String formatRowAsJson(Map<String, Object> row, List<String> outputColumnNames) {
        StringBuilder sb = new StringBuilder("{");
        boolean firstField = true;

        for (String columnName : outputColumnNames) {
            if (row.containsKey(columnName)) {
                Object value = row.get(columnName);

                if (!firstField) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeJsonString(columnName)).append("\":");
                sb.append(formatJsonValue(value));
                firstField = false;
            }
        }

        sb.append("}");
        return sb.toString();
    }


    private static String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJsonString((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof LocalDate) {
            // formats as "YYYY-MM-DD"
            return "\"" + ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE) + "\"";
        } else if (value instanceof ZonedDateTime) {
            // formats as ISO date-time string with offset
            return "\"" + ((ZonedDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\"";
        } else {
            // treats unknown types as escaped strings
            return "\"" + escapeJsonString(value.toString()) + "\"";
        }
    }

    // Escape characters within a string according to JSON rules (minimal required set).
    private static String escapeJsonString(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                // basic control characters often escaped in JSON:
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }
}