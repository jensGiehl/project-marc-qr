# Marc QR

Marc QR ist eine mobile-first Spring-Boot-Webanwendung zum Erstellen gestalteter QR-Codes und eindeutiger Zufallscodes. Das Projekt verwendet Java 25, Maven, Thymeleaf und Bootstrap aus einem WebJar. Es benötigt im Browser keine externen CDN-Ressourcen.

## Funktionen und Endpunkte

| Endpunkt | Funktion |
| --- | --- |
| `GET /` | Übersichtsseite mit Links zu allen Werkzeugen |
| `GET /qr` | Einzelnen QR-Code mit Live-Vorschau erzeugen |
| `GET /qr/batch` | Für jede nicht leere Textzeile explizit einen QR-Code erzeugen |
| `GET /codes` | Eindeutige Codes konfigurieren und als Excel-Datei exportieren |
| `POST /api/qr` | PNG für die Live-Vorschau erzeugen |
| `POST /api/qr/batch` | QR-Code-Stapel als JSON erzeugen |
| `POST /codes/excel` | Generierte Codes direkt als `.xlsx` herunterladen |

Die Startseite öffnet jeden ausgewählten Service in einem neuen Browser-Tab.

### QR-Codes

- Inhaltstypen Text, Webadresse, E-Mail, Telefon und SMS
- zeitnah aktualisierte Live-Vorschau
- Größe von 160 bis 1.200 Pixeln
- frei wählbare Vorder- und Hintergrundfarbe
- optionales Logo in der Mitte (PNG oder JPG bis 2 MB)
- automatische stärkere Fehlerkorrektur bei Verwendung eines Logos
- Kapazitätsanzeige und serverseitige Prüfung passend zu Inhalt und Fehlerkorrektur
- Download als PNG
- Stapelerzeugung für bis zu 100 nicht leere Zeilen; Web- und E-Mail-Adressen werden automatisch erkannt

### Zufallscodes und Excel

- einzelne Auswahl der Ziffern `0–9` und Großbuchstaben `A–Z`
- `0`, `1` und `O` sind standardmäßig deaktiviert
- konfigurierbare Codelänge und maximale Anzahl von Ziffern
- exakte Berechnung des möglichen eindeutigen Coderaums
- duplikatfreie Zufallsauswahl ohne vollständiges Durchprobieren großer Codemengen
- Export von bis zu 100.000 Codes
- Excel-Spalten `Code` und `Beschreibung`; die Beschreibung bleibt leer
- fett formatierte und fixierte Kopfzeile, Filter und automatisch angepasste Spaltenbreiten

Die Anwendung enthält außerdem eigene, humorvolle Fehlerseiten für HTTP 404 und 500.

## Voraussetzungen

- Java 25
- Maven 3.6.3 oder neuer

## Lokal starten

```bash
mvn spring-boot:run
```

Danach ist die Anwendung unter [http://localhost:8080/qr](http://localhost:8080/qr) erreichbar.

Für einen anderen internen Port kann die Umgebungsvariable `PORT` gesetzt werden:

```bash
PORT=9090 mvn spring-boot:run
```

Unter PowerShell:

```powershell
$env:PORT=9090
mvn spring-boot:run
```

## Bauen und testen

```bash
mvn clean verify
java -jar target/project-marc-qr-1.0.0-SNAPSHOT.jar
```

## Docker

Image lokal bauen:

```bash
docker build -t marc-qr .
```

Container auf Host-Port 8080 starten:

```bash
docker run --rm -p 8080:8080 marc-qr
```

Einen anderen Host-Port, zum Beispiel 9090, angeben:

```bash
docker run --rm -p 9090:8080 marc-qr
```

Die Anwendung ist dann unter `http://localhost:9090` erreichbar. Alternativ lässt sich auch der Port im Container ändern:

```bash
docker run --rm -e PORT=9090 -p 9090:9090 marc-qr
```

## GitHub Container Registry

Bei jedem Push auf `main` oder `master` baut die GitHub Action ein Multi-Arch-Docker-Image für `linux/amd64` und `linux/arm64` und veröffentlicht es in der GitHub Container Registry:

```bash
docker pull ghcr.io/jensgiehl/project-marc-qr:latest
docker run --rm -p 8080:8080 ghcr.io/jensgiehl/project-marc-qr:latest
```

Je nach Sichtbarkeit des GitHub-Pakets ist vor dem Pull gegebenenfalls `docker login ghcr.io` erforderlich.

## Technischer Überblick

- Spring Boot 4.1
- Java 25
- Thymeleaf und Bootstrap 5 (WebJar)
- ZXing für QR-Codes
- Apache POI für `.xlsx`-Dateien
- JUnit 5 und AssertJ für Tests
