package edu.du.iit.cms.repository;

import edu.du.iit.cms.db.Database;
import edu.du.iit.cms.domain.AssessmentComponent;
import edu.du.iit.cms.domain.AssessmentComponentType;
import edu.du.iit.cms.domain.CeStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class EvaluationRepository {
    private final Database database;

    public EvaluationRepository(Database database) {
        this.database = database;
    }

    public long addComponent(long courseId, String title, double weight, double maximumMark) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long componentId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO assessment_components(course_id,title,weight_percentage,maximum_mark) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, courseId);
                    statement.setString(2, title);
                    statement.setDouble(3, weight);
                    statement.setDouble(4, maximumMark);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        componentId = keys.getLong(1);
                    }
                }
                setCeStatus(connection, courseId, CeStatus.DRAFT);
                connection.commit();
                return componentId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("add assessment component", exception);
        }
    }

    public void updateWeight(long courseId, long componentId, double weight) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE assessment_components SET weight_percentage=? WHERE id=? AND course_id=?")) {
                    statement.setDouble(1, weight);
                    statement.setLong(2, componentId);
                    statement.setLong(3, courseId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Assessment component was not found.");
                    }
                }
                setCeStatus(connection, courseId, CeStatus.DRAFT);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("update assessment weight", exception);
        }
    }

    public void deleteComponent(long courseId, long componentId) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM assessment_components WHERE id=? AND course_id=?")) {
                    statement.setLong(1, componentId);
                    statement.setLong(2, courseId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Assessment component was not found.");
                    }
                }
                setCeStatus(connection, courseId, CeStatus.DRAFT);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("delete assessment component", exception);
        }
    }

    public List<AssessmentComponent> findComponents(long courseId) {
        List<AssessmentComponent> components = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id,course_id,title,weight_percentage,maximum_mark,component_type FROM assessment_components WHERE course_id=? ORDER BY id")) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    components.add(new AssessmentComponent(result.getLong("id"), result.getLong("course_id"),
                            result.getString("title"), result.getDouble("weight_percentage"),
                            result.getDouble("maximum_mark"),
                            AssessmentComponentType.valueOf(result.getString("component_type"))));
                }
            }
            return components;
        } catch (SQLException exception) {
            throw failure("load assessment components", exception);
        }
    }

    public void finalizeStructure(long courseId) {
        try (Connection connection = database.openConnection()) {
            setCeStatus(connection, courseId, CeStatus.FINALIZED);
        } catch (SQLException exception) {
            throw failure("finalize CE structure", exception);
        }
    }

    public double totalWeight(long courseId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COALESCE(SUM(weight_percentage),0) FROM assessment_components WHERE course_id=?")) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getDouble(1);
            }
        } catch (SQLException exception) {
            throw failure("calculate CE weight", exception);
        }
    }

    public void saveMark(long componentId, long studentId, double obtainedMark) {
        String sql = """
                INSERT INTO assessment_marks(component_id,student_id,obtained_mark) VALUES(?,?,?)
                ON CONFLICT(component_id,student_id) DO UPDATE SET obtained_mark=excluded.obtained_mark
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, componentId);
            statement.setLong(2, studentId);
            statement.setDouble(3, obtainedMark);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("save assessment mark", exception);
        }
    }

    public Double findMark(long componentId, long studentId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT obtained_mark FROM assessment_marks WHERE component_id=? AND student_id=?")) {
            statement.setLong(1, componentId);
            statement.setLong(2, studentId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getDouble(1) : null;
            }
        } catch (SQLException exception) {
            throw failure("load assessment mark", exception);
        }
    }

    public double calculateCe(long courseId, long studentId) {
        String sql = """
                SELECT COALESCE(SUM((CASE c.component_type
                    WHEN 'ATTENDANCE' THEN COALESCE((
                        SELECT AVG(CASE WHEN ar.status='PRESENT' THEN 1.0 ELSE 0.0 END)
                        FROM attendance_records ar
                        JOIN attendance_sessions ats ON ats.id=ar.session_id
                        WHERE ats.course_id=c.course_id AND ar.student_id=?
                    ),0)
                    ELSE m.obtained_mark / c.maximum_mark END) * (c.weight_percentage / 100.0)
                    * CASE course.course_type WHEN 'LAB' THEN 70.0 ELSE 40.0 END),0)
                FROM assessment_components c
                JOIN courses course ON course.id=c.course_id
                LEFT JOIN assessment_marks m ON m.component_id=c.id AND m.student_id=?
                WHERE c.course_id=?
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            statement.setLong(2, studentId);
            statement.setLong(3, courseId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getDouble(1);
            }
        } catch (SQLException exception) {
            throw failure("calculate CE", exception);
        }
    }

    public int countMissingMarks(long courseId) {
        String sql = """
                SELECT COUNT(*)
                FROM enrollments e
                JOIN assessment_components c ON c.course_id=e.course_id
                LEFT JOIN assessment_marks m ON m.component_id=c.id AND m.student_id=e.student_id
                WHERE e.course_id=? AND c.component_type='MANUAL' AND m.component_id IS NULL
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw failure("check missing marks", exception);
        }
    }

    private void setCeStatus(Connection connection, long courseId, CeStatus status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE courses SET ce_status=? WHERE id=?")) {
            statement.setString(1, status.name());
            statement.setLong(2, courseId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Course was not found.");
            }
        }
    }

    private IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + ": " + exception.getMessage(), exception);
    }
}
