#ifndef LimitSwitch_h
#define LimitSwitch_h

#include "Arduino.h"

class LimitSwitch {
  public:
    LimitSwitch(int pin);
    unsigned long isOn();
    unsigned long getTimeOn();
    
  private:
    int _pin;
    unsigned long _timeOn;
    unsigned long _lastTimeOn;
};

#endif
