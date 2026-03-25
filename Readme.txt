

# Poker Game (Java)

**Opis projektu:**
Projekt implementuje pełną logikę gry w pokera w trybie klient-serwer w Java. Aplikacja została stworzona w oparciu o zasady OOP, z użyciem Mavena do zarządzania projektem oraz narzędzi do testowania i analizy jakości kodu (JUnit, Jacoco, SonarQube).
Gracze mogą dołączać do gry, wykonywać wszystkie typowe akcje w rundach pokerowych (BET, CALL, FOLD, DRAW), a serwer automatycznie obsługuje kolejne fazy gry, takie jak ante, rozdanie kart i showdown.

---

## Funkcjonalności

* Pełna logika gry w pokera (5-card draw)
* Tryb klient-serwer umożliwiający grę wieloosobową
* Automatyczne przechodzenie faz gry: ANTE → DEAL → BET1 → DRAW → BET2 → SHOWDOWN → PAYOUT → END → LOBBY
* Komendy dla graczy: `JOIN`, `START`, `CALL`, `BET AMOUNT`, `FOLD`, `DRAW CARDS=<indeksy>`, `CHECK`
* Integracja z Mavenem i testy jednostkowe przy użyciu JUnit
* Analiza jakości kodu z Jacoco i SonarQube

---

## Jak uruchomić

1. Zbuduj projekt:

```bash
mvn clean install
```

2. Uruchom serwer (przykład z 4 graczami):

```bash
java -jar poker-server/target/poker-server-1.0-SNAPSHOT.jar 4
```

3. Uruchom klientów w osobnych terminalach:

```bash
java -jar poker-client/target/poker-client-1.0-SNAPSHOT.jar
```

4. Dołączanie graczy do gry:

```
JOIN GAME=GAME_0 NAME=ADAM
```

5. Rozpoczęcie gry (host):

```
START
```

6. Kolejne fazy gry:

* **ANTE, DEAL:** serwer automatycznie pobiera ante i rozdaje karty
* **BET1:** gracze wyrównują stawki (`CALL`), podbijają (`BET AMOUNT`) lub pasują (`FOLD`)
* **DRAW:** wymiana kart:

```
DRAW CARDS=<indeksy kart>
```

jeśli nie wymieniasz kart: `DRAW CARDS=,`

* **BET2:** opcje: `CHECK`, `BET AMOUNT`, `CALL`
* **SHOWDOWN → PAYOUT:** serwer ogłasza zwycięzcę i przyznaje żetony
* **END → LOBBY:** powrót do lobby

---

## Technologie

* Java 
* Maven
* JUnit
* Jacoco
* SonarQube

---



