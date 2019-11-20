#define DEBUG

#include <RGBLed.h>
#include <Phone.h>
#include <DebugUtils.h>

#define PIN_LDR         A0 // where is the value from photo resistor readed

#define LED_RED_PIN     6
#define LED_GREEN_PIN   10
#define LED_BLUE_PIN    3

RGBLed led(LED_RED_PIN, LED_GREEN_PIN, LED_BLUE_PIN);


unsigned long ledChange = 0;
unsigned long timeSignalCheck = 0;
#define TIME_SIGNAL_CHECK 10000

int workingResistorValue = 600;
int minimalMessageDelay = 2; // number of seconds, when the message can not be sand again

Phone *phone;

char cmd[150];
unsigned long lastTimeMessageSend = 0; // when the last start message was send
unsigned long laserDetectedTime = 0;
unsigned long lastMessageTime = 0;
unsigned long timeToChangeLed = 300;


void setup() {
  Serial.begin(9600);
  led.setColor(250, 40, 0);
  led.on();
  phone = new Phone(NULL);
  phone->init();
  // sending message about starting box
  DEBUG_PRINTLN(F("Sending FINISHBOX_STARTED"));
  unsigned long seconds = 0;
  unsigned long milliSeconds = 0;
  phone->getCurrentUnixTimeStamp(&seconds, &milliSeconds);
  sprintf(cmd, "http://skimb.xelfi.cz/timing/add?when=%ld%03d&type=FINISHBOX_STARTED", seconds, milliSeconds);
  phone->sendRequest(cmd);
}

void loop() {
  unsigned long current = millis();
  int resistorValue = getResistorValue();
  if ((current - timeSignalCheck) > TIME_SIGNAL_CHECK) {
    timeSignalCheck = current;
    checkSignalStrength();
    DEBUG_PRINT("LDR value is ");
    DEBUG_PRINTLN(resistorValue);
  }
  if (resistorValue > workingResistorValue) {
    laserDetectedTime = current;
    timeToChangeLed = 1000;
  } else {
    if ((current - laserDetectedTime) >  10000) {     // laser was not detected for 10 seconds
      //DEBUG_PRINT("Laser is not detected longer then 10s, LDR value is: ");
      // DEBUG_PRINTLN(resistorValue);
      led.setColor(250, 0, 0);
      timeToChangeLed = 300;
      delay(100);
    } else {
      unsigned long messageDelay = current - lastMessageTime;
      if (messageDelay > 2000  && messageDelay > (minimalMessageDelay * 1000)
          && (laserDetectedTime > lastMessageTime)) {
        unsigned long seconds = 0;
        unsigned long milliSeconds = 0;
        current = millis();
        DEBUG_PRINTLN(F("Sending finish time"));
        phone->getCurrentUnixTimeStamp(&seconds, &milliSeconds);
        int size = sprintf(cmd, "http://skimb.xelfi.cz/timing/add?when=%ld%03d&type=FINISH", seconds, milliSeconds);
        lastMessageTime = current;
        led.setColor(0, 0, 250);
        led.on();
        phone->sendRequest(cmd);
        timeSignalCheck = 0;
        lastMessageTime = current;
      }
    }
  }

  if ((millis() - ledChange) > timeToChangeLed) {
    ledChange = millis();
    if (led.isOn()) {
      led.off();
    } else {
      led.on();
    }
  }
}

boolean isLaserDetected() {
  int resistorValue = getResistorValue();
  if (resistorValue < workingResistorValue) {
    unsigned long current = millis();
    if ((current - ledChange) > 1000) {
      led.setColor(255, 0, 0);
      return false;
    }
  }
  return true;
}

int getResistorValue() {
  return analogRead(PIN_LDR);
}

void checkSignalStrength() {
  int strength = phone->signalStrength();
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
