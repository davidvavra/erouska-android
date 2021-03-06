# eRouška

## <a name="device_registration"></a> Device Registration

1. App asks the user to enter the phone number.
2. App sends the phone number to [Firebase Authentication](https://firebase.google.com/docs/auth).
3. Firebase sents back SMS with authorisation token (authentication?).
4. User authorises the device registration by entering the authorisation token from the SMS into the App (This is done automatically on most devices.)
5. After a successful authorisation, the App finishes the registration of the phone number and creates an Application User Profile in [Collection users](#kolekce-users) and [Collection registrations](kolekce-registrations)
	1. The profile creates a callable Firebase function **registerBuid**. The function performs the following:
		1. Gets an access to device information (platform, brand/model, etc.) to enable debugging of possible **aplikace-bluetooth** issues.
		1. Fetches the _phoneNumber_ with [Firebase Authentication](https://firebase.google.com/docs/auth)
		1. Generates a unique [BUID](#buid) (10 characters, unique for each registration, checks for BUID collisions).
		1. If the _phoneNumber_ has more than 50 [BUIDs](#buid) attached, the BUID generation is halted (DDoS prevention).
		1. Everything is written under [Collection users](#kolekce-users) in Firestore under the keys _users/$[FUID](#fuid)_ and  _registrations/$[BUID](#buid)_
		1. The app saves the [BUID](#buid) and a list of [TUIDs](#tuid) from the response function. This is used for Bluetooth application see [Details of BLE (Bluetooth Low Power)](https://docs.google.com/document/d/1uNlHkx3oXWHktTd853gM2rZsv8O1BGzfVIYfvQAnLzo/edit#heading=h.javrvw19429x)


## <a name="integration-with-smart-quarantine"></a> Integration with Smart Quarantine (Chytrá karanténa)

- An epidemiologist asks the user for the permission to use the data from eRouška and asks to upload the data.
- User gives permission by tapping "Upload data" in the eRouška.
    1. eRouška remembers the timestamp of the last successful upload and the CSVs contain only the data collected after.
    2. CSVs can be uploaded only once per 15 minutes (The timer runs on client app.)
    3. The file is saved in Firebase Storage in the [Collection proximity](#kolekce-proximity). The user owns the data.
	
- Every 30 minutes Keboola creates a _phones.csv_ file with phone numbers in a dedicated AWS S3 bucket.
  
  
- Epidemiologist uses a web GUI to see a data for a particular phone number:
    1. Web app fetches a FUID related to the phone number.
    1. A blob is loaded for the particular FUID from the Firebase Storage.
    1. After the raw data is analysed, the TUIDs of people the person met are loaded from the blob, with:
		- their respective phone numbers from the Collection _Users_
		- a flag representing whether the person met is infected or not
		- their respective device names
    1. Epidemiologist contacts the possibly newly infected.
	1. Epidemiologist marks the phone number as "infected".
	<!-- ktore cislo? -->
<!--    8. TODO: možnost zrušit tento příznak, uchovávat data, od kdy do kdy je člověk nakažený?
-->

<!-- preklad skoncil tu cca -->

## Data format


### Firebase Firestore


#### <a name="kolekce-users"></a>Collection _Users_
*   Key: FUID
*   Attributes:
*   **phoneNumber** (telefonní číslo v mezinárodním formátu)
*   createdAt (timestamp kdy byl uživatel vytvořen)
*   registrationCount (počet registrací)


#### <a name="kolekce-registrations"></a>Collection _Registrations_

*   Key: BUID
*   Attributes
*   fuid
*   platform (android/ ios)
*   platformVersion (version systému, např. 10.0.4)
*   manufacturer (výrobce telefonu, např. Samsung)
*   model (model telefonu, např. Galaxy S7)
*   locale (jazyk telefonu, např. cs_CZ)
*   createdAt (timestamp kdy byla registrace vytvořena)
*   pushRegistrationToken (push token z Firebase Cloud Messaging)


#### <a name="kolekce-tuids"></a> Collection _tuids_
*   Key: TUID
*   Attributes
    *   fuid
    *   buid
    *   createdAt (timestamp kdy bylo TUID vytvořeno)


### Firebase Storage

#### <a name="kolekce-proximity"></a>Collection _proximity_
    *   Key FUID/BUID
        *   Filename $timestamp.csv
        *   Metadata
            *   version (version CSV, aktuálně 3)
        *   Sloupečky CSV
            *   tuid (20 znaků hex)
            *   timestampStart (millis)
            *   timestampEnd (millis)
            *   avgRssi (průměrná síla signálu, číslo)
            *   medRssi (medián síly signálu, číslo)


### Firebase Remote Config

Obsahuje konstanty, které jdou později měnit [bez updatu aplikace](https://firebase.google.com/docs/remote-config). Hodnoty jdou dokonce měnit i pro subset uživatelů (např. lidi s konkrétním devicem)



*   collectionSeconds (doba scanování v sekundách, default = 120)
*   waitingSeconds (doba čekání mezi scany, default = 0)
*   advertiseTxPower (vysílací výkon, hodnota 0-3 (ultra low, low, medium, high))
*   advertiseMode (frekvence vysílání, hodnota 0-2) (0 - low power/high latency, 1 - kompromis, 2 - low latency/high power)), default = 1
*   scanMode (chování skenu, hodnota 0-2; 0 - low power/high latency, 1 - kompromis, 2 - low latency/high power), default = 1
*   advertiseRestartMinutes (frekvence restartování BLE advertisingu/broadcastu v minutách - workaround pro zastaveny advertising bez vedomi aplikace)
*   smsTimeoutSeconds (timeout na automatické ověření SMS, default = 120s) (povolený rozsah = 0-120s )
*   smsErrorTimeoutSeconds (timeout po kterém je zoobrazena chyba SMS verifikace, default = 15*60s)
*   criticalExpositionRssi (pro in-app statistiky, úroveň rssi kdy je kontakt nebezpečný, číslo, default = -75)
*   uploadWaitingMinutes(doba mezi uploady, v minutách, číslo, default = 15)
*   persistDataDays(počet dní, jak dlouho se mají držet data v telefonu, default = 30)
*   faqLink (odkaz na FAQ - vede z obrazovky Kontakty)
*   aboutApi (odkaz na json pro about screen)
*   aboutLink (odkaz na web pro about screen, v případě že api call selže)
*   importantLink (odkaz na důležité kontakty - vede z obrazovky Kontakty)
*   emergencyNumber (nouzové číslo - 1212)
*   proclamationLink (odkaz na prohlášení o podpoře - vede z úvodní obrazovky a z nápovědy)
*   showBatteryOptimizationTutorial (default true, zruší tutoriál screen pro nějakou audience)
*   batteryOptimizationHuaweiMarkdown
*   batteryOptimizationAsusMarkdown
*   batteryOptimizationLenovoMarkdown
*   batteryOptimizationSamsungMarkdown
*   batteryOptimizationSonyMarkdown
*   batteryOptimizationXiaomiMarkdown
*   aboutLink (odkaz na tým)
*   termsAndConditionsLink (podminky zpracovani)
*   homepageLink (erouska.cz)
*   shareAppDynamicLink (odkaz na sdileni aplikace)
*   helpMarkdown ([markdown s help textem](https://docs.google.com/document/d/1HptiQuzjnCVNi9_NeMFdVY2zOr2VfdLD86jc-6zymG0/edit#))


### Firebase Functions

Region pro funkce: europe-west1

Pokud není uvedeno jinak, tak výstup i input funkcí jsou slovníky.

Pokud výstup není uveden, tak funkce nic nevrací (může ale vyhodit výjimku).

*   **registerBuid**
    *   Description:
        *   zaregistruje nové BUID pro současného uživatele
    *   Input
        *   platform: string (android/ ios)
        *   platformVersion: string (version systému, např. 10.0.4)
        *   manufacturer: string (výrobce telefonu, např. Samsung)
        *   model: string (model telefonu, např. Galaxy S7)
        *   locale: string (jazyk telefonu, např. cs_CZ)
        *   pushRegistrationToken: string (push token z Firebase Cloud Messaging)
    *   Output
        *   buid: string (vytvořené BUID)
        *   tuids: string[] (seznam TUID pro vysílání)
*   **deleteUploads**
    *   Description:
        *   smaže nahraná CSV pro dané BUID
    *   Input
        *   buid: string (BUID, pro které se mají CSV smazat)
*   **deleteBuid**
    *   Description:
        *   smaže BUID z databáze a všechna nahraná CSV pro dané BUID
    *   Input
        *   buid: string (BUID, které se má smazat)
*   **deleteUser**
    *   Description:
        *   smaže telefonní číslo, všechna BUID, všechna CSV i Firebase uživatele
*   **changePushToken**
    *   Description:
        *   změní push token pro dané BUID
    *   Input
        *   buid: string (BUID, pro které se má token změnit)
        *   pushRegistrationToken: string (nový push token)
*   **isBuidActive**
    *   Description:
        *   zkontroluje, jestli je uživatelův účet (FUID) a zadané BUID aktivní
    *   Input:
        *   buid: string (BUID, které se má zkontrolovat)
    *   Output: boolean (aktivní/neaktivní)



## Security
- Transport layer protocol uses HTTPS.
### Roles, Interfaces, Actions
|role|interface|akce|Protokol|Auth Provider|
|:-:|:-:|:-:|:-:|:-:|
|jakýkoliv uživatel|app|instalace, aktivace|SSL|Google auth pro aktivaci
|infikovaný uživatel|app|upload|SSL|Google auth
|Technicky uživatel (App server)| API call: frontend na Firestore|práce s daty dle FE/hygienik usecasů pod admin právy|SSL|Google auth

### Mobile App
#### Who can install the App?

Everybody, with Google Play Store.

#### App Activation

Po instalaci aplikace je potřeba zaručit, že s naším backendem komunikuje jen aktivovaná/ověřená instance aplikace (a ne nějaký hacker, který nám posílá randomní data).

Aktivace musí spočívat v použití nezávislého kanálu (SMS, email), nemůžeme spoléhat jen na náš datový kanál, aby si hacker nemohl jednoduše naskriptovat vytvoření milionu uživatelů a pak to do nás prát. Tím se dostáváme od tématu aktivace k tématu autentizace.


#### Firebase - App Activation, Authentication and Authentication

Aktuální aplikace využívá  [Google Firebase Authentication](https://firebase.google.com/docs/auth) . Tato služba zajistí aktivaci/ověření aplikace a telefonního čísla přes další kanál (SMS). Při komunikaci s dalšími Firebase službami máme automaticky ověřeného uživatele. Security rules zajišťují, že uživatel může přistupovat jenom ke svým datům.


### Technický setup

[Firebase Storage](https://firebase.google.com/docs/storage) se používá  pro ukládání/upload _proximity dat_. Pak po aktivaci běží autentizace behind-the-scenes automagicky. Pro mobilní vývojáře dost zjednodušení, nulová práce pro backendisty - žádné handlování interakce mobil - server. Přístup je povolen danému uživateli na základě jeho [FUID](#bookmark=id.tee23py6y245) pouze na jeho kolekci dat.

Na cloudovém storage [Firestore](https://firebase.google.com/docs/firestore), je [collection Users](#kolekce-users) (metadata o uživatelích), přístupná pouze hygienikům/admin uživateli.


## Bluetooth

Technické detaily ohledně BLE zde (zvolený přístup pro detekci “blízkých zařízení”): [Covid19 - eRouška - BLE](https://docs.google.com/document/d/1uNlHkx3oXWHktTd853gM2rZsv8O1BGzfVIYfvQAnLzo/edit?usp=sharing)


## Source code

*   Web application [https://github.com/covid19cz/bt-tracing-webapp/](https://github.com/covid19cz/bt-tracing-webapp/)
*   Android [https://github.com/covid19cz/bt-tracing-android](https://github.com/covid19cz/bt-tracing-android)
*   iOS [https://github.com/covid19cz/bt-tracing-ios](https://github.com/covid19cz/bt-tracing-ios)
*   Serverless funkce [https://github.com/covid19cz/erouska-firebase](https://github.com/covid19cz/erouska-firebase)



## Glossary

### <a name="fuid"></a>FUID
Firebase User ID, jednoznačné user ID přiřazené Firebasem při aktivaci zařízení, aktuálně 28 ASCII znaků, ale může být i delší

**způsob vytvoření**: generuje Firebase


### <a name="buid"></a>BUID

Broadcasted user ID, ID registrace zařízení, lenght: 10 bytů

**způsob vytvoření**: generuje server (callable Firebase functions)


### <a name="tuid"></a>TUID

Transmitted user ID, opět jednoznačné user ID, ale takové, které se vejde do BLE broadcastu, délka: 10 bytů

**způsob vytvoření**: generuje server (callable Firebase functions)


### Proximity data

data o detekované blízkosti ostatních instalací aplikace s údajem o době setkání

**způsob vytvoření**: získává mobil skenováním okolí

