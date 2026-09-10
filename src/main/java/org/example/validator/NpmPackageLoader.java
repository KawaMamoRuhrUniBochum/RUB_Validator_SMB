package org.example.validator;

import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

public class NpmPackageLoader {
    public void loadAllTgzPackagesFromClasspath(NpmPackageValidationSupport npmSupport) {

        try {
            npmSupport.loadPackageFromClasspath("packages/basicPackages/hl7.fhir.uv.xver-r5.r4-0.1.0.tgz");
            npmSupport.loadPackageFromClasspath("packages/basicPackages/hl7.fhir.r4.core-4.0.1.tgz");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            // Find all .tgz files in the classpath root and any subdirectories
            Resource[] resources = resolver.getResources("classpath*:/packages/*.tgz");

            for (Resource resource : resources) {
                // Get the filename or the relative classpath location
                String filename = resource.getFilename();
                System.out.println("loadAllTgzPackagesFromClasspath "+filename);
                if (filename != null) {
                    npmSupport.loadPackageFromClasspath(resource.getFilePath().getParent().getFileName()+"/"+filename);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
