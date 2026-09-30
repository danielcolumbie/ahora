# Samsung Galaxy Store — ficha lista

> ✅ **AUTORIZADO POR DANIEL (2026-09-29, «Ya súbelo a la tiendas correspondientes»).** Datos de la ficha listos para la 1.27.1. Pendiente solo lo que le toca a Daniel desde su teléfono: email de soporte, screenshots en su A14 y crear su cuenta de vendedor en Samsung.

## Datos de la ficha (listos para pegar)

**Título:** Ahora

**Categoría:** Productivity

**Email de soporte:** [LO PONE DANIEL]

**URL de política de privacidad:** https://github.com/danielcolumbie/ahora/blob/master/PRIVACY.md

### Descripción corta

- **ES:** Sácalo de tu cabeza. Tus tareas, claras y sin ruido.
- **EN:** Get it out of your head. Your tasks, clear and noise-free.

### Descripción larga

**ES:**
```
Ahora es una app de tareas minimalista que te ayuda a sacar las cosas de tu cabeza y ponerlas en claro.

• Crea, edita, completa y elimina tareas en segundos
• Vista "Hoy" con lo que toca ahora y vista "Todas" con el resto
• Recordatorios y notificaciones locales
• Entrada por voz: dicta tus tareas sin escribir
• Deshacer al eliminar, por si te arrepientes
• Temas claro, oscuro y automático
• Funciona 100% sin conexión: tus datos nunca salen de tu teléfono

Sin cuentas, sin publicidad, sin rastreo. Solo tus tareas.
```

**EN:**
```
Ahora is a minimalist to-do app that helps you get things out of your head and into the clear.

• Create, edit, complete and delete tasks in seconds
• "Today" view with what's due now and "All" view with the rest
• Local reminders and notifications
• Voice input: dictate your tasks instead of typing
• Undo on delete, in case you change your mind
• Light, dark and automatic themes
• Works 100% offline: your data never leaves your phone

No accounts, no ads, no tracking. Just your tasks.
```

## Cuestionario de datos/privacidad (respuestas listas)

La app **no recolecta ningún dato**. Respuestas para el formulario de Samsung:

- **¿La app recolecta datos personales?** No.
- **¿Comparte datos con terceros?** No.
- **¿Tiene publicidad o analítica?** No.
- **Permisos y por qué:**
  - Micrófono → solo al dictar una tarea por voz (el audio lo procesa el servicio de voz del propio teléfono).
  - Notificaciones → mostrar recordatorios.
  - Alarmas exactas → que los recordatorios suenen a la hora elegida.
- **¿La app usa internet?** No — ni siquiera declara el permiso en el manifiesto.

## Assets

- **Icono 512×512 PNG (<1 MB):** listo en `docs/distribucion/assets/icono-512.png` (2,6 KB; punto azul sobre negro, el icono real de la app). Es el archivo que se sube en el portal.
- **APK firmado:** listo — 1.27.1 en https://github.com/danielcolumbie/ahora/releases (archivo `ahora-1.27.1-sdk36.apk`, ~1,7 MB, firmado con la llave oficial). Se actualiza sin borrar tareas.
- **Screenshots (2–3):** ⏳ los toma Daniel en su A14. Especificaciones de Samsung: PNG o JPEG, mínimo 320 px, máximo 3840 px. Recomendado: 1080×2400 (vertical). Contenido sugerido:
  1. Vista «Hoy» con varias tareas.
  2. Diálogo de crear tarea.
  3. La app en tema oscuro.

## Guía para Daniel (paso a paso, desde su teléfono o PC)

1. Entra a **seller.samsungapps.com** y crea tu cuenta de vendedor (tipo **Individual** — es gratis). Verifica tu email.
2. En el portal: **My Apps → Add New App** → plataforma **Android**, tipo **Application**, idioma **English** (después agregas español).
3. Pega los datos de la ficha de arriba (título, descripciones, email de soporte, URL de privacidad, categoría Productivity) y responde el cuestionario de datos con las respuestas de arriba.
4. Sube el icono (`icono-512.png`, lo descargas del repo) y los 2–3 screenshots que tomes en tu A14.
5. En la pestaña **Binary**: sube el APK firmado descargado del release final de GitHub.
6. Revisa todo y envía a revisión (tarda 1–3 días hábiles). Si Samsung pide algún cambio, me lo pasas.
