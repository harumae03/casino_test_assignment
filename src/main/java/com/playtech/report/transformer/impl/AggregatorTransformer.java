package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlIDREF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AggregatorTransformer implements Transformer {
    public static final String NAME = "Aggregator";

    private final String groupByColumnName;
    private final List<AggregateBy> aggregateColumns;

    public AggregatorTransformer(Column groupByColumn, List<AggregateBy> aggregateColumns) {
        Objects.requireNonNull(groupByColumn, "groupByColumn cannot be null");
        Objects.requireNonNull(aggregateColumns, "aggregateColumns cannot be null");
        this.groupByColumnName = groupByColumn.getName();
        this.aggregateColumns = aggregateColumns;


        for(AggregateBy agg : aggregateColumns) {
            Objects.requireNonNull(agg.getInput(), "AggregateBy input column cannot be null");
            Objects.requireNonNull(agg.getOutput(), "AggregateBy output column cannot be null");
            Objects.requireNonNull(agg.getMethod(), "AggregateBy method cannot be null");
            Column.DataType inputType = agg.getInput().getType();
            if (inputType != Column.DataType.INTEGER && inputType != Column.DataType.DOUBLE) {
                throw new IllegalArgumentException("Aggregator supports aggregation only on INTEGER/DOUBLE inputs. Found: " + inputType + " for input '" + agg.getInput().getName() + "'");
            }
        }
        if (aggregateColumns.isEmpty()) {
            System.err.println("Warning: AggregatorTransformer created with no aggregation columns specified.");
        }
    }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (groupByColumnName == null || groupByColumnName.isEmpty()) {
            System.err.println("Error: AggregatorTransformer has no groupBy column defined. Skipping aggregation.");
            return;
        }
        if (aggregateColumns.isEmpty()) {
            System.err.println("Warning: AggregatorTransformer has no aggregation columns. Resulting data will be empty.");
            rows.clear();
            return;
        }

        Map<Object, AggregationState> groupedData = new LinkedHashMap<>();

        // groups data and accumulate sums/counts
        for (Map<String, Object> row : rows) {
            Object groupKey = row.get(groupByColumnName);
            // groups null keys together
            AggregationState state = groupedData.computeIfAbsent(groupKey, k -> new AggregationState(k, groupByColumnName, aggregateColumns));
            state.accumulate(row);
        }

        // finalizes results and create output rows
        List<Map<String, Object>> aggregatedRows = new ArrayList<>();
        for (AggregationState state : groupedData.values()) {
            aggregatedRows.add(state.getResult());
        }

        // replaces original rows with aggregated results
        rows.clear();
        rows.addAll(aggregatedRows);
    }

    // helper class to manage state (sums, counts) for each group during aggregation
    private static class AggregationState {
        private final Object groupKeyValue;
        private final String groupByKeyName;
        private final List<AggregateBy> aggregatesToPerform;
        private final Map<String, Double> sums = new HashMap<>();
        private final Map<String, Integer> counts = new HashMap<>();

        AggregationState(Object groupKey, String groupByKeyName, List<AggregateBy> aggregatesToPerform) {
            this.groupKeyValue = groupKey;
            this.groupByKeyName = groupByKeyName;
            this.aggregatesToPerform = aggregatesToPerform;
            for (AggregateBy agg : aggregatesToPerform) {
                sums.put(agg.getOutput().getName(), 0.0);
                counts.put(agg.getInput().getName(), 0);
            }
        }

        // accumulates values from one row into the group's state
        void accumulate(Map<String, Object> row) {
            for (AggregateBy agg : aggregatesToPerform) {
                String inputName = agg.getInput().getName();
                String outputName = agg.getOutput().getName();
                Object valueObj = row.get(inputName);

                if (valueObj instanceof Number) {
                    double value = ((Number) valueObj).doubleValue();
                    sums.put(outputName, sums.get(outputName) + value);
                    counts.put(inputName, counts.get(inputName) + 1);
                }
            }
        }

        Map<String, Object> getResult() {
            Map<String, Object> resultRow = new HashMap<>();
            resultRow.put(groupByKeyName, groupKeyValue);

            for (AggregateBy agg : aggregatesToPerform) {
                String inputName = agg.getInput().getName();
                String outputName = agg.getOutput().getName();
                double sum = sums.get(outputName);
                int count = counts.get(inputName);

                if (agg.getMethod() == Method.SUM) {
                    resultRow.put(outputName, sum);
                } else if (agg.getMethod() == Method.AVG) {
                    // avoids division by zero if no valid inputs
                    resultRow.put(outputName, (count == 0) ? 0.0 : sum / count);
                }
            }
            return resultRow;
        }
    }
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AggregateBy {
        @XmlIDREF private Column input;
        private Method method;
        @XmlIDREF private Column output;
        public Column getInput() { return input; }
        public Method getMethod() { return method; }
        public Column getOutput() { return output; }
    }
    public enum Method { SUM, AVG }
}