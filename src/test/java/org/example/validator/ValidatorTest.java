package org.example.validator;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.example.bean.AppProperties;
import org.example.context.FhirCtx;
import org.example.parser.BundleProcessor;
import org.hl7.fhir.r4.model.Bundle;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    static Validator validator;
    static BundleProcessor bundleProcessor;

    @BeforeAll
    static void setUp(){
        AppProperties appProperties = new AppProperties();
        appProperties.setTerminologyServerUrl("https://tx.fhir.org/r4");
        validator = new Validator(FhirCtx.getFhirContext(), appProperties);
        validator.init();
        bundleProcessor = new BundleProcessor(validator);
    }

    @Test
    void MIIExampleBundleShouldBeValid() throws IOException {
        Bundle miiBundle = getBundle("src/main/resources/test/mii-resources/Bundle-mii-exa-base-test-data-bundle-1.json");
        ValidationResult validationResult = validator.validate(miiBundle);
        for (SingleValidationMessage message : validationResult.getMessages()) {
            System.out.println(message.getSeverity().getCode()+" "+message.getMessage());
        }
        assertTrue(validationResult.isSuccessful());
    }

    private static @NonNull Bundle getBundle(String path) throws IOException {
        IParser iParser = FhirCtx.getFhirContext().newJsonParser();
        return iParser.parseResource(Bundle.class,
                Files.newInputStream(Path.of(path)));
    }
}