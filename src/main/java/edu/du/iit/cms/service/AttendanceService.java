package edu.du.iit.cms.service;

import edu.du.iit.cms.domain.AttendanceStatus;
import edu.du.iit.cms.domain.AttendanceSummary;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.pattern.state.CourseLifecycle;
import edu.du.iit.cms.repository.AttendanceRepository;
import edu.du.iit.cms.repository.CourseRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AttendanceService {
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;

    public AttendanceService(CourseRepository courseRepository, AttendanceRepository attendanceRepository) {
        this.courseRepository = courseRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public long createSession(long teacherId, long courseId, LocalDate date, String title,
                              Map<Long, AttendanceStatus> attendance) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ValidationException("Course was not found."));
        new CourseLifecycle(course.status()).ensureCanManageAcademics();
        if (!courseRepository.isTeacherAssigned(courseId, teacherId)) {
            throw new ValidationException("You are not assigned to this course.");
        }
        if (date == null) {
            throw new ValidationException("Class date is required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Attendance cannot be recorded for a future date.");
        }
        List<CourseStudent> roster = courseRepository.findStudents(courseId);
        Set<Long> expected = new HashSet<>();
        roster.forEach(student -> expected.add(student.studentId()));
        if (!expected.equals(attendance.keySet())) {
            throw new ValidationException("Attendance must include every enrolled Student exactly once.");
        }
        return attendanceRepository.createSession(courseId, date, title, teacherId, attendance);
    }

    public List<AttendanceSummary> studentHistory(long courseId, long studentId) {
        if (!courseRepository.isStudentEnrolled(courseId, studentId)) {
            throw new ValidationException("Student is not enrolled in this course.");
        }
        return attendanceRepository.findStudentHistory(courseId, studentId);
    }

    public AttendanceRepository.AttendanceCount count(long courseId, long studentId) {
        return attendanceRepository.countForStudent(courseId, studentId);
    }
}

