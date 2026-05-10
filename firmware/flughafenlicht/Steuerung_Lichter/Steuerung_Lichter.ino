#include <HCSR04.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include "../../brueckensteuerung/secrets.h"

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
UltraSonicDistanceSensor distanceSensor(5, 4);
int runwayLightPins[] = {21, 19, 32, 33, 25, 26, 27, 14, 12, 13};
bool lightsEnabled = true;
const char* MQTT_TOPIC_COMMAND = "airport";

void turnAllRunwayLightsOff() {
  for (int i = 0; i < 10; i++) {
    digitalWrite(runwayLightPins[i], LOW);
  }
}

void showApproachingAircraft() {
  while (true) {
    const int distanceCm = round(distanceSensor.measureDistanceCm());
    int firstActiveLightIndex = (distanceCm - 4) / 4;

    if (distanceCm <= 0 || distanceCm > 40) {
      break;
    }

    turnAllRunwayLightsOff();

    if (distanceCm < 6) {
      Serial.println(String("Distanz: ") + distanceCm);
      digitalWrite(runwayLightPins[0], HIGH);
    } else {
      // Die Sensorwerte werden in 4-cm-Zonen auf LED-Positionen abgebildet.
      if (firstActiveLightIndex < 0) firstActiveLightIndex = 0;
      if (firstActiveLightIndex > 9) firstActiveLightIndex = 9;

      // Drei benachbarte Lichter markieren die aktuelle Position des Flugzeugs.
      for (int i = firstActiveLightIndex; i <= firstActiveLightIndex + 2 && i < 10; i++) {
        digitalWrite(runwayLightPins[i], HIGH);
      }
    }
    delay(200);
  }
}

void setup() {
  Serial.begin(9600);
  for (int i = 0; i < 10; i++) {
    pinMode(runwayLightPins[i], OUTPUT);
  }
  /*ensureWifiConnected();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);
  ensureMqttConnected();*/
}

void loop() {
  while (lightsEnabled) {
    digitalWrite(runwayLightPins[0], LOW);
    for (int i = 9; i > 0; i--) {
      const double distanceCm = distanceSensor.measureDistanceCm();

      // Erkennt ein Objekt im relevanten Bereich vor der Lichterkette
      // und wechselt von der Standard-Laufanimation in den Anflugmodus.
      if (2 < distanceCm && distanceCm < 40) {
        turnAllRunwayLightsOff();
        i = 0;
        showApproachingAircraft();
      }
      digitalWrite(runwayLightPins[i], HIGH);
      Serial.println(String("Pin: ") + runwayLightPins[i]);
      delay(100);
      if (i == 9) {
        digitalWrite(runwayLightPins[1], LOW);
      } else {
        digitalWrite(runwayLightPins[i + 1], LOW);
      }
    }
  }
}

void ensureMqttConnected() {
  while (!mqttClient.connected()) {
    if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
      mqttClient.subscribe(MQTT_TOPIC_COMMAND);
      return;
    }
    delay(2000);
  }
}

void ensureWifiConnected() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void handleCommand(char *topic, byte *payload, unsigned int length) {
  String message;
  message.reserve(length);
  for (unsigned int i = 0; i < length; ++i) {
    message += static_cast<char>(payload[i]);
  }

  if (String(topic) != MQTT_TOPIC_COMMAND) {
    return;
  }

  if (message.indexOf("ON") != -1) {
    Serial.println("Lichter an");
    lightsEnabled = true;
  } else if (message.indexOf("OFF") != -1) {
    Serial.println("Lichter aus");
    lightsEnabled = false;
    turnAllRunwayLightsOff();
  }
}






