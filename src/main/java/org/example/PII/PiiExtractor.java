package org.example.PII;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PiiExtractor {

    private static final Set<String> FIRST_NAMES = loadFileIntoSet("src/main/resources/PII/2000_german_firstnames.txt");
    private static final Set<String> FEMALE_NAMES = loadFileIntoSet("src/main/resources/PII/top_female_names_germany.txt");
    private static final Set<String> SURNAMES = loadFileIntoSet("src/main/resources/PII/craft-data-german-surnames.txt");
    private static final Set<String> LAST_NAMES = loadFileIntoSet("src/main/resources/PII/german_lastnames.txt");
    private static final Set<String> CITIES = loadFileIntoSet("src/main/resources/PII/craft-data-german-cities.txt");

    static HashSet<String> pii = new HashSet<>();
    // Regular Expressions
    // Matches phone numbers like +49 30 1234567, 030-123456, or (089) 12345
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<![\\w\\-])(?:(?:\\+|00)\\d{2,3}[\\s\\-]?\\d{9,11}|0\\d{1,4}[\\s\\-]?\\d{4,8})\\b"
    );

    // Matches standard German street addresses (e.g., Hauptstraße 12, 10115 Berlin)
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "([A-ZÄÖÜ][a-zäöüß]+(?:straße|str\\.|weg|platz)?\\s+\\d+[a-zA-Z]?)[,\\s]+(\\d{5}\\s+[A-ZÄÖÜ][a-zäöüß]+)"
    );

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "(?:urn:uuid:)?[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );


    public static HashSet<String> extractEntities(String text) {

        text = UUID_PATTERN.matcher(text).replaceAll("");
        // 1. Extract and Validate Phone Numbers using Regex
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            int start = Math.max(0, phoneMatcher.start() - 20);
            int end = Math.min(text.length(), phoneMatcher.end() + 20);

            pii.add(text.substring(start, end));
        }

        // 2. Extract and Validate Addresses using Regex
        Matcher addressMatcher = ADDRESS_PATTERN.matcher(text);
        while (addressMatcher.find()) {
            pii.add(addressMatcher.group(1));
            pii.add(addressMatcher.group(2));
        }
        // merge both lists
        FIRST_NAMES.addAll(FEMALE_NAMES);
        SURNAMES.addAll(LAST_NAMES);

        // 3. Check for Names and Cities using List Lookups
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (FIRST_NAMES.contains(word)) {
                pii.add(word);
            }
            if (SURNAMES.contains(word)) {
                pii.add(word);
            }
            if (CITIES.contains(word)) {
                pii.add(word);
            }
        }
        return pii;
    }


    public static Set<String> loadFileIntoSet(String filePath) {
        Set<String> uniqueEntries = new HashSet<>();

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.ISO_8859_1);

            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    uniqueEntries.add(trimmed);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }

        return uniqueEntries;
    }
}
