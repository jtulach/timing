#ifndef RGBLed_h
#define RGBLed_h

#include "Arduino.h"

class RGBLed {
  public:
    RGBLed(int redPin, int greenPin, int bluePin);
    void setColor(int red, int green, int blue);
    void on();
    void off();
    boolean isOn();
    
  private:
    int _redPin;
    int _greenPin;
    int _bluePin;
    int _redColor;
    int _blueColor;
    int _greenColor;
    boolean _isOn;
};

#endif
