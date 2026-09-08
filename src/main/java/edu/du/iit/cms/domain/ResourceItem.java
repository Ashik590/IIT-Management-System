package edu.du.iit.cms.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record ResourceItem(
        long id,
        long courseId,
        String originalFilename,
        String storedFilename,
        Path storedPath,
        String contentType,
        long fileSize,
        long uploaderId,
        LocalDateTime uploadedAt
) {
    @Override
    public String toString() {
        return originalFilename + " (" + fileSize + " bytes)";
    }
}

