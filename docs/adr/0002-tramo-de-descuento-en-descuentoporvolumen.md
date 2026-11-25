# ADR-0002 · El tramo nuevo de descuento vive solo en DescuentoPorVolumen

Variante: Q1 / fila 1

- Fecha: 2026-11-24 19:19
- Estado: aceptada (mantiene la ADR-0001)

## Contexto

La Hoja 2 (Q1) agrega un tramo de descuento que rige desde ya: si la cantidad es ≥ 3u, el descuento es d + 5 en lugar de d. Para la fila 1 (Mouse, $45.000, u = 5, d = 10 %), eso es 15 % desde 15 unidades. De u a 3u − 1 (5 a 14 unidades) sigue el 10 %, y por debajo de 5 no hay descuento. La línea 4 de la muestra de la Hoja 1 (31 unidades) pasa de 1255500 a 1185750, como lo indica la Hoja 2. El ADR-0001 apostó a que este cambio quedaría dentro de `inventory/DescuentoPorVolumen`.

## Alternativas consideradas

- **Pasar el tramo como parámetro nuevo desde `Tienda`** (umbral alto y porcentaje alto en el constructor). Descartada porque obliga a modificar `Tienda`, el cableado, y a crear un segundo constructor o cambiar el actual, que usan las pruebas de la Hoja 1. Para un tramo que la Hoja 2 define en función de u y d (3u, d + 5), basta derivarlo dentro de la regla.
- **Calcular el tramo en `InventoryService`**, al lado del cálculo del total. Descartada porque mezcla la regla comercial con RN-3 (stock) en la clase que decide si un pedido se reserva. Es justo lo que la ADR-0001 quiso evitar.
- **Un motor configurable de tramos** (una lista de tramos leída de configuración). Descartada por sobreabstracción: es más código para dos tramos fijos.

## Decisión

El tramo vive en `DescuentoPorVolumen.porcentajePara`, como dos constantes (`MULTIPLO_TRAMO_ALTO = 3` y `PUNTOS_EXTRA_TRAMO_ALTO = 5`) y una condición antes de la de RN-1. Se agregó una prueba nueva, `DescuentoPorVolumenTramoTest`, y una muestra nueva, `MuestraHoja2`. No se tocó ninguna prueba de la Hoja 1.

## Consecuencias

**Se gana:** el cambio midió 1 archivo modificado de `src/main` y 2 creados en `src/test` (`git diff --name-status hoja-1 final`). `OrderService`, `InventoryService`, los eventos, el bus y `Tienda` no cambiaron. Las 20 pruebas pasan, incluidas las de la Hoja 1 sin editar.

**Se pierde:** el tramo alto quedó fijo en código (3u y d + 5). Si el equipo comercial pide otro multiplicador o un tramo distinto por producto, hay que volver a editar la clase y no basta con configurarla en `Tienda`. Además, las pruebas de la Hoja 1 no se rompieron porque no cubrían cantidades ≥ 3u. Ninguna prueba previa protegía ese rango.

**Veredicto sobre la ADR-0001.** La predicción acertó en todo: 2 archivos creados con los nombres exactos que dijo, 1 modificado (`DescuentoPorVolumen`), sí se tocó la clase que calcula el total, no se tocó la que registra el pedido (`OrderService`) ni ninguna prueba de la Hoja 1. Lo que la ADR-0001 dijo que se perdería sí ocurrió, aunque no pesó en este cambio. La muestra de la Hoja 2 tuvo que llamar a `bus.processAll()` antes de imprimir, porque los tres pedidos nacen `PENDIENTE` y sin total (consistencia eventual). Y la muestra siguió sin recorrer RabbitMQ. Mantenemos la decisión.
