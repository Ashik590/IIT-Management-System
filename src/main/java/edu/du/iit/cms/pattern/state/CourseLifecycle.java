package edu.du.iit.cms.pattern.state;

import edu.du.iit.cms.domain.CourseStatus;

public final class CourseLifecycle {
    private CourseState state;

    public CourseLifecycle(CourseStatus status) {
        state = switch (status) {
            case DRAFT -> new DraftCourseState();
            case ACTIVE -> new ActiveCourseState();
            case FINISHED -> new FinishedCourseState();
        };
    }

    public CourseStatus status() {
        return state.status();
    }

    public void ensureCanConfigure() {
        state.ensureCanConfigure();
    }

    public void ensureCanManageAcademics() {
        state.ensureCanManageAcademics();
    }

    public void activate() {
        state.activate(this);
    }

    public void finish() {
        state.finish(this);
    }

    void changeState(CourseState newState) {
        state = newState;
    }
}

