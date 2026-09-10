package edu.du.iit.cms.repository;

import edu.du.iit.cms.db.Database;
import edu.du.iit.cms.domain.CeStatus;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.CourseStatus;
import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.domain.CourseType;
import edu.du.iit.cms.domain.EnrollmentStatus;
import edu.du.iit.cms.domain.FinalResult;
import edu.du.iit.cms.domain.Teacher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CourseRepository {
    private static final String COURSE_COLUMNS =
            "c.id,c.course_code,c.title,c.course_type,c.credit,c.academic_session,c.semester,c.status,c.ce_status,c.finished_at";

    private final Database database;

    public CourseRepository(Database database) {
        this.database = database;
    }

    public long create(String code, String title, CourseType type, double credit,
                       String academicSession, String semester) {
        String sql = "INSERT INTO courses(course_code,title,course_type,credit,academic_session,semester) VALUES(?,?,?,?,?,?)";
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long courseId;
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, code);
                    statement.setString(2, title);
                    statement.setString(3, type.name());
                    statement.setDouble(4, credit);
                    statement.setString(5, academicSession);
                    statement.setString(6, semester);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        courseId = keys.getLong(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO assessment_components(course_id,title,weight_percentage,maximum_mark,component_type) "
                                + "VALUES(?,'Attendance',15,100,'ATTENDANCE')")) {
                    statement.setLong(1, courseId);
                    statement.executeUpdate();
                }
                connection.commit();
                return courseId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("create course", exception);
        }
    }

    public void updateDraft(long courseId, String code, String title, CourseType type, double credit,
                            String academicSession, String semester) {
        String sql = """
                UPDATE courses
                SET course_code=?,title=?,course_type=?,credit=?,academic_session=?,semester=?
                WHERE id=? AND status='DRAFT'
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setString(2, title);
            statement.setString(3, type.name());
            statement.setDouble(4, credit);
            statement.setString(5, academicSession);
            statement.setString(6, semester);
            statement.setLong(7, courseId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Only a Draft course can be updated.");
            }
        } catch (SQLException exception) {
            throw failure("update course", exception);
        }
    }

    public void deleteDraft(long courseId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM courses WHERE id=? AND status='DRAFT'")) {
            statement.setLong(1, courseId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Only a Draft course can be deleted.");
            }
        } catch (SQLException exception) {
            throw failure("delete Draft course", exception);
        }
    }

    public void resetFinished(long courseId) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        DELETE FROM assessment_marks
                        WHERE component_id IN (SELECT id FROM assessment_components WHERE course_id=?)
                        """)) {
                    statement.setLong(1, courseId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM attendance_sessions WHERE course_id=?")) {
                    statement.setLong(1, courseId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM enrollments WHERE course_id=?")) {
                    statement.setLong(1, courseId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE courses SET status='DRAFT',academic_session='',finished_at=NULL
                        WHERE id=? AND status='FINISHED'
                        """)) {
                    statement.setLong(1, courseId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Only a Finished course can be reset.");
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("reset Finished course", exception);
        }
    }

    public List<Course> findAll() {
        return queryCourses("SELECT " + COURSE_COLUMNS + " FROM courses c ORDER BY c.course_code", null);
    }

    public List<Course> findByTeacher(long teacherId) {
        String sql = "SELECT " + COURSE_COLUMNS + " FROM courses c "
                + "JOIN course_teachers ct ON ct.course_id=c.id "
                + "WHERE ct.teacher_id=? AND c.status<>'DRAFT' ORDER BY c.status,c.course_code";
        return queryCourses(sql, teacherId);
    }

    public List<Course> findByStudent(long studentId) {
        String sql = "SELECT " + COURSE_COLUMNS + " FROM courses c "
                + "JOIN enrollments e ON e.course_id=c.id "
                + "WHERE e.student_id=? AND c.status<>'DRAFT' ORDER BY c.status,c.course_code";
        return queryCourses(sql, studentId);
    }

    public Optional<Course> findById(long courseId) {
        String sql = "SELECT " + COURSE_COLUMNS + " FROM courses c WHERE c.id=?";
        List<Course> courses = queryCourses(sql, courseId);
        return courses.stream().findFirst();
    }

    public void assignTeacher(long courseId, long teacherId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO course_teachers(course_id,teacher_id) VALUES(?,?)")) {
            statement.setLong(1, courseId);
            statement.setLong(2, teacherId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("assign Teacher", exception);
        }
    }

    public void removeTeacher(long courseId, long teacherId) {
        deleteRelationship("DELETE FROM course_teachers WHERE course_id=? AND teacher_id=?",
                courseId, teacherId, "Teacher is not assigned to this course.");
    }

    public void enrollStudent(long courseId, long studentId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO enrollments(course_id,student_id) VALUES(?,?)")) {
            statement.setLong(1, courseId);
            statement.setLong(2, studentId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("enroll Student", exception);
        }
    }

    public void removeStudent(long courseId, long studentId) {
        deleteRelationship("DELETE FROM enrollments WHERE course_id=? AND student_id=?",
                courseId, studentId, "Student is not enrolled in this course.");
    }

    public int countTeachers(long courseId) {
        return count("SELECT COUNT(*) FROM course_teachers WHERE course_id=?", courseId);
    }

    public int countStudents(long courseId) {
        return count("SELECT COUNT(*) FROM enrollments WHERE course_id=?", courseId);
    }

    public boolean isTeacherAssigned(long courseId, long teacherId) {
        return exists("SELECT 1 FROM course_teachers WHERE course_id=? AND teacher_id=?", courseId, teacherId);
    }

    public boolean isStudentEnrolled(long courseId, long studentId) {
        return exists("SELECT 1 FROM enrollments WHERE course_id=? AND student_id=?", courseId, studentId);
    }

    public List<Teacher> findTeachers(long courseId) {
        String sql = """
                SELECT u.id,u.full_name,t.employee_id,t.designation
                FROM course_teachers ct
                JOIN users u ON u.id=ct.teacher_id
                JOIN teacher_profiles t ON t.user_id=u.id
                WHERE ct.course_id=? ORDER BY u.full_name
                """;
        List<Teacher> teachers = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    teachers.add(new Teacher(result.getLong("id"), result.getString("full_name"),
                            result.getString("employee_id"), result.getString("designation")));
                }
            }
            return teachers;
        } catch (SQLException exception) {
            throw failure("load assigned Teachers", exception);
        }
    }

    public List<CourseStudent> findStudents(long courseId) {
        String sql = """
                SELECT u.id,u.full_name,s.roll_number,e.status,e.final_exam_mark,e.ce_mark,e.total_mark
                FROM enrollments e
                JOIN users u ON u.id=e.student_id
                JOIN student_profiles s ON s.user_id=u.id
                WHERE e.course_id=? ORDER BY s.roll_number
                """;
        List<CourseStudent> students = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    students.add(new CourseStudent(result.getLong("id"), result.getString("full_name"),
                            result.getString("roll_number"), EnrollmentStatus.valueOf(result.getString("status")),
                            nullableDouble(result, "final_exam_mark"), nullableDouble(result, "ce_mark"),
                            nullableDouble(result, "total_mark")));
                }
            }
            return students;
        } catch (SQLException exception) {
            throw failure("load enrolled Students", exception);
        }
    }

    public void saveFinalExamMark(long courseId, long studentId, double mark) {
        String sql = "UPDATE enrollments SET final_exam_mark=? WHERE course_id=? AND student_id=?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, mark);
            statement.setLong(2, courseId);
            statement.setLong(3, studentId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Student is not enrolled in this course.");
            }
        } catch (SQLException exception) {
            throw failure("save final-exam mark", exception);
        }
    }

    public boolean allFinalExamMarksPresent(long courseId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE course_id=? AND final_exam_mark IS NULL";
        return count(sql, courseId) == 0;
    }

    public void updateStatus(long courseId, CourseStatus status) {
        String sql = "UPDATE courses SET status=? WHERE id=?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, courseId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("update course status", exception);
        }
    }

    public void finishCourse(long courseId, List<FinalResult> results) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String updateEnrollment = "UPDATE enrollments SET ce_mark=?,final_exam_mark=?,total_mark=?,status=? WHERE course_id=? AND student_id=?";
                try (PreparedStatement statement = connection.prepareStatement(updateEnrollment)) {
                    for (FinalResult result : results) {
                        statement.setDouble(1, result.ceMark());
                        statement.setDouble(2, result.finalExamMark());
                        statement.setDouble(3, result.totalMark());
                        statement.setString(4, result.enrollmentStatus().name());
                        statement.setLong(5, courseId);
                        statement.setLong(6, result.studentId());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE courses SET status='FINISHED',finished_at=CURRENT_TIMESTAMP WHERE id=? AND status='ACTIVE'")) {
                    statement.setLong(1, courseId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Course was not Active during completion.");
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("finish course", exception);
        }
    }

    private List<Course> queryCourses(String sql, Long argument) {
        List<Course> courses = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (argument != null) {
                statement.setLong(1, argument);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    courses.add(readCourse(result));
                }
            }
            return courses;
        } catch (SQLException exception) {
            throw failure("load courses", exception);
        }
    }

    private Course readCourse(ResultSet result) throws SQLException {
        String finishedAt = result.getString("finished_at");
        return new Course(result.getLong("id"), result.getString("course_code"), result.getString("title"),
                CourseType.valueOf(result.getString("course_type")), result.getDouble("credit"),
                result.getString("academic_session"), result.getString("semester"),
                CourseStatus.valueOf(result.getString("status")), CeStatus.valueOf(result.getString("ce_status")),
                finishedAt == null ? null : LocalDateTime.parse(finishedAt.replace(' ', 'T')));
    }

    private int count(String sql, long id) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw failure("count records", exception);
        }
    }

    private boolean exists(String sql, long first, long second) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            statement.setLong(2, second);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw failure("check relationship", exception);
        }
    }

    private void deleteRelationship(String sql, long first, long second, String missingMessage) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            statement.setLong(2, second);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException(missingMessage);
            }
        } catch (SQLException exception) {
            throw failure("remove course allocation", exception);
        }
    }

    private Double nullableDouble(ResultSet result, String column) throws SQLException {
        double value = result.getDouble(column);
        return result.wasNull() ? null : value;
    }

    private IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + ": " + exception.getMessage(), exception);
    }
}
