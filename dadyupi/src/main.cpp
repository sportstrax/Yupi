#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WebServer.h>
#include <ESP8266WiFi.h>
#include <RestClient.h>
#include <PubSubClient.h>



WiFiClient espClient;
PubSubClient pubsubClient(espClient);
char msg[50];
const char* serverIP = "192.168.137.1";

ESP8266WebServer http_rest_server(8080);
RestClient client = RestClient(serverIP, 8083);

const char* ssid = "DESKTOP-EOVN908 4624";
const char* password = "665329654";
const int sensorH = A0;
double currentMillis = 0;
//const int led1 = 0;
//const int luz1 = 16;



// Construct an LCD object and pass it the
// I2C address, width (in characters) and
// height (in characters). Depending on the
// Actual device, the IC2 address may change.


//Create response;
int statusCode;
String response = "";

const size_t capacity = JSON_OBJECT_SIZE(3) + JSON_ARRAY_SIZE(2) + 60;
DynamicJsonBuffer jsonBuffer(capacity);
JsonObject& root = jsonBuffer.parseObject(response);



//METODOS
void callback(char* topic, byte* payload, unsigned int length) {
	 String response = String((char *)payload);

	 const size_t capacity = JSON_OBJECT_SIZE(3) + JSON_ARRAY_SIZE(2) + 60;
	 DynamicJsonBuffer jsonBuffer(capacity);
	 JsonObject& root = jsonBuffer.parseObject(response);
	 bool action = root["Action"];
	 String id = root["id"];
	 String topicS= topic;
	 Serial.print("Mensaje recibido [");
	 Serial.print(topic);
	 Serial.print("] ");
	 Serial.println(action);

	 if(topicS=="topic_2") {
if(id=="02"){
		 if(action){

			 digitalWrite(2,HIGH);
			 snprintf (msg, 75, "Riego activado a las %ld", millis());

			 Serial.println(msg);

		 }else if (!action){
	 			digitalWrite(2,LOW);
				snprintf (msg, 75, "Riego desactivado a las %ld", millis());

 			 Serial.println(msg);
}


}else{

}


}else{

}
}
void putLuz (double value){

	response = "";



	JsonObject& newJson = jsonBuffer.createObject();
	newJson["id_SensorL"] = 2;
	newJson["state"] = 1;
	newJson["value"] = value;
	char jsonStr[100];
	newJson.printTo(jsonStr);
	statusCode = client.put("/api/luz/put", jsonStr, &response);

	Serial.print("Valor subido, luminosudad:");
	Serial.println(value);

}

void putHumedad(double value){



	response = "";

	JsonObject& newJson = jsonBuffer.createObject();
	newJson["id_SensorH"] = 2;
	newJson["state"] = 1;
	newJson["value"] = value;
	char jsonStr[100];
	newJson.printTo(jsonStr);
	statusCode = client.put("/api/humedad/put", jsonStr, &response);

	Serial.print("Valor subido, humedad:");
	Serial.println(value);

}
void putAcidez(double value){


	response = "";

	JsonObject& newJson = jsonBuffer.createObject();
	newJson["id_SensorA"] = 2;
	newJson["state"] = 1;
	newJson["value"] = value;
	char jsonStr[100];
	newJson.printTo(jsonStr);
	statusCode = client.put("/api/acidez/put", jsonStr, &response);

	Serial.print("Valor subido, acidez:");
	Serial.println(value);

}

void putTemperatura(double value){


	response = "";


	JsonObject& newJson = jsonBuffer.createObject();
	newJson["id_SensorT"] = 2;
	newJson["state"] = 1;
	newJson["value"] = value;
	char jsonStr[100];
	newJson.printTo(jsonStr);
	statusCode = client.put("/api/temperatura/put", jsonStr, &response);

	Serial.print("Valor subido, temperatura:");
	Serial.println(value);

}

double humedad (){

	double humedad = analogRead(sensorH);
	Serial.print("Humedad: ");
	Serial.println(humedad);
	return humedad;
}

double luz (){


	double value = 1.0;
	if(value = 1){
	value = random(1.0,1024.0);
	Serial.print("Luz: ");
	Serial.println(value);
}else{
	value = 0;
}


	return value;
}

double acidez (){

	double value = random(0.0,1024.0);
	Serial.print("Acidez: ");
	Serial.println(value);

	return value;
}

double temperatura (){

	double value = random(0.0,1024.0);
	Serial.print("Temperatura: ");
	Serial.println(value);

	return value;
}


void setup() {
	pinMode(2,OUTPUT);
//	pinMode(luz1,INPUT);
  Serial.begin(115200);
  delay(10);



  Serial.println();
  Serial.print("Conectando a ");
  Serial.println(ssid);

// Modo cliente
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.print("Red conectada. Dirección IP: ");
  Serial.println(WiFi.localIP());

  pubsubClient.setServer(serverIP, 1883);
  pubsubClient.setCallback(callback);
}

void reconnect() {
	while (!pubsubClient.connected()) {
		Serial.println("Conectando al servidor MQTT");
		if (pubsubClient.connect("ESP2")) {
			Serial.println("Conectado");
	//	pubsubClient.publish("topic_2", "Hola a todos");
			pubsubClient.subscribe("topic_2");
		} else {
			Serial.print("Error, rc=");
			Serial.print(pubsubClient.state());
			Serial.println(" Reintentando en 5 segundos");
			delay(5000);
		}
	}
}

void loop() {








  // MQTT
  if (!pubsubClient.connected()) {
    reconnect();
  }

  pubsubClient.loop();
  delay(5000);




	if((millis() - currentMillis) >= 60000){
		putHumedad(humedad());
		putTemperatura(temperatura());
		putAcidez(acidez());
		putLuz(luz());

		currentMillis = millis();


	}




}
