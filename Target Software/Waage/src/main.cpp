#include <Arduino.h>

#define pinData 36
#define pinSCLK 32

bool checkWeightSensorDataAvailable() {
  return digitalRead(pinData);
}

unsigned int fetchWeightSensorData() {
  for(unsigned char i = 0; i < 24; i++) {
    // 
  }
}

void setup() {
  // set up weight sensor
  pinMode(pinData, INPUT);
  pinMode(pinData, OUTPUT);
}

void loop() {
  // put your main code here, to run repeatedly:
}