# Cambios por retroalimentación y conexión futura con Arduino

## Historias de usuario

1. Como operador, quiero ingresar con usuario y contraseña para limitar el acceso al historial.
2. Como operador, quiero consultar el último evento y los pasos del día para supervisar el peaje.
3. Como operador, quiero simular tarjetas autorizadas y denegadas para ensayar el registro sin accionar una barrera física.
4. Como supervisor, quiero recibir eventos por Wi-Fi para revisar la actividad desde otro teléfono en la red autorizada.
5. Como operador local, quiero recibir eventos por Bluetooth para consultar la actividad cerca del equipo.
6. Como supervisor, quiero buscar y filtrar el historial por UID, resultado y fecha para revisar incidentes.
7. Como supervisor, quiero confirmar la exportación de los registros mostrados para compartir solo la información seleccionada.
8. Como responsable de seguridad, quiero que la app no envíe órdenes de apertura para impedir una apertura remota desde esta versión.
9. Como responsable de seguridad, quiero que ante pérdida de comunicación se indique que el estado físico es desconocido para no confundirlo con un cierre real.

## Cambios aplicados

Se retiraron el botón de apertura y el tratamiento de órdenes INICIAR, DETENER y ABRIR recibidas por Bluetooth. El interruptor de activación y los eventos solo afectan la simulación. La app dispone de Bluetooth entre teléfonos y de un canal Wi-Fi entre teléfonos para supervisión, que solo intercambia eventos. La barrera se muestra como desconocida porque no hay comunicación con el equipo real.

El canal Wi-Fi experimental utiliza una contraseña compartida introducida por los usuarios, derivación PBKDF2 y mensajes AES-GCM. Se emplean valores aleatorios nuevos para cada conexión, contadores por dirección y autenticación mutua del secreto. No es TLS, no se ha sometido a auditoría ni debe utilizarse en un peaje real. Usar una clave larga, aleatoria y única, no una contraseña reutilizada. El emisor de una tarjeta simulada intenta enviar el evento por Wi-Fi y, si no hay conexión, por Bluetooth. El dispositivo receptor asigna la fecha y hora al recibirlo.

## Prueba entre teléfonos por Wi-Fi

1. Instalar la misma versión en ambos teléfonos, conectarlos a la misma red local e iniciar sesión.
2. En ambos, entrar en Conexión y escribir la misma clave compartida de al menos 12 caracteres. Usar una clave larga aleatoria; no usar las credenciales del teléfono.
3. En uno, consultar su dirección IP en los ajustes de la red Wi-Fi y pulsar «Esperar supervisión Wi-Fi». La aplicación escucha el puerto TCP 37373 mientras esté esperando.
4. En el otro, escribir esa IP y pulsar «Conectar por Wi-Fi». Verificar en ambas pantallas el estado conectado.
5. En el emisor, activar la simulación y generar una tarjeta. Comprobar que aparece en el historial del receptor. Probar también una clave incorrecta y la desconexión.

La conexión puede fallar si la red aísla los dispositivos o bloquea el puerto. Estas pruebas y las de Bluetooth siguen pendientes de ejecución en equipos reales. La app no garantiza entrega si se interrumpe la conexión; no hay cola ni sincronización posterior.

## Integración posterior con el prototipo Arduino

El informe identifica un Arduino UNO, lector RFID, sensor ultrasónico, servomotor y HC-05, pero no proporciona el sketch, el cableado ni el formato real de mensajes. Para integrar el prototipo se necesitan estos datos, los modelos exactos, la alimentación y el comportamiento actual. El UNO y un HC-05 no proporcionan Wi-Fi por sí solos; se requerirá un módulo Wi-Fi compatible o una pasarela independiente para supervisión. Su selección y cableado deben verificarse con las fichas técnicas de los componentes concretos.

Arquitectura propuesta para una maqueta: el Arduino sigue leyendo la tarjeta y el sensor, decide localmente el acceso y registra o transmite eventos; el HC-05 envía eventos al teléfono cercano; una pasarela Wi-Fi supervisa el estado y los eventos sin capacidad para abrir la barrera. La app incluye un cliente experimental de solo recepción usando el UUID SPP estándar: en Conexión, después de emparejar el módulo, seleccionar «Conectar como HC-05». Espera líneas ASCII terminadas en salto de línea con el formato `EVENTO|A1B2C3|Autorizado` o `EVENTO|D4E5F6|Denegado`. El UID debe tener de 1 a 32 letras, números, guiones o guiones bajos. El sketch real puede usar otro formato: no hay compatibilidad garantizada ni se ha probado con hardware. El cliente no transmite eventos ni órdenes al módulo. La autenticación, protección real del enlace y las limitaciones del HC-05 deben evaluarse con el hardware concreto; no se debe conectar un actuador a este protocolo de demostración.

La retroalimentación solicita un comportamiento «fail-secure»: ante corte de energía o pérdida de red, no conceder nuevos accesos y mantener la barrera cerrada o bloqueada. Eso debe diseñarse y verificarse físicamente en el controlador y el mecanismo, no se puede garantizar desde una app ni suponiendo que un servo permanezca en posición sin energía. Además, las necesidades de evacuación, detección de obstáculos y seguridad de personas requieren una evaluación independiente y pueden exigir comportamientos distintos. La solución debe revisarse con el docente y un profesional competente antes de cualquier uso físico; esta aplicación es solamente una simulación educativa.

Para continuar hacen falta el sketch Arduino, esquema de conexiones, modelos de los componentes y ejemplos de mensajes reales. Con eso se puede definir el intercambio de eventos, comprobar niveles eléctricos, permisos y emparejamiento, y elaborar pruebas de desconexión y recuperación en una maqueta controlada.
