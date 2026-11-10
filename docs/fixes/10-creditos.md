# Hallazgo 10: rango de créditos compatible con el backend

## Decisión de compatibilidad

Se preserva el contrato existente del backend: créditos opcionales (`null`) o
enteros positivos entre 1 y 32767, compatibles con `Short`. El límite de 100
era exclusivo de la UI e impedía editar asignaturas con créditos válidos mayores
que 100. No se restringen ni transforman datos existentes; no se modifica el
backend ni se requieren migraciones.

## Cambios

- `frontend/src/utils/credits.ts` centraliza los límites y una validación pura.
- `CourseForm.tsx` usa esos límites en el campo numérico, conserva `step={1}` y
  convierte el campo vacío a `null`. También valida en `handleSubmit` para
  rechazar decimales, valores no finitos y números fuera del rango aunque se
  omita la validación HTML. Un valor inválido muestra un error y no se envía.
- `frontend/src/utils/credits.test.ts` cubre `null`, 1, 100, 101, 32767, 32768,
  cero, negativos, decimales y valores no finitos.

## Validación pendiente de integración

No se ejecutaron pruebas ni builds en esta entrega. Durante la integración,
ejecutar las pruebas Vitest de créditos y verificar en el formulario que se
puedan guardar créditos vacíos, 101 y 32767, así como editar una asignatura
existente con más de 100 créditos sin alterar su valor. Confirmar que 32768,
negativos y decimales se rechacen.
