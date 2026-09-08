package edu.du.iit.cms.service;

import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.domain.StudentAcademicSummary;
import edu.du.iit.cms.repository.AttendanceRepository;
import edu.du.iit.cms.repository.CourseRepository;
import edu.du.iit.cms.repository.EvaluationRepository;

import java.util.List;

public final class ReportingService {
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final EvaluationRepository evaluationRepository;

    public ReportingService(CourseRepository courseRepository, AttendanceRepository attendanceRepository,
                            EvaluationRepository evaluationRepository) {
        this.courseRepository = courseRepository;
        this.attendanceRepository = attendanceRepository;
        this.evaluationRepository = evaluationRepository;
    }

    public StudentAcademicSummary studentSummary(long courseId, long studentId) {
        CourseStudent student = courseRepository.findStudents(courseId).stream()
                .filter(item -> item.studentId() == studentId)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Student is not enrolled in this course."));
        AttendanceRepository.AttendanceCount attendance = attendanceRepository.countForStudent(courseId, studentId);
        double currentCe = evaluationRepository.calculateCe(courseId, studentId);
        return new StudentAcademicSummary(student.studentId(), student.fullName(), student.rollNumber(),
                attendance.total(), attendance.present(), attendance.percentage(), currentCe,
                student.finalExamMark(), student.totalMark(), student.enrollmentStatus());
    }

    public List<StudentAcademicSummary> courseResultSheet(long courseId) {
        return courseRepository.findStudents(courseId).stream()
                .map(student -> studentSummary(courseId, student.studentId()))
                .toList();
    }
}

