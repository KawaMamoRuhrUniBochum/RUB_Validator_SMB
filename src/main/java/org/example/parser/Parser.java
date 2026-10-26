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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public IBaseResource parseResource(String fileContent){
        fileContent = replaceAbbreviation(fileContent);
        IBaseResource iBaseResource = iParser.parseResource(fileContent);
        if(iBaseResource instanceof Bundle bundle)
            return processBundle(bundle);
        return iBaseResource;
    }

    private static String replaceAbbreviation(String fileContent) {
        Pattern TARGET_PATTERN = Pattern.compile("([a-zA-Z]{3}-m1)(/)");
        Matcher matcher = TARGET_PATTERN.matcher(fileContent);
        fileContent = matcher.replaceAll(matchResult -> {
            String digits = matchResult.group(1);
            String hashedDigits = generateSha256Hash(digits).substring(0, 8);
            String suffix = matchResult.group(2);
            return hashedDigits + suffix;
        });
        return fileContent;
    }

    public Bundle parseBundle(String fileContent){
        fileContent = replaceAbbreviation(fileContent);
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

    public static String generateSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert byte array into a hex string
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
