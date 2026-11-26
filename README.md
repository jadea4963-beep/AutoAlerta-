# AutoAlerta Android

Aplicación Android nativa para el proyecto educativo AutoAlerta. Permite conectarse a un módulo Bluetooth clásico HC-05 previamente emparejado, enviar los comandos `1` y `0`, escuchar el mensaje `ALERTA` y abrir el marcador telefónico para emergencias.

## Abrir y ejecutar

1. Abre esta carpeta en Android Studio (Meerkat o posterior) y deja que Gradle sincronice el proyecto.
2. Conecta el HC-05 al Arduino y empareja el módulo desde los ajustes Bluetooth del teléfono (PIN habitual: `1234` o `0000`; depende de la configuración).
3. Ejecuta la app en un teléfono Android con Bluetooth clásico. Concede el permiso Bluetooth cuando se solicite y toca **Conectar HC-05** para elegir el módulo emparejado.
4. Usa **Activar** / **Desactivar** para enviar `1` / `0` (se envía también salto de línea). El Arduino debe implementar ese protocolo.

Al recibir `ALERTA`, la app muestra el aviso y reproduce un tono mientras permanezca abierta. **Emergencias 911** abre el marcador; la app no inicia llamadas automáticamente.

> Proyecto de demostración: no sustituye una alarma automotriz certificada ni garantiza protección del vehículo. La detección requiere un sensor y un programa Arduino compatibles. No se incluye firmware Arduino. El HC-05 usa Bluetooth clásico y primero debe estar emparejado en Android.
