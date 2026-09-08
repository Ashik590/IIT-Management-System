package edu.du.iit.cms.pattern.state;

import edu.du.iit.cms.domain.CourseStatus;

final class FinishedCourseState extends AbstractCourseState {
    @Override
    public CourseStatus status() {
        return CourseStatus.FINISHED;
    }
}

