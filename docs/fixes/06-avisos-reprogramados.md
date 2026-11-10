# Hallazgo 06: avisos al reprogramar evaluaciones

## Corrección

`AssessmentService.update` compara la fecha anterior (después del refresh bajo bloqueo)
con la solicitada usando `Objects.equals`. Solo cuando son distintas, carga todos los
avisos mediante `ReminderRepository.findAllByAssessmentId` y establece `sent=false`
y `sentAt=null`. Se conservan el aviso, su anticipación y su asociación.

- Fecha a otra fecha: reactiva los avisos, incluidos los descartados ese mismo día.
- Misma fecha o `null` a `null`: no consulta ni modifica avisos.
- `null` a fecha: reactiva los avisos para la nueva programación.
- Fecha a `null`: limpia también el descarte anterior. Sin fecha no hay vencimiento
  notificable; al asignar una fecha posterior los avisos vuelven a quedar pendientes.

El reset y la actualización se hacen sobre entidades managed en la misma transacción:
no hay actualización masiva, `clear` ni transacción independiente. Se preservan
íntegros la autorización, el bloqueo pesimista del curso, el refresh de la evaluación
y las validaciones previas. Un rollback revierte tanto la fecha como el reset.
No se modifica `ReminderService` ni su manejo de `Clock`.

## Pruebas añadidas

- `AssessmentReschedulingTest`: cambio de fecha, ambas transiciones con `null`,
  fecha igual, ambos `null`, avisos pendientes y descartados, y validación fallida.
- `AssessmentReschedulingIntegrationTest`: persistencia conjunta manteniendo entidades
  managed y rollback de fecha/avisos incluso después de `flush`; usa exclusivamente
  el perfil `test` y requiere PostgreSQL de pruebas disponible.
- `AssessmentServiceLockingTest`: solo se añade el mock de la nueva dependencia;
  se mantienen sus comprobaciones de autorización, bloqueo y refresh.

No se ejecutó Maven ni las pruebas, conforme a la instrucción recibida.
