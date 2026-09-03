package org.example.smb;

import org.apache.camel.Exchange;
import org.jspecify.annotations.Nullable;

public class FileNameExtractor {

    public static @Nullable String getFileNameWithoutExt(Exchange exchange) {
        String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
        String fileNameWithoutExt = fileName;

        if (fileName != null && fileName.contains(".")) {
            fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        return fileNameWithoutExt;
    }
}
