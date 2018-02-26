#define DEBUG

#include <RGBLed.h>
#include <Phone.h>
#include <DebugUtils.h>
#include "LimitSwitch.h"

#define LIMIT_SWITCH_PIN 2
#define LIMIT_SWITCH_TIME_TO_SEND_START 300

#define LED_RED_PIN     6
#define LED_GREEN_PIN   10
#define LED_BLUE_PIN    3

#define BREAK_AFTER_SENDING 15000   // time in millis, when there is nothing check after sending start message. 

LimitSwitch limitSwitch(LIMIT_SWITCH_PIN);
RGBLed led(LED_RED_PIN, LED_GREEN_PIN, LED_BLUE_PIN);


unsigned long ledChange = 0;
unsigned long timeSignalCheck = 0;
#define TIME_SIGNAL_CHECK 10000

Phone phone;

char cmd[150];
unsigned long lastTimeMessageSend = 0; // when the last start message was send 

void setup() {
  Serial.begin(9600);
  led.setColor(250, 40, 0);
  led.on();
  phone.init();
}

void loop() {
  if ((millis() - timeSignalCheck) > TIME_SIGNAL_CHECK) { 
    timeSignalCheck = millis();
    checkSignalStrength();
  }
  unsigned long switchTime = limitSwitch.isOn();
  

  if (switchTime > LIMIT_SWITCH_TIME_TO_SEND_START) {
    unsigned long current = millis();
    if ((current - switchTime) > lastTimeMessageSend) {
      DEBUG_PRINTLN(F("Sending start time"));
      unsigned long seconds = 0;
      unsigned long milliSeconds = 0;
      phone.getCurrentUnixTimeStamp(&seconds, &milliSeconds);
      int size = sprintf(cmd, "http://skimb.xelfi.cz/timing/add?when=%ld%d&type=START", seconds, milliSeconds);
      lastTimeMessageSend = current;
      DEBUG_PRINTLN(cmd);
      led.setColor(0, 0, 250);
      led.on();
      phone.sendRequest(cmd);
      led.setColor(250, 0, 0);
      delay(BREAK_AFTER_SENDING);
      timeSignalCheck = 0;
    }
  }

  if ((millis() - ledChange) > 1000) {
    ledChange = millis();
    if (led.isOn()) {
      led.off();
    } else {
      led.on();
    }
  }
}

void checkSignalStrength() {
  int strength = phone.signalStrength();
  if (strength < 10) {
    led.setColor(255, 0, 0);  // red
  } else if (strength > 9 && strength < 15) {
    led.setColor(250, 0, 100); // magnata
  } else if (strength > 14 && strength < 20) {
    led.setColor(250, 40, 0); // orange
  } else if (strength > 19 ) {
    led.setColor(0, 250, 0);
  }
}

