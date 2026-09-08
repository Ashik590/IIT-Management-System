package edu.du.iit.cms.repository;

import edu.du.iit.cms.db.Database;
import edu.du.iit.cms.domain.ResourceItem;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ResourceRepository {
    private final Database database;

    public ResourceRepository(Database database) {
        this.database = database;
    }

    public long insert(long courseId, String originalFilename, String storedFilename, Path storedPath,
                       String contentType, long fileSize, long uploaderId) {
        String sql = """
                INSERT INTO resources(course_id,original_filename,stored_filename,stored_path,content_type,file_size,uploader_id)
                VALUES(?,?,?,?,?,?,?)
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, courseId);
            statement.setString(2, originalFilename);
            statement.setString(3, storedFilename);
            statement.setString(4, storedPath.toAbsolutePath().toString());
            statement.setString(5, contentType);
            statement.setLong(6, fileSize);
            statement.setLong(7, uploaderId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not store resource metadata: " + exception.getMessage(), exception);
        }
    }

    public List<ResourceItem> findByCourse(long courseId) {
        String sql = "SELECT * FROM resources WHERE course_id=? ORDER BY uploaded_at DESC,id DESC";
        List<ResourceItem> resources = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    resources.add(new ResourceItem(result.getLong("id"), result.getLong("course_id"),
                            result.getString("original_filename"), result.getString("stored_filename"),
                            Path.of(result.getString("stored_path")), result.getString("content_type"),
                            result.getLong("file_size"), result.getLong("uploader_id"),
                            parseDateTime(result.getString("uploaded_at"))));
                }
            }
            return resources;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load resources: " + exception.getMessage(), exception);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value.replace(' ', 'T'));
    }
}
