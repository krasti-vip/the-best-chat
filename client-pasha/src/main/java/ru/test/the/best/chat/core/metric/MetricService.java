package ru.test.the.best.chat.core.metric;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MetricService {

    private final MeterRegistry registry;
    private final String serviceName;
    private final String entityName;

    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Counter notFoundCounter;

    public MetricService(
            final MeterRegistry registry,
            final Class<?> serviceClass,
            final Class<?> entityClass) {
        this.registry = registry;
        this.serviceName = serviceClass.getCanonicalName();
        this.entityName = entityClass.getCanonicalName();

        // Создаем метрики которые не зависят от параметров СРАЗУ
        this.notFoundCounter = Counter.builder("service.entity.notfound")
                .tag("service", serviceName)
                .tag("entity", entityName)
                .register(registry);
    }

    public Timer timer(final OperationMetric operationMetric) {
        final var operation = operationMetric.createNameOperationForMetric(this.entityName);
        return timers.computeIfAbsent(operation, op ->
                Timer.builder("service.operation.duration")
                        .tag("service", serviceName)
                        .tag("entity", entityName)
                        .tag("operation", op)
                        .register(registry)
        );
    }

    public void recordSuccess(final OperationMetric operationMetric) {
        final var operation = operationMetric.createNameOperationForMetric(this.entityName);
        successCounters.computeIfAbsent(operation, op ->
                Counter.builder("service.operation.success")
                        .tag("service", serviceName)
                        .tag("entity", entityName)
                        .tag("operation", op)
                        .register(registry)
        ).increment();
    }

    public void recordError(final OperationMetric operationMetric) {
        final var operation = operationMetric.createNameOperationForMetric(this.entityName);
        errorCounters.computeIfAbsent(operation, op ->
                Counter.builder("service.operation.error")
                        .tag("service", serviceName)
                        .tag("entity", entityName)
                        .tag("operation", op)
                        .register(registry)
        ).increment();
    }

    public void recordNotFound() {
        notFoundCounter.increment();
    }
}

