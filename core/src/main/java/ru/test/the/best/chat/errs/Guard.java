package ru.test.the.best.chat.errs;

import java.util.Collection;
import java.util.UUID;

public final class Guard {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private Guard() {
    }

    // Object
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    // String
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isBlank();
    }

    // UUID
    public static boolean isNullOrEmpty(UUID uuid) {
        return uuid == null || uuid.equals(EMPTY_UUID);
    }

    // Collection
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}

