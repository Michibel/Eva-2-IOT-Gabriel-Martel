# Plan y registro de pruebas

No completar la columna Resultado hasta realizar la prueba. El código puede compilar sin que su comportamiento en un teléfono, el Bluetooth o el Arduino hayan sido verificados.

El 15 de noviembre de 2026 se ejecutó `testDebugUnitTest` en el computador: las dos pruebas automatizadas del canal Wi-Fi pasaron. Conectaron dos instancias en la interfaz local, verificaron la recepción de un evento y el rechazo de una orden de apertura y una clave incorrecta. No prueban la red Wi-Fi real ni el funcionamiento en Android físico.

| N.º | Escenario | Resultado esperado | Resultado observado |
| --- | --- | --- | --- |
| 1 | Crear usuario e ingresar con contraseña válida. | Acceso al monitoreo. | Pendiente en teléfono. |
| 2 | Ingresar tres contraseñas incorrectas. | Bloqueo por un minuto; luego permite reintentar. | Pendiente en teléfono. |
| 3 | Inactividad durante cinco minutos. | La sesión se cierra. | Pendiente en teléfono. |
| 4 | Simular eventos autorizado y denegado. | Aparecen en el historial con UID, resultado y fecha. | Pendiente en teléfono. |
| 5 | Filtros por UID, resultado y fecha; exportación. | Pantalla y CSV coinciden; cancelar no exporta. | Pendiente en teléfono. |
| 6 | Cerrar y abrir la aplicación. | Los registros persisten. | Pendiente en teléfono. |
| 7 | Conectar dos teléfonos mediante Wi-Fi y enviar eventos en ambos sentidos. | Se muestra conexión, y cada evento recibido aparece en historial. | Pendiente en teléfonos. |
| 8 | Conectar Wi-Fi con una clave diferente. | Se rechaza la conexión; no se aceptan eventos. | Pendiente en teléfonos. |
| 9 | Desconectar Wi-Fi y Bluetooth durante el uso. | Se informa la desconexión y el evento generado se conserva localmente; no se supone recepción. | Pendiente en teléfonos. |
| 10 | Emparejar teléfonos por Bluetooth y enviar eventos. | Receptor registra el evento; no se admiten órdenes de apertura. | Pendiente en teléfonos. |
| 11 | Permiso Bluetooth denegado y Bluetooth apagado. | La app informa que no puede establecer el enlace. | Pendiente en teléfono. |
| 12 | Rotar la pantalla. | Comprobar si se requiere volver a iniciar sesión y que el historial guardado siga disponible. | Pendiente en teléfono. |
| 13 | Generar y consultar unos 200 registros. | Historial y filtros siguen funcionando. | Pendiente en teléfono. |
| 14 | Integración con Arduino y HC-05. | Verificar el cliente SPP de solo recepción con el sketch real y el comportamiento físico acordado con el docente. | Pendiente: no se dispone del sketch ni se ha probado el módulo. |

Para la demostración, mostrar el acceso, iniciar simulación, registrar tarjetas, conectar los dispositivos, comprobar el historial recibido, filtrar y exportar, y explicar las medidas y limitaciones de seguridad. Presentar también los cambios de la retroalimentación formativa. Registrar modelo, versión de Android, fecha y observaciones reales de cada ensayo.
