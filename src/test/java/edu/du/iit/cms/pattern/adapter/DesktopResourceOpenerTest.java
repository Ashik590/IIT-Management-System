package edu.du.iit.cms.pattern.adapter;

import edu.du.iit.cms.service.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DesktopResourceOpenerTest {
    @TempDir
    Path temporaryDirectory;

    private final ResourceOpener opener = new DesktopResourceOpener();

    @Test
    void rejectsMissingResourceBeforeCallingTheDesktopApi() {
        assertThrows(ValidationException.class, () -> opener.open(null));
        assertThrows(ValidationException.class, () -> opener.open(temporaryDirectory.resolve("missing.pdf")));
    }
}
