#!/usr/bin/env bash
# Regla de arquitectura (eventos): ningun modulo importa a otro, event y bus no importan
# modulos, y solo bus importa RabbitMQ o JSON. Se corre desde la carpeta codigo/.
# Es el mismo control que ModuleIndependenceTest, como grep documentado.
cd "$(dirname "$0")/src/main/java/com/ejemplo/tienda" || exit 2
MODULOS="orders inventory billing notifications"
fallas=0
for m in $MODULOS; do
  otros=$(echo $MODULOS | tr ' ' '\n' | grep -vx "$m" | paste -sd'|')
  grep -rnE "import com\.ejemplo\.tienda\.($otros)\." "$m" && fallas=1
done
grep -rnE "import com\.ejemplo\.tienda\.(orders|inventory|billing|notifications)\." event bus && fallas=1
grep -rnE "import com\.(rabbitmq|fasterxml)" event orders inventory billing notifications && fallas=1
if [ $fallas -eq 0 ]; then echo "REGLA OK: los modulos solo conocen event y bus"; else echo "REGLA VIOLADA"; exit 1; fi
