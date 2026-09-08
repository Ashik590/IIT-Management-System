package edu.du.iit.cms.domain;

import java.time.LocalDate;

public record AttendanceSummary(
        LocalDate classDate,
        String title,
        AttendanceStatus status
) {
}

