# Informe · Variante Q1 / fila 1 · Laura Navarrete y Joiner Pineda

## Medición (`git diff --name-status hoja-1 final`)

| Medida | Resultado |
|--------|-----------|
| Archivos creados | 2: `codigo/src/test/java/com/ejemplo/tienda/MuestraHoja2.java`, `codigo/src/test/java/com/ejemplo/tienda/inventory/DescuentoPorVolumenTramoTest.java` |
| Archivos modificados | 1: `codigo/src/main/java/com/ejemplo/tienda/inventory/DescuentoPorVolumen.java` |
| ¿Se modificó el archivo o la clase que calcula el total? | Sí, `DescuentoPorVolumen` |
| ¿Se modificó el archivo o la clase que registra el pedido? | No, `OrderService` no aparece en el diff |
| ¿Se tocó una prueba de la Hoja 1? | No |

## Predicción contra medición

El ADR-0001 predijo crear exactamente `MuestraHoja2` y `DescuentoPorVolumenTramoTest`, modificar solo `DescuentoPorVolumen`, tocar la clase del total y no tocar `OrderService` ni las pruebas de la Hoja 1. La tabla muestra 2 creados y 1 modificado con esos mismos nombres, así que la predicción acertó en las cinco filas. El acierto se explica porque el total ya se calculaba en `inventory`, separado por el bus de quien registra el pedido, y la regla de descuento estaba aislada en su propia clase desde la Hoja 1. Hay que matizarlo: las pruebas de la Hoja 1 sobrevivieron en parte porque no cubrían cantidades de 15 o más. Si una prueba de la Hoja 1 hubiera fijado el total de 31 unidades en 1255500, habría tenido que cambiar.

## El límite

Supongamos que el equipo comercial pide un descuento de fidelidad: 3 puntos más para el cliente cuyo total comprado (RN-4) ya pase de $500.000. En eventos esto sale peor que en hexagonal. El total comprado vive en `orders` y el descuento se calcula en `inventory`, y la regla prohíbe que un módulo consulte al otro. Habría que agregar un campo a `OrderPlaced` y modificar al productor, o crear un evento nuevo, justo lo que la guía de eventos advierte cuando dice que el flujo es difícil de seguir y que la regla no se cumple sola. En hexagonal, el caso de uso del pedido tendría los dos datos en el dominio, a través de un puerto de repositorio, y el cambio sería una clase de dominio, como en «el dominio tiene reglas que cambian».

## Lo que la regla no ve

Para la evidencia agregamos a `OrderService` la línea `import com.ejemplo.tienda.inventory.InventoryService;`, y `verificar-regla.sh` la detectó, imprimió la línea culpable y salió con código 1 antes de que la revirtiéramos con `git checkout`. Pero la regla, igual que `ModuleIndependenceTest`, solo busca el texto `import com.ejemplo.tienda.<modulo>.`. Si en `OrderService` se declara un campo `com.ejemplo.tienda.inventory.InventoryService` con el nombre completo y sin `import`, compila y la regla sigue diciendo `REGLA OK`; lo comprobamos en una copia aparte, sin hacer commit. Tampoco ve el acoplamiento que nosotros mismos metimos por el cableado. `OrderService` recibe `inventory::exists` como un `LongPredicate`, así que depende en tiempo de ejecución de `inventory` sin importar nada.
