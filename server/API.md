# Planung zur Server-API

Geplante Domänen der API:

- Telemetrie
- Schätzfragen
- Quizpoker Spiele

## Telemetrie

- [X] POST: /telemetrie/upload auth: NONE body: `TelemetrieReport`
  desc: Empfängt die Telemetriedaten (ggf. große Dateien. -> Pro LogDomain max. 3 kB Logs) und speichert diese in einer Datenbank ab.

## Entitled Users

CRUD USER

- [x] PUT /user
- [x] GET /user/all
- [x] GET /user/{ID}
- [x] POST /user/{ID}
- [x] DELETE /user/{ID}

## Schätzfragen

### API Endpunkte

CRUD Suggestion:

- [X] PUT /guesstimate/suggestion -> NO_AUTH
- [X] GET /guesstimate/suggestion/all -> SUGGESTION_READ
- [X] GET /guesstimate/suggestion/unread -> SUGGESTION_READ ; first unread suggestion
- [X] GET /guesstimate/suggestion/{UUID} -> SUGGESTION_READ
- [X] POST /guesstimate/suggestion/{UUID} -> NO_AUTH
- [X] DELETE /guesstimate/suggestion

ACTIONS Suggestion:

- [X] PUT /guesstimate/suggestion/{UUID}/comment -> SUGGESTION_COMMENT_CRATE ; Add comment
- [ ] POST /guesstimate/suggestion/{UUID}/accept -> SUGGESTION_UPDATE & QUESTION_CREATE
- [ ] POST /guesstimate/suggestion/{UUID}/decline -> SUGGESTION_UPDATE

CRUD question:

- [X] PUT /guesstimate/question
- [X] GET /guesstimate/question/all
- [X] GET /guesstimate/question/{id}
- [X] POST /guesstimate/question/{id}
- [X] DELETE /guesstimate/question/{id}

User-Query (quasi READ):

- [X] GET /guesstimate/questions ->
  (body- or path-)params:
    - number: Anzahl der Fragen. Max: 50
    - category: Liste der gewünschten Kategorienamen (enum), Kommagetrennt
    - difficulty: Gewünschte Schwierigkeitsgrade (namen, kommagetrennt, ...)

## Quizpoker

### REST

Lobbies:

- [x] Host/Owner
- [x] Spieler
- [x] Name
- [x] Öffentlich oder privat
- [x] UUID
- [x] Zugangscode (6-digit alphanumerical) -> Nur gültig während Lobby Waiting
- [x] Einstellungen:
    - [x] Standardstartpunkte
    - [x] Fragekategorien
    - [x] Frageschwierigkeiten
    - [x] Fragesprachen
    - [x] Watch or kick nach pleite
    - [x] Max. Anzahl Fragen
    - [x] Blinds Strategie
    - [x] Blinds größen
    - [x] Aufdeckverhalten
    - [x] Timeout/Disconnect
    - [x] Verhalten bei Disconnect/Timeout (autofold or kick)
    - [x] Ask dropout if call possible
    - [x] Allow late join
    - [x] Late Join behaviour (def. coins, min. coins, ...)
- [x] Verbindungseinstellungen:
    - [x] Server- [ ]IP
    - [x] REST- [ ]Server- [ ]Port
    - [x] Websocket- [ ]Server- [ ]Port
    - [x] WebSocket Path
    - [x] Heartbeat- [ ]Rate (ms)
    - [x] max. missed Heartbeats
- [x] Spielzustand:
    - [x] Status (LOBBY | STARTING | QUESTION | PAUSE)
    - [x] Runde
    - [x] Aktuelle Frage
    - [x] Aufgedeckte Hinweise
    - [x] Aktuelle Blinds
    - [x] Einsätze und Antworten pro Spieler -> Spieler Obj.

Spieler:

- [x] Name
- [x] Verbindungszustand
- [x] letzter Ping
- [x] Rolle (DEFAULT|BIG_BLIND|SMALL_BLIND|HOST)
- [x] Punkte

CRUD Lobby:

- [x] PUT    /quizpoker/lobby -> Input Player, Returns Lobby-Object (with given player as joined host/creator)
- [x] POST   /quizpoker/lobby/{uuid} -> Input Player, Joins Lobby
- [x] DELETE /quizpoker/lobby/{uuid} -> Input Player, Disconnects Lobby (aktiv)
- [x] GET    /quizpoker/lobby/{uuid} -> Returns current status
- [x] GET    /quizpoker/lobby/token/{token} -> Returns lobby uuid

### WEBSOCKET

WebsocketPayload type: String timestamp: Long data: Map<String, String>?

Heartbeat

````text
  CLIENT...........................SERVER
    | ------------------------------> |
    |  { "type": "Heartbeat",         |
    |    "timestamp": 0 }             |
    | <------------------------------ |
    |  { "type": "HeartbeatAck",      |
    |    "timestamp": 0 }             |
````
REST-Aktionen:

- Konfigurationen
- Spielstart
- Refresh

WS-Aktionen (Client):

CLIENT_HELLO:
  - Draco.Game.Lobby.Id
  - Draco.Game.Player.Name


- Heartbeat
- Connect
- Reconnect
- Disconnect
- Answer
- spielaktion (raise, call, fold)
-

WS-Aktionen (Server):

- Broadcast für Spieleraktionen
- New Question
- New Hint
- Answer published
- Your Turn
- Antwort(en) offengelegt
- Score changed
