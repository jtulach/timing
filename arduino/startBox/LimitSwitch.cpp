#include "Arduino.h"
#include "LimitSwitch.h"

LimitSwitch::LimitSwitch(int pin) {
  pinMode(pin, INPUT);
  _pin = pin;
}

unsigned long LimitSwitch::isOn() {
  unsigned long result = 0;
  
  int state = digitalRead(_pin);
  if (state == 1 && _timeOn == 0) {
    _timeOn = millis();
    _lastTimeOn = _timeOn;
  } 

  if (_timeOn > 0) {
    result = millis() - _timeOn;
    if (state == 0) {
      _timeOn = 0;
    }
  }
  
  return result;
}

unsigned long LimitSwitch::getTimeOn() {
  return _lastTimeOn;
}

