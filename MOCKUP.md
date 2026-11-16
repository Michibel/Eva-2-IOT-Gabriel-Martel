# Bosquejo de pantallas

```text
┌─────────────────────────────┐
│ REGISTRO DE PEAJE           │
│ Usuario       [__________]  │
│ Contraseña    [••••••••••]  │
│ [Ingresar o crear usuario]  │
│ Mensaje de acceso           │
└─────────────────────────────┘
             │ acceso
             ▼
┌─────────────────────────────┐
│ MONITOREO     [conectado]   │
│ Peaje: en marcha/detenido   │
│ Barrera: estado simulado    │
│ Último evento · Total hoy   │
├─────────────────────────────┤
│ Monitoreo Activador Historial│
│ Conexión                    │
└─────────────────────────────┘
             │ navegación inferior
             ▼
┌─────────────────────────────┐
│ ACTIVADOR                   │
│ Peaje en marcha      [  ]   │
│ Sin apertura desde la app   │
│ [Simular autorizada]        │
│ [Simular denegada]          │
│ Solo eventos simulados      │
└─────────────────────────────┘

┌─────────────────────────────┐
│ HISTORIAL                   │
│ Buscar UID [____________]   │
│ ○ Todos ○ Permitidos ○ No   │
│ Desde [fecha] Hasta [fecha] │
│ Totales por resultado       │
│ [Exportar CSV]              │
│ Fecha · UID · Resultado     │
│ Fecha · UID · Resultado     │
└─────────────────────────────┘

┌─────────────────────────────┐
│ CONEXIÓN                    │
│ Estado Wi-Fi y Bluetooth   │
│ [Conectar Wi-Fi]            │
│ [Consultar emparejados]     │
│ [Esperar conexión]          │
│ [Conectar a teléfono]       │
│ [Desconectar] [Cerrar sesión]│
└─────────────────────────────┘
```

Dos teléfonos intercambian eventos simulados por Wi-Fi o Bluetooth. No se envían órdenes de apertura. La aplicación muestra y almacena los eventos recibidos; el control y la barrera son simulados. El selector de archivos del sistema permite escoger dónde guardar el CSV.
