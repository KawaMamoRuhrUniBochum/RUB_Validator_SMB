package org.example.bean;

import ca.uhn.fhir.context.FhirContext;
import org.example.context.FhirCtx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Beans {

    @Bean
    public FhirContext getFhirContext(){
        return FhirCtx.getFhirContext();
    }
}
