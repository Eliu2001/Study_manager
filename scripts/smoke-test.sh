#!/usr/bin/env bash
# Prueba de humo de la API de study-manager.
#
# Requisitos: la base de datos y la aplicacion corriendo (por defecto en http://localhost:8080).
# Uso:        ./scripts/smoke-test.sh
#             BASE_URL=http://localhost:9090 ./scripts/smoke-test.sh
#
# Crea dos usuarios nuevos con correos aleatorios, ejercita todos los endpoints y comprueba los
# codigos de respuesta y algunos valores. Deja datos de prueba en la base (puedes borrarlos con
# "docker compose down -v" si quieres empezar de cero).

set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0
FAIL=0
RESP_CODE=""
RESP_BODY=""
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

# call METHOD RUTA [TOKEN] [JSON]  ->  deja RESP_CODE y RESP_BODY
call() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -o "$TMP" -w '%{http_code}' -X "$method" "$BASE_URL$path")
  if [ -n "$token" ]; then args+=(-H "Authorization: Bearer $token"); fi
  if [ -n "$body" ]; then args+=(-H 'Content-Type: application/json' -d "$body"); fi
  RESP_CODE="$(curl "${args[@]}")"
  RESP_BODY="$(cat "$TMP")"
}

# check NOMBRE CODIGO_ESPERADO
check() {
  if [ "$RESP_CODE" = "$2" ]; then
    PASS=$((PASS + 1)); echo "  ok    $1 ($RESP_CODE)"
  else
    FAIL=$((FAIL + 1)); echo "  FALLO $1: esperado $2, obtenido $RESP_CODE"
    echo "        ${RESP_BODY:0:300}"
  fi
}

# contains NOMBRE TEXTO  (busca TEXTO en el cuerpo de la ultima respuesta)
contains() {
  if [[ "$RESP_BODY" == *"$2"* ]]; then
    PASS=$((PASS + 1)); echo "  ok    $1"
  else
    FAIL=$((FAIL + 1)); echo "  FALLO $1: no aparece '$2'"
    echo "        ${RESP_BODY:0:300}"
  fi
}

# not_contains NOMBRE TEXTO
not_contains() {
  if [[ "$RESP_BODY" != *"$2"* ]]; then
    PASS=$((PASS + 1)); echo "  ok    $1"
  else
    FAIL=$((FAIL + 1)); echo "  FALLO $1: aparece '$2' y no deberia"
    echo "        ${RESP_BODY:0:300}"
  fi
}

# field CAMPO  ->  valor del primer "CAMPO" del cuerpo (numero o texto)
field() {
  echo "$RESP_BODY" | sed -n -E "s/.*\"$1\":\"?([^\",}]*)\"?.*/\1/p" | head -1
}

section() { echo; echo "== $1"; }

# ---------------------------------------------------------------------------
section "Estado de la aplicacion"
call GET /actuator/health
if [ "$RESP_CODE" != "200" ]; then
  echo "La aplicacion no responde en $BASE_URL (codigo $RESP_CODE)."
  echo "Levanta la base de datos y ejecuta la app antes de correr esta prueba."
  exit 2
fi
check "health" 200
call GET /api/terms
check "ruta protegida sin token" 401

# ---------------------------------------------------------------------------
section "Registro y login"
SUFFIX="$(date +%s)$RANDOM"
EMAIL_A="smoke-a-$SUFFIX@example.com"
EMAIL_B="smoke-b-$SUFFIX@example.com"
PASS_USER="Clave12345"

call POST /api/auth/register "" "{\"name\":\"Usuario A\",\"email\":\"$EMAIL_A\",\"password\":\"$PASS_USER\"}"
check "registro A" 201
call POST /api/auth/register "" "{\"name\":\"Usuario A\",\"email\":\"$EMAIL_A\",\"password\":\"$PASS_USER\"}"
check "registro duplicado" 409
call POST /api/auth/register "" '{"name":"","email":"mal","password":"123"}'
check "registro con datos invalidos" 400
call POST /api/auth/register "" "{\"name\":\"Usuario B\",\"email\":\"$EMAIL_B\",\"password\":\"$PASS_USER\"}"
check "registro B" 201

call POST /api/auth/login "" "{\"email\":\"$EMAIL_A\",\"password\":\"$PASS_USER\"}"
check "login A" 200
TOKEN_A="$(field accessToken)"
call POST /api/auth/login "" "{\"email\":\"$EMAIL_B\",\"password\":\"$PASS_USER\"}"
check "login B" 200
TOKEN_B="$(field accessToken)"
call POST /api/auth/login "" "{\"email\":\"$EMAIL_A\",\"password\":\"incorrecta123\"}"
check "login con clave incorrecta" 401
call GET /api/users/me "$TOKEN_A"
check "usuario actual" 200
contains "usuario actual es A" "$EMAIL_A"

if [ -z "$TOKEN_A" ] || [ -z "$TOKEN_B" ]; then
  echo "No se pudo obtener un token; no tiene sentido seguir."
  exit 2
fi

# ---------------------------------------------------------------------------
section "Periodos"
call POST /api/terms "$TOKEN_A" '{"name":"2027-1","startDate":"2027-03-01","endDate":"2027-07-15"}'
check "crear periodo" 201
TERM_ID="$(field id)"
call POST /api/terms "$TOKEN_A" '{"name":"2027-1","startDate":"2027-03-01","endDate":"2027-07-15"}'
check "periodo duplicado" 409
call POST /api/terms "$TOKEN_A" '{"name":"2027-9","startDate":"2027-12-18","endDate":"2027-08-03"}'
check "periodo con fechas al reves" 400
call GET /api/terms "$TOKEN_A"
check "listar periodos" 200
call GET "/api/terms/$TERM_ID" "$TOKEN_A"
check "ver periodo" 200
call PUT "/api/terms/$TERM_ID" "$TOKEN_A" '{"name":"2027-1","startDate":"2027-03-02","endDate":"2027-07-16"}'
check "editar periodo" 200
call GET "/api/terms/$TERM_ID" "$TOKEN_B"
check "otro usuario no ve el periodo" 404

# ---------------------------------------------------------------------------
section "Asignaturas"
call POST "/api/terms/$TERM_ID/courses" "$TOKEN_A" '{"name":"Bases de Datos","code":"BD101","credits":6}'
check "crear asignatura" 201
COURSE_ID="$(field id)"
call POST "/api/terms/$TERM_ID/courses" "$TOKEN_A" '{"name":"Bases de Datos"}'
check "asignatura duplicada" 409
call POST "/api/terms/$TERM_ID/courses" "$TOKEN_A" '{"name":"Otra","credits":0,"passingGrade":8.0}'
check "asignatura con datos invalidos" 400
call GET "/api/terms/$TERM_ID/courses" "$TOKEN_A"
check "listar asignaturas" 200
call GET "/api/courses/$COURSE_ID" "$TOKEN_A"
check "ver asignatura" 200
call PUT "/api/courses/$COURSE_ID" "$TOKEN_A" '{"name":"Bases de Datos","code":"BD101","credits":6,"passingGrade":4.0}'
check "editar asignatura" 200
call GET "/api/courses/$COURSE_ID" "$TOKEN_B"
check "otro usuario no ve la asignatura" 404
call POST "/api/terms/$TERM_ID/courses" "$TOKEN_B" '{"name":"Intrusa"}'
check "otro usuario no crea asignaturas en mi periodo" 404

# ---------------------------------------------------------------------------
section "Evaluaciones"
EXAM_DUE="$(date -d '+2 days' +%F)"
NEXT_MONTH="$(date -d '+1 month' +%F)"

call POST "/api/courses/$COURSE_ID/assessments" "$TOKEN_A" '{"name":"Sumativa 1","type":"SUMMATIVE","weight":35,"topics":"Modelo entidad-relacion"}'
check "crear sumativa 1 (sin fecha)" 201
S1_ID="$(field id)"
call POST "/api/courses/$COURSE_ID/assessments" "$TOKEN_A" "{\"name\":\"Sumativa 2\",\"type\":\"SUMMATIVE\",\"weight\":35,\"dueDate\":\"$NEXT_MONTH\"}"
check "crear sumativa 2" 201
S2_ID="$(field id)"
call POST "/api/courses/$COURSE_ID/assessments" "$TOKEN_A" "{\"name\":\"Examen\",\"type\":\"EXAM\",\"weight\":30,\"dueDate\":\"$EXAM_DUE\"}"
check "crear examen" 201
EXAM_ID="$(field id)"
call POST "/api/courses/$COURSE_ID/assessments" "$TOKEN_A" '{"name":"Extra","type":"SUMMATIVE","weight":10}'
check "ponderacion total sobre 100%" 400
call GET "/api/courses/$COURSE_ID/assessments" "$TOKEN_A"
check "listar evaluaciones" 200
call GET "/api/assessments/$S1_ID" "$TOKEN_A"
check "ver evaluacion" 200

call PUT "/api/assessments/$S1_ID" "$TOKEN_A" '{"name":"Sumativa 1","type":"SUMMATIVE","weight":35,"grade":8.0}'
check "nota fuera de rango" 400
call PUT "/api/assessments/$S1_ID" "$TOKEN_A" '{"name":"Sumativa 1","type":"SUMMATIVE","weight":35,"status":"GRADED"}'
check "estado GRADED sin nota" 400
call GET "/api/assessments/$S1_ID" "$TOKEN_B"
check "otro usuario no ve la evaluacion" 404
call GET "/api/courses/$COURSE_ID/assessments" "$TOKEN_B"
check "otro usuario no lista evaluaciones" 404

# ---------------------------------------------------------------------------
section "Recordatorios"
call POST "/api/assessments/$EXAM_ID/reminders" "$TOKEN_A" '{"daysBefore":3}'
check "crear recordatorio" 201
REM_ID="$(field id)"
call POST "/api/assessments/$EXAM_ID/reminders" "$TOKEN_A" '{"daysBefore":3}'
check "recordatorio duplicado" 409
call POST "/api/assessments/$EXAM_ID/reminders" "$TOKEN_A" '{"daysBefore":-1}'
check "recordatorio con dias negativos" 400
call POST "/api/assessments/$S1_ID/reminders" "$TOKEN_A" '{"daysBefore":1}'
check "recordatorio en evaluacion sin fecha" 400
call GET "/api/assessments/$EXAM_ID/reminders" "$TOKEN_A"
check "listar recordatorios" 200
call GET /api/reminders/upcoming "$TOKEN_A"
check "recordatorios pendientes" 200
contains "el aviso del examen esta vigente" "\"reminderId\":$REM_ID"
call GET /api/reminders/upcoming "$TOKEN_B"
check "pendientes del otro usuario" 200
not_contains "el otro usuario no ve mis avisos" "\"reminderId\":$REM_ID"
call POST "/api/reminders/$REM_ID/dismiss" "$TOKEN_B"
check "otro usuario no descarta mi aviso" 404
call POST "/api/reminders/$REM_ID/dismiss" "$TOKEN_A"
check "descartar aviso" 204
call GET /api/reminders/upcoming "$TOKEN_A"
not_contains "el aviso descartado ya no aparece" "\"reminderId\":$REM_ID"
call DELETE "/api/reminders/$REM_ID" "$TOKEN_B"
check "otro usuario no borra mi aviso" 404
call DELETE "/api/reminders/$REM_ID" "$TOKEN_A"
check "borrar aviso" 204

# ---------------------------------------------------------------------------
section "Fechas academicas"
call POST /api/academic-dates "$TOKEN_A" '{"title":"Feriado","eventDate":"2027-05-21","type":"HOLIDAY"}'
check "crear fecha general" 201
DATE_ID="$(field id)"
call POST /api/academic-dates "$TOKEN_A" "{\"title\":\"Entrega informe\",\"eventDate\":\"$NEXT_MONTH\",\"type\":\"DEADLINE\",\"courseId\":$COURSE_ID}"
check "crear fecha de una asignatura" 201
call POST /api/academic-dates "$TOKEN_A" '{"title":"X","eventDate":"2027-05-21","type":"INVENTADO"}'
check "tipo de fecha invalido" 400
call POST /api/academic-dates "$TOKEN_B" "{\"title\":\"Intrusa\",\"eventDate\":\"2027-05-21\",\"type\":\"OTHER\",\"courseId\":$COURSE_ID}"
check "otro usuario no usa mi asignatura" 404
call GET /api/academic-dates "$TOKEN_A"
check "listar fechas" 200
call GET "/api/academic-dates?from=2027-05-01&to=2027-05-31" "$TOKEN_A"
check "listar fechas de un rango" 200
contains "el rango incluye el feriado" "Feriado"
not_contains "el rango excluye la entrega" "Entrega informe"
call GET "/api/academic-dates?from=2027-06-01&to=2027-05-01" "$TOKEN_A"
check "rango invertido" 400
call GET "/api/academic-dates/$DATE_ID" "$TOKEN_A"
check "ver fecha" 200
call PUT "/api/academic-dates/$DATE_ID" "$TOKEN_A" '{"title":"Feriado local","eventDate":"2027-05-21","type":"HOLIDAY"}'
check "editar fecha" 200
call GET "/api/academic-dates/$DATE_ID" "$TOKEN_B"
check "otro usuario no ve la fecha" 404
call DELETE "/api/academic-dates/$DATE_ID" "$TOKEN_A"
check "borrar fecha" 204
call GET "/api/academic-dates/$DATE_ID" "$TOKEN_A"
check "la fecha borrada ya no existe" 404

# ---------------------------------------------------------------------------
section "Notas y resumenes"
call PUT "/api/assessments/$S1_ID" "$TOKEN_A" '{"name":"Sumativa 1","type":"SUMMATIVE","weight":35,"grade":5.57,"comment":"Me fue bien"}'
check "ponerle nota a la sumativa 1 (5.57)" 200
contains "la segunda decimal se corta (5.5)" '"grade":5.5'
contains "queda calificada" '"status":"GRADED"'
call GET "/api/courses/$COURSE_ID/summary" "$TOKEN_A"
check "resumen de la asignatura (parcial)" 200
contains "sigue en curso" '"status":"IN_PROGRESS"'
contains "promedio necesario 3.2" '"requiredAverage":3.2'

call PUT "/api/assessments/$S2_ID" "$TOKEN_A" "{\"name\":\"Sumativa 2\",\"type\":\"SUMMATIVE\",\"weight\":35,\"dueDate\":\"$NEXT_MONTH\",\"grade\":5.0}"
check "nota de la sumativa 2 (5.0)" 200
call PUT "/api/assessments/$EXAM_ID" "$TOKEN_A" "{\"name\":\"Examen\",\"type\":\"EXAM\",\"weight\":30,\"dueDate\":\"$EXAM_DUE\",\"grade\":4.0}"
check "nota del examen (4.0)" 200
call GET "/api/courses/$COURSE_ID/summary" "$TOKEN_A"
check "resumen de la asignatura (completo)" 200
contains "nota final 4.8" '"finalGrade":4.8'
contains "aprobada" '"status":"PASSED"'
call GET "/api/terms/$TERM_ID/summary" "$TOKEN_A"
check "resumen del periodo" 200
contains "promedio del periodo 4.8" '"average":4.8'
call GET /api/summary "$TOKEN_A"
check "resumen acumulado" 200
contains "promedio acumulado 4.8" '"cumulativeAverage":4.8'
call GET "/api/courses/$COURSE_ID/summary" "$TOKEN_B"
check "otro usuario no ve el resumen de mi asignatura" 404
call GET "/api/terms/$TERM_ID/summary" "$TOKEN_B"
check "otro usuario no ve el resumen de mi periodo" 404
call GET /api/summary "$TOKEN_B"
check "resumen acumulado del otro usuario" 200
contains "el otro usuario no tiene promedio" '"cumulativeAverage":null'

# ---------------------------------------------------------------------------
section "Borrado en cascada"
call DELETE "/api/terms/$TERM_ID" "$TOKEN_A"
check "borrar periodo" 204
call GET "/api/courses/$COURSE_ID" "$TOKEN_A"
check "la asignatura desaparece con su periodo" 404
call GET "/api/assessments/$S1_ID" "$TOKEN_A"
check "la evaluacion desaparece con su periodo" 404

# ---------------------------------------------------------------------------
echo
echo "Resultado: $PASS correctas, $FAIL con fallo."
if [ "$FAIL" -gt 0 ]; then exit 1; fi
