package edu.du.iit.cms.domain;

public record StudentAcademicSummary(
        long studentId,
        String studentName,
        String rollNumber,
        int totalSessions,
        int presentSessions,
        Double attendancePercentage,
        double ceMark,
        Double finalExamMark,
        Double totalMark,
        EnrollmentStatus enrollmentStatus
) {
}

