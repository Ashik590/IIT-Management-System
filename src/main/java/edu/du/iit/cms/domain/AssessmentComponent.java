package edu.du.iit.cms.domain;

public record AssessmentComponent(
        long id,
        long courseId,
        String title,
        double weightPercentage,
        double maximumMark,
        AssessmentComponentType type
) {
    @Override
    public String toString() {
        String source = type == AssessmentComponentType.ATTENDANCE ? " | calculated automatically" : " | max " + format(maximumMark);
        return title + " | weight " + format(weightPercentage) + "%" + source;
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.format("%.0f", value) : String.format("%.2f", value);
    }
}
