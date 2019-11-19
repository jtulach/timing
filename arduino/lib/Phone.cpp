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

Phone::Phone(LiquidCrystal_I2C *d) {
  DEBUG_PRINTLN(F("Sim900 bude vytvoren"));
  sim900 = new SoftwareSerial(PIN_PHONE_TX, PIN_PHONE_RX);
  DEBUG_PRINTLN(F("Sim900 vytvoren"));
  sim900->begin(PHONE_BAUDRATE);
  lcd = d;
  isStarted = false;
}

const char AtCLTS[] PROGMEM = "AT+CLTS=1";
//const char AtCCLK[] PROGMEM = "AT+CCLK?";
//const char AtSAPBR1[] PROGMEM = "AT+SAPBR=3,1,\"CONTYPE\",\"GPRS\"";
//const char AtSAPBR2[] PROGMEM = "AT+SAPBR=3,1,\"APN\",\"internet\"";
//const char AtSAPBR3[] PROGMEM = "AT+SAPBR=1,1";
//const char AtCSQ[] PROGMEM = "AT+CSQ";
//const char AtCGATT[] PROGMEM = "AT+CGATT?";
//const char AtHTTPINIT[] PROGMEM = "AT+HTTPINIT";
//const char AtHTTPREAD[] PROGMEM = "AT+HTTPREAD";
//const char AtHTTACTION[] PROGMEM = "AT+HTTPACTION=%d";
//const char AtHTTPPARA[] PROGMEM = "AT+HTTPPARA=\"URL\",\"%s\"";

const char NORMAL_POWER_DOWN[] PROGMEM = "NORMAL POWER DOWN";

void Phone::init() {
  DEBUG_PRINTLN(F("Phone init ...."));
  printLCD(0, 2, F("Inicializace GSM...  "));
  DEBUG_PRINTLN(F("nal lcd vytisteno"));
  switchOn();
  DEBUG_PRINTLN(F("phone switched on"));
  atCommand2(F("AT+CLTS=1"));
  atCommand2(F("AT+CCLK?"));
  atCommand2(F("AT+CSQ"));
  atCommand2(F("AT+CGATT?"));
  atCommand2(F("AT+SAPBR=3,1,\"CONTYPE\",\"GPRS\""));
  atCommand2(F("AT+SAPBR=3,1,\"APN\",\"internet\""));
  atCommand2(F("AT+SAPBR=1,1"));
  atCommand2(F("AT+HTTPINIT"));
  printLCD(0, 3, F("Synchronizace casu  "));
  synchronizeTime();
  if (isStarted) {
    // sending log message, that the phone was restarted. 
    char cmd[150];
    unsigned long seconds = 0;
    unsigned long milliSeconds = 0;
    getCurrentUnixTimeStamp(&seconds, &milliSeconds);
    //int size = sprintf(cmd, "http://skimb.xelfi.cz/timing/add?when=%ld%d&type=PHONE_RESET", seconds, milliSeconds);
    //printLCD(0, 3, F("Sending PHONE_RESET"));
    //sendRequest(cmd); 
  }
  isStarted = false;
  lcd->clear();
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
  int size = atCommand2(F("AT+CSQ"));
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

int Phone::atCommand2(__FlashStringHelper *cmd) {
  DEBUG_PRINTLN(cmd);
  sim900->println(cmd);
  int size = read(buffer, BUFFER_SIZE);
  DEBUG_WRITE(buffer, size);  
  //Serial.write(buffer, size);
  return size;
}

int Phone::atCommand(const char *cmd) {
  DEBUG_PRINTLN(cmd);
  sim900->println(cmd);
  int size = read(buffer, BUFFER_SIZE);
  DEBUG_WRITE(buffer, size);  
  //Serial.write(buffer, size);
  return size;
}

int Phone::read(char *message, int len) {
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
      if (count == len) {
          isEnd = true;
          DEBUG_PRINTLN(F("Pocet prectenych znaku dosazeno"));
      }
    }
    if ((millis() - startReading) > 10000) {
      DEBUG_PRINTLN(F("Timeout in reading response"));
      DEBUG_PRINT(F("Readed chars: "));
      DEBUG_PRINTLN(count);
      if (count > 0) {
        DEBUG_PRINTLN(message);
      }
      isEnd = true; 
      if (count == 0) {
        // sim900 is not responding
        // try to reset
        init();
      }
    }
  }
  if (sim900->available()) {
    DEBUG_PRINT (F("Message too long, only "));
    DEBUG_PRINT (len);
    DEBUG_PRINTLN (F(" was read. Whole message:"));
    for (int i = 0; i < len; i++) {
        DEBUG_PRINT(message[i]);
    }
    while(sim900->available()) {
      // read the rest of the message ..... We should to save it?
      DEBUG_PRINT(sim900->read());
    }
    DEBUG_PRINTLN();
    DEBUG_PRINTLN (F("--- End of message ---"));
  }
  DEBUG_PRINT(F("Reading took: "));
  DEBUG_PRINTLN(millis() - startReading);
  DEBUG_PRINT(F("Readed chars: "));
  DEBUG_PRINTLN(count);
  return count;
  
}

void Phone::sendRequest(char *url) __attribute__((__optimize__("O2")));  // this is a hack due the bug in arduino
void Phone::sendRequest(char *url) {
  DEBUG_PRINT(F("Send request: "));
  DEBUG_PRINTLN(url);
  unsigned long start = millis();
  sprintf(buffer, "AT+HTTPPARA=\"URL\",\"%s\"", url);
  atCommand(buffer);
//  atCommand2(F("AT+HTTPPARA=\"ACCEPT\",\"text/plain\""));
  int bytes = atHTTPACTIONCommand(0);
  DEBUG_PRINT(F("Bytes: "));
  DEBUG_PRINTLN(bytes);
  atCommand2(F("AT+HTTPREAD"));
  DEBUG_PRINT(F("Http request took: "));
  DEBUG_PRINTLN(millis() - start);
}

void Phone::printLCD(byte column, byte row, __FlashStringHelper *message) {
  if(lcd) {
    lcd->setCursor(column,row);
    lcd->print(message);
  }
}

int Phone::atHTTPACTIONCommand(int action) {
  char cmd[18];
  sprintf_P(cmd, PSTR("AT+HTTPACTION=%d"), action); 
  atCommand(cmd);

  boolean end = false;
  boolean state = 0;
  char result[150] = "";
  int index = 0;
  unsigned long startReading = millis();
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
    if ((millis() - startReading) > 10000) {
      DEBUG_PRINTLN(F("Problem in reading in atHTTPACTIONComand"));
      init();
      end = true;
    }
  }
  DEBUG_PRINTLN(result);
  index -= 3;
  int bytes = -1;
  int decimal = 10;
  while(result[index] != ',' && index > 0) {
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
  DEBUG_PRINTLN("Zapnuti GSM");
  printLCD(0, 3, F("Zapnuti GSM         "));
  int count = 0;
  boolean isPhoneOn = true;
  boolean isEnd = false;
  phoneSwitch();
  DEBUG_PRINTLN(F("Telefon zapnut"));
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
      DEBUG_PRINTLN(F("Timeout for reading switching off the phone"));
      printLCD(0, 3, F("Nutny restart GSM   "));
    }
  }
  if (!isPhoneOn) {
    DEBUG_PRINTLN(F("Phone is restarted ...."));
    printLCD(0, 3, F("Restartovani GSM    "));
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
    atCommand2(F("AT+CLTS=1"));
  }
  int counter = 1;
  int sec =-1;
  unsigned long syncStart = millis(); // when was the synchronization started
  while (counter < 50) {
    syncStart = millis();
    atCommand2(F("AT+CCLK?"));
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
//        sprintf(buffer, "Cas: %d:%d:%d", hour, min, sec);
//        DEBUG_PRINTLN(buffer); 
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
