package edu.du.iit.cms.pattern.adapter;

import java.nio.file.Path;

public interface ResourceOpener {
    void open(Path resourcePath);
}
