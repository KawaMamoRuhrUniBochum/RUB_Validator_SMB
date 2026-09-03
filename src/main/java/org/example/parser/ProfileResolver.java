package org.example.parser;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileResolver {

    private static final Logger log = LoggerFactory.getLogger(ProfileResolver.class);

    public static Map<String, String> setProfiles(){

        Map<String, String> profiles = new HashMap<>();

        String prefix = "https://www.medizininformatik-initiative.de/fhir/core/";

        profiles.put("Patient", prefix+"modul-person/StructureDefinition/PatientPseudonymisiert|2026.0.1");
        profiles.put("Procedure", prefix+"modul-prozedur/StructureDefinition/Procedure|2025.0.1");
        profiles.put("Encounter", prefix+"modul-fall/StructureDefinition/KontaktGesundheitseinrichtung|2026.0.1");
        profiles.put("Condition", prefix+"modul-diagnose/StructureDefinition/Diagnose|2025.0.1");
        profiles.put("MedicationStatement", prefix+"modul-medikation/StructureDefinition/MedicationStatement|2026.0.1");
        profiles.put("Medication", prefix+"modul-medikation/StructureDefinition/Medication|2026.0.1");
        profiles.put("List", prefix+"modul-medikation/StructureDefinition/medikationsliste|2026.0.1");
        profiles.put("MedicationRequest", prefix+"modul-medikation/StructureDefinition/MedicationRequest|2026.0.1");
        profiles.put("Observation", prefix+"modul-labor/StructureDefinition/ObservationLab|2026.0.1");
        profiles.put("DiagnosticReport", prefix+"modul-labor/StructureDefinition/DiagnosticReportLab|2026.0.3");
        profiles.put("MedicationAdministration", prefix+"modul-medikation/StructureDefinition/MedicationAdministration|2026.0.1");
        return profiles;
    }

    public String addProfile(Resource resource){
        Map<String, String> profiles;
        profiles = ProfileResolver.setProfiles();
        String profileUrl = profiles.get(resource.getResourceType().name());

        if(Objects.nonNull(profileUrl)) {
            resource.getMeta().addProfile(profileUrl);
        }else {
            log.atInfo().log("No Profile Set for "+resource.getResourceType().name());
        }
        return profileUrl;
    }

    public String setProfile(Patient patient){
        patient.getMeta().getProfile().removeIf(r -> true);

        Map<String, String> profiles;
        profiles = ProfileResolver.setProfiles();
        String profileUrl = profiles.get(patient.getResourceType().name());

        patient.getMeta().addProfile(profileUrl);
        return profileUrl;
    }
}
