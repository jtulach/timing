#include "Arduino.h"
#include "RGBLed.h"

RGBLed::RGBLed(int redPin, int greenPin, int bluePin) {
  _redPin = redPin;
  _greenPin = greenPin;
  _bluePin = bluePin;
  _redColor = 255;
  _greenColor = 255;
  _blueColor = 255;
  _isOn = false;
}

void RGBLed::setColor(int red, int green, int blue){
  _redColor = red > 250 ? 250 : red;
  _greenColor = green > 250 ? 250 : green;
  _blueColor = blue > 250 ? 250 : blue;
  if (isOn()) {
    on();
  }
}
    
void RGBLed::on() {
  analogWrite(_redPin, _redColor);
  analogWrite(_greenPin, _greenColor);
  analogWrite(_bluePin, _blueColor);
  _isOn = true;
}

void RGBLed::off() {
  analogWrite(_redPin, 0);
  analogWrite(_greenPin, 0);
  analogWrite(_bluePin, 0);
  _isOn = false;
}

boolean RGBLed::isOn() {
  return _isOn;
}



