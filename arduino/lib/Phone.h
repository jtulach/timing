#ifndef Phone_h
#define Phone_h

#include "Arduino.h"
#include <SoftwareSerial.h>
#include <TimeLib.h>        // for managing time
#include <Time.h>



class Phone {
  public:
    Phone();
    void init();
    void switchOn();
    int atCommand(const char *cmd);
    int signalStrength();
    int read(char *message);
    void sendRequest(char *url);
    void getCurrentUnixTimeStamp(unsigned long *seconds, unsigned long *milliSeconds);
    
  private:
    SoftwareSerial *sim900;
    char buffer[500];
    void phoneSwitch();
    int atHTTPACTIONCommand(int action);
    void synchronizeTime();
    unsigned long syncTimeMillis = 0; // last local time, when the time was synced
    time_t syncTime;  // the date time, that was, when syncTimeMillis was set
    boolean isStarted;
};

#endif
