package edu.du.iit.cms.domain;

public record AssessmentComponent(
        long id,
        long courseId,
        String title,
        double weightPercentage,
        double maximumMark
) {
    @Override
    public String toString() {
        return title + " | weight " + format(weightPercentage) + "% | max " + format(maximumMark);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.format("%.0f", value) : String.format("%.2f", value);
    }
}

