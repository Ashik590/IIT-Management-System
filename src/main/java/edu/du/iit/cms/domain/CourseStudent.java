package edu.du.iit.cms.domain;

public record CourseStudent(
        long studentId,
        String fullName,
        String rollNumber,
        EnrollmentStatus enrollmentStatus,
        Double finalExamMark,
        Double ceMark,
        Double totalMark
) {
    @Override
    public String toString() {
        return rollNumber + " - " + fullName + " [" + enrollmentStatus + "]";
    }
}

