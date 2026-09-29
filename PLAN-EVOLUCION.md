# Plan maestro de evolución — Ahora

Adoptado por Daniel el 2026-09-28 (prompt de ChatGPT, guardado verbatim).
Es el plan director para evolucionar la app progresivamente.
La mejora diaria (`mejora-diaria-app-ahora`) sigue estas etapas.

---

Actúa como desarrollador Android senior, arquitecto de software, especialista en rendimiento móvil y diseñador UX/UI de aplicaciones premium.

Estás trabajando sobre MI PROYECTO ANDROID EXISTENTE:

Repositorio:
https://github.com/danielcolumbie/ahora

Aplicación:
Ahora

IMPORTANTE:

NO reconstruyas la aplicación desde cero.

NO reemplaces la arquitectura actual simplemente porque exista otra arquitectura más popular.

NO elimines funcionalidades que ya funcionan.

Quiero evolucionar progresivamente el proyecto existente.

Antes de modificar cualquier cosa, inspecciona el código REAL del repositorio actual.

==================================================
0. CONTEXTO REAL DEL PROYECTO ACTUAL

El proyecto actual utiliza:

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore Preferences
- StateFlow/Flow
- ViewModel
- Navigation Compose
- KSP
- inyección manual mediante AppContainer
- AlarmManager
- BroadcastReceiver
- SpeechRecognizer

La arquitectura actual está organizada aproximadamente como:

ui
→ domain
→ data

con módulos separados para:

notifications
speech
di

Actualmente la aplicación utiliza:

- una Activity principal;
- Room para tareas;
- DataStore para ajustes;
- AlarmManager para recordatorios;
- BroadcastReceiver para mostrar recordatorios;
- BootReceiver para reprogramarlos después del reinicio;
- SpeechRecognizer para voz a texto.

La base de datos Room actualmente está en versión 1.

NO asumas que ya existen migraciones.

NO asumas que ya existen tests completos.

NO asumas que todas las optimizaciones mencionadas en documentación ya están implementadas.

Comprueba el código real antes de actuar.

==================================================

1. OBJETIVO GENERAL
   ==================================================

Convertir Ahora progresivamente en una aplicación de productividad:

- rápida;
- ligera;
- privada;
- fiable;
- moderna;
- minimalista;
- visualmente premium;
- fácil de utilizar;
- eficiente en batería;
- eficiente en RAM;
- optimizada para dispositivos modestos;
- preparada para crecer durante años.

La filosofía central debe permanecer:

RÁPIDA
SIMPLE
PRIVADA
FIABLE
LIGERA

La aplicación no debe convertirse en una suite gigantesca de productividad.

Cada nueva función debe justificar su existencia.

==================================================
2. REGLA FUNDAMENTAL: AUDITAR ANTES DE CAMBIAR

Antes de modificar código:

1. inspecciona el proyecto;
2. entiende la arquitectura;
3. identifica responsabilidades;
4. identifica duplicación;
5. identifica código muerto;
6. identifica problemas;
7. identifica posibles regresiones;
8. identifica oportunidades de optimización.

Analiza:

- Gradle;
- dependencias;
- Compose;
- Room;
- DataStore;
- ViewModels;
- StateFlow;
- navegación;
- ciclo de vida;
- coroutines;
- notificaciones;
- AlarmManager;
- BroadcastReceiver;
- BootReceiver;
- SpeechRecognizer;
- permisos;
- backup;
- memoria;
- CPU;
- batería;
- operaciones en Main Thread;
- recomposiciones.

Clasifica los hallazgos:

CRÍTICO
ALTO
MEDIO
BAJO

NO cambies una parte simplemente porque podría hacerse de otra forma.

Primero determina si realmente existe un problema.

==================================================
3. NO REESCRIBIR SIN NECESIDAD

Mantén la arquitectura actual si sigue siendo adecuada.

No introduzcas:

- Hilt;
- Dagger;
- Firebase;
- servicios externos;
- librerías de terceros;
- frameworks adicionales;

simplemente porque sean populares.

Una dependencia nueva debe justificar claramente:

- funcionalidad;
- rendimiento;
- mantenimiento;
- seguridad;

y su coste debe ser evaluado.

Para una aplicación como Ahora, la simplicidad arquitectónica es una ventaja.

==================================================
4. ESTABILIDAD ANTES DE NUEVAS FUNCIONES

Antes de añadir funciones importantes, comprueba:

- crear tarea;
- editar tarea;
- completar;
- descompletar;
- eliminar;
- deshacer;
- persistencia;
- recordatorios;
- notificaciones;
- voz;
- ajustes;
- navegación.

Una nueva función nunca debe romper una función existente.

==================================================
5. TESTS

Actualmente el proyecto tiene JUnit configurado, pero NO asumas que existe una cobertura amplia.

Añade tests progresivamente.

Prioridad:

TaskRepository:

- crear;
- modificar;
- completar;
- descompletar;
- eliminar;
- establecer recordatorio;
- cancelar recordatorio.

TaskDao:

- insertar;
- actualizar;
- eliminar;
- obtener tareas;
- obtener pendientes;
- obtener recordatorios.

Room:

- preservar datos;
- comprobar futuras migraciones;
- evitar pérdida de información.

ReminderScheduler:

- programar;
- cancelar;
- reprogramar;
- múltiples tareas;
- IDs diferentes;
- alarmas futuras;
- comportamiento sin permiso de alarma exacta.

NotificationHelper:

- evitar duplicados;
- respetar configuración;
- comprobar canales;
- comprobar permisos.

No persigas un porcentaje de cobertura artificial.

Prioriza las partes críticas.

==================================================
6. ROOM Y MIGRACIONES

La base de datos actual está en:

version = 1

y no tiene todavía una estrategia de migraciones implementada.

Antes de ampliar significativamente el modelo:

establece una estrategia de migraciones segura.

Cuando se añadan campos:

version 1
→ version 2
→ version 3

etc.

Nunca permitas que una actualización destruya tareas existentes.

Si es posible, incorpora tests de migración.

==================================================
7. RECORDATORIOS

El proyecto actual utiliza AlarmManager y distingue entre alarmas exactas y no exactas según disponibilidad del sistema.

Mantén esta arquitectura salvo que exista una razón técnica real para cambiarla.

Comprueba:

- Android 12+;
- permiso de alarmas exactas;
- fallback;
- reinicio;
- cancelación;
- reprogramación;
- actualización de una tarea;
- eliminación de una tarea;
- múltiples recordatorios.

Una tarea eliminada NO debe dejar una alarma pendiente.

Una tarea cuya hora cambia debe cancelar la alarma anterior y crear la nueva.

Después de reiniciar el dispositivo solamente deben restaurarse los recordatorios realmente pendientes.

No hagas polling.

No utilices servicios permanentes para esto.

==================================================
8. NOTIFICACIONES — AUDITORÍA REAL

El código actual utiliza un canal llamado:

ahora_recordatorios_v2

y ya contempla que Android controla determinadas propiedades del canal.

NO asumas que está roto.

NO asumas que funciona perfectamente.

AUDÍTALO.

Comprueba específicamente:

- creación del canal;
- configuración inicial;
- sonido;
- vibración;
- importancia;
- cambios de configuración;
- comportamiento después de que el canal ya exista;
- actualización de la aplicación;
- migración desde el canal anterior;
- Android 8+;
- Android 13+.

IMPORTANTE:

Android puede impedir que determinadas propiedades de un NotificationChannel existente se modifiquen como espera la aplicación.

Por eso:

si los switches internos de Ahora prometen controlar sonido/vibración, comprueba que realmente lo hagan.

Si Android no permite controlar una propiedad después de crear el canal:

no engañes al usuario.

Rediseña la experiencia para que la UI refleje la realidad del sistema.

Si es necesario, proporciona acceso a los ajustes de notificaciones de Android.

==================================================
9. PRIVACIDAD Y BACKUP

La aplicación actualmente no utiliza servidor propio ni Firebase para las tareas.

Mantén esa filosofía.

NO introducir:

- publicidad;
- tracking;
- analytics;
- cuentas obligatorias;
- servidores;
- recopilación innecesaria.

PERO:

el proyecto actual utiliza:

android:allowBackup="true"

y tiene reglas de backup/data extraction.

Por tanto, NO afirmes automáticamente que:

"todos los datos permanecen exclusivamente en el dispositivo".

Primero audita exactamente qué datos pueden incluirse en backup.

Decide conscientemente entre:

A) privacidad estrictamente local;

o

B) almacenamiento local + posibilidad de backup de Android.

La documentación debe describir exactamente el comportamiento real.

==================================================
10. RECONOCIMIENTO DE VOZ

La voz debe continuar funcionando como:

voz
→ reconocimiento
→ revisión
→ guardar.

Nunca guardar automáticamente una interpretación ambigua.

SpeechRecognizer debe:

- iniciar solamente cuando se utiliza;
- liberar recursos correctamente;
- manejar errores;
- respetar permisos;
- no permanecer activo;
- no crear memory leaks.

Documenta correctamente que el funcionamiento del reconocimiento puede depender del servicio de voz disponible en el dispositivo.

La app puede seguir siendo funcional offline para tareas y recordatorios aunque el reconocimiento de voz pueda depender del servicio disponible.

==================================================
11. OPTIMIZACIÓN PROGRESIVA

Esta es una prioridad PERMANENTE.

No quiero una única fase llamada "optimización".

Quiero que cada versión de Ahora incluya una revisión progresiva del rendimiento.

Analiza:

CPU
RAM
BATERÍA
STARTUP
I/O
RECOMPOSICIONES
ROOM
COROUTINES
TRABAJO EN SEGUNDO PLANO
TAMAÑO DEL APK/AAB

Antes de aceptar una optimización:

determina primero si existe realmente un problema.

NO hagas micro-optimizaciones inútiles.

==================================================
12. COMPOSE

Audita:

- recomposiciones;
- estado;
- estabilidad de parámetros;
- objetos creados dentro de composables;
- cálculos repetidos;
- listas;
- efectos;
- animaciones.

Evita recomposiciones innecesarias.

Utiliza LazyColumn correctamente.

No elimines animaciones simplemente para mejorar rendimiento.

Optimízalas.

Prioridad:

FLUIDEZ > ESPECTÁCULO.

==================================================
13. ROOM

Evita:

- consultas repetidas;
- consultas innecesariamente grandes;
- escrituras duplicadas;
- observar información que no se necesita;
- cargar datos innecesarios.

Utiliza Flow/StateFlow de manera eficiente.

No hagas consultas a Room desde Main Thread cuando impliquen trabajo relevante.

==================================================
14. COROUTINES Y CICLO DE VIDA

Audita:

- Dispatchers;
- scopes;
- cancellation;
- lifecycle;
- concurrencia.

No dejes trabajos ejecutándose después de que ya no sean necesarios.

Evita trabajo pesado en Main Thread.

==================================================
15. MEMORIA

Busca:

- memory leaks;
- listeners sin liberar;
- referencias persistentes;
- SpeechRecognizer;
- BroadcastReceivers;
- colecciones innecesarias;
- objetos grandes;
- recursos duplicados.

La aplicación debe mantenerse ligera incluso después de utilizarla durante mucho tiempo.

==================================================
16. BATERÍA

Cuando el usuario no está utilizando Ahora, la aplicación debe realizar prácticamente ningún trabajo continuo.

Evita:

- polling;
- timers permanentes;
- servicios permanentes;
- wakelocks;
- consultas periódicas innecesarias.

Los recordatorios deben delegarse al sistema Android.

==================================================
17. OPTIMIZACIÓN PARA GAMA BAJA

Utiliza como referencia dispositivos como:

Samsung Galaxy A14
4 GB RAM
hardware económico/media.

La aplicación debe:

- iniciar rápidamente;
- mantener scroll fluido;
- utilizar poca RAM;
- evitar congelamientos;
- minimizar trabajo en background;
- utilizar poca batería.

No diseñes únicamente para teléfonos flagship.

==================================================
18. DISEÑO VISUAL

Ahora debe evolucionar hacia una estética:

MINIMALISTA
MODERNA
PREMIUM
TRANQUILA
LIMPIA

Quiero inspiración en los principios de diseño de iOS y aplicaciones modernas de productividad.

NO quiero copiar iOS.

NO copiar:

- iconos propietarios;
- componentes específicos;
- diseños reconocibles;
- elementos que hagan parecer que Ahora es un clon de Apple.

La fórmula debe ser:

FILOSOFÍA DE DISEÑO MODERNA
+
IMPLEMENTACIÓN ANDROID
+
IDENTIDAD PROPIA DE AHORA.

==================================================
19. OBJETIVO DE EXPERIENCIA

Cuando alguien abra Ahora debería sentir:

"Esto es muy limpio."

Después:

"Es muy fácil de usar."

Y finalmente:

"Está sorprendentemente pulido."

La sofisticación debe provenir de:

- proporción;
- tipografía;
- espacio;
- jerarquía;
- consistencia;
- microinteracciones.

No de añadir decoración.

==================================================
20. JERARQUÍA VISUAL

El usuario debe identificar inmediatamente:

1. dónde está;
2. qué tareas tiene;
3. qué está pendiente;
4. cómo crear una tarea.

No llenar la interfaz de botones.

La acción principal debe destacar.

==================================================
21. ESPACIADO

Utiliza un sistema consistente de spacing.

Evita:

- elementos pegados;
- padding arbitrario;
- márgenes inconsistentes;
- espacios desperdiciados.

La interfaz debe respirar.

==================================================
22. TIPOGRAFÍA

Crear una jerarquía clara entre:

- títulos;
- tareas;
- información secundaria;
- fechas;
- recordatorios;
- acciones.

Utilizar pocos tamaños y pesos.

No sacrificar legibilidad por estética.

==================================================
23. COLOR

Reducir la cantidad de colores.

Principalmente:

- fondo;
- superficies;
- texto principal;
- texto secundario;
- acento;
- estados.

Utilizar el color de acento con moderación.

El contenido debe ser protagonista.

==================================================
24. SUPERFICIES

No convertir todo en una tarjeta.

Utilizar superficies únicamente cuando aporten jerarquía.

Evitar:

- exceso de cards;
- sombras fuertes;
- neumorfismo;
- gradientes excesivos.

==================================================
25. ICONOS

Utilizar iconografía simple y coherente.

Evitar iconos puramente decorativos.

Los iconos deben ayudar a comprender la acción.

==================================================
26. CREACIÓN DE TAREAS

Debe seguir siendo extremadamente rápida:

abrir
→ escribir/hablar
→ opcionalmente recordar
→ guardar.

No añadir pasos innecesarios.

Las opciones avanzadas deben aparecer progresivamente.

==================================================
27. CHECKBOX Y MICROINTERACCIONES

El completado debe tener:

- respuesta inmediata;
- animación breve;
- feedback visual;
- opcionalmente feedback háptico sutil.

No retrasar la acción.

==================================================
28. ANIMACIONES

Animaciones:

- rápidas;
- suaves;
- naturales;
- discretas.

Evitar:

- rebotes exagerados;
- transiciones largas;
- efectos innecesarios.

Cada animación debe comunicar:

ACCIÓN → RESPUESTA.

==================================================
29. MODO OSCURO

El modo oscuro debe diseñarse correctamente.

Revisar:

- fondo;
- superficies;
- texto;
- iconos;
- divisores;
- acentos;
- estados.

Mantener contraste suficiente.

==================================================
30. ESTADOS VACÍOS

Crear estados vacíos elegantes y minimalistas.

No utilizar ilustraciones gigantes.

El usuario debe entender:

"Todo está organizado."

y saber cómo crear una tarea.

==================================================
31. ACCESIBILIDAD

El minimalismo no puede perjudicar accesibilidad.

Comprobar:

- contraste;
- tamaños;
- áreas táctiles;
- TalkBack;
- descripciones;
- estados accesibles;
- tamaños de fuente.

==================================================
32. ADAPTABILIDAD

El diseño debe funcionar correctamente en:

- teléfonos pequeños;
- teléfonos grandes;
- diferentes densidades;
- diferentes tamaños de fuente;
- modo claro;
- modo oscuro.

==================================================
33. BÚSQUEDA

Después de estabilizar la base:

añadir búsqueda rápida.

Debe funcionar eficientemente con muchas tareas.

==================================================
34. PRIORIDADES

Añadir progresivamente:

- baja;
- normal;
- alta.

Mantenerlo visualmente sencillo.

==================================================
35. FECHAS

Diferenciar:

FECHA LÍMITE

de

RECORDATORIO.

No tratarlos como la misma cosa.

==================================================
36. RECURRENCIA

Añadir progresivamente:

- diaria;
- semanal;
- mensual;
- personalizada cuando tenga sentido.

Integrar correctamente:

Room
+
AlarmManager
+
notificaciones.

Evitar alarmas duplicadas.

==================================================
37. WIDGET

Añadir posteriormente un widget Android ligero.

Debe permitir:

- consultar tareas;
- completar cuando corresponda;
- crear una tarea rápidamente.

No debe generar trabajo innecesario.

==================================================
38. LENGUAJE NATURAL

Evolucionar posteriormente la captura de tareas.

Ejemplo:

"Comprar detergente mañana a las 8"

puede interpretarse como:

Título:
Comprar detergente

Fecha:
mañana

Hora:
08:00

Pero:

NO inventar datos.

Si no hay hora:

no inventarla.

Si hay ambigüedad:

pedir confirmación.

==================================================
39. DOCUMENTACIÓN DE RENDIMIENTO

Crear:

PERFORMANCE.md

Registrar:

- problemas encontrados;
- decisiones;
- soluciones;
- métricas reales cuando existan;
- regresiones;
- futuras optimizaciones.

NO inventar métricas.

Si algo no puede medirse:

decirlo.

==================================================
40. DEPENDENCIAS Y ACTUALIZACIONES

El proyecto actual utiliza versiones concretas de:

- AGP;
- Kotlin;
- KSP;
- AndroidX;
- Room;
- DataStore;
- Compose.

NO actualices todas las dependencias automáticamente.

Primero evalúa:

- compatibilidad;
- estabilidad;
- cambios de API;
- impacto;
- beneficio real.

Si se actualizan dependencias:

hazlo progresivamente.

No mezcles una actualización masiva de dependencias con un rediseño completo salvo que sea estrictamente necesario.

==================================================
41. TARGET Y COMPATIBILIDAD ANDROID

El proyecto actual declara:

compileSdk 34
targetSdk 34
minSdk 26

NO cambies estos valores simplemente porque exista una versión más nueva.

Antes de actualizar compileSdk/targetSdk:

comprueba requisitos actuales de Android y compatibilidad del proyecto.

La aplicación debe seguir siendo compatible con su minSdk salvo que exista una razón fuerte para aumentarlo.

==================================================
42. COMPATIBILIDAD

Prestar especial atención a:

Android 12
Android 13
Android 14
Android 15

Comprobar:

- permisos;
- notificaciones;
- alarmas;
- batería;
- backup;
- voz;
- background restrictions.

No asumir que todas las versiones funcionan igual.

==================================================
43. CALIDAD DEL CÓDIGO

Mantener:

- nombres claros;
- funciones pequeñas;
- responsabilidades separadas;
- cero código muerto;
- cero imports innecesarios;
- cero duplicación innecesaria.

Comentarios únicamente cuando aporten contexto.

No sobreingenierizar.

==================================================
44. GIT

Mantener commits claros.

Ejemplos:

feat: add task search

fix: prevent duplicate reminders

perf: reduce unnecessary recompositions

test: add repository tests

refactor: simplify reminder scheduling

Evitar mezclar sin necesidad:

feature
+
refactor masivo
+
actualización de dependencias.

==================================================
45. PREVENCIÓN DE REGRESIONES

Antes y después de cada modificación importante comprobar:

- crear;
- editar;
- completar;
- eliminar;
- deshacer;
- recordar;
- notificar;
- voz;
- persistencia;
- ajustes;
- navegación.

Una nueva función no debe empeorar una función existente.

==================================================
46. RELEASES

Antes de cada release:

BUILD

- compila correctamente;
- release build;
- no depender de debug signing;
- no incluir secretos.

DATABASE

- migraciones;
- preservación de datos.

NOTIFICATIONS

- canales;
- permisos;
- duplicados.

REMINDERS

- reinicio;
- actualización;
- cancelación.

UI

- ausencia de crashes;
- navegación;
- estados vacíos.

VOICE

- permisos;
- errores;
- liberación.

PRIVACY

- sin tracking;
- sin analytics;
- documentación correcta.

PERFORMANCE

- sin trabajo pesado en Main Thread;
- sin polling;
- sin servicios innecesarios;
- sin leaks evidentes.

==================================================
47. ORDEN DE EJECUCIÓN

NO implementes todo de una vez.

Trabaja en etapas.

ETAPA 1
Auditoría completa del proyecto actual.

ETAPA 2
Correcciones críticas y estabilidad.

ETAPA 3
Tests fundamentales.

ETAPA 4
Auditoría y corrección de recordatorios/notificaciones.

ETAPA 5
Auditoría de privacidad y backup.

ETAPA 6
Optimización Compose/UI.

ETAPA 7
Optimización Room/coroutines/memoria/batería.

ETAPA 8
Rediseño visual minimalista premium.

ETAPA 9
Búsqueda.

ETAPA 10
Prioridades y fechas.

ETAPA 11
Recurrencia.

ETAPA 12
Widget.

ETAPA 13
Lenguaje natural.

ETAPA 14
Optimización y estabilización final.

==================================================
48. REGLA CONTRA LA SOBREINGENIERÍA

Antes de añadir:

- librería;
- framework;
- servicio;
- abstracción;
- dependencia;

pregunta:

¿Realmente hace falta?

Si la funcionalidad puede resolverse correctamente con las herramientas actuales:

prefiere la solución existente.

==================================================
49. FORMA DE TRABAJO

Después de cada etapa informa:

1. qué analizaste;
2. qué encontraste;
3. qué cambiaste;
4. por qué;
5. impacto esperado;
6. riesgos;
7. pruebas necesarias;
8. siguiente etapa.

NO declares una etapa terminada simplemente porque compile.

Compilar es solamente el primer requisito.

La aplicación debe seguir funcionando correctamente.

==================================================
50. REGLA FINAL DE PRODUCTO

Quiero que cada versión de Ahora sea mejor que la anterior en:

ESTABILIDAD
+
RENDIMIENTO
+
EFICIENCIA
+
DISEÑO
+
USABILIDAD
+
PRIVACIDAD.

No sacrifiques:

simplicidad por funciones;

rendimiento por estética;

privacidad por conveniencia;

estabilidad por velocidad de desarrollo.

El diseño debe evolucionar hacia una estética minimalista, moderna y premium, inspirada en principios de iOS pero implementada correctamente para Android y con identidad propia.

El rendimiento debe mejorar progresivamente.

La arquitectura debe mantenerse sencilla.

La aplicación debe seguir sintiéndose como:

AHORA.

No como una aplicación diferente.

==================================================
OBJETIVO FINAL

No busques solamente:

"que funcione".

Busca:

"que funcione muy bien".

Después:

"que funcione muy bien utilizando la menor cantidad razonable de recursos".

Y finalmente:

"que además se sienta excelente al utilizarla".

Quiero que Ahora pueda evolucionar durante años sin perder:

su velocidad;
su simplicidad;
su privacidad;
su identidad;
su fiabilidad.

Empieza SIEMPRE por inspeccionar el estado REAL actual del repositorio.

No asumas que algo está implementado solo porque aparece en README, roadmap o documentación.

Comprueba el código.

Después comienza por la ETAPA 1.
