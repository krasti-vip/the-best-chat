package ru.test.the.best.chat.core.metric;

public enum MetricOperationNameCore implements OperationMetric {

    FIND_ALL("find.all."),
    FIND_BY_ID("find.by.id."),
    DELETE_BY_ID("delete.by.id."),
    UPDATE("update."),
    SAVE("save.");

    private final String value;

    MetricOperationNameCore(final String value) {
        this.value = value;
    }

    @Override
    public String createNameOperationForMetric(final String nameClass) {
        return value + nameClass;
    }
}
