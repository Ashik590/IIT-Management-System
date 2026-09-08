package edu.du.iit.cms.repository;

import edu.du.iit.cms.db.Database;
import edu.du.iit.cms.domain.AttendanceStatus;
import edu.du.iit.cms.domain.AttendanceSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AttendanceRepository {
    private final Database database;

    public AttendanceRepository(Database database) {
        this.database = database;
    }

    public long createSession(long courseId, LocalDate classDate, String title, long teacherId,
                              Map<Long, AttendanceStatus> attendance) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long sessionId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO attendance_sessions(course_id,class_date,title,created_by) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, courseId);
                    statement.setString(2, classDate.toString());
                    statement.setString(3, title == null || title.isBlank() ? null : title.trim());
                    statement.setLong(4, teacherId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        sessionId = keys.getLong(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO attendance_records(session_id,student_id,status) VALUES(?,?,?)")) {
                    for (Map.Entry<Long, AttendanceStatus> entry : attendance.entrySet()) {
                        statement.setLong(1, sessionId);
                        statement.setLong(2, entry.getKey());
                        statement.setString(3, entry.getValue().name());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
                return sessionId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("save attendance session", exception);
        }
    }

    public List<AttendanceSummary> findStudentHistory(long courseId, long studentId) {
        String sql = """
                SELECT s.class_date,s.title,r.status
                FROM attendance_records r
                JOIN attendance_sessions s ON s.id=r.session_id
                WHERE s.course_id=? AND r.student_id=?
                ORDER BY s.class_date,s.id
                """;
        List<AttendanceSummary> history = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, studentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    history.add(new AttendanceSummary(LocalDate.parse(result.getString("class_date")),
                            result.getString("title"), AttendanceStatus.valueOf(result.getString("status"))));
                }
            }
            return history;
        } catch (SQLException exception) {
            throw failure("load attendance history", exception);
        }
    }

    public AttendanceCount countForStudent(long courseId, long studentId) {
        String sql = """
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN r.status='PRESENT' THEN 1 ELSE 0 END),0) AS present
                FROM attendance_records r
                JOIN attendance_sessions s ON s.id=r.session_id
                WHERE s.course_id=? AND r.student_id=?
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, studentId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return new AttendanceCount(result.getInt("total"), result.getInt("present"));
            }
        } catch (SQLException exception) {
            throw failure("calculate attendance", exception);
        }
    }

    private IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + ": " + exception.getMessage(), exception);
    }

    public record AttendanceCount(int total, int present) {
        public Double percentage() {
            return total == 0 ? null : present * 100.0 / total;
        }
    }
}

