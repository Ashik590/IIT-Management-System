package edu.du.iit.cms.pattern.state;

import edu.du.iit.cms.domain.CourseStatus;

public interface CourseState {
    CourseStatus status();

    void ensureCanConfigure();

    void ensureCanManageAcademics();

    void activate(CourseLifecycle course);

    void finish(CourseLifecycle course);
}

