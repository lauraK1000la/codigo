package com.ejemplo.tienda.support;

import com.ejemplo.tienda.bus.RabbitMqEventBus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.Assumptions;

/** Tells whether the RabbitMQ of compose.yaml is up. */
public final class Broker {

    public static final RabbitMqEventBus.Settings SETTINGS = RabbitMqEventBus.Settings.fromEnv();

    private Broker() {
    }

    /** Marks the test as SKIPPED (not green) when nobody listens on the broker port. */
    public static void assumeRunning() {
        Assumptions.assumeTrue(isReachable(),
            "RabbitMQ is not reachable at " + SETTINGS.address() + " -> run: docker compose up -d");
    }

    private static boolean isReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(SETTINGS.host(), SETTINGS.port()), 500);
            return true;
        } catch (IOException error) {
            return false;
        }
    }
}
