# Nueva fase: verificación post-etapas + rediseño premium

Prompt entregado por Daniel el 2026-09-29 (verbatim). Se ejecuta DESPUÉS de que
terminen las etapas 10–14 del plan de evolución.

---

Quiero comenzar una nueva fase de trabajo sobre el proyecto Android "Ahora".

IMPORTANTE:

Las etapas anteriores ya fueron ejecutadas por ti.

ANTES DE HACER CUALQUIER CAMBIO EN ESTA NUEVA FASE, debes realizar obligatoriamente una FASE POST-ETAPA DE VERIFICACIÓN.

No empieces directamente con el rediseño.

==================================================
FASE 0 — POST-ETAPA OBLIGATORIA
VERIFICACIÓN DEL ESTADO REAL

Tu primera tarea NO es modificar código.

Tu primera tarea es inspeccionar nuevamente el estado REAL actual del proyecto después de las etapas anteriores.

No confíes únicamente en:

- README
- roadmap
- documentación
- comentarios
- commits
- lo que esperabas haber implementado
- lo que crees que quedó implementado

Comprueba directamente el código.

Debes revisar:

1. Arquitectura actual.
2. Dependencias actuales.
3. Estado real de Jetpack Compose.
4. Estado real de Material 3.
5. ViewModels.
6. StateFlow/Flow.
7. Room.
8. DataStore.
9. AlarmManager.
10. Notifications.
11. BroadcastReceivers.
12. SpeechRecognizer.
13. navegación.
14. pantallas existentes.
15. componentes reutilizables.
16. sistema de temas.
17. dark mode.
18. animaciones.
19. tests.
20. configuración de release.
21. manifest.
22. permisos.
23. backup.
24. rendimiento.
25. cambios realizados durante las etapas anteriores.

==================================================
FASE 0.1 — COMPROBAR QUÉ QUEDÓ REALMENTE IMPLEMENTADO

Crea internamente una comparación:

ANTES → DESPUÉS

Para cada etapa anterior identifica:

- qué se pretendía hacer
- qué se implementó realmente
- qué quedó parcialmente implementado
- qué no se implementó
- qué quedó técnicamente diferente a lo planeado
- qué problemas nuevos aparecieron
- qué riesgos quedaron pendientes

No inventes resultados.

Si algo no puede verificarse, indícalo explícitamente.

==================================================
FASE 0.2 — VALIDACIÓN TÉCNICA

Antes de continuar:

Ejecuta las comprobaciones apropiadas del proyecto.

Como mínimo, cuando estén disponibles:

- build
- tests
- lint
- comprobaciones de compilación de release cuando corresponda

Comprueba especialmente:

- errores de compilación
- warnings importantes
- tests fallando
- regresiones
- referencias rotas
- APIs obsoletas relevantes
- problemas de lifecycle
- problemas de coroutines
- problemas de persistencia
- problemas de notificaciones
- problemas de permisos

No continúes automáticamente si encuentras una regresión crítica.

Primero documenta el problema.

==================================================
FASE 0.3 — VALIDACIÓN FUNCIONAL

Comprueba que siguen funcionando correctamente:

- crear tarea
- editar tarea
- completar tarea
- deshacer
- eliminar
- persistencia
- navegación
- configuración
- modo claro
- modo oscuro
- voz
- recordatorios
- notificaciones
- cancelación de recordatorios
- comportamiento después de reiniciar
- permisos

Si alguna funcionalidad todavía no existe, no finjas que existe.

==================================================
FASE 0.4 — INFORME POST-ETAPA

ANTES de tocar el diseño, entrega un informe estructurado:

ESTADO ACTUAL

1. Qué funciona correctamente.
2. Qué fue implementado.
3. Qué quedó incompleto.
4. Qué problemas encontraste.
5. Qué regresiones encontraste.
6. Qué deuda técnica existe.
7. Qué elementos visuales actuales deben conservarse.
8. Qué elementos visuales necesitan rediseño.
9. Qué riesgos técnicos pueden afectar el rediseño.

Después de este informe puedes continuar.

NO necesito que me pidas permiso para continuar si todo está correcto.

Si encuentras problemas críticos, corrígelos primero cuando sea seguro hacerlo y vuelve a validar.

==================================================
FASE 1 — OBJETIVO DEL REDISEÑO

Ahora sí comienza la nueva fase.

Quiero transformar Ahora en una aplicación visualmente PREMIUM.

La aplicación actual está bien, pero quiero llevarla considerablemente más lejos.

El objetivo NO es simplemente hacerla "más bonita".

Quiero que se sienta como un producto profesional cuidadosamente diseñado.

Al abrir Ahora, el usuario debería percibir:

- simplicidad
- elegancia
- claridad
- tranquilidad
- velocidad
- precisión
- modernidad
- coherencia
- personalidad

La sofisticación debe venir de:

- proporciones
- espacio
- tipografía
- jerarquía
- consistencia
- movimiento
- interacción

No de añadir efectos.

==================================================
FASE 2 — REFERENCIA DE DISEÑO

Quiero que estudies los principios actuales de diseño de Apple, especialmente:

- Apple Human Interface Guidelines
- SwiftUI
- principios de layout
- typography
- color
- materials
- accessibility
- adaptive layouts
- interaction feedback
- hierarchy
- consistency

IMPORTANTE:

Ahora sigue siendo:

Kotlin
Jetpack Compose
Material 3
Android

NO quiero convertirla en una app iOS.

NO copies Apple.

NO copies SwiftUI.

NO copies interfaces de aplicaciones de Apple.

NO copies iconos propietarios.

NO copies layouts exactos.

NO copies branding.

Quiero aplicar los PRINCIPIOS de diseño al ecosistema Android.

La referencia es:

"Apple-level attention to detail"

NO:

"una copia de iOS".

==================================================
FASE 3 — AUDITORÍA VISUAL

Antes de modificar la interfaz, analiza todas las pantallas actuales.

Evalúa:

- jerarquía
- spacing
- typography
- colores
- iconos
- componentes
- botones
- navegación
- formularios
- empty states
- dark mode
- animaciones
- feedback
- accesibilidad
- adaptación a pantallas
- densidad visual

Identifica:

- elementos innecesarios
- inconsistencias
- componentes repetidos
- pantallas menos refinadas
- problemas de jerarquía
- oportunidades de simplificación

No hagas cambios arbitrarios.

Primero define un lenguaje visual coherente.

==================================================
FASE 4 — DESIGN SYSTEM DE AHORA

Crea un sistema visual propio.

Define y centraliza cuando corresponda:

TYPOGRAPHY

- títulos
- subtítulos
- cuerpo
- labels
- captions
- botones
- pesos
- tamaños
- alturas de línea

SPACING

- escala de espaciado
- padding
- margins
- separación entre elementos
- separación entre secciones

SHAPES

- radios
- botones
- campos
- superficies
- diálogos

COLORS

- background
- surface
- elevated surface
- primary
- secondary
- text
- secondary text
- error
- success
- priority states

ICONOGRAPHY

- estilo
- tamaño
- alineación
- consistencia

COMPONENTS

- botones
- inputs
- task rows
- checkboxes
- chips
- dialogs
- sheets
- navigation
- empty states
- feedback

Evita valores visuales duplicados por toda la aplicación.

==================================================
FASE 5 — HOME

La pantalla principal debe ser el corazón de Ahora.

Debe ser extremadamente limpia.

El usuario debe entender inmediatamente:

- qué tiene pendiente
- qué necesita hacer
- cómo crear una tarea

Reduce el ruido visual.

Utiliza:

- espacio respirable
- excelente jerarquía
- alineación precisa
- tipografía refinada
- elementos mínimos
- acción principal clara

No conviertas todo en tarjetas.

==================================================
FASE 6 — CREACIÓN DE TAREAS

Esta experiencia debe ser excepcional.

Objetivo:

ABRIR
→ ESCRIBIR/HABLAR
→ GUARDAR

Debe sentirse inmediata.

Las opciones adicionales:

- prioridad
- fecha
- recordatorio
- recurrencia
- etc.

deben estar disponibles sin convertir la creación rápida en un formulario pesado.

Separar claramente:

CREACIÓN RÁPIDA

de

CONFIGURACIÓN AVANZADA.

==================================================
FASE 7 — TAREAS

Rediseña las filas de tareas.

Deben ser:

- limpias
- fáciles de escanear
- táctiles
- elegantes
- accesibles

El checkbox debe sentirse excelente.

Al completar:

- feedback inmediato
- animación corta
- transición suave
- haptic sutil cuando corresponda

Nunca exagerar.

==================================================
FASE 8 — MICROINTERACCIONES

Mejora:

- completar
- crear
- eliminar
- deshacer
- abrir
- editar
- cambiar prioridad
- activar recordatorio
- navegación
- cambio de tema

Las animaciones deben ser:

- rápidas
- naturales
- discretas
- consistentes

Evita:

- rebotes exagerados
- animaciones largas
- movimiento constante
- efectos innecesarios
- efectos costosos

==================================================
FASE 9 — HAPTICS

Utiliza feedback háptico únicamente cuando aporte valor.

Debe sentirse como una confirmación.

Nunca debe ser molesto.

Respeta las preferencias del sistema.

==================================================
FASE 10 — DARK MODE

Diseña dark mode como una experiencia de primera clase.

No simplemente inviertas colores.

Utiliza:

- jerarquía de superficies
- contraste correcto
- profundidad sutil
- colores de acento controlados
- excelente legibilidad

Comprueba cada pantalla.

==================================================
FASE 11 — EMPTY STATES

Crea estados vacíos elegantes.

Deben:

- explicar el estado
- orientar al usuario
- mantener el minimalismo

No agregues ilustraciones gigantes ni decoración innecesaria.

==================================================
FASE 12 — SETTINGS

Rediseña Settings.

Debe ser:

- limpio
- organizado
- comprensible
- consistente

Agrupa las opciones correctamente.

Evita una lista interminable.

==================================================
FASE 13 — NAVEGACIÓN

Revisa:

- navegación
- back behavior
- transiciones
- títulos
- selección
- estados

Debe sentirse natural.

==================================================
FASE 14 — ACCESIBILIDAD

Comprueba:

- contraste
- touch targets
- TalkBack
- content descriptions
- text scaling
- navegación
- estados independientes del color
- textos largos

No sacrifiques accesibilidad por estética.

==================================================
FASE 15 — ADAPTABILIDAD

Debe funcionar correctamente en:

- teléfonos pequeños
- teléfonos grandes
- diferentes densidades
- orientación
- tamaños de texto diferentes

Utiliza las herramientas apropiadas de Jetpack Compose para layouts adaptativos.

==================================================
FASE 16 — RENDIMIENTO

El nuevo diseño NO puede empeorar el rendimiento.

Prioriza:

- pocas recomposiciones innecesarias
- listas eficientes
- animaciones eficientes
- evitar objetos innecesarios en composables
- evitar efectos costosos
- evitar overdraw
- evitar sombras excesivas
- recursos optimizados

Usa como referencia un dispositivo de gama baja/media como:

Samsung Galaxy A14 4 GB RAM.

Quiero:

PREMIUM VISUALS

+ 

LOW COMPUTATIONAL COST

==================================================
FASE 17 — MATERIAL 3

Mantén Jetpack Compose + Material 3.

Utiliza Material 3 cuando sea apropiado.

Crea componentes propios únicamente cuando exista una razón clara.

No introduzcas una librería visual enorme para conseguir efectos.

No añadas dependencias innecesarias.

==================================================
FASE 18 — IDENTIDAD DE AHORA

Ahora debe tener personalidad propia.

Concepto:

"Ahora"
"Sácalo de tu cabeza."

La interfaz debe comunicar:

- claridad mental
- rapidez
- acción
- tranquilidad
- simplicidad

No quiero una plantilla genérica de Material 3.

Tampoco quiero una copia de iOS.

Quiero que alguien pueda reconocer que está utilizando Ahora.

==================================================
FASE 19 — REGLA DE SIMPLICIDAD

PREMIUM ≠ MÁS ELEMENTOS.

PREMIUM = MEJORES DECISIONES.

Evita:

- gradientes innecesarios
- glassmorphism excesivo
- sombras fuertes
- tarjetas para todo
- decoración innecesaria
- colores excesivos
- animaciones permanentes

Si un elemento puede eliminarse sin perder funcionalidad o claridad, considera eliminarlo.

==================================================
FASE 20 — IMPLEMENTACIÓN PROGRESIVA

No rediseñes todo de golpe.

Trabaja en este orden:

A. Design system
B. Home
C. Creación de tareas
D. Task rows/details
E. Navegación
F. Settings
G. Dark mode
H. Microinteracciones
I. Accessibility
J. Adaptive layouts
K. Performance optimization

Después de cada bloque:

1. Compila.
2. Ejecuta tests.
3. Comprueba regresiones.
4. Comprueba funcionamiento.
5. Revisa visualmente.
6. Corrige problemas antes de continuar.

==================================================
FASE 21 — NO ROMPER FUNCIONALIDAD

El rediseño NO debe romper:

- crear
- editar
- completar
- deshacer
- eliminar
- Room
- DataStore
- navegación
- voz
- recordatorios
- notificaciones
- permisos
- AlarmManager
- BootReceiver
- dark mode
- persistencia

No modifiques arquitectura sin necesidad.

==================================================
FASE 22 — NO REESCRIBIR POR REESCRIBIR

No hagas una reescritura completa.

Conserva lo que funciona.

Refactoriza solamente si:

- mejora mantenimiento
- reduce bugs
- mejora rendimiento
- simplifica código
- es necesario para el nuevo diseño

==================================================
FASE 23 — DOCUMENTACIÓN

Crea o actualiza:

DESIGN_SYSTEM.md

Debe documentar:

- principios
- typography
- spacing
- colors
- shapes
- components
- animations
- accessibility
- dark mode
- adaptive behavior

No inventes métricas.

==================================================
FASE 24 — POST-REDISEÑO
AUDITORÍA OBLIGATORIA

Cuando hayas terminado TODO el rediseño, NO declares la fase terminada inmediatamente.

Detente y vuelve a inspeccionar el proyecto completo.

Comprueba nuevamente:

VISUAL

- consistencia
- jerarquía
- spacing
- typography
- colores
- iconografía
- dark mode

UX

- creación
- edición
- completado
- eliminación
- undo
- navegación
- recordatorios
- voz

TECHNICAL

- build
- tests
- lint
- recompositions
- performance
- memory
- lifecycle
- coroutines

ACCESSIBILITY

- TalkBack
- contraste
- touch targets
- font scaling
- content descriptions

ADAPTABILITY

- tamaños de pantalla
- orientación
- texto grande

==================================================
FASE 25 — CORRECCIÓN POST-REDISEÑO

Después de la auditoría:

NO hagas una lista genérica de "posibles mejoras".

Clasifica los problemas encontrados como:

CRÍTICO
IMPORTANTE
MENOR
OPCIONAL

Después corrige primero:

1. Críticos.
2. Importantes.
3. Menores que tengan impacto real.

No implementes los opcionales automáticamente si aumentan complejidad sin beneficio claro.

Después vuelve a ejecutar las comprobaciones.

==================================================
FASE 26 — INFORME FINAL

Al terminar, entrega un informe completo:

1. Estado inicial encontrado.
2. Problemas visuales encontrados.
3. Problemas técnicos encontrados.
4. Design system creado/modificado.
5. Pantallas rediseñadas.
6. Cambios de Home.
7. Cambios de creación de tareas.
8. Cambios en tareas.
9. Cambios en navegación.
10. Cambios en Settings.
11. Cambios en dark mode.
12. Microinteracciones.
13. Haptics.
14. Accesibilidad.
15. Adaptabilidad.
16. Rendimiento.
17. Tests ejecutados.
18. Build/lint realizados.
19. Problemas encontrados en la auditoría post-rediseño.
20. Problemas corregidos.
21. Problemas pendientes.
22. Archivos modificados.
23. Dependencias añadidas o modificadas.
24. Recomendación para la siguiente fase.

No digas simplemente:

"Rediseño completado."

Quiero saber exactamente qué hiciste y qué quedó pendiente.

==================================================
REGLA FINAL

El proceso obligatorio es:

VERIFICAR
↓
AUDITAR
↓
INFORMAR
↓
CORREGIR PROBLEMAS PREVIOS
↓
DISEÑAR
↓
IMPLEMENTAR PROGRESIVAMENTE
↓
VALIDAR CADA BLOQUE
↓
AUDITAR NUEVAMENTE
↓
CORREGIR
↓
VALIDAR FINALMENTE
↓
INFORMAR

No saltes directamente de una etapa a otra.

No asumas.

No inventes.

No sobre-diseñes.

No copies iOS.

No sacrifiques rendimiento por estética.

No sacrifiques estabilidad por diseño.

Quiero que Ahora termine esta fase sintiéndose como un producto premium, pero conservando su identidad Android, su simplicidad y su filosofía:

"Sácalo de tu cabeza."

COMIENZA ÚNICAMENTE POR LA FASE 0: POST-ETAPA DE VERIFICACIÓN.

No modifiques el diseño hasta haber terminado esa verificación.
