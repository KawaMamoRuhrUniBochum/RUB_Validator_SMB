package org.example.pdf;

import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.example.validator.ResourceStatistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Report {

    private final String generationTime;
    private ValidationStatus status;
    private final String validationStatus;  // e.g., "PASSED WITH WARNINGS", "FAILED"
    private final String validationTime;
    private final String processingTime;
    private int totalResources;
    private int errorCount;
    private int warningCount;
    private int infoCount;

    private final List<ValidatedResource> validatedResources = new ArrayList<>();
    private final List<AppliedProcessing> appliedProcessing = new ArrayList<>();
    private final List<ValidationMessage> validationMessages = new ArrayList<>();

    /**
     * Constructor that populates report fields from a HAPI FHIR ValidationResult
     *
     * @param result HAPI FHIR ValidationResult instance
     * @param resourceTypeCounts Map of Resource Type to count (e.g., "Patient" -> 1)
     * @param processingDurationMs Duration of the validation/processing in milliseconds
     */
    public Report(ValidationResult result, Map<String, ResourceStatistics> resourceTypeCounts, long processingDurationMs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        this.generationTime = now.format(formatter);
        this.validationTime = now.format(formatter);
        this.processingTime = String.format("%.2f seconds", processingDurationMs / 1000.0);

        // Determine overall status
        boolean isSuccessful = result.isSuccessful();
        this.status = ValidationStatus.valueOf(isSuccessful ? "passed" : "failed");

        // Extract messages and count severity levels from HAPI FHIR result
        if (result.getMessages() != null) {
            for (SingleValidationMessage msg : result.getMessages()) {
                String severity = msg.getSeverity() != null ? msg.getSeverity().name() : "INFORMATION";

                switch (severity.toUpperCase()) {
                    case "ERROR", "FATAL" -> this.errorCount++;
                    case "WARNING" -> this.warningCount++;
                    default -> this.infoCount++;
                }

                // Map location and details from HAPI message
                String location = msg.getLocationString() != null ? msg.getLocationString() : "Global";
                String details = msg.getMessage();

                this.validationMessages.add(new ValidationMessage(severity, location, details));
            }
        }

        // Set high-level status text
        if (this.errorCount > 0) {
            this.validationStatus = "FAILED (" + this.errorCount + " ERRORS)";
        } else if (this.warningCount > 0) {
            this.validationStatus = "PASSED WITH WARNINGS";
        } else {
            this.validationStatus = "PASSED";
        }

        // Populate resource breakdown
        if (resourceTypeCounts != null) {
            int total = 0;
            for (Map.Entry<String, ResourceStatistics> entry : resourceTypeCounts.entrySet()) {
                total += entry.getValue().counter();
                this.validatedResources.add(new ValidatedResource(entry.getKey(), entry.getValue().counter(), "Validated", entry.getValue().percentage(), entry.getValue().missingPaths()));
            }
            this.totalResources = total;
        }
    }

    public String getGenerationTime() { return generationTime; }

    public ValidationStatus getStatus() { return status; }
    public void setStatus(ValidationStatus status) { this.status = status; }

    public String getValidationStatus() { return validationStatus; }

    public String getValidationTime() { return validationTime; }

    public String getProcessingTime() { return processingTime; }

    public int getTotalResources() { return totalResources; }

    public int getErrorCount() { return errorCount; }

    public int getWarningCount() { return warningCount; }

    public int getInfoCount() { return infoCount; }

    public List<ValidatedResource> getValidatedResources() { return validatedResources; }

    public List<AppliedProcessing> getAppliedProcessing() { return appliedProcessing; }

    public List<ValidationMessage> getValidationMessages() { return validationMessages; }

    public static class ValidatedResource {
        private String type;
        private final Long count;
        private String status;
        private final double mustSupportPercentage;
        private final Set<String> missingPaths;

        public ValidatedResource(String type, Long count, String status, double mustSupportPercentage, Set<String> missingPaths) {
            this.type = type;
            this.count = count;
            this.status = status;
            this.mustSupportPercentage = mustSupportPercentage;
            this.missingPaths = missingPaths;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getCount() { return count; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Set<String> getMissingPaths() {
            return missingPaths;
        }

        public double getMustSupportPercentage() {
            return mustSupportPercentage;
        }
    }

    public record AppliedProcessing(String resourceId, String action, String targetField) {
    }

    public static class ValidationMessage {
        private String type;
        private final String location;
        private final String details;

        public ValidationMessage(String type, String location, String details) {
            this.type = type;
            this.location = location;
            this.details = details;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLocation() { return location; }
        public String getDetails() { return details; }
    }
}