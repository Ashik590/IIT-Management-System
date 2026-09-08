package edu.du.iit.cms.domain;

public record FinalResult(
        long studentId,
        double ceMark,
        double finalExamMark,
        double totalMark,
        EnrollmentStatus enrollmentStatus
) {
}

