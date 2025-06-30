package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MathOperationTransformer implements Transformer {
    public final static String NAME = "MathOperation";

    private final List<String> inputColumnNames;
    private final MathOperation operation;
    private final String outputColumnName;
    private final Column.DataType resultType; // INTEGER or DOUBLE

    public MathOperationTransformer(List<Column> inputs, MathOperation operation, Column output) {
        this.inputColumnNames = inputs.stream().map(Column::getName).collect(Collectors.toList());
        this.operation = operation;
        this.outputColumnName = output.getName();

        if (this.inputColumnNames.size() != 2) {
            throw new IllegalArgumentException("MathOperationTransformer (ADD/SUBTRACT) requires exactly 2 input columns. Found: " + this.inputColumnNames.size());
        }


        boolean anyDouble = false;
        for (Column input : inputs) {
            Column.DataType type = input.getType();
            if (type != Column.DataType.INTEGER && type != Column.DataType.DOUBLE) {
                throw new IllegalArgumentException("MathOperationTransformer only supports INTEGER and DOUBLE inputs. Found: " + type + " for column '" + input.getName() + "'");
            }
            if (type == Column.DataType.DOUBLE) anyDouble = true;
        }
        this.resultType = anyDouble ? Column.DataType.DOUBLE : Column.DataType.INTEGER;
    }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (outputColumnName == null || outputColumnName.isEmpty()) {
            System.err.println("Error: MathOperationTransformer has no output column defined. Skipping.");
            return;
        }

        String input1Name = inputColumnNames.get(0);
        String input2Name = inputColumnNames.get(1);

        for (Map<String, Object> row : rows) {
            Object val1Obj = row.get(input1Name);
            Object val2Obj = row.get(input2Name);


            Number num1 = (val1Obj instanceof Number) ? (Number) val1Obj : 0;
            Number num2 = (val2Obj instanceof Number) ? (Number) val2Obj : 0;

            try {
                Number result;
                if (resultType == Column.DataType.DOUBLE) {
                    double d1 = num1.doubleValue();
                    double d2 = num2.doubleValue();
                    result = (operation == MathOperation.ADD) ? (d1 + d2) : (d1 - d2);
                } else { // both inputs were INTEGER
                    int i1 = num1.intValue();
                    int i2 = num2.intValue();
                    result = (operation == MathOperation.ADD) ? (i1 + i2) : (i1 - i2);
                }
                row.put(outputColumnName, result);
            } catch (Exception e) {
                System.err.println("Warning: Failed math operation '" + operation + "' for inputs '" + input1Name + "', '" + input2Name + "' in row: " + row + ". Error: " + e.getMessage());
                row.put(outputColumnName, null);
            }
        }
    }

    public enum MathOperation { ADD, SUBTRACT }
}