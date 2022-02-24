# Planung zur Server-API

Geplante Domänen der API:

- Telemetrie
- Schätzfragen
- Quizpoker Spiele

## Telemetrie

 - [X] POST: /telemetrie/upload
         auth: NONE
         body: `TelemetrieReport`
         desc: Empfängt die Telemetriedaten (ggf. große Dateien. -> Pro LogDomain max. 3 kB Logs) und speichert diese in einer Datenbank ab.

## Entitled Users

CRUD USER
 - [x] PUT    /user
 - [x] GET    /user/all
 - [x] GET    /user/{ID}
 - [x] POST   /user/{ID}
 - [x] DELETE /user/{ID}

## Schätzfragen

### API Endpunkte

CRUD Suggestion:
 - [X] PUT    /guesstimate/suggestion         -> NO_AUTH
 - [X] GET    /guesstimate/suggestion/all     -> SUGGESTION_READ
 - [X] GET    /guesstimate/suggestion/unread  -> SUGGESTION_READ ; first unread suggestion
 - [X] GET    /guesstimate/suggestion/{UUID}  -> SUGGESTION_READ
 - [X] POST   /guesstimate/suggestion/{UUID}  -> NO_AUTH
 - [X] DELETE /guesstimate/suggestion

ACTIONS Suggestion:
 - [X] PUT    /guesstimate/suggestion/{UUID}/comment     -> SUGGESTION_COMMENT_CRATE ; Add comment
 - [ ] POST   /guesstimate/suggestion/{UUID}/accept      -> SUGGESTION_UPDATE & QUESTION_CREATE
 - [ ] POST   /guesstimate/suggestion/{UUID}/decline     -> SUGGESTION_UPDATE

CRUD question:
 - [X] PUT    /guesstimate/question
 - [X] GET    /guesstimate/question/all
 - [X] GET    /guesstimate/question/{id}
 - [X] POST   /guesstimate/question/{id}
 - [X] DELETE /guesstimate/question/{id}

User-Query (quasi READ):
 - [X] GET    /guesstimate/questions ->
    (body- or path-)params:
      - number: Anzahl der Fragen. Max: 50
      - category: Liste der gewünschten Kategorienamen (enum), Kommagetrennt
      - difficulty: Gewünschte Schwierigkeitsgrade (namen, kommagetrennt, ...)

## Quizpoker