package edu.du.iit.cms.service;

import edu.du.iit.cms.db.Database;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.ResourceItem;
import edu.du.iit.cms.pattern.state.CourseLifecycle;
import edu.du.iit.cms.repository.CourseRepository;
import edu.du.iit.cms.repository.ResourceRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ResourceService {
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "txt", "md", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "png", "jpg", "jpeg", "zip"
    );

    private final Database database;
    private final CourseRepository courseRepository;
    private final ResourceRepository resourceRepository;

    public ResourceService(Database database, CourseRepository courseRepository,
                           ResourceRepository resourceRepository) {
        this.database = database;
        this.courseRepository = courseRepository;
        this.resourceRepository = resourceRepository;
    }

    public long upload(long teacherId, long courseId, Path source) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ValidationException("Course was not found."));
        new CourseLifecycle(course.status()).ensureCanManageAcademics();
        if (!courseRepository.isTeacherAssigned(courseId, teacherId)) {
            throw new ValidationException("You are not assigned to this course.");
        }
        if (source == null || !Files.isRegularFile(source)) {
            throw new ValidationException("Select an existing file.");
        }
        try {
            long size = Files.size(source);
            if (size > MAX_SIZE) {
                throw new ValidationException("Resource must not exceed 20 MB.");
            }
            String extension = extension(source.getFileName().toString());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new ValidationException("Unsupported file type: " + extension);
            }
            String storedName = UUID.randomUUID() + "." + extension;
            Path destination = database.resourceDirectory().resolve(storedName).normalize();
            if (!destination.startsWith(database.resourceDirectory())) {
                throw new ValidationException("Invalid resource destination.");
            }
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
            try {
                String contentType = Files.probeContentType(source);
                return resourceRepository.insert(courseId, source.getFileName().toString(), storedName,
                        destination, contentType, size, teacherId);
            } catch (RuntimeException exception) {
                Files.deleteIfExists(destination);
                throw exception;
            }
        } catch (IOException exception) {
            throw new ValidationException("Could not copy the resource file: " + exception.getMessage(), exception);
        }
    }

    public List<ResourceItem> resources(long courseId, long userId) {
        boolean authorized = courseRepository.isStudentEnrolled(courseId, userId)
                || courseRepository.isTeacherAssigned(courseId, userId);
        if (!authorized) {
            throw new ValidationException("You cannot access resources for this course.");
        }
        return resourceRepository.findByCourse(courseId);
    }

    private String extension(String filename) {
        int separator = filename.lastIndexOf('.');
        if (separator < 0 || separator == filename.length() - 1) {
            throw new ValidationException("The selected file has no supported extension.");
        }
        return filename.substring(separator + 1).toLowerCase();
    }
}

