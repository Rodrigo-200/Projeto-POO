# MonitorizaPT

MonitorizaPT is a Java 17 desktop simulator for the Instituto Politécnico da Lusofonia final OOP project. It generates dummy environmental readings (temperature, humidity, air quality), publishes them to MQTT topics, reacts to remote commands, and visualises the live stream inside a Swing dashboard.

## Features

- Seven fixed Portuguese locations mapped to concrete sensor classes (temperature, humidity, or air quality) using the IDs defined in the brief.
- Deterministic acquisition loop (`while(true)` + `Thread.sleep(3333)`) with occasional out-of-range spikes to stress-test alert handling.
- Alert thresholds enforced per type (temperature > 30 °C, humidity > 80 %, air quality index > 50).
- JSON payload builder adds `hash_validacao` as the SHA-256 of the payload string without spaces to satisfy broker validation.
- MQTT publish topics: `envira/pt/sensores/dados/<Localizacao>`; command topics: `envira/pt/sensores/comandos/<Localizacao>`.
- Swing UI (`MonitorizaPT - Sensores Ambientais v1.0`) showing MQTT state, sensor table, controls for activation/interval, broker test, and formatted logs (`dd-MM-yyyy HH:mm:ss [INFO] ...`).

## Project Structure

- `pt.monitorizapt.domain`: sensor contracts, base class, enums, and immutable reading record.
- `pt.monitorizapt.sensors`: concrete generators for temperature, humidity, and air quality.
- `pt.monitorizapt.mqtt`: wrapper around Eclipse Paho for publish/subscribe with auto-reconnect and connection listeners.
- `pt.monitorizapt.service`: `SensorController` orchestrates activation, logging, telemetry dispatch, and MQTT command routing.
- `pt.monitorizapt.ui`: Swing table model and main frame implementing the specified layout.
- `pt.monitorizapt.app.MonitorizaPTApplication`: entry point bootstrapping the MQTT manager, controller, and UI.

## Prerequisites

- Java 17+
- Maven 3.9+
- Internet access to reach the broker (defaults to `tcp://broker.hivemq.com:1883`, override via `-Dmonitorizapt.broker=<url>` if you need `172.237.103.61`).

## Building & Running

```bash
mvn clean package
mvn exec:java -Dexec.mainClass=pt.monitorizapt.app.MonitorizaPTApplication
```

The Swing window opens at 800x600 with the exact title required. Use the combo box to choose a location, optional custom interval (default `3333` ms), then press `INICIAR`/`PARAR`. The log area automatically records sensor publications and inbound MQTT commands; hit `LIMPAR LOGS` to reset the text area. `TESTAR BROKER` runs an asynchronous connection check and updates the indicator (green = connected, red = disconnected).

## MQTT Payload Schema

```json
{
  "campus": "Lisboa - Campus IPLuso",
  "sensor": "PT-SENSOR-LISBOA_CAMPUS_IPLUSO",
  "ID Unico": "PT-SENSOR-LISBOA_CAMPUS_IPLUSO",
  "Owner": "a22508678_Rodrigo_Martins",
  "tipo": "temperatura",
  "valor": 24.73,
  "unidade": "Celsius",
  "alerta": false,
  "timestamp": 1734947823000,
  "hash_validacao": "<sha256>"
}
```

### Command Payload

Publish to `envira/pt/sensores/comandos/<Localizacao>`:

```json
{
  "acao": "ATIVAR",
  "intervalo": 3333
}
```

`acao` accepts `ATIVAR` or `DESATIVAR`. `intervalo` is optional; when present (and ≥ 1000 ms) the loop uses the new cadence.

## Sensors & Locations

| Localização               | Tipo            | Publicação MQTT                               |
|---------------------------|-----------------|------------------------------------------------|
| Lisboa - Campus IPLuso    | Temperatura     | envira/pt/sensores/dados/Lisboa_Campus_IPLuso  |
| Lisboa - Baixa            | Humidade        | envira/pt/sensores/dados/Lisboa_Baixa          |
| Porto - Matosinhos        | Qualidade do Ar | envira/pt/sensores/dados/Porto_Matosinhos      |
| Coimbra - Centro          | Temperatura     | envira/pt/sensores/dados/Coimbra_Centro        |
| Faro - Marina             | Humidade        | envira/pt/sensores/dados/Faro_Marina           |
| Braga - Sameiro           | Qualidade do Ar | envira/pt/sensores/dados/Braga_Sameiro         |
| Évora - Universidade      | Temperatura     | envira/pt/sensores/dados/Evora_Universidade    |

All topics are automatically subscribed for commands, so remote MQTT clients can activate or stop sensors without using the GUI.

## Testing Tips

- Use MQTT Explorer or `mosquitto_sub` to watch `envira/pt/sensores/dados/#` and confirm payloads + hashes.
- Publish commands with `mosquitto_pub -t envira/pt/sensores/comandos/Lisboa_Campus_IPLuso -m '{"acao":"ATIVAR","intervalo":3333}'` to toggle sensors remotely.
- Adjust the system property `monitorizapt.broker` if the default broker is unreachable: `mvn exec:java -Dmonitorizapt.broker=tcp://172.237.103.61:1883`.

## Next Steps

- Wire an actual persistence or alerting backend if required by evaluation.
- Replace the placeholder owner string before delivering the project.
- Consider adding automated tests (e.g., `SensorAbstrato` scheduling) if you need CI validation.
