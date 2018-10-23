## Startovací zařízení
Po zapnutí krabičky, se rozsvítí červeně a svítí (nebliká) dokud se telefon neinicializuje. Jakmile telefon naváže spojení se sítí, začne ledka blikat sekundovým intervalem. Barva záleží na síle signálu, jakou telefon má. Signál může mít sílu 0 až 30. 

* bliká červeně -> síla signálu je menší než 10 a v takovém případě telefon defakto není schopen komunikovat
* bliká fialově -> síla signálu je mezi 10 a 14, telefon je schopen komunikovat, ale někdy se může stát, že nastane chyba
* bliká oranžově -> síla signálu je mezi 15 a 19, telefon by měl fungovat bez problému, ale odesílání zpráv na server může trvat delší dobu
* bliká zeleně -> síla signálu je větší než 19 a vše je super :)

Takže pokud začne blikat telefon sekundovým intervalem danou barvou, víme zhruba jak silný signál je. 

Když se projede startem, začne se odesílat zpráva na server. V tom případě začne svítít dioda modře (nebliká). Modře svítí po celou dobu komunikace se serverem. Délka závisi na kvalitě signálu a počtu chyb, které nastanou. Normálně by měla dioda svítít modře kolem jedná až třech sekund.  Jakmile se zpráva odešle, tak začne dioda svítít červeně po dobu 15 sekund. Po tuto dobu se jen čeká, aby nebyly starty příliš za sebou. Po uplynutí 15 sekund, zase začne dioda blikat sekundovým intervalem barvou podle síly signálu. 

Kdyby náhodou svítíla dioda modře dlouhou dobu, tak komunikace se serverem se nějak zacyklila, zasekla,  a je nutné krabičku vypnout a zapnout znovu. 


## Cílové zařízení:
Je to podobné jako na startu. Jen s tím rozdílem, že dioda ještě bliká červeně v intervalu 300ms, pokud není zaměřen laser správně. Takže po zapnutí krabičky svítí dioda červeně dokud se telefon neinicializuje a nepřihlásí se do sítě. 

Pak začne blikat rychle červeně (interval 300 milisekund)dokud není laser zaměřen správně. Laser by měl směřovat do prostřed čidla - čočky. Do  čidla by nemělo svítít přímé sluníčko, pak si čidlo totiž myslí, že tam svítí laser. Proto tam je teleskopické stínítko, které má také zabránit odleskům od sněhu. Za slunečního dne bude asi potřeba stínítko vysunout celé. 

Jakmile je laser zaměřen správně (čidlo snímá dostatečně silné světlo), tak začne dioda blikat sekundovým intervalem barvou podle síly signálu. Stejné barvy jako u startu. 

Pokud někdo projede cílem (přeruší laserový paprsek), tak se odešle zpráva na server. Během komunikace svítí dioda (nebliká) modře. Jakmile je zpráva odeslána, začne opět blikat podle síly signálu. V cíli není žádná pauza po odeslání jako na startu. Pokud dojde k přerušení laseru na delší dobu, začne opět blikat dioda červeně intervalem 300 ms a je nutné laser znovu zaměřit. 
