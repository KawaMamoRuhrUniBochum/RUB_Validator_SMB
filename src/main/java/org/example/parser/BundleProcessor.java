package org.example.parser;

import org.example.context.FhirCtx;
import org.example.pdf.Report;
import org.example.validator.MustSupportResolver;
import org.example.validator.ResourceStatistics;
import org.example.validator.Validator;
import org.hl7.fhir.r4.model.*;

import java.util.*;

public class BundleProcessor {

    private Map<String, ResourceStatistics> resourceCounter;
    private final MustSupportResolver mustSupportResolver;
    public Set<Report.AppliedProcessing> appliedProcessing = new HashSet<>();


    public BundleProcessor(Validator validator) {
        mustSupportResolver = new MustSupportResolver(FhirCtx.getFhirContext(), validator);
    }

    public Map<String, ResourceStatistics> getResourceCounter() {
        return resourceCounter;
    }

    public void process(Bundle bundle, Bundle generatedBundle) {
        resourceCounter = new HashMap<>();

        ProfileResolver profileResolver = new ProfileResolver();

        if (bundle == null || !bundle.hasEntry()) {
            return;
        }

        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {

            if (component == null || component.getResource() == null) {
                continue;
            }
            Resource resource = component.getResource();
            String resourceType = resource.getResourceType().name();

            String profileUrl;

            if(resource.getMeta().getProfile().isEmpty()) {
                profileUrl = profileResolver.addProfile(resource);
                appliedProcessing.add(new Report.AppliedProcessing(
                                resourceType,
                                "Profile wurde basierend auf dem Ressourcentyp hinzugefügt",
                        "meta.profile"));
            }else if(resource.getResourceType().equals(ResourceType.Patient)) {
                profileUrl = profileResolver.setProfile((Patient) resource);
                appliedProcessing.add(new Report.AppliedProcessing(
                        resourceType,
                        "Profile PatientPseudonymisiert|2026.0.1 wurde hinzugefügt",
                        "meta.profile"));
            }else {
                profileUrl = resource.getMeta().getProfile().getFirst().getValue();
            }

            MustSupportResolver.CoverageResult coverageResult = mustSupportResolver.calculateCoverage(resource, profileUrl);
            double percentage = coverageResult.percentage();

            if (resource instanceof DomainResource domainResource) {
                long count = domainResource.getExtension().stream().filter(extension -> extension.hasUrl() && extension.getUrl()
                        .startsWith("http://vitagroup.local")).count();
                if(count>0)
                    appliedProcessing.add(new Report.AppliedProcessing(resourceType, "Extensions, die mit http://vitagroup.local beginnen, wurden entfernt", "extension"));
                domainResource.getExtension().removeIf(extension -> extension.hasUrl() && extension.getUrl()
                        .startsWith("http://vitagroup.local"));

            }

            String uuid = "urn:uuid:" + java.util.UUID.randomUUID();
            generatedBundle.addEntry()
                    .setResource(resource)
                    .setFullUrl(uuid);
            appliedProcessing.add(new Report.AppliedProcessing("Alle", "für jede Entry wurde eine FullURL ergänzt", "fullUrl"));

            if(Objects.isNull(resourceCounter.get(resourceType))){
                resourceCounter.put(resourceType, new ResourceStatistics(1L, percentage, coverageResult.missingPaths()));
            }else {
                //resourceCounter.get(resourceType).missingPaths().addAll(coverageResult.missingPaths());
                resourceCounter.put(resourceType,
                        new ResourceStatistics(resourceCounter.get(resourceType).counter()+1,
                                (resourceCounter.get(resourceType).counter()*resourceCounter.get(resourceType).percentage()+percentage)/(resourceCounter.get(resourceType).counter()+1),
                                resourceCounter.get(resourceType).missingPaths()));
            }
        }
    }
}
