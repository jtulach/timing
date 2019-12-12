#ifndef Phone_h
#define Phone_h

#include "Arduino.h"
#include <SoftwareSerial.h>
#include <LiquidCrystal_I2C.h>
#include <TimeLib.h>        // for managing time
#include <Time.h>

#define BUFFER_SIZE  250

class Phone {
  public:
    Phone(LiquidCrystal_I2C *d);
    void init();
    void switchOn();
    int atCommand2(__FlashStringHelper *message);
    int atCommand(const char *cmd);
    int signalStrength();
    int read(char *message, int len);
    void sendRequest(char *url);
    void getCurrentUnixTimeStamp(unsigned long *seconds, unsigned long *milliSeconds);
    
  private:
    SoftwareSerial *sim900;
    LiquidCrystal_I2C *lcd;
    char buffer[BUFFER_SIZE];
    void phoneSwitch();
    int atHTTPACTIONCommand(int action);
    void printLCD(byte column, byte row, __FlashStringHelper *message);
    void synchronizeTime();
    unsigned long syncTimeMillis = 0; // last local time, when the time was synced
    time_t syncTime;  // the date time, that was, when syncTimeMillis was set
    boolean isStarted;
};

#endif
