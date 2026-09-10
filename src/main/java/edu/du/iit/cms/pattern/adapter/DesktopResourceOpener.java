package edu.du.iit.cms.pattern.adapter;

import edu.du.iit.cms.service.ValidationException;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DesktopResourceOpener implements ResourceOpener {
    @Override
    public void open(Path resourcePath) {
        if (resourcePath == null || !Files.isRegularFile(resourcePath)) {
            throw new ValidationException("The stored file is missing or inaccessible.");
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new ValidationException("Opening files is not supported on this computer.");
        }
        try {
            Desktop.getDesktop().open(resourcePath.toFile());
        } catch (IOException | SecurityException exception) {
            throw new ValidationException("Could not open the resource: " + exception.getMessage(), exception);
        }
    }
}
