# Greitas pranešimas → tvarkaumiesta.lt

Android programėlė, kuri **neturi savo paleidimo ikonos**. Ji gyvena tik
dalijimosi meniu („Share"): nufotografuoji problemą → Galerijoje spaudi
„Bendrinti" → pasirenki šią programėlę → per ~3 s matai jau užpildytą
pranešimą → spaudi „Siųsti".

---

## APK sukūrimas be Android Studio (žingsnis po žingsnio)

APK surenka GitHub serveriai. Tavo kompiuteryje nieko diegti nereikia — tik naršyklė.

### A. Įkelk projektą į GitHub

1. Susikurk paskyrą github.com (jei dar neturi).
2. Viršuje dešinėje **+ → New repository**. Pavadinimą bet kokį,
   pažymėk **Private**, spausk **Create repository**.
3. Naujame puslapyje spausk nuorodą **uploading an existing file**.
4. Iš šio ZIP ištrauktą turinį **nutempk į naršyklės langą**. Svarbu:
   tempk *aplanko vidų* (kad `app`, `settings.gradle.kts` ir kiti atsidurtų
   repozitoriumo šaknyje), o ne patį aplanką.
5. Apačioje spausk **Commit changes**.

> `.github` aplankas per tempimą kartais neįkeliamas (paslėptas). Jei po
> įkėlimo repozitoriume nematai `.github/workflows/build.yml` — žr. C dalį.

### B. Įrašyk Claude raktą kaip paslaptį

1. Repozitoriume: **Settings → Secrets and variables → Actions**.
2. **New repository secret**.
3. Name: `ANTHROPIC_API_KEY`, Secret: tavo raktas. **Add secret**.

Taip raktas nepatenka į kodą. (`user_token` ir `user_id` čia NEreikia —
juos suvesi telefone.)

### C. Jei `.github` neįsikėlė

1. Repozitoriume **Add file → Create new file**.
2. Failo pavadinimo laukelyje įrašyk tiksliai: `.github/workflows/build.yml`
   (pasvirieji brūkšniai sukurs aplankus automatiškai).
3. Į turinį įklijuok viso `build.yml` failo tekstą iš ZIP.
4. **Commit changes**.

### D. Paleisk surinkimą ir parsisiųsk APK

1. Repozitoriume atsidaryk skiltį **Actions**.
2. Jei paprašys — patvirtink, kad nori įjungti workflow'us.
3. Kairėje pasirink **Surinkti APK → Run workflow → Run workflow**.
   (Arba jis pasileidžia pats po įkėlimo.)
4. Palauk ~5 min., kol prie darbo atsiras žalia varnelė.
5. Spustelėk tą darbą → apačioje **Artifacts → app-debug-apk**.
   Parsisiųs ZIP, jo viduje `app-debug.apk`.

### E. Įdiek telefone

1. Persiųsk APK į telefoną (el. paštu sau, Google Drive ar USB).
2. Telefone atidaryk failą per Failų tvarkyklę.
3. Android paprašys leisti diegti iš šio šaltinio → įjungi jungiklį → grįžti.
4. Įdiegta. Rasi „Pranešti savivaldybei" ikoną.

### F. Suvesk prisijungimą

1. Atidaryk programėlę (ikoną) → atsivers nustatymai.
2. Įrašyk **user_id** `114824` ir naują **user_token** iš naršyklės
   (F12 → Application → Local Storage, žr. žemiau).
3. Spausk **Patikrinti** — jei rodo galiojimo datą, viskas veikia.

### Kai norėsi ką nors pakeisti

Redaguoji failą tiesiai GitHub'e (pieštuko ikona) → Commit → Actions
automatiškai surenka naują APK. Įdiegi ant seno, duomenys išlieka.

---

## 1. Kaip veikia (srautas)

```
ACTION_SEND (image/*)
   │
   ├─ 1. EXIF GPS iš nuotraukos ──┐
   │     (jei nėra) → įrenginio    ├──→ koordinatės
   │     dabartinė vieta ──────────┘
   │
   ├─ 2. Geocoder → gatvė + namo nr.
   │
   ├─ 3. GET /report_types → aktualus kategorijų sąrašas
   │
   ├─ 4. Claude vision: nuotrauka + kategorijų sąrašas + adresas
   │        → {report_type_id, description (LT), confidence}
   │
   ├─ 5. Peržiūros ekranas (viskas redaguojama)
   │
   ├─ 6. POST /report_photos (multipart) → uuid
   └─ 7. POST /reports → pranešimas pateiktas
```

---

## 2. API — oficiali dokumentacija

**Swagger/OpenAPI 2.0.0: https://api-tvarkau.vilnius.lt/docs-v2**

Bazė: `https://api-tvarkau.vilnius.lt/api/v2/` (Laravel + Cloudflare, CORS atviras).

### Pateikimas

```
POST /problems/register     Content-Type: application/json
                            user_token: <tik jei prisijungęs>
```

Privaloma: `city_id`, `type`, `latitude`, `longitude`, `address`,
`violation_date_time` (`YYYY-MM-DD HH:mm`), `description`.

Neprivaloma: `car_plate_no`, `reporter_name`, `reporter_email`,
`reporter_address`, `reporter_phone`, `serial_number`, `files[]` (base64).

### Kiti naudingi

| Endpoint | Ką duoda |
|---|---|
| `GET categories/problem-types?cityId=1` | kategorijos su tipais ir vėliavėlėmis |
| `GET city/{id}` | miesto nustatymai |
| `GET problems?serial_number=X` | savi pranešimai |
| `PATCH problems/{id}` | redagavimas, `delete_files[]` |
| `PATCH user/synchronize` | anoniminių pranešimų susiejimas su paskyra |

### Problemų tipų vėliavėlės — svarbu

Ne kiekvieną tipą galima pateikti automatiškai:

- `redirectUrl` — pranešimas teikiamas visai kitoje svetainėje
- `onlyRegister: 1` — tik prisijungusiems
- `additionalFields[].required` — reikia papildomų laukų (pvz. šviestuvo tipas)
- `seasonal: 1` — priimama tik tam tikru metų laiku

`TvarkauApi.submittableTypes()` tokius atmeta, kad Claude jų nepasiūlytų.

### serial_number

Anoniminio pranešėjo ID. Programėlė jį sugeneruoja **kartą** ir laiko
SharedPreferences — todėl vėliau savo pranešimus galėsi rasti arba susieti
su paskyra. Svetainė generuoja naują kaskart ir tą galimybę praranda.

### Prisijungimas

Tik Vilniaus paskyra (`login`) arba e. valdžios vartai (`e-government/login`).
El. pašto ir slaptažodžio nėra. Pirmajai versijai anoniminis teikimas
paprastesnis; `TvarkauApi.userToken` paliktas ateičiai.

## 3. Ką reikia įsirašyti

`local.properties` (į git neina) — tik Claude raktui:

```properties
ANTHROPIC_API_KEY=sk-ant-...
```

Visa kita įvedama **programėlės nustatymuose telefone**, ne kode — kad
pasibaigus žetono galiojimui nereikėtų iš naujo surinkinėti APK.

### Kaip gauti user_token

1. Naršyklėje prisijunk prie tvarkaumiesta.lt (Vilniaus paskyra arba
   e. valdžios vartai).
2. `F12` → **Application** → Local Storage → `https://tvarkaumiesta.lt`
3. Ieškok įrašo su `user` — jame bus `user_token` ir naudotojo `id`.
   Jei ten nerastum: **Network** skiltyje spustelėk bet kurią užklausą po
   prisijungimo → Request Headers → `user_token`.
4. Telefone atidaryk programėlę („Pranešti savivaldybei" ikona) ir įklijuok
   `user_token` bei `user_id`. Spausk **Patikrinti** — parodys galiojimo datą.

Žetonas kada nors baigs galioti; tada tiesiog įklijuok naują. `Patikrinti`
mygtukas kviečia `GET token/check?user_id=`.

### Pilnas prisijungimo srautas — sąmoningai nedarytas

API turi `login/url/app`, `e-government/login` ir `login/user`
(su `accessCode` + `clientKey` + `clientSecret`). Įgyvendinti tai Android'e
reikštų naršyklės langą, atgalinį `redirectUrl` ir sesijos valdymą — daug
darbo vienam telefonui. `clientKey`/`clientSecret` reikštumėtų reikėtų imti iš
svetainės Network skilties prisijungimo metu.

Žetono įklijavimas duoda tą patį rezultatą per dvi minutes.

### Ar iš viso reikia prisijungti?

`problems/register` priima `reporter_email` ir `reporter_phone` **ir be
žetono**. Gali būti, kad atsakymui gauti pakanka vien el. pašto — verta
pabandyti pirmiau. Paskyra reikalinga „Mano pranešimų" sąrašui, statusų
sekimui ir tipams su `onlyRegister: 1`.

Programėlė įspėja, jei nenurodytas nei žetonas, nei el. paštas.

### Anoniminių pranešimų susiejimas

Jei jau siuntei be prisijungimo, nustatymuose yra mygtukas
„Susieti anoniminius pranešimus su paskyra" — jis kviečia
`PATCH user/synchronize` su šio įrenginio `serial_number`.

## 4. Failai

| Failas | Ką daro |
|---|---|
| `AndroidManifest.xml` | Registruoja programėlę „Share" meniu, leidimai |
| `ShareActivity.kt` | Visas srautas + peržiūros ekranas (Compose) |
| `Settings.kt` | Nustatymų ekranas ir SharedPreferences |
| `Theme.kt` | Bendra tema, seka sistemos šviesus/tamsus režimą |
| `MapPicker.kt` | OpenStreetMap žemėlapis vietai pažymėti (be API rakto) |
| `PhotoMeta.kt` | EXIF GPS, vietos atsarginis variantas, adreso paieška |
| `Vision.kt` | Claude iškvietimas: kategorija + aprašymas lietuviškai |
| `TvarkauApi.kt` | OAuth, nuotraukos įkėlimas, pranešimo pateikimas |
| `build.gradle.kts` | Priklausomybės |

---

## 5. Patvirtinimo žingsnis ir tema

Numatytoji reikšmė `AUTO_SEND_SECONDS = 0` — programėlė viską užpildo, bet
laukia tavo spustelėjimo. Nustatęs `= 5` gausi beveik visiškai automatinį
veikimą su 5 s atšaukimo langu (`ShareActivity.kt`, viršuje).

Kodėl verta palikti bent trumpą pauzę:

- Vilniuje pranešimai automatiškai registruojami savivaldybės dokumentų
  valdymo sistemoje „Avilys" — tai jau oficialus dokumentas, ne juodraštis.
- Nuotraukoje gali netyčia patekti valstybiniai numeriai, veidai, langai.
  Vaizdo modelis to už tave neįvertins.
- Klaidinga kategorija reiškia, kad pranešimas nukeliaus ne tam skyriui ir
  tiesiog užstrigs.
- Verta peržvelgti portalo naudojimo taisykles dėl automatizuoto teikimo —
  asmeniniam naudojimui problemų neturėtų kilti, bet masinis srautas
  greičiausiai bus vertinamas kitaip.

---


**Patvirtinimo langas.** Paspaudus „Siųsti" pranešimas dar neišsiunčiamas —
atsiveria suvestinė su galutiniais duomenimis (kategorija, adresas, tikslios
koordinatės, data, aprašymas, ar teikiama anonimiškai). Tik paspaudus
„Pateikti" jis keliauja į serverį. „Grįžti taisyti" grąžina į redagavimą.

**Šviesus / tamsus režimas.** Programėlė seka sistemos nustatymą automatiškai
(`Theme.kt`). Android 12+ perima ir sistemos akcentinę spalvą.

## 6. Kai nuotraukoje nėra GPS

Dalis nuotraukų būna be vietos žymos (išjungta kameroje arba nuvalyta
persiunčiant). Tada:

1. Duok programėlei **vietos leidimą** — pirmą kartą dalijantis Android
   paklaus; jei atmetei, įjunk: Nustatymai → Programos → Pranešti savivaldybei
   → Leidimai → Vieta. Tada bus imama dabartinė telefono vieta.
2. Jei ir to nėra — peržiūros ekrane atsiranda **žemėlapis**. Baksteli tašką,
   kur stovi objektas (arba nutempi smeigtuką) — koordinatės nustatomos tiksliai.
   Po bakstelėjimo koordinatės paverčiamos adresu automatiškai.
3. Po žemėlapiu yra ir **adreso laukas** — įvedi gatvę, spaudi „Rasti vietą".

**Google Photos ypatumas:** dalijantis per Photos „Bendrinti", GPS iš nuotraukos
dažnai pašalinamas (privatumo sumetimais) — todėl telefone koordinatės būna
tuščios. Sprendimai: (a) duok vietos leidimą ir fotografuok problemą vietoje —
tada koordinates duoda pats telefonas; (b) naudok žemėlapį peržiūros ekrane.

Be koordinačių serveris pranešimo nepriima (`422`), nes `latitude`/`longitude`
yra privalomi.

**Dažna spąstų vieta:** persiuntus nuotrauką per pokalbių programėlę (Messenger,
WhatsApp), GPS blokas dažnai lieka nuotraukoje, bet jo reikšmės tampa tuščios
(`NaN`). Programėlė tokias atmeta ir griebia įrenginio GPS. Todėl geriausia
dalintis **originalu iš Galerijos**, o ne persiųsta kopija.

## 7. Našumas ir tinklas

Nuotrauka siunčiama base64 pavidalu pačiame JSON'e, todėl prieš siuntimą ji
sumažinama iki 900 px ir suspaudžiama, kol JPEG telpa į ~500 KB. Be to nustatyti
tinklo timeout'ai (jungimasis 15 s, siuntimas/skaitymas po 60 s), tad jei
serveris neatsako, programėlė nepakimba amžinai, o parodo aiškią klaidą.

Jei Pixel daro labai raiškias nuotraukas ir siuntimas vis tiek lėtas —
sumažink `maxSide` (`PhotoMeta.kt`) iki 720.

## 8. Kas liko nepatikrinta

Kodas parašytas pagal oficialią specifikaciją, bet realia užklausa dar
neišbandytas. Jei `register` grąžins 422, atsakyme bus `errors` su konkrečiu
lauku — klaida rodoma tiesiai ekrane.

Vienas galimas netikslumas: specifikacijoje `problems/register` pažymėtas
`security: user_token`, nors svetainės kodas pateikia ir be jo, kai miestas
leidžia anoniminį teikimą. Jei gausi 401, reikės prisijungimo per
e. valdžios vartus.
