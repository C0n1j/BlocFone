# BlocFone

BlocFone es una aplicación Android local para filtrar y rechazar llamadas entrantes mediante la API pública `CallScreeningService`. Su objetivo es ofrecer un control simple sobre qué llamadas bloquear, sin enviar números, contactos ni preferencias a servicios externos.

El proyecto es un MVP para Android 10 (API 29) o superior. El filtrado depende de las llamadas que Android entregue al servicio y, por lo tanto, tiene límites que varían según la versión del sistema, el fabricante y la operadora.

## Funcionalidades principales

- Activación como aplicación de identificación y filtrado mediante `ROLE_CALL_SCREENING`.
- Tres modos de bloqueo: todas las llamadas, números no guardados y números seleccionados.
- Consulta puntual de contactos con `ContactsContract.PhoneLookup`.
- Lista local de números con control de caracteres admitidos, normalización y coincidencia exacta.
- Persistencia de modo y números mediante Preferences DataStore.
- Rechazo automático con una política de fallo abierto ante errores o demoras.
- Interfaz en Jetpack Compose para gestionar rol, permiso, modo y lista de números.

## Cómo funciona el filtrado

1. Android recibe una llamada entrante y, si BlocFone conserva `ROLE_CALL_SCREENING`, invoca `BlocFoneCallScreeningService`.
2. El servicio programa inmediatamente una respuesta de respaldo a los 3,5 segundos. Ese respaldo deja pasar la llamada para evitar bloquearla por una evaluación incompleta.
3. `SettingsRepository` obtiene de DataStore una instantánea del modo activo y de los números seleccionados. Las lecturas reintentan los errores de entrada/salida (`IOException`) con una espera progresiva limitada a 2 segundos entre intentos.
4. El servicio extrae el número únicamente si Android proporciona un URI con esquema `tel` y lo normaliza con `PhoneNumberUtils.normalizeNumber`.
5. En el modo de números no guardados, `ContactLookup` consulta `PhoneLookup` solo para ese número. No recorre ni copia la agenda.
6. `CallRuleEvaluator` aplica una regla pura según el modo seleccionado.
7. Si corresponde rechazar, el servicio responde con `setDisallowCall(true)` y `setRejectCall(true)`. En caso contrario, responde sin restricciones y deja pasar la llamada.
8. Una compuerta atómica hace que la evaluación y el respaldo compitan de forma segura: solo una respuesta puede enviarse. Si hay una excepción, falta información, se agota el tiempo o se destruye el servicio, BlocFone intenta dejar pasar la llamada.

Esta política es **fail-open**: ante una condición que impide decidir con seguridad, se prioriza no bloquear una llamada legítima.

## Modos de bloqueo

### Todas las llamadas entrantes

Rechaza toda llamada entrante que Android entregue al servicio, incluso si no incluye un número visible. No puede actuar sobre llamadas que el sistema no le presente.

### Números no guardados

Rechaza solamente cuando existe un número visible y `PhoneLookup` confirma que no está en contactos. Si el número no está disponible, falta el permiso o falla la consulta, la llamada se deja pasar.

Este es el modo inicial cuando todavía no hay una preferencia guardada.

### Números seleccionados

Rechaza únicamente los números incluidos en la lista local después de normalizarlos. La entrada acepta dígitos, `+`, espacios, guiones y paréntesis, y exige al menos tres dígitos; no acepta letras, puntos, barras ni tabulaciones.

Este control es sintáctico: después de comprobar los caracteres admitidos, BlocFone normaliza la entrada con `PhoneNumberUtils.normalizeNumber` y exige que el resultado contenga al menos tres dígitos. No verifica que el número exista, que tenga una longitud válida para un país ni que sea telefónicamente plausible.

La comparación es exacta y no usa coincidencias por sufijo. Por ejemplo, `+541155551234`, `1155551234` y `55551234` no se consideran automáticamente equivalentes. Conviene guardar el número en el mismo formato, preferentemente internacional, que recibe el dispositivo.

## Requisitos y tecnologías

| Componente | Configuración |
| --- | --- |
| Sistema mínimo | Android 10, API 29 |
| SDK de compilación y objetivo | API 35 |
| Java/JVM | JDK 17 |
| Lenguaje | Kotlin 2.2.20 |
| Interfaz | Jetpack Compose con Material 3 |
| Persistencia | AndroidX Preferences DataStore |
| Concurrencia | Kotlin Coroutines |
| Build | Gradle 8.14 y Android Gradle Plugin 8.11.1 |
| Identificador | `es.c0n1j.blocfone` |

Para comprobar el filtrado real se necesita un teléfono con Android 10 o superior, capacidad de recibir llamadas y una SIM o servicio equivalente. Las vistas previas de Compose y un emulador sin telefonía real no permiten validar el comportamiento completo.

## Estructura y arquitectura

El proyecto contiene un único módulo Android, `app`, y separa la integración con el sistema de las reglas puras:

| Ruta | Responsabilidad |
| --- | --- |
| [`app/src/main/java/es/c0n1j/blocfone/MainActivity.kt`](app/src/main/java/es/c0n1j/blocfone/MainActivity.kt) | UI Compose, solicitud del rol y permiso, selección de modo y edición de la lista. |
| [`app/src/main/java/es/c0n1j/blocfone/screening/BlocFoneCallScreeningService.kt`](app/src/main/java/es/c0n1j/blocfone/screening/BlocFoneCallScreeningService.kt) | Entrada desde Android Telecom, evaluación asíncrona, rechazo, compuerta atómica y respaldo fail-open. |
| [`app/src/main/java/es/c0n1j/blocfone/domain/CallRuleEvaluator.kt`](app/src/main/java/es/c0n1j/blocfone/domain/CallRuleEvaluator.kt) | Modos, modelos y decisión pura de permitir o rechazar. |
| [`app/src/main/java/es/c0n1j/blocfone/domain/NumberInputValidator.kt`](app/src/main/java/es/c0n1j/blocfone/domain/NumberInputValidator.kt) | Control de caracteres admitidos antes de normalizar y comprobación posterior del mínimo de tres dígitos. |
| [`app/src/main/java/es/c0n1j/blocfone/data/SettingsRepository.kt`](app/src/main/java/es/c0n1j/blocfone/data/SettingsRepository.kt) | Lectura y escritura de preferencias, normalización y reintentos de DataStore. |
| [`app/src/main/java/es/c0n1j/blocfone/contacts/ContactLookup.kt`](app/src/main/java/es/c0n1j/blocfone/contacts/ContactLookup.kt) | Consulta puntual de existencia mediante `PhoneLookup`. |
| [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) | Declaración de `READ_CONTACTS`, actividad y servicio protegido por `BIND_SCREENING_SERVICE`. |
| [`app/src/test/java/es/c0n1j/blocfone/domain/`](app/src/test/java/es/c0n1j/blocfone/domain/) | Tests unitarios de reglas y validación. |

## Abrir e instalar desde Android Studio

1. Instalá Android Studio, JDK 17 y Android SDK Platform 35.
2. Abrí la carpeta raíz del proyecto en Android Studio.
3. Permití que Android Studio sincronice el proyecto Gradle y descargue las dependencias declaradas si no están disponibles localmente.
4. Conectá un dispositivo físico con Android 10 o superior y habilitá la depuración USB si vas a instalar desde Android Studio.
5. Seleccioná el módulo `app` y ejecutalo sobre el dispositivo.

El proyecto no contiene credenciales ni requiere claves de servicios externos. La descarga de herramientas y dependencias durante el desarrollo sí puede necesitar conexión, aunque la aplicación instalada no declara el permiso `INTERNET`.

## Compilar y probar desde la terminal

Ejecutá los comandos desde la raíz del proyecto. El Gradle Wrapper usa la versión de Gradle configurada por el repositorio.

En Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

En macOS o Linux:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Si la compilación debug finaliza correctamente, el APK generado por el módulo `app` se encuentra normalmente en `app/build/outputs/apk/debug/app-debug.apk`. Este artefacto es de desarrollo y no equivale a una versión publicada para usuarios finales.

## Activación y uso

1. Abrí BlocFone.
2. Tocá **Activar protección** y aceptá que BlocFone sea la aplicación de identificación y filtrado de llamadas. Sin `ROLE_CALL_SCREENING`, Android no invoca el servicio.
3. Tocá **Permitir contactos** y concedé `READ_CONTACTS` si vas a usar **Números no guardados**.
4. Elegí uno de los tres modos. El cambio queda guardado automáticamente.
5. Para **Números seleccionados**, agregá los números que querés rechazar y verificá el formato mostrado en la lista.
6. Realizá pruebas con llamadas reales. Probá también después de reiniciar la aplicación y tras conceder o revocar rol y permiso.

## Permisos y rol del sistema

- `ROLE_CALL_SCREENING`: no es un permiso del manifiesto. El usuario lo concede desde el diálogo administrado por Android y es imprescindible para recibir eventos de filtrado.
- `READ_CONTACTS`: es el único permiso de ejecución declarado. Se usa exclusivamente para comprobar si el número entrante existe en contactos.
- `BIND_SCREENING_SERVICE`: protege la declaración del servicio para que Android Telecom pueda enlazarlo; no se solicita al usuario.

BlocFone no solicita acceso al registro de llamadas, estado del teléfono, realización o respuesta de llamadas, audio, accesibilidad, notificaciones ni Internet. Revocar el rol detiene el filtrado. Revocar contactos hace que el modo de números no guardados deje pasar las llamadas que no pueda clasificar.

## Persistencia y privacidad

- El modo activo y la lista normalizada se guardan en el dispositivo con Preferences DataStore, en `screening_settings`.
- El respaldo de datos de Android está desactivado mediante `android:allowBackup="false"`.
- No existe una base de datos remota, analítica ni integración de red declarada.
- La agenda no se copia ni se enumera: se consulta la existencia de un único número por llamada cuando el modo lo requiere.
- Los errores de almacenamiento, permisos, contactos o evaluación no provocan un rechazo automático; activan el comportamiento fail-open.

## Pruebas disponibles

Hay tests unitarios JUnit 4, independientes del framework Android, para:

- `CallRuleEvaluatorTest`: cubre los tres modos, números ausentes, estado desconocido de contactos y la ausencia de comparación por sufijo.
- `NumberInputValidatorTest`: cubre caracteres admitidos y rechazados, entradas vacías y el mínimo de tres dígitos.

No hay tests instrumentados ni una suite que simule `CallScreeningService`, permisos, contactos u operadoras. Es necesario complementar los tests puros con pruebas manuales en dispositivos físicos y distintas versiones de Android.

## Limitaciones reales

- Android decide qué llamadas entrega a `CallScreeningService`. BlocFone no puede filtrar una llamada que nunca llega al servicio.
- En Android 10, el sistema puede no entregar a servicios de terceros las llamadas de contactos guardados, incluso con `READ_CONTACTS`. Por eso **Todas las llamadas entrantes** y **Números seleccionados** pueden quedar incompletos en esa versión.
- Las llamadas privadas, ocultas o restringidas tampoco están garantizadas: pueden no llegar al servicio o no incluir un número utilizable.
- El comportamiento puede variar entre versiones de Android, fabricantes, aplicaciones de teléfono, configuraciones de SIM y operadoras.
- BlocFone solicita el rechazo, pero no controla el tono, la locución, el código de red, el desvío al buzón ni lo que escucha quien llama. Esos resultados corresponden a Android y a la operadora.
- El límite interno de 3,5 segundos reduce el riesgo de superar el plazo de respuesta de Android, pero prioriza dejar pasar la llamada si la evaluación no termina a tiempo.
- No se garantiza el bloqueo de todas las llamadas. Cada combinación de dispositivo y operadora debe validarse con llamadas reales.

## Estado del proyecto

BlocFone se encuentra en estado de MVP. Implementa el flujo principal, persistencia local, manejo defensivo de errores y tests unitarios de dominio, pero todavía depende de validación manual en una matriz representativa de dispositivos, versiones y operadoras.

El repositorio contiene actualmente el código fuente del MVP para desarrollo y evaluación. No incluye un APK publicado ni una distribución preparada para usuarios finales.

El repositorio no incluye un archivo de licencia ni una guía formal de contribución. No debe asumirse una licencia de uso, modificación o distribución hasta que el proyecto declare una explícitamente.
