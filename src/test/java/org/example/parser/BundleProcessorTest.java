package org.example.parser;

import ca.uhn.fhir.parser.IParser;
import org.example.bean.AppProperties;
import org.example.config.ProcessingException;
import org.example.context.FhirCtx;
import org.example.validator.ResourceStatistics;
import org.example.validator.Validator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DomainResource;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BundleProcessorTest {

    static BundleProcessor bundleProcessor;
    static Parser parser;

    @BeforeAll
    static void setUp(){
        AppProperties appProperties= new AppProperties();
        appProperties.setTerminologyServerUrl("https://tx.fhir.org/r4");
        var validator = new Validator(FhirCtx.getFhirContext(), appProperties);
        validator.init(); // init() made public for test purposes TODO: make init() private
        bundleProcessor = new BundleProcessor(validator);
        parser = new Parser(validator);
    }

    @Test
    void generatedBundlePatientsShouldHaveProfileOfPatientPseudonymisiert2026() throws IOException {
        Bundle generatedBundle = getBundle("src/main/resources/test/mii-resources/Bundle-mii-exa-base-test-data-bundle-1.json");
        assertEquals(Bundle.BundleType.TRANSACTION, generatedBundle.getType());
        // First Entry is a Patient and should be PatientPseudonymisiert
        assertEquals(new CanonicalType("https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/PatientPseudonymisiert|2026.0.1").asStringValue(),
                generatedBundle.getEntry().getFirst().getResource().getMeta().getProfile().getFirst().asStringValue());
    }

    @Test
    void allResourcesShouldHaveProfile() throws IOException {
        Bundle generatedBundle = getBundle("src/main/resources/test/mii-resources/Bundle-mii-exa-base-test-data-bundle-1.json");

        for (Bundle.BundleEntryComponent component : generatedBundle.getEntry()) {
            assertNotNull(component.getResource().getMeta().getProfile().getFirst());
        }

    }

    private static @NonNull Bundle getBundle(String path) throws IOException {
        IParser iParser = FhirCtx.getFhirContext().newJsonParser();
        Bundle bundle = iParser.parseResource(Bundle.class,
                Files.newInputStream(Path.of(path)));
        Bundle generatedBundle = new Bundle();
        generatedBundle.setType(bundle.getType());
        bundleProcessor.process(bundle, generatedBundle);
        return generatedBundle;
    }

    @Test
    void nonBundleShouldThrowException() throws IOException {
        IParser iParser = FhirCtx.getFhirContext().newJsonParser();
        try (InputStream inputStream = Files.newInputStream(Path.of("src/main/resources/test/mii-resources/Patient-mii-exa-person-patient-pseudonymisiert.json"))) {
            assertThrows(Exception.class, () ->
                    iParser.parseResource(Bundle.class, inputStream)
            );
        }
    }

    @Test
    void nonBundleShouldThrowProcessingException() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/test/mii-resources/Patient-mii-exa-person-patient-pseudonymisiert.json"));
        assertThrows(ProcessingException.class, ()->parser.parseBundle(content));
    }

    @Test
    void resourceCountShouldBe21() throws IOException {
        getBundle("src/main/resources/test/mii-resources/Bundle-mii-exa-base-test-data-bundle-1.json");
        Long count = bundleProcessor.getResourceCounter().values().stream().mapToLong(ResourceStatistics::counter).reduce(0, Long::sum);
        assertEquals(21, count);
    }

    @Test
    void allResourcesInMIIBundleExampleShouldHaveTypeAndId() throws IOException {
        Bundle bundle = getBundle("src/main/resources/test/mii-resources/Bundle-mii-exa-base-test-data-bundle-1.json");
        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
            DomainResource domainResource = (DomainResource) component.getResource();
            assertNotNull(domainResource.getResourceType().name());
            assertNotNull(domainResource.getIdPart());
        }
    }

    @Test
    void allResourcesInDMTBundleExampleShouldHaveType() throws IOException {
        Bundle bundle = getBundle("src/main/resources/test/export_Testantrag_PreProd_01_2025-03-31T12-55-06.json");
        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
            DomainResource domainResource = (DomainResource) component.getResource();
            assertNotNull(domainResource.getResourceType().name());
        }
    }
}