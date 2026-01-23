# 🌿 MonitorizaPT - Environmental IoT Simulator

![Java](https://img.shields.io/badge/Language-Java_17-orange?style=flat-square)
![System](https://img.shields.io/badge/Architecture-MVC_%2B_Observer-blue?style=flat-square)
![Protocol](https://img.shields.io/badge/IoT-MQTT-green?style=flat-square)
![Student ID](https://img.shields.io/badge/Author_ID-a22508678-critical?style=flat-square)

## 📖 About the Project

**MonitorizaPT** is a Java application developed for the Object-Oriented Programming (OOP) final assessment at **Instituto Politécnico da Lusofonia**. 

The goal was to simulate a network of environmental sensors spread across Portugal. The app generates realistic data (temperature, humidity, air quality), sends it to an MQTT Broker, and visualizes everything in a real-time Swing Dashboard.

> **Note to teacher:** This repository represents the **final, optimized iteration** of the project authored by me **Rodrigo Martins**. While earlier development versions may have been shared within study groups for collaborative learning, this codebase contains specific architectural refinements and UI polishes unique to this submission.

---

## 🚀 Key Features

* **📍 7 Locations:** Pre-defined spots (Lisboa, Porto, Faro, etc.) mapped to specific sensor types.
* **📡 MQTT Integration:** Uses Eclipse Paho to publish data (`envira/pt/sensores/dados/...`) and subscribe to remote commands.
* **🛡️ Robustness:** Implements a "Fire-and-Forget" strategy with auto-reconnect logic.
* **📊 Swing Dashboard:**
    * **Live Table:** Real-time updates with visual alerts (rows turn red on critical values).
    * **Remote Control:** Start/Stop sensors or change intervals directly from the UI.
    * **Logs:** Scrollable event log with timestamp.
* **💾 Data Persistence:** (New!) Automatically saves sensor readings to organized CSV files (`registos_csv/`) for data auditing.

---

## 🛠️ Project Structure

The project follows a strict **MVC (Model-View-Controller)** pattern with **Observer** for event handling:

| Package | Description |
| :--- | :--- |
| `pt.monitorizapt.domain` | Core logic, Interfaces, and the `DadosSensor` record. |
| `pt.monitorizapt.sensors` | Concrete implementations (Temperature, Humidity, Air Quality). |
| `pt.monitorizapt.service` | `SensorController` (The brain) & `CsvLogService` (Persistence). |
| `pt.monitorizapt.mqtt` | Wrapper for the Eclipse Paho client. |
| `pt.monitorizapt.ui` | Swing `JFrame` and custom `TableModel`. |
| `pt.monitorizapt.util` | Helpers for JSON building and SHA-256 Hashing. |

---

## ⚙️ Configuration & Running

### Prerequisites
* Java 17 or higher
* Maven 3.9+
* Internet connection (for HiveMQ Broker)

### How to Run
You can run the application directly via Maven:

```bash
mvn clean package
mvn exec:java -Dexec.mainClass=pt.monitorizapt.app.MonitorizaPTApplication

```

*Optional:* To change the broker URL without touching the code, create a `config.properties` file in the root folder or use the command line:
`mvn exec:java -Dmonitorizapt.broker="tcp://127.0.0.1:1883"`

---

## 📡 Payload Examples

### Sensor Data (Publish)

The system generates a JSON with a security hash to ensure integrity.

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
  "hash_validacao": "55c279c09c313a5332e1762c2f7041f021796d66e76878b671151670977a456c"
}

```

### Remote Command (Subscribe)

Send this to `envira/pt/sensores/comandos/<Location>` to control the app remotely:

```json
{
  "acao": "ATIVAR",
  "intervalo": 5000
}

```

---

<details>
<summary><strong>🔍 System Metadata & Author Signature (Click to expand)</strong></summary>

| Property | Value |
| --- | --- |
| **Author** | Rodrigo Martins |
| **Student ID** | a22508678 |
| **Course** | Object Oriented Programming |
| **Hash Algorithm** | SHA-256 |
| **Persistence** | CSV (ISO-8601 Standards) |
</details>
