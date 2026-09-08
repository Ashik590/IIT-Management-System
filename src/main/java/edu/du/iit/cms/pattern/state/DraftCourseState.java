package edu.du.iit.cms.pattern.state;

import edu.du.iit.cms.domain.CourseStatus;

final class DraftCourseState extends AbstractCourseState {
    @Override
    public CourseStatus status() {
        return CourseStatus.DRAFT;
    }

    @Override
    public void ensureCanConfigure() {
        // Draft courses allow Teacher assignment and Student enrollment.
    }

    @Override
    public void activate(CourseLifecycle course) {
        course.changeState(new ActiveCourseState());
    }
}

