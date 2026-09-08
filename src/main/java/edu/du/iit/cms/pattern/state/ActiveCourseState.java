package edu.du.iit.cms.pattern.state;

import edu.du.iit.cms.domain.CourseStatus;

final class ActiveCourseState extends AbstractCourseState {
    @Override
    public CourseStatus status() {
        return CourseStatus.ACTIVE;
    }

    @Override
    public void ensureCanManageAcademics() {
        // Active courses allow attendance, CE, resources, and final-exam marks.
    }

    @Override
    public void finish(CourseLifecycle course) {
        course.changeState(new FinishedCourseState());
    }
}

