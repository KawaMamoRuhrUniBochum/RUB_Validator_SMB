package org.example.bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class AppProperties {
    public String getTerminologyServerUrl() {
        return terminologyServerUrl;
    }

    public void setTerminologyServerUrl(String terminologyServerUrl) {
        this.terminologyServerUrl = terminologyServerUrl;
    }

    private String terminologyServerUrl;
}
