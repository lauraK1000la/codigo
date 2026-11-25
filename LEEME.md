# Proyecto en pareja · Tienda con arquitectura de eventos

Laura Navarrete y Joiner Pineda · Variante **Q1 / fila 1** (Mouse, $45.000, stock 40, u = 5, d = 10 %).

El código está en `codigo/`. Partimos de `Arquitectura eventos/tienda`, armado tal como lo describe `GUIA-paso-a-paso-eventos.md`, y lo adaptamos a la Hoja 1. Las decisiones están en `docs/adr/`.

Se necesita JDK 21 y Maven (`mvn -v`). Las muestras **no** necesitan Docker: usan el bus en memoria (ver ADR-0001). Cada corrida arranca limpia, porque todo vive en memoria.

## Muestra de la Hoja 1

Desde la raíz del proyecto:

```bash
cd codigo
mvn -q test-compile exec:java "-Dexec.classpathScope=test" "-Dexec.mainClass=com.ejemplo.tienda.MuestraHoja1"
```

Salida esperada (fila 1):

```text
PEDIDO 1 CONFIRMADO total=180000
PEDIDO 2 CONFIRMADO total=202500
PEDIDO 3 RECHAZADO total=0
PEDIDO 4 CONFIRMADO total=1255500
```

En la etiqueta `final`, la línea 4 sale `PEDIDO 4 CONFIRMADO total=1185750`: la Hoja 2 (Q1) dice que esa línea cambia, porque 31 unidades ≥ 3u = 15 reciben el tramo nuevo. Las líneas 1 a 3 no cambian.

## Muestra de la Hoja 2 (Q1, tramo nuevo de descuento)

En una corrida aparte, desde la raíz del proyecto:

```bash
cd codigo
mvn -q test-compile exec:java "-Dexec.classpathScope=test" "-Dexec.mainClass=com.ejemplo.tienda.MuestraHoja2"
```

Salida esperada (fila 1):

```text
PEDIDO 1 CONFIRMADO total=573750
PEDIDO 2 CONFIRMADO total=567000
PEDIDO 3 CONFIRMADO total=45000
```

## Regla de arquitectura

Ningún módulo (`orders`, `inventory`, `billing`, `notifications`) importa a otro; `event` y `bus` no importan módulos; solo `bus` importa RabbitMQ y JSON. Se verifica de dos formas, cualquiera basta:

```bash
bash codigo/verificar-regla.sh
```

```bash
cd codigo
mvn test "-Dtest=ModuleIndependenceTest"
```

## Pruebas

```bash
cd codigo
mvn test
```

Sin RabbitMQ, las 3 pruebas de `RabbitMqEventBusTest` salen como omitidas (*skipped*). Para correrlas, y para correr la demo `TiendaApplication` sobre el broker real: `docker compose up -d --wait` dentro de `codigo/`, luego `mvn test` o `mvn compile exec:java`.
