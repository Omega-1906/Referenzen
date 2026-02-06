# Character Plugin (MultiCharPlugin)

> [!IMPORTANT]
> **Kritische Abhängigkeit:** Dieses Plugin funktioniert **nur** in Verbindung mit dem `core-api` Plugin! Die Core-API verbindet alle Systeme miteinander; ohne sie ist dieses Plugin nicht funktionsfähig.

Ein speziell entwickeltes Multi-Charakter-System für meinen ehemaligen Fantasy Roleplay Server.

## Überblick
Dieses Plugin ermöglicht Spielern das Erstellen und Verwalten mehrerer Charaktere mit individuellen Eigenschaften, Rassen, Skills und Inventaren. Es wurde geschrieben, um die spezifischen Anforderungen des Servers zu erfüllen und bietet tiefgreifende RPG-Mechaniken.

## Features
- **Multi-Charakter-System**: GUI-basierte Auswahl und Verwaltung (`/characters`, `/charmenu`).
- **Rassen-System**: Erstellung von Rassen mit permanenten Potion-Effekten (`/race`).
- **RPG-Mechaniken**:
  - **Würfelsystem**: d20-Würfe mit Attributen (`/rpdice`), Angriffswürfe (`/attackroll`) und Rüstungsklasse (`/armorclass`).
  - **Skilltree**: Verwaltung von Skillpunkten (`/skilltree`).
  - **Alterung**: Anti-Aging-Mechaniken (`/antiagingtrank`).
  - **Verwandlungen**: Integration mit LibsDisguises (`/disguise`).
- **Persona-Verwaltung**: Detaillierte Charakterbeschreibungen und Lore (`/persona`).

## Befehle
### Spieler
- `/characters` - Öffnet die Charakterauswahl.
- `/persona <subcommand>` - Verwaltet Charakterdetails.
- `/skilltree` - Öffnet den Skilltree.
- `/rpdice 1d20 [Attribut]` - Führt einen Würfelwurf aus.
- `/verwandlung` - Verwandelt den eigenen Charakter selbst.

### Admin / Team
- `/charmenu` - Öffnet das Admin-GUI zur Charakterverwaltung.
- `/race <create|edit|delete|list|info>` - Vollständige Rassenverwaltung.
- `/charedit <name> <feld> <wert>` - Bearbeitet spezifische Charakterwerte.
- `/disguise <Spieler> <ID>` - Verwandelt einen Spieler.
- `/antiagingchest` - Verwaltet Anti-Aging-Kisten.

## Installation & Abhängigkeiten
- **Java**: 21
- **Server**: Paper 1.21.1
- **Abhängigkeiten**:
  - `core-api` (Intern)
  - `LibsDisguises`

## Berechtigungen
- `laemedir.admin`: Voller Zugriff.
- `laemedir.team` / `laemedir.loreteam`: Zugriff auf Verwaltungs-Tools für RP-Support.
