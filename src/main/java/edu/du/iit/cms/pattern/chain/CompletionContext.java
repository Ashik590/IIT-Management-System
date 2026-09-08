package edu.du.iit.cms.pattern.chain;

import edu.du.iit.cms.domain.Course;

public record CompletionContext(
        Course course,
        int requiredTeacherCount,
        int actualTeacherCount,
        int enrolledStudentCount,
        double totalCeWeight,
        int missingAssessmentMarks,
        int missingFinalExamMarks
) {
}

