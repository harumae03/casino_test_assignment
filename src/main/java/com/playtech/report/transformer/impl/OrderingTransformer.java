package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class OrderingTransformer implements Transformer {
    public final static String NAME = "Ordering";

    private final String inputColumnName;
    private final Order sortOrder;

    public OrderingTransformer(Column input, Order order) {
        this.inputColumnName = input.getName();
        this.sortOrder = order;
        Column.DataType inputType = input.getType();

        if (inputType != Column.DataType.STRING && inputType != Column.DataType.DATE && inputType != Column.DataType.DATETIME) {
            throw new IllegalArgumentException("OrderingTransformer only supports STRING, DATE, DATETIME column types. Found: " + inputType + " for column '" + inputColumnName + "'");
        }
    }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (inputColumnName == null || inputColumnName.isEmpty()) {
            System.err.println("Error: OrderingTransformer has no input column defined. Skipping sort.");
            return;
        }

        Comparator<Map<String, Object>> comparator = (row1, row2) -> {
            Object val1 = row1.get(inputColumnName);
            Object val2 = row2.get(inputColumnName);

            // nulls first in ASC, last in DESC
            if (val1 == null && val2 == null) return 0;
            if (val1 == null) return (sortOrder == Order.ASC) ? -1 : 1;
            if (val2 == null) return (sortOrder == Order.ASC) ? 1 : -1;


            try {
                @SuppressWarnings({"unchecked", "rawtypes"}) // Safe due to type validation in constructor & CSV parsing
                Comparable c1 = (Comparable) val1;
                @SuppressWarnings({"unchecked", "rawtypes"})
                Comparable c2 = (Comparable) val2;
                int compareResult = c1.compareTo(c2);
                return (sortOrder == Order.DESC) ? -compareResult : compareResult;
            } catch (ClassCastException e) {
                // just in case
                System.err.println("Warning: Type mismatch during sorting column '" + inputColumnName + "'. Values: '" + val1 + "', '" + val2 + "'. Treating as equal.");
                return 0;
            }
        };

        // Sort the list in place
        rows.sort(comparator);
    }

    public enum Order {
        ASC,
        DESC }
}