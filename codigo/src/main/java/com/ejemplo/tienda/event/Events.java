package com.ejemplo.tienda.event;

import java.util.Map;

/**
 * The list of events that exist. Messages travel as text, so a consumer needs to
 * turn the name written in the message ("OrderPlaced") into a class.
 */
public final class Events {

    private static final Map<String, Class<? extends Event>> BY_NAME = Map.of(
        "OrderPlaced", OrderPlaced.class,
        "StockReserved", StockReserved.class,
        "StockRejected", StockRejected.class);

    private Events() {
    }

    /** The name that travels with the message. */
    public static String nameOf(Event event) {
        return event.getClass().getSimpleName();
    }

    /** True if the class is one of the concrete events of this shop. */
    public static boolean isConcrete(Class<?> type) {
        return BY_NAME.containsValue(type);
    }

    public static Class<? extends Event> classNamed(String name) {
        Class<? extends Event> type = BY_NAME.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown event: " + name);
        }
        return type;
    }
}
