package org.example.smb;

import ca.uhn.fhir.validation.ValidationResult;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.example.config.ProcessingException;
import org.example.parser.Parser;
import org.example.pdf.PdfGenerator;
import org.example.pdf.Report;
import org.example.validator.Validator;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FileProcessorRoute extends RouteBuilder {

    @Autowired
    private Validator validator;

    @Override
    public void configure() {

        onException(ProcessingException.class)
                .handled(true)
                .log("Moving original file to .unprocessed directory: ${header.CamelFileName}")
                .to("smb:{{smb.host}}:{{smb.port}}/{{smb.share}}/{{smb.path}}/.unprocessed"
                        + "?username={{smb.username}}"
                        + "&password={{smb.password}}")
                .log("Successfully moved failed file to .unprocessed.")
                // Step 2: Prepare the error report text file
                .process(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String exceptionMessage = (cause != null) ? cause.getMessage() : "Unknown processing error";

                    String baseName = exchange.getProperty("BaseFileName", String.class);
                    if (baseName == null) {
                        String fileName = exchange.getIn().getHeader("CamelFileNameOnly", String.class);
                        baseName = (fileName != null && fileName.contains("."))
                                ? fileName.substring(0, fileName.lastIndexOf('.'))
                                : fileName;
                    }

                    // Set body content and filename for the error text file
                    exchange.getMessage().setBody("Processing Error:\n" + exceptionMessage);
                    exchange.getMessage().setHeader("CamelFileName", "Error_" + baseName + ".txt");
                })
                .log("Writing error log file to output SMB path: ${header.CamelFileName}")

                // Step 3: Write the error text file to the output directory
                .to("smb:{{smb.host}}:{{smb.port}}/{{smb.share}}/{{smb.outputPath}}"
                        + "?username={{smb.username}}"
                        + "&password={{smb.password}}")
                .log("Successfully saved error report for: ${header.CamelFileName}");


        from("smb:{{smb.host}}:{{smb.port}}/{{smb.share}}/{{smb.path}}"
                + "?username={{smb.username}}"
                + "&password={{smb.password}}"
                + "&delay=10000"
                + "&delete=false"
                + "&move=.done/${file:name}")
                .routeId("smb-file-poller")
                .log("Downloading file from SMB share: ${header.CamelFileName}")
                .process(exchange -> {
                    String fileContent = exchange.getIn().getBody(String.class);

                    Parser parser = new Parser(validator);
                    Bundle processBundle = parser.parseBundle(fileContent);

                    long startTime = System.currentTimeMillis();
                    ValidationResult result = validator.validate(processBundle);
                    long endTime = System.currentTimeMillis();
                    long validationDurationMs = endTime - startTime;

                    String fileNameWithoutExt = FileNameExtractor.getFileNameWithoutExt(exchange);

                    // Serialize the processed bundle back to JSON
                    String processedBundleJson = parser.encodeResourceToString(processBundle);

                    // Generate the PDF report
                    Report report = new Report(result, parser.getResourceCounter(), validationDurationMs);
                    if(Objects.nonNull(parser.getBundleProcessor().appliedProcessing))
                        report.getAppliedProcessing().addAll(parser.getBundleProcessor().appliedProcessing);

                    PdfGenerator pdfGenerator = new PdfGenerator();
                    byte[] pdfReport = pdfGenerator.generateReport(report);

                    // Store parameters and validation result flag
                    exchange.setProperty("ProcessedBundleJson", processedBundleJson);
                    exchange.setProperty("PdfReportBytes", pdfReport);
                    exchange.setProperty("BaseFileName", fileNameWithoutExt);
                    exchange.setProperty("IsValid", result.isSuccessful());
                })
                // Step 1: Write the processed JSON bundle ONLY if valid
                .choice()
                .when(exchangeProperty("IsValid").isEqualTo(true))
                .process(exchange -> {
                    String bundleJson = exchange.getProperty("ProcessedBundleJson", String.class);
                    String baseName = exchange.getProperty("BaseFileName", String.class);

                    exchange.getMessage().setBody(bundleJson);
                    exchange.getMessage().setHeader("CamelFileName", "Processed_" + baseName + ".json");
                })
                .log("Writing valid processed JSON bundle to valid data SMB path: ${header.CamelFileName}")
                .to("smb:{{smb.host}}:{{smb.port}}/{{smb.share}}/{{smb.validDatPath}}"
                        + "?username={{smb.username}}"
                        + "&password={{smb.password}}")
                .otherwise()
                .log("Resource is invalid. Skipping writing bundle to valid data folder.")
                .end()

                // Step 2: Always write the PDF report
                .process(exchange -> {
                    byte[] pdfBytes = exchange.getProperty("PdfReportBytes", byte[].class);
                    String baseName = exchange.getProperty("BaseFileName", String.class);

                    exchange.getMessage().setBody(pdfBytes);
                    exchange.getMessage().setHeader("CamelFileName", "Report_" + baseName + ".pdf");
                })
                .log("Writing PDF report to output SMB path: ${header.CamelFileName}")
                .to("smb:{{smb.host}}:{{smb.port}}/{{smb.share}}/{{smb.outputPath}}"
                        + "?username={{smb.username}}"
                        + "&password={{smb.password}}")
                .log("Successfully processed and saved files for: ${header.CamelFileName}");
    }


}