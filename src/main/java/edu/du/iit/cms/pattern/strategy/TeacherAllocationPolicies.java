package edu.du.iit.cms.pattern.strategy;

import edu.du.iit.cms.domain.CourseType;

public final class TeacherAllocationPolicies {
    private final TeacherAllocationPolicy theory = new TheoryTeacherAllocationPolicy();
    private final TeacherAllocationPolicy lab = new LabTeacherAllocationPolicy();

    public TeacherAllocationPolicy forType(CourseType courseType) {
        return courseType == CourseType.THEORY ? theory : lab;
    }
}

