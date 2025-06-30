// src/main/java/com/playtech/util/CsvDataReader.java
package com.playtech.util.xml;

import com.playtech.report.column.Column;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvDataReader {


    public static List<Map<String, Object>> readData(String filePath, List<Column> inputColumns) {
        List<Map<String, Object>> dataRows = new ArrayList<>();
        Map<String, Column.DataType> inputColumnTypes = inputColumns.stream()
                .collect(Collectors.toMap(Column::getName, Column::getType));

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                System.err.println("Warning: CSV file is empty or missing header: " + filePath);
                return dataRows;
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> headerIndexMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndexMap.put(headers[i].trim(), i);
            }

            for (Column inputCol : inputColumns) {
                if (!headerIndexMap.containsKey(inputCol.getName())) {
                    System.err.println("Warning: Input column '" + inputCol.getName() + "' defined in XML not found in CSV header.");
                }
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] values = line.split(",", -1); // Keep trailing empty strings

                //skips row if column count doesn't match header
                if (values.length != headers.length) {
                    System.err.println("Warning: Skipping line " + lineNumber + ". Expected " + headers.length + " columns, but found " + values.length + ".");
                    continue;
                }

                Map<String, Object> rowMap = new HashMap<>();
                boolean rowParseSuccess = true;

                for (Column inputCol : inputColumns) {
                    String columnName = inputCol.getName();
                    Integer columnIndex = headerIndexMap.get(columnName);

                    if (columnIndex == null) {
                        // column defined in XML but not found in header (already warned)
                        rowMap.put(columnName, null);
                        continue;
                    }

                    String rawValue = values[columnIndex].trim();
                    Column.DataType expectedType = inputCol.getType();

                    // treat empty strings as null for non-string types
                    if (rawValue.isEmpty() && expectedType != Column.DataType.STRING) {
                        rowMap.put(columnName, null);
                        continue;
                    }

                    try {
                        Object parsedValue = parseValue(rawValue, expectedType);
                        rowMap.put(columnName, parsedValue);
                    } catch (NumberFormatException | DateTimeParseException e) {
                        System.err.println("Warning: Skipping line " + lineNumber + ". Failed to parse value '" + rawValue + "' for column '" + columnName + "' as type " + expectedType + ". Error: " + e.getMessage());
                        rowParseSuccess = false;
                        break; // stop processing this row on first parse error
                    } catch (Exception e) {
                        System.err.println("Warning: Skipping line " + lineNumber + ". Unexpected error parsing column '" + columnName + "'. Error: " + e.getMessage());
                        rowParseSuccess = false;
                        break;
                    }
                }

                if (rowParseSuccess) {
                    dataRows.add(rowMap);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + filePath);
            throw new RuntimeException("Failed to read CSV file: " + filePath, e);
        }

        return dataRows;
    }

    // parses a string value into the specified data type according to ISO standards where applicable.
    private static Object parseValue(String value, Column.DataType type) throws DateTimeParseException, NumberFormatException {
        if (value == null || value.isEmpty()) {
            return (type == Column.DataType.STRING) ? "" : null;
        }

        return switch (type) {
            case STRING -> value;
            case INTEGER -> Integer.parseInt(value);
            case DOUBLE -> Double.parseDouble(value);
            case DATE -> LocalDate.parse(value); // ISO-8601 YYYY-MM-DD
            case DATETIME -> ZonedDateTime.parse(value); // ISO-8601 YYYY-MM-DDTHH:mm:ss[.SSS]XXX
        };
    }
}