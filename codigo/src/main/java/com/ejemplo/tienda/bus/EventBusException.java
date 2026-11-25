package com.ejemplo.tienda.bus;

/** The bus could not do its job (broker down, message rejected...). */
public class EventBusException extends RuntimeException {

    public EventBusException(String message, Throwable cause) {
        super(message, cause);
    }
}
