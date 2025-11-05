package ru.test.the.best.chat.message.metric;

import ru.test.the.best.chat.core.metric.OperationMetric;

public enum MetricOperationNameMessage implements OperationMetric {
    FIND_ALL_BY_FROM("find.all.by.from"),
    FIND_ALL_BY_TO("find.all.by.to"),
    FIND_CONVERSATION("find.conversation");

    private final String nameOperation;

    MetricOperationNameMessage(final String nameOperation) {
        this.nameOperation = nameOperation;
    }

    @Override
    public String createNameOperationForMetric(final String nameClass) {
        return nameOperation;
    }
}
