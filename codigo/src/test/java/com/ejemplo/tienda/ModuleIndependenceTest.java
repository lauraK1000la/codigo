package com.ejemplo.tienda;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The dependency rule: modules only know {@code event} and {@code bus}, never each other,
 * and only {@code bus} knows RabbitMQ and JSON. It reads the source files and looks for
 * forbidden imports. The MODULES list is written by hand: a new module is not watched
 * until it is added here.
 */
class ModuleIndependenceTest {

    private static final Path ROOT = Path.of("src/main/java/com/ejemplo/tienda");
    private static final List<String> MODULES =
        List.of("orders", "inventory", "billing", "notifications");

    @Test
    void modulesDoNotImportEachOther() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String module : MODULES) {
            for (String other : MODULES) {
                if (!other.equals(module)) {
                    violations.addAll(filesImporting(module, "import com.ejemplo.tienda." + other + "."));
                }
            }
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void eventAndBusDoNotImportAnyModule() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String shared : List.of("event", "bus")) {
            for (String module : MODULES) {
                violations.addAll(filesImporting(shared, "import com.ejemplo.tienda." + module + "."));
            }
        }
        assertThat(violations).isEmpty();
    }

    // RabbitMQ and JSON are details of the bus adapter: nobody else may import them
    @Test
    void onlyTheBusAdapterTouchesRabbitMqAndJson() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String folder : List.of("event", "orders", "inventory", "billing", "notifications")) {
            violations.addAll(filesImporting(folder, "import com.rabbitmq"));
            violations.addAll(filesImporting(folder, "import com.fasterxml"));
        }
        assertThat(violations).isEmpty();
    }

    private static List<String> filesImporting(String folder, String forbidden) throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ROOT.resolve(folder))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (Files.readString(file).contains(forbidden)) {
                    found.add(folder + " -> " + forbidden + " (" + file.getFileName() + ")");
                }
            }
        }
        return found;
    }
}
