package edu.du.iit.cms.pattern.state;

import edu.du.iit.cms.service.ValidationException;

abstract class AbstractCourseState implements CourseState {
    @Override
    public void ensureCanConfigure() {
        throw unavailable("configure the course");
    }

    @Override
    public void ensureCanManageAcademics() {
        throw unavailable("manage academic records");
    }

    @Override
    public void activate(CourseLifecycle course) {
        throw unavailable("activate the course");
    }

    @Override
    public void finish(CourseLifecycle course) {
        throw unavailable("finish the course");
    }

    protected ValidationException unavailable(String operation) {
        return new ValidationException("Cannot " + operation + " while the course is " + status() + ".");
    }
}

