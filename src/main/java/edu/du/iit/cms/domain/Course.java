package edu.du.iit.cms.domain;

import java.time.LocalDateTime;

public record Course(
        long id,
        String courseCode,
        String title,
        CourseType courseType,
        double credit,
        String academicSession,
        String semester,
        CourseStatus status,
        CeStatus ceStatus,
        LocalDateTime finishedAt
) {
    @Override
    public String toString() {
        return courseCode + " - " + title + " [" + courseType + ", " + status + "]";
    }
}

