# ADR-0001 · Eventos para la tienda, con el descuento aislado en inventario

Variante: Q1 / fila 1

- Fecha: 2026-11-24 19:13
- Estado: aceptada

## Contexto

La tienda vende un solo producto: Mouse (`productoId` 1), a $45.000, con 40 unidades de stock inicial, umbral de volumen u = 5 y descuento d = 10 %. El atributo prioritario es Q1, modificabilidad de las reglas de descuento: el equipo comercial cambia esas reglas con frecuencia, y cambiar una no debe obligar a tocar lo que no es descuento. Ya sabemos qué cambio viene en la Hoja 2: un tramo nuevo, d + 5 = 15 % desde 3u = 15 unidades. Con el stock de 40 y un umbral de 5, el tramo de 15 unidades es una cantidad que la tienda sí puede vender, porque cabe 2 veces en el stock. No es un caso teórico.

Hay que decidir dónde vive la regla de descuento. Lo que se mide es si se puede cambiar sin reabrir el registro del pedido (el que asigna el `id` y el estado) ni el control de stock (RN-3).

## Alternativas consideradas

- **Capas** (`Arquitectura capas/catalogo`). Descartada porque en capas la regla queda en el `service`, que es el mismo que registra el pedido y descuenta el stock. El tramo nuevo de 15 unidades reabriría la clase que asigna el `id` y decide RN-3. Para Q1 eso es justo lo que no se quiere. Además, la carpeta no trae pedidos, así que habría que escribirlos completos. La diapositiva «Arquitectura por capas» ordena por tipo de tarea (controlador, servicio, repositorio), no por qué regla cambia.
- **Hexagonal** (`Arquitectura hexagonal/catalogo`). Descartada aunque su guía la recomienda cuando «el dominio tiene reglas que cambian». Lo que hexagonal separa es el negocio de la tecnología. El tramo nuevo es una regla de negocio dentro del dominio, y quedaría en una clase del dominio igual que aquí. Para un cambio de un umbral (5 → 15 unidades) en un único producto, la ganancia para Q1 es la misma, pero hay que construir desde cero pedido, puertos, adaptadores y mapeos (la carpeta no trae pedidos). Es el costo que la guía llama «mantener los mapeos pesa más que el beneficio».
- **Monolito modular** (`Arquitectura monilito modular/tienda`). Descartada porque el total con descuento se calcula en `pedidos`, en la misma transacción que registra el `Order`. Allí el tramo nuevo vuelve a caer en la clase que registra el pedido. Sacarlo a otra clase obliga a «decidir dónde cortar» (Desventajas de su guía) entre `catalogo`, que tiene el precio de $45.000, y `pedidos`, que tiene la cantidad. Además, el punto de partida lanza 409 y no guarda el pedido sin stock, y nosotros necesitamos guardarlo como `RECHAZADO` con `id`.
- **Eventos** (`Arquitectura eventos/tienda`). **Elegida.** En el punto de partida, el total ya se calcula en `InventoryService`, que es quien conoce el precio, y no en `OrderService`, que registra el pedido. Entre los dos hay un bus, y `ModuleIndependenceTest` impide que uno importe al otro. Si la regla de descuento vive en su propia clase dentro de `inventory`, cambiarla no puede tocar `orders` sin que la regla de arquitectura falle.

## Decisión

Usamos la arquitectura de eventos. La regla de descuento (RN-1 y RN-2) vive en una sola clase, `inventory/DescuentoPorVolumen`, con u y d como parámetros. `InventoryService` la usa para calcular el total cuando reserva stock (RN-3). `OrderService` solo valida (RN-5, de forma síncrona), asigna el `id` consecutivo y publica `OrderPlaced`. Para RN-5 recibe un `LongPredicate` ("¿existe este producto?"), conectado en el arranque, para no importar `inventory`.

La muestra usa el bus en memoria, `InMemoryEventBus` de `src/test`, y no RabbitMQ. Lo que la muestra comprueba son las reglas del pedido, no el broker, y así corre sin Docker en cualquier equipo. RabbitMQ (`RabbitMqEventBus` con el `compose.yaml` de la carpeta) se conserva y lo usa `TiendaApplication`. La persistencia es memoria, como en el punto de partida.

## Consecuencias

**Se gana:** el descuento está en una clase que no registra pedidos ni decide el stock. Un cambio de tramo no toca `OrderService`, y la regla de arquitectura (`ModuleIndependenceTest` y `verificar-regla.sh`) impide que el descuento se mezcle con `orders`.

**Se pierde:**
1. `placeOrder` devuelve el pedido en `PENDIENTE` y sin total. Nadie puede leer el total en la respuesta: hay que esperar a que el bus entregue `StockReserved` (consistencia eventual). Se comprueba con la prueba `elPedidoQuedaPendienteHastaQueElBusEntregaLosEventos`.
2. La muestra no recorre RabbitMQ: usa un doble de pruebas que la guía describe como tal y no como entorno alternativo.
3. Una regla de descuento que dependa de datos de `orders` (por ejemplo, el total comprado por el cliente, RN-4) no se puede calcular en `inventory` sin un evento nuevo o un campo nuevo en `OrderPlaced`, y eso sí tocaría al productor.

**Predicción del costo de la Hoja 2:** dentro de `codigo/`, esperamos **crear 2 archivos**: `src/test/java/com/ejemplo/tienda/MuestraHoja2.java` y una prueba nueva del tramo, `src/test/java/com/ejemplo/tienda/inventory/DescuentoPorVolumenTramoTest.java`. Esperamos **modificar 1 archivo**: `src/main/java/com/ejemplo/tienda/inventory/DescuentoPorVolumen.java`. **Sí** se modificará la clase que calcula el total (`DescuentoPorVolumen`). **No** se modificará la clase que registra el pedido (`OrderService`), ni `InventoryService`, ni `Tienda` (el cableado), ni ningún evento. **No** se tocará ninguna prueba de la Hoja 1. Si el diff muestra algún otro archivo de `src/main` modificado, la predicción falló.
