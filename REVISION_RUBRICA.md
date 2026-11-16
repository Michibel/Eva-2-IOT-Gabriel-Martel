# Revisión de la rúbrica sumativa

Esta revisión describe la evidencia disponible, no una calificación ni una garantía de cumplimiento. El prototipo Arduino real y la demostración en clase todavía deben verificarse.

| Criterio | Evidencia disponible | Estado y trabajo restante |
| --- | --- | --- |
| 2.1.1.1 Problema y dispositivos | El informe original y RETROALIMENTACION_Y_ARDUINO.md describen el peaje, dos teléfonos, el Arduino y sus componentes. | Definido; la conexión con el Arduino aún no se ha probado. |
| 2.1.1.2 Funciones | Inicio de sesión, monitoreo simulado, historial, filtros, exportación y recepción de eventos. Hay historias de usuario en formato Connextra. | Implementadas para la simulación; la transmisión en dispositivos físicos está pendiente de comprobar. |
| 2.1.1.3 Herramientas Android | Android Studio, Kotlin, Compose, Material 3, SQLiteOpenHelper y Android Keystore. | Definidas y justificadas en esta guía y en el README. |
| 2.1.1.4 Presentación de la propuesta | Existe retroalimentación de la evaluación formativa y se documentaron cambios en RETROALIMENTACION_Y_ARDUINO.md. | La presentación sumativa y su evaluación corresponden al estudiante y al docente. |
| 2.1.2.5 Método inalámbrico | Bluetooth para el enlace local y Wi-Fi para supervisión entre teléfonos. | Implementados como demostración; falta medir y probar en la red y equipos reales. La integración física requiere adaptar el sistema. |
| 2.1.2.6 Mockup | MOCKUP.md muestra pantallas, navegación, interacción y datos. | Bosquejo textual disponible; puede complementarse con capturas reales de la app después de probarla. |
| 2.1.2.7 Validación con docente | Se han aplicado cambios basados en la retroalimentación formativa. | Falta que el docente revise la versión actual, incluido el diseño y la autenticación. |
| 2.1.3.8 Aplicación Kotlin | Proyecto Android y código fuente, organizados por almacenamiento y conexiones. | Compilación comprobable mediante Gradle; las pruebas de funcionamiento no se deducen solo de compilar. |
| 2.1.3.9 Permisos | El manifiesto declara INTERNET y los permisos Bluetooth; Android 12 o superior solicita BLUETOOTH_CONNECT durante el uso. | Verificar en dispositivos y distintas versiones. El cliente HC-05 es solo de lectura y no está probado con el módulo real. |
| 2.1.3.10 Seguridad | Contraseña con PBKDF2 y sal; UID con AES-GCM y clave de Keystore; backup desactivado; cierre por inactividad; socket Bluetooth seguro; canal Wi-Fi experimental cifrado con clave compartida. | No hay certificación ISO. Fecha, resultado y origen no están cifrados ni tienen protección criptográfica de integridad en SQLite; el CSV tampoco está cifrado. El canal propio no está auditado y no es TLS. |
| 2.1.3.11 Pruebas | Las dos pruebas automatizadas de Wi-Fi en el computador pasaron; plan en PRUEBAS.md. | Deben registrarse resultados reales de autenticación, almacenamiento, exportación, Bluetooth y conexión física. |
| 2.1.4.12 Demostración | Guía de demostración en PRUEBAS.md. | Pendiente de hacer en clase; la conexión a Arduino no está verificada. |
| 2.1.4.13 Explicación | Este archivo y RETROALIMENTACION_Y_ARDUINO.md describen el diseño y sus decisiones. | El estudiante debe explicar el proyecto y responder preguntas en la presentación. |
| 2.1.4.14 Evidencia de seguridad | Código de autenticación, almacenamiento y comunicaciones. | Mostrar las medidas realmente implementadas y reconocer sus límites; no afirmar cumplimiento integral ni ensayos que no se realizaron. |
| 2.1.4.15 Respuestas y ajustes | Se documentan los cambios solicitados en la evaluación formativa. | Respuestas en clase y ajustes posteriores sujetos a la retroalimentación efectiva del docente. |

## Justificación de conectividad

Bluetooth clásico es apropiado para una conexión directa de corto alcance sin infraestructura de red, como la supervisión local. Wi-Fi permite conectar teléfonos en una red local y ampliar la cobertura según el punto de acceso y el entorno; normalmente requiere una red disponible y puede consumir más energía dependiendo del dispositivo y del patrón de uso. Ambas tecnologías pueden sufrir interferencias y desconexiones: su alcance, consumo y estabilidad deben medirse en el lugar, sin asumir un valor fijo. En esta implementación no hay reconexión ni entrega garantizada; ante fallo se conserva el evento en el teléfono que lo generó y no se afirma que el receptor lo haya recibido.

Se seleccionó SQLite por su sencillez y funcionamiento sin conexión, a costa de tener cuentas e historiales separados en cada dispositivo. Compose y Material 3 permiten construir las pantallas con componentes de Android similares a los usados en los ejemplos de clase. El prototipo no reemplaza el control local del Arduino ni demuestra por sí mismo un mecanismo físico seguro.
