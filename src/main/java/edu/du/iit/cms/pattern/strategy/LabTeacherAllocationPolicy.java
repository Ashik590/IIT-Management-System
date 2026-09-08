package edu.du.iit.cms.pattern.strategy;

import edu.du.iit.cms.domain.CourseType;

public final class LabTeacherAllocationPolicy implements TeacherAllocationPolicy {
    @Override
    public CourseType courseType() {
        return CourseType.LAB;
    }

    @Override
    public int requiredTeachers() {
        return 2;
    }
}

