package org.example.parser;

import ca.uhn.fhir.parser.IParser;
import org.example.config.ProcessingException;
import org.example.context.FhirCtx;
import org.example.validator.ResourceStatistics;
import org.example.validator.Validator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Parser {


    private final IParser iParser;
    private final BundleProcessor bundleProcessor;

    public Parser(Validator validator) {
        iParser = FhirCtx.getFhirContext().newJsonParser();
        bundleProcessor = new BundleProcessor(validator);
    }

    public BundleProcessor getBundleProcessor() {
        return bundleProcessor;
    }

    public Bundle processBundle(Bundle bundle){
        Bundle generatedBundle = generateBundle(bundle);
        bundleProcessor.process(bundle, generatedBundle);
        return generatedBundle;
    }

    private static @NonNull Bundle generateBundle(Bundle bundle) {
        Bundle generatedBundle = new Bundle();
        generatedBundle.setType(Bundle.BundleType.COLLECTION);
        return generatedBundle;
    }

    public Bundle parseBundle(String fileContent){
        try {
            Bundle bundle = iParser.parseResource(Bundle.class, fileContent);
            return processBundle(bundle);
        }catch (Exception e){
            throw new ProcessingException(e.getMessage());
        }
    }

    public static CodeSystem loadCodeSystem(String path, String url, String version){
        CodeSystem codeSystem;
        try {
            IParser parser = FhirCtx.getFhirContext().newJsonParser();
            InputStream inputStream = Files.newInputStream(Path.of(path));
            codeSystem = parser.parseResource(CodeSystem.class, inputStream);
            codeSystem.setUrl(url);
            codeSystem.setVersion(version);
            codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
            codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return codeSystem;
    }

    public String encodeResourceToString(IBaseResource resource){
        return iParser.encodeResourceToString(resource);
    }

    public Map<String, ResourceStatistics> getResourceCounter() {
        return bundleProcessor.getResourceCounter();
    }
}
