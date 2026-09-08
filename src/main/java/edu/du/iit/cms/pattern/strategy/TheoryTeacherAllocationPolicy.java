package edu.du.iit.cms.pattern.strategy;

import edu.du.iit.cms.domain.CourseType;

public final class TheoryTeacherAllocationPolicy implements TeacherAllocationPolicy {
    @Override
    public CourseType courseType() {
        return CourseType.THEORY;
    }

    @Override
    public int requiredTeachers() {
        return 1;
    }
}

