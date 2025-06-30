package com.playtech;

import com.playtech.report.Report;
import com.playtech.report.transformer.Transformer;
import com.playtech.util.xml.CsvDataReader;
import com.playtech.util.xml.JsonlWriter;
import com.playtech.util.xml.XmlParser;
import jakarta.xml.bind.JAXBException;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ReportGenerator {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java com.playtech.ReportGenerator <csv-input-path> <xml-report-path> <output-directory-path>");
            System.exit(1);
        }
        String csvDataFilePath = args[0];
        String reportXmlFilePath = args[1];
        String outputDirectoryPath = args[2];

        System.out.println("Starting report generation...");
        System.out.println("Input CSV: " + csvDataFilePath);
        System.out.println("Report XML: " + reportXmlFilePath);
        System.out.println("Output Dir: " + outputDirectoryPath);

        try {
            System.out.println("Parsing report definition XML...");
            Report report = XmlParser.parseReport(reportXmlFilePath);
            System.out.println("Successfully parsed report: " + report.getReportName());

            // basic validation
            if (report.getOutputFormat() != Report.FileFormat.JSONL) {
                System.err.println("Error: This implementation only supports JSONL output format.");
                System.exit(1);
            }
            if (report.getInputs() == null || report.getInputs().isEmpty()) {
                System.err.println("Error: Report definition requires at least one input column.");
                System.exit(1);
            }
            if (report.getOutputs() == null || report.getOutputs().isEmpty()) {
                System.err.println("Error: Report definition requires at least one output column.");
                System.exit(1);
            }

            System.out.println("Reading and parsing CSV data...");
            List<Map<String, Object>> dataRows = CsvDataReader.readData(csvDataFilePath, report.getInputs());
            System.out.println("Read " + dataRows.size() + " valid data rows.");

            // applying Transformers in specified order
            System.out.println("Applying transformations...");
            List<Transformer> transformers = report.getTransformers();
            if (transformers != null && !transformers.isEmpty()) {
                int i = 1;
                for (Transformer transformer : transformers) {
                    String transformerName = transformer.getClass().getSimpleName();
                    System.out.println("Applying transformer " + i++ + "/" + transformers.size() + ": " + transformerName);
                    transformer.transform(report, dataRows);
                    System.out.println(" -> Data rows after " + transformerName + ": " + dataRows.size());
                }
            } else {
                System.out.println("No transformers defined in the report.");
            }

            System.out.println("Writing output file...");
            JsonlWriter.writeData(
                    Paths.get(outputDirectoryPath),
                    report.getReportName(),
                    report.getOutputs(),
                    dataRows
            );

            System.out.println("Report generation completed successfully!");
            System.out.println("Output file: " + Paths.get(outputDirectoryPath).resolve(report.getReportName() + ".jsonl"));

        } catch (JAXBException e) {
            System.err.println("FATAL: Parsing of the XML report definition file failed: " + reportXmlFilePath);
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("FATAL: Invalid configuration detected:");
            e.printStackTrace();
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("FATAL: An unexpected error occurred during report generation:");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("FATAL: An unknown error occurred:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}