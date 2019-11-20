#define DEBUG

#include <Phone.h>
#include <DebugUtils.h>
#include "LimitSwitch.h"

#define LIMIT_SWITCH_PIN 2
#define LIMIT_SWITCH_TIME_TO_SEND_START 300
#define BREAK_AFTER_SENDING 30   // time in seconds, when there is nothing check after sending start message. 


#define LED_RED_PIN A1  // this is analog pin now
#define LED_GREEN_PIN A2
#define LED_BLUE_PIN A3

#include <PN5180.h>
#include <PN5180ISO15693.h>

#define BUZZER_PIN 3

#include <LiquidCrystal_I2C.h>

// definování pinů pro PN5180
#define PN5180_NSS  10
#define PN5180_BUSY 6
#define PN5180_RST  5
PN5180ISO15693 nfc(PN5180_NSS, PN5180_BUSY, PN5180_RST);


LimitSwitch limitSwitch(LIMIT_SWITCH_PIN);

//unsigned long ledChange = 0;
unsigned long timeSignalCheck = 0;
#define TIME_SIGNAL_CHECK 10000




Phone *phone;
LiquidCrystal_I2C *lcd;
#define LCD_SAVE_TIMEOUT 60000 // switch lcd light off of
bool lcdLightOn = true;

char cmd[150];
unsigned long lastTimeMessageSend = 0; // when the last start message was send 
unsigned long lastActionTime = 0; // when the last action happend

// when time should be refreshed on display
byte lastSecond = 66;
byte lastMinute = 66;
byte lastHour = 25;

// remember the last readed id. It sended before the start.
char lastStartId[17];

void setup() {
  // init buzzer pin
  pinMode(BUZZER_PIN, OUTPUT);
  // init led pins
  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(LED_GREEN_PIN, OUTPUT);
  pinMode(LED_BLUE_PIN, OUTPUT);
  
  playBootMelody();
  ledColor(1);
  
  Serial.begin(9600);
  // inicialization of LCD dislplay
  lcd = new LiquidCrystal_I2C(0x27,20,4);
  lcd->init();
  lcd->backlight();

  // init phone
  DEBUG_PRINTLN(F("Init phone..."));
  phone = new Phone(lcd);
  DEBUG_PRINTLN(F("calling Init phone..."));
  phone->init();
  lcd->clear();
  lcd->setCursor(0,0);
  lcd->print(F("Signal:00%"));
  lcd->setCursor(12,0);
  lcd->print(F("00:00:00"));
  ledColor(2);

  delay(2000);
  
  
  // for communication with FRID
  DEBUG_PRINTLN(F("Init nfc..."));
//  nfc = new PN5180ISO15693(PN5180_NSS, PN5180_BUSY, PN5180_RST);
  DEBUG_PRINTLN(F("nfc object vtytvoren"));
  nfc.begin();
  DEBUG_PRINTLN(F("nfc begin"));
  nfc.reset();
  DEBUG_PRINTLN(F("nfc reset"));

  DEBUG_PRINTLN(F("Enable RF field..."));
  nfc.setupRF();
  
  lastActionTime = millis();
  DEBUG_PRINTLN(F("Setup done!"));

  // sending message to the server about the start
  unsigned long seconds = 0;
  unsigned long milliSeconds = 0;
  phone->getCurrentUnixTimeStamp(&seconds, &milliSeconds);
  sprintf_P(cmd, PSTR("http://skimb.xelfi.cz/timing/add?when=%ld%03d&type=STARTBOX_STARTED"), seconds, milliSeconds);
  DEBUG_PRINTLN(cmd);
  phone->sendRequest(cmd);
}

void loop() {

  if ((millis() - timeSignalCheck) > TIME_SIGNAL_CHECK) { 
    timeSignalCheck = millis();
    checkSignalStrength();
  }


  if (lcdLightOn && (millis() - lastActionTime) > LCD_SAVE_TIMEOUT) {
    // save battery
    lcd->noBacklight();
    lcdLightOn = false;
  }
  
  displayTime();
  checkStart();
  checkCardReader();
}

void checkStart() {
  unsigned long switchTime = limitSwitch.isOn();

  if (switchTime > LIMIT_SWITCH_TIME_TO_SEND_START) {
    unsigned long current = millis();
    if ((current - switchTime) > lastTimeMessageSend) {
      DEBUG_PRINTLN(F("Sending start time"));
      unsigned long seconds = 0;
      unsigned long milliSeconds = 0;
      phone->getCurrentUnixTimeStamp(&seconds, &milliSeconds);
      ledColor(3);
      setLastActionTime();
      clearLcd();
      lcd->setCursor(0, 1);
      lcd->print(F("Odstartovano"));
      playStartMelody();
      sprintf_P(cmd, PSTR("http://skimb.xelfi.cz/timing/add?when=%ld%03d&type=START"), seconds, milliSeconds);
      lastTimeMessageSend = current;
      DEBUG_PRINTLN(cmd);
      lcd->setCursor(0, 2);
      lcd->print(F("Posilani casu startu"));
      phone->sendRequest(cmd);
      lcd->setCursor(0, 2);
      lcd->print(F("Cas startu odeslan  "));
      lcd->setCursor(0, 3);
      lcd->print(F("Cekej    sekund"));
      ledColor(1);
      for (int i = BREAK_AFTER_SENDING; i >= 0; i--) {
        printTimePart(3, 6, i);
        delay(1000);
        displayTime();
        if (i ==  BREAK_AFTER_SENDING - 5) {
          clearLcd();
          lcd->setCursor(0, 3);
          lcd->print(F("Cekej    sekund"));
        }
        if(checkCardReader()) {
          clearLcd();
          lcd->setCursor(0, 3);
          lcd->print(F("Cekej    sekund"));
          i = i - 4;  // the action of card read takes about 4 seconds (melody, sending, timeout2s)
        }
      }
      printReady();
      timeSignalCheck = 0;
      ledColor(0);
    }
  }
  
}

// needed for reading PN5180
bool errorFlag = false;

bool checkCardReader() {
  if (errorFlag) {
    uint32_t irqStatus = nfc.getIRQStatus();
//    showIRQStatus(irqStatus);

//    if (0 == (RX_SOF_DET_IRQ_STAT & irqStatus)) { // no card detected
//      Serial.println(F("*** No card detected!"));
//    }

//    DEBUG_PRINTLN(F("*** No card detected!"));
    nfc.reset();
    nfc.setupRF();

    errorFlag = false;
  }

  uint8_t uid[8];
  ISO15693ErrorCode rc = nfc.getInventory(uid);
  if (ISO15693_EC_OK != rc) {
    //DEBUG_PRINT(F("Error in getInventory: "));
    //DEBUG_PRINTLN(nfc.strerror(rc));
    errorFlag = true;
    return false;
  }
  rc = nfc.getInventory(uid);
  if (ISO15693_EC_OK != rc) {
    errorFlag = true;
    return false;
  }
  rc = nfc.getInventory(uid);
  if (ISO15693_EC_OK != rc) {
    errorFlag = true;
    return false;
  }
  setLastActionTime();
  DEBUG_PRINT(F("Adresa RFID tagu: "));
  bytesToString(uid, 8, lastStartId);
  DEBUG_PRINTLN(lastStartId);
  
  clearLcd();
  lcd->setCursor(0, 1);
  lcd->print(F("Identifikovan cip"));
  lcd->setCursor(4, 2);
  lcd->print(lastStartId);
  playCardReadMelody();
  
  unsigned long seconds = 0;
  unsigned long milliSeconds = 0;
  ledColor(3);
  phone->getCurrentUnixTimeStamp(&seconds, &milliSeconds);
  
  unsigned long id = 0;
//  id += (unsigned long)uid[0] << 56;
//  id += (unsigned long)uid[1] << 48;
//  id += (unsigned long)uid[2] << 40;
//  id += (unsigned long)uid[3] << 32;
  id += (unsigned long)uid[4] << 24;
  id += (unsigned long)uid[5] << 16;
  id += (unsigned long)uid[6] << 8;
  id += (unsigned long)uid[7];
  DEBUG_PRINTLN(id);
  sprintf_P(cmd, PSTR("http://skimb.xelfi.cz/timing/add?who=%s&when=%ld%03d&type=ASSIGN"), lastStartId, seconds, milliSeconds);
  DEBUG_PRINTLN(cmd);
  lcd->setCursor(0, 3);
  lcd->print(F("Posilani na server"));
  phone->sendRequest(cmd);
  delay(2000);
  timeSignalCheck = 0;
  ledColor(0);
  printReady();
  return true;
}

void checkSignalStrength() {
  DEBUG_PRINTLN(F("Checking signal started: "));
  int strength = phone->signalStrength();
  DEBUG_PRINTLN(F("Signal: "));
  DEBUG_PRINTLN(strength);
  
  if (strength >=0 && strength < 100) {
    // very easy way to conver it to % (30 should be max)
    int printValue = strength * 3;
    if (printValue >=0 && printValue < 100) {
      printTimePart(0, 7, printValue);
    }
  }
  if (strength < 10) {
    ledColor(1);
    clearLcd();
    lcd->setCursor(0,2);
    lcd->print(F("Slaby signal. Mozny"));
    lcd->setCursor(0,3);
    lcd->print(F("problem se spojenim."));
  } else {
    ledColor(2);
    printReady();
  }
}

void setLastActionTime() {
  lastActionTime = millis();
  if (!lcdLightOn) {
    lcd->backlight();
    lcdLightOn = true;
  }
}

void bytesToString(byte array[], unsigned int len, char buffer[]){
    for (unsigned int i = 0; i < len; i++) {  
        byte nib1 = (array[i] >> 4) & 0x0F;
        byte nib2 = (array[i] >> 0) & 0x0F;
        buffer[i*2+0] = nib1  < 0xA ? '0' + nib1  : 'A' + nib1  - 0xA;
        buffer[i*2+1] = nib2  < 0xA ? '0' + nib2  : 'A' + nib2  - 0xA;
    }
    buffer[len*2] = '\0';
}

void displayTime() {
  byte s = second();
  byte m = minute();
  byte h = hour();
  if (s != lastSecond) {
    printTimePart(0, 18, s);
    lastSecond = s;
    if (m != lastMinute) {
      printTimePart(0, 15, m);
      lastMinute = m;
      if (h != lastHour) {
        printTimePart(0, 12, h);
        lastHour = h;
      }
   }
  }
}

void printTimePart(byte row, byte column, byte number) {
  lcd->setCursor(column,row);
  if (number < 10) {
    lcd->print(0);
    lcd->print(number);
  } else {
    lcd->print(number);
  }
  
}

void printReady() {
  clearLcd();
  lcd->setCursor(0,3);
  lcd->print(F("Muzes startovat"));  
}

void clearLcd(){
  lcd->setCursor(0,1);
  lcd->print(F("                    "));
  lcd->setCursor(0,2);
  lcd->print(F("                    "));
  lcd->setCursor(0,3);
  lcd->print(F("                    "));  
}

void playBootMelody() {
  tone(BUZZER_PIN, 200, 250);
  delay(300);
  tone(BUZZER_PIN, 400, 250);
  delay(300);
  tone(BUZZER_PIN, 600, 250);
  delay(300);
}

void playCardReadMelody() {
  tone(BUZZER_PIN, 300, 400);
  delay(450);
  tone(BUZZER_PIN, 600, 800);
  delay(800);
}

void playStartMelody() {
  tone(BUZZER_PIN, 200, 100);
  delay(150);
  tone(BUZZER_PIN, 200, 100);
  delay(150);
  tone(BUZZER_PIN, 200, 100);
  delay(150);
  tone(BUZZER_PIN, 200, 100);
  delay(150);
  tone(BUZZER_PIN, 100, 2000);
  delay(800);
}

void ledColor(byte color) {
  if (color == 1) {
    digitalWrite (LED_RED_PIN, HIGH);
    digitalWrite (LED_BLUE_PIN, LOW);
    digitalWrite (LED_GREEN_PIN, LOW);
  } else if (color == 3) {
    digitalWrite (LED_RED_PIN, LOW);
    digitalWrite (LED_BLUE_PIN, HIGH);
    digitalWrite (LED_GREEN_PIN, LOW);
  } else if (color == 2) {
    digitalWrite (LED_RED_PIN, LOW);
    digitalWrite (LED_BLUE_PIN, LOW);
    digitalWrite (LED_GREEN_PIN, HIGH);
  } else {
    digitalWrite (LED_RED_PIN, LOW);
    digitalWrite (LED_BLUE_PIN, LOW);
    digitalWrite (LED_GREEN_PIN, LOW);
  }
}
