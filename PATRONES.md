# PATRONES.md

## Abstract Factory

Se encuentra en `Main.java`, en `FabricaProductoParametrico` y sus clases `FabricaSequia`, `FabricaExceso` y `FabricaHelada`. Cada fábrica crea la fuente del índice, la regla de disparo y el generador del certificado que pertenecen al mismo producto. Así se evita mezclar, por ejemplo, la regla de helada con una póliza de sequía.

## Factory Method

Se encuentra en `CanalDeVenta`, que tiene el método público `emitir` y deja la creación del comprobante en `crearComprobante`. Las clases `CanalCooperativa`, `CanalAppMovil` y `CanalCorresponsal` implementan la numeración, el contenido y la comisión de cada canal. Cada objeto conserva su propio consecutivo.

## Builder

Se encuentra en `Poliza.Builder`. La póliza queda inmutable después de construirse y el builder permite encadenar los datos opcionales. `build()` valida los datos obligatorios, el rango de hectáreas, la vigencia mínima y la renovación.
