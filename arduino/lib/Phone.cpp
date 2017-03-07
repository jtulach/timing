#include "Arduino.h"
#include "Phone.h"
#define DEBUG
#include "DebugUtils.h"
#include <SoftwareSerial.h>


#define log
#define PIN_PHONE_TX    7
#define PIN_PHONE_RX    8

#define PHONE_BAUDRATE  19200

#define PARSER_STATE_TEXT           0
#define PARSER_STATE_AFTER_NEW_LINE 1
#define PARSER_STATE_IN_OK          2
#define PARSER_STATE_IN_ERROR       3

Phone::Phone() {
  sim900 = new SoftwareSerial(PIN_PHONE_TX, PIN_PHONE_RX);
  sim900->begin(PHONE_BAUDRATE);
}


void Phone::init() {
  DEBUG_PRINTLN(F("Phone init ..."));
  switchOn();
  atCommand("AT+CLTS=1");
  atCommand("AT+CCLK?");
  atCommand("AT+CSQ");
  atCommand("AT+CGATT?");
  atCommand("AT+SAPBR=3,1,\"CONTYPE\",\"GPRS\"");
  atCommand("AT+SAPBR=3,1,\"APN\",\"internet\"");
  atCommand("AT+SAPBR=1,1");
  atCommand("AT+HTTPINIT");
  synchronizeTime();
}

/*
 * Returns strength of signal. 
 * Meaning of values: 
 *    -1 : is was not possible to decode strang signal. Phone doesn't answer the expected resutl.
 *    0 - 9: signal is marginal
 *    10 - 14: signal is ok
 *    15 - 19: signal is good
 *    20 - 30: signal is excelent
 */
int Phone::signalStrength() {
  int size = atCommand("AT+CSQ");
  int strength = -1;
  if (size > 18) {
    // we are reading the right value
    char c1 = buffer[16];
    char c2 = buffer[17];
    if (c2 == ',') {
      // signal only from 0-9
      strength = c1 - '0';
    } else {
      strength = (c1 - '0') * 10 + (c2 -'0');
    }
  }
  return strength;
}

void Phone::getCurrentUnixTimeStamp(unsigned long *seconds, unsigned long *milliSeconds) {
  *seconds = now();
  *milliSeconds = (millis() - syncTimeMillis)%1000;
}

int Phone::atCommand(const char *cmd) {
  sim900->println(cmd);
  int size = read(buffer);
  DEBUG_WRITE(buffer, size);  
  //Serial.write(buffer, size);
  return size;
}

int Phone::read(char *message) {
  int count = 0;
  boolean isEnd = false;
  unsigned long startReading = millis();
  int state = PARSER_STATE_TEXT;
  int index = 0;
  while (!isEnd) {
    if (sim900->available()) {
      message[count] = sim900->read();
//      if (message[count] == '\n') { Serial.print("\\n");}
//      else if (message[count] == '\r') { Serial.print("\\r");}
//      else {Serial.print(message[count]);}
      switch (state) {
        case PARSER_STATE_TEXT:
          if (message[count] == '\n') {
            state = PARSER_STATE_AFTER_NEW_LINE;
          }
          break;
        case PARSER_STATE_AFTER_NEW_LINE:
          if (message[count] == 'O') {
            state = PARSER_STATE_IN_OK;
            index = 1;
          } else if (message[count] == 'E') {
            state = PARSER_STATE_IN_ERROR;
            index = 1;
          } else {
            state = PARSER_STATE_TEXT;
          }
          break;
        case PARSER_STATE_IN_OK:
          if ((message[count] == 'K' && index == 1)
              || (message[count] == '\r' && index == 2)) {
            index++;
          } else if (message[count] == '\n' && index == 3) {
            isEnd = true;
          } else {
            state = state = PARSER_STATE_TEXT;
          }
          break;
        case PARSER_STATE_IN_ERROR:
          if ((message[count] == 'R' && index == 1)
              || (message[count] == 'R' && index == 2)
              || (message[count] == 'O' && index == 3)
              || (message[count] == 'R' && index == 4)
              || (message[count] == '\r' && index == 5)) {
            index ++;
          } else if (message[count] == '\n' && index == 6) {
              isEnd = true;
          } else {
            state = state = PARSER_STATE_TEXT;
          }
          break;
      }
      count++;
    }
    if ((millis() - startReading) > 10000) {
      DEBUG_PRINTLN(F("end of time"));
      isEnd = true; 
    }
  }
  while(sim900->available()) {
    sim900->read();
  }
  DEBUG_PRINT(F("Reading took: "));
  DEBUG_PRINTLN(millis() - startReading);
  return count;
  
}

void Phone::sendRequest(char *url) {
  unsigned long start = millis();
  sprintf(buffer, "AT+HTTPPARA=\"URL\",\"%s\"", url);
  atCommand(buffer);
  int bytes = atHTTPACTIONCommand(0);
  DEBUG_PRINT(F("Bytes: "));
  DEBUG_PRINTLN(bytes);
  atCommand("AT+HTTPREAD");
  DEBUG_PRINT(F("Http request took: "));
  DEBUG_PRINTLN(millis() - start);
}

int Phone::atHTTPACTIONCommand(int action) {
  char cmd[18];
  sprintf(cmd, "AT+HTTPACTION=%d", action); 
  atCommand(cmd);

  boolean end = false;
  boolean state = 0;
  char result[150] = "";
  int index = 0;
  while(!end) {
    if (sim900->available()) {
      char c = sim900->read();
      if (c == '+') state = 1;
      if (c == '\n' && state == 1) {
        end = true;
      }
      if (state == 1) {
        result[index] = c;
        index++;
      }
    }
  }
  DEBUG_PRINTLN(result);
  index -= 3;
  int bytes = -1;
  int decimal = 10;
  while(result[index] != ',') {
    int number = result[index] - '0';
    index--;
    if (bytes > -1) {
       bytes = bytes + number * decimal;
       decimal = decimal * 10;
    } else {
      bytes = number;
    }
  }
  return bytes;
}

void Phone::switchOn() {
  phoneSwitch();
  int count = 0;
  boolean isPhoneOn = true;
  boolean isEnd = false;
  unsigned long startReading = millis();
  int index = 0;
  while (!isEnd && isPhoneOn) {
    if (sim900->available()) { 
      buffer[index] = sim900->read();
      DEBUG_PRINT(buffer[index]);
      if (strstr(buffer, "NORMAL POWER DOWN")) {
        DEBUG_PRINTLN();
        isPhoneOn = false;
      }
      if (strstr(buffer, "Call Ready")) {
        isEnd = true;
      }
      index++;
    }
    if ((millis() - startReading) > 10000) {
      isEnd = true;
    }
  }
  if (!isPhoneOn) {
    DEBUG_PRINTLN(F("Phone is restarted ...."));
    phoneSwitch();
    startReading = millis();
    delay(1000);
  }
  while ((millis() - startReading) < 20000) {
    if (sim900->available()) {
      char c = sim900->read(); 
      DEBUG_PRINT(c);  
    }
  }
  DEBUG_PRINTLN();
  DEBUG_PRINTLN(F("Phone is on"));
}

void Phone::synchronizeTime() {
  if(syncTimeMillis == 0) {
    atCommand("AT+CLTS=1");
  }
  int counter = 1;
  int sec =-1;
  unsigned long syncStart = millis(); // when was the synchronization started
  while (counter < 50) {
    syncStart = millis();
    atCommand("AT+CCLK?");
    unsigned long  syncEnd = millis();
    int year = (((buffer[20])-48)*10)+((buffer[21])-48);
    int month = (((buffer[23])-48)*10)+((buffer[24])-48);
    int day = (((buffer[26])-48)*10)+((buffer[27])-48);
    int hour = (((buffer[29])-48)*10)+((buffer[30])-48);
    int min = (((buffer[32])-48)*10)+((buffer[33])-48);
    int seconds = (((buffer[35])-48)*10)+((buffer[36])-48);
    setTime(hour, min, seconds, day, month, year);
    syncTime = now();
    if (sec == -1) {
      sec = seconds;
    } else {
      if (sec != seconds && year > 16 // wee are also checking, whether
          && month > 0 && month < 13    // the time is possible
          && day > 0 && day < 32
          && hour > -1 && hour < 25
          && min > -1 && min < 61
          && seconds > -1 && seconds < 61) {
        int delta = syncEnd - syncTimeMillis;
        syncTimeMillis = syncEnd;
        DEBUG_PRINT(F("Pocet cyklu: "));
        DEBUG_PRINT(counter);
        DEBUG_PRINT(F(", delta: "));
        DEBUG_PRINT(delta);
        DEBUG_PRINT(F(" sync trval: "));
        DEBUG_PRINTLN(syncEnd - syncStart);
        sprintf(buffer, "Cas: %d:%d:%d", hour, min, sec);
        DEBUG_PRINTLN(buffer); 
        break;
      }
    }
    counter++;
  }
}

void Phone::phoneSwitch() {
  digitalWrite(9, HIGH);
  delay(1000);
  digitalWrite(9, LOW);
  delay(5000);
}
