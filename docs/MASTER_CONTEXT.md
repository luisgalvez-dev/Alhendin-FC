# ALHENDINFC 2.0 — MASTER CONTEXT

> Documento maestro de contexto para Cursor.
> Este archivo describe el estado actual del proyecto, el objetivo final, las decisiones funcionales ya cerradas y las restricciones que deben respetarse en todas las fases del desarrollo.

---

# 1. PROPÓSITO DE ESTE DOCUMENTO

Este proyecto **NO se empieza desde cero**.

Existe una aplicación Android nativa llamada **AlhendinFC**, ya funcional y con datos reales del club. La aplicación debe evolucionar para convertirse en una herramienta completa de trabajo para el cuerpo técnico del Alhendín CF.

Antes de realizar cualquier cambio importante:

1. Leer este documento completo.
2. Revisar el estado actual del repositorio.
3. Respetar todas las decisiones funcionales descritas aquí.
4. No eliminar funcionalidades existentes salvo orden explícita.
5. No realizar migraciones destructivas.
6. No asumir que los datos actuales son de prueba.
7. Trabajar por fases controladas.

**No se debe intentar implementar todo AlhendinFC 2.0 en una sola ejecución.**

Este documento define el destino final, pero la implementación debe hacerse por fases.

---

# 2. CONTEXTO GENERAL DEL PRODUCTO

AlhendinFC será una aplicación Android específica para el cuerpo técnico del **Alhendín CF**.

No se pretende convertir en un SaaS genérico para otros clubes en esta fase.

La aplicación se utilizará principalmente en:

- teléfonos Android;
- tablets Android.

Debe funcionar correctamente tanto en móvil como en tablet.

La app debe centralizar:

- plantilla;
- partidos;
- convocatorias;
- alineaciones;
- estadísticas;
- partido en directo;
- calendario de temporada;
- entrenamientos;
- tareas de entrenamiento;
- pizarra táctica;
- rivales;
- análisis de rivales;
- informes;
- fotos;
- documentos;
- enlaces RFAF / RFAF TV;
- backups;
- sincronización entre dispositivos.

---

# 3. USUARIOS

Inicialmente habrá dos usuarios:

- Migue.
- Analista.

Cada usuario tendrá:

- su propia cuenta;
- correo + contraseña;
- sesión persistente;
- configuración personal del Inicio.

Ambos usuarios pertenecen al mismo espacio compartido:

**ALHENDÍN CF**

## 3.1 Permisos

No habrá roles restrictivos en esta fase.

Los dos usuarios podrán:

- ver;
- crear;
- editar;
- eliminar;

cualquier información deportiva de la app.

No se implementará una separación rígida Administrador / Analista.

La diferenciación práctica vendrá principalmente de la configuración del Inicio.

Ambos siguen teniendo acceso a todos los módulos mediante **Todos los módulos**.

---

# 4. APLICACIÓN ACTUAL — PRINCIPIO GENERAL

La aplicación actual contiene funcionalidades reales y útiles.

**Todas deben conservarse.**

No se debe rehacer el proyecto desde cero.

No se debe eliminar ningún módulo existente simplemente porque no aparezca en el nuevo flujo.

La app actual permite activar/desactivar los módulos que aparecen en Inicio. Esa capacidad debe mantenerse.

Los nuevos módulos deben integrarse en ese sistema.

---

# 5. TECNOLOGÍA ACTUAL

Aplicación Android nativa.

Base tecnológica existente:

- Kotlin.
- Jetpack Compose.
- Room.
- Gradle Kotlin DSL.

Se mantiene Android nativo.

No migrar a React Native, Flutter, Expo o Web/PWA salvo orden explícita futura.

---

# 6. DATOS ACTUALES: SON DATOS REALES

Los datos locales actuales **NO SON DATOS DE PRUEBA**.

Actualmente ya existen, entre otros:

- jugadores reales;
- posiciones reales;
- rivales reales;
- nombres de rivales;
- estadios;
- equipaciones;
- colores;
- escudos/fotos;
- partidos;
- estadísticas;
- configuraciones.

Por tanto:

**NO BORRAR DATOS.**

**NO REINICIAR LA BASE DE DATOS.**

**NO UTILIZAR MIGRACIONES DESTRUCTIVAS.**

Antes de cualquier migración relevante debe existir una copia de seguridad verificable.

---

# 7. PROBLEMAS CRÍTICOS DETECTADOS EN LA BASE ACTUAL

Antes de añadir funcionalidades nuevas importantes, se debe revisar y corregir la base técnica.

## 7.1 Room

Existe/ha existido uso de `fallbackToDestructiveMigration()`.

Esto no es aceptable con datos reales.

Debe sustituirse por migraciones Room explícitas y no destructivas.

## 7.2 Limpieza de tablas

Existe/ha existido lógica con `clearAllTables()` relacionada con eliminación de datos demo.

Debe revisarse cuidadosamente y eliminarse o blindarse para que jamás pueda borrar datos reales accidentalmente.

## 7.3 Fechas

Hay fechas almacenadas como texto en formato similar a `dd/MM/yyyy`.

Esto puede provocar ordenaciones SQL incorrectas.

Debe establecerse una estrategia coherente para fechas mediante epoch millis, ISO-8601 o TypeConverters adecuados.

La migración debe conservar correctamente todas las fechas existentes.

## 7.4 Estadísticas

Se detectó un posible error en la lógica de dobles amarillas/rojas de temporada.

Dos amarillas en partidos diferentes no deben poder interpretarse como una roja.

Revisar y probar.

## 7.5 Movimiento de jugadores en partido/campo

Se detectó una anomalía en cierta lógica de movimiento/reubicación de jugadores en el campo.

Revisar el comportamiento real antes de modificarlo.

## 7.6 Contadores hardcodeados

Se detectó al menos un contador mostrado como `"0"` fijo.

Revisar valores hardcodeados que deberían provenir del estado real.

## 7.7 Integridad relacional

Revisar entidades Room y relaciones.

Añadir claves foráneas / índices donde sea apropiado sin romper datos actuales.

Evitar datos huérfanos.

## 7.8 Backups

La restauración/importación actual debe reforzarse.

Nunca sustituir datos reales antes de validar backup, versión e integridad.

## 7.9 Tests

El proyecto necesita tests mínimos sobre:

- migraciones;
- estadísticas críticas;
- persistencia;
- sincronización;
- eliminación;
- restauración.

---

# 8. ESTRATEGIA DE IDENTIFICADORES

Los IDs locales actuales no deben romperse.

Si una entidad usa actualmente IDs numéricos Room, se mantendrán para preservar relaciones existentes.

Para sincronización se añadirá un identificador global estable, por ejemplo `syncId: UUID`.

Ejemplo conceptual:

- `localId = 17`
- `syncId = "550e8400-e29b-41d4-a716-446655440000"`

No utilizar IDs locales autoincrementales como identificadores globales entre dispositivos.

---

# 9. SINCRONIZACIÓN Y NUBE

La aplicación debe sincronizar los datos entre los dispositivos de Migue y del analista.

Arquitectura objetivo:

- Room = almacenamiento local / offline.
- Firebase Authentication = usuarios.
- Firestore = datos compartidos.
- Firebase Storage = fotos, PDFs y documentos.

La aplicación debe seguir funcionando sin Internet.

## 9.1 Comportamiento offline

Si no hay conexión:

- se puede consultar información local;
- se pueden crear/modificar datos;
- se pueden registrar estadísticas;
- se puede usar partido en directo;
- los cambios quedan pendientes.

Cuando vuelva Internet:

- sincronización automática;
- sin botón manual obligatorio.

Mostrar un estado discreto:

- Sincronizado.
- Sin conexión.
- Cambios pendientes.

## 9.2 Conflictos

Si ambos usuarios modifican exactamente el mismo elemento:

**prevalece el último cambio guardado/sincronizado.**

No implementar colaboración tipo Google Docs.

Guardar metadatos cuando sea apropiado:

- createdAt;
- updatedAt;
- createdBy;
- updatedBy;
- deletedAt;
- syncState.

## 9.3 Eliminaciones

Las eliminaciones deben sincronizarse correctamente.

Usar una estrategia tipo tombstone / `deletedAt` cuando sea necesario para impedir que registros eliminados reaparezcan desde dispositivos offline.

## 9.4 Primera migración a Firebase

La base local actual será la fuente inicial de verdad.

Proceso esperado:

1. backup completo;
2. migración Room no destructiva;
3. añadir identificadores globales/metadatos necesarios;
4. subir datos locales existentes a Firebase;
5. verificar cantidades y relaciones;
6. permitir que otros dispositivos descarguen los mismos datos.

Nunca interpretar Firebase vacío como orden de borrar datos locales existentes.

---

# 10. PREFERENCIAS PERSONALES VS DATOS COMPARTIDOS

Datos deportivos: **compartidos entre los dos usuarios.**

Preferencias personales: **no deben pisarse entre usuarios.**

Especialmente:

- módulos visibles en Inicio;
- orden de módulos;
- determinadas preferencias visuales.

Cada usuario puede tener un Inicio completamente distinto.

---

# 11. INICIO Y NAVEGACIÓN

Se mantiene el sistema actual que permite activar/desactivar y organizar módulos en Inicio.

Añadir **Todos los módulos**.

Este apartado permite acceder a cualquier funcionalidad aunque no esté visible en Inicio.

Ocultar un módulo del Inicio:

- no lo desactiva;
- no elimina datos;
- no bloquea acceso desde Todos los módulos.

## 11.1 Identidad visual

Se mantiene la identidad visual del Alhendín CF y su paleta de colores.

La interfaz puede mejorar progresivamente en jerarquía, tarjetas, espaciados, tipografía, iconos, consistencia y responsive.

No realizar un rediseño visual completo de golpe.

---

# 12. MÓVIL Y TABLET

La misma app debe funcionar en ambos.

## 12.1 Móvil

Priorizar:

- una columna;
- controles compactos;
- navegación sencilla;
- formularios cómodos;
- consulta rápida.

## 12.2 Tablet

Aprovechar ancho:

- más columnas;
- paneles master-detail cuando aporte valor;
- mejor espacio para pizarra;
- mejor espacio para análisis.

La funcionalidad debe ser equivalente.

La pizarra se beneficiará especialmente de modo horizontal.

---

# 13. EQUIPO / PLANTILLA

Se conserva el módulo actual.

Los jugadores actuales son reales.

No borrar ni reconstruir la plantilla.

Cualquier mejora debe ser incremental y compatible con los datos actuales.

---

# 14. RIVALES

Existe una base de rivales actualmente.

Los rivales ya pueden contener información como:

- nombre;
- nombre corto;
- estadio;
- escudo/foto;
- equipaciones;
- colores.

Esta información debe conservarse.

La ficha del rival debe evolucionar hacia:

## 14.1 Resumen

- escudo;
- nombre;
- campo;
- equipación;
- próximo enfrentamiento;
- información principal.

## 14.2 Análisis

El análisis del rival tendrá secciones estructuradas:

### Sistema

- sistema habitual;
- variantes.

### Con balón

- salida de balón;
- progresión;
- último tercio/finalización cuando sea útil.

### Sin balón

- presión alta;
- bloque medio;
- bloque bajo.

### Transiciones

- ataque → defensa;
- defensa → ataque.

### ABP

- córners ofensivos;
- córners defensivos;
- faltas si se desea.

### Fortalezas

Texto editable.

### Debilidades

Texto editable.

### Jugadores clave

No se va a crear una base de datos completa duplicada de todos los jugadores del rival.

Se podrán guardar observaciones sobre jugadores relevantes.

### Notas generales

Texto libre.

El análisis debe poder editarse continuamente a lo largo de la temporada.

## 14.3 Archivos

Permitir adjuntar imágenes, PDF y documentos.

Acciones: añadir, ver, eliminar.

## 14.4 Enlaces

Cada rival puede guardar URLs editables:

- ficha RFAF;
- RFAF TV;
- YouTube;
- otros enlaces personalizados.

No hardcodear URLs por equipo.

## 14.5 RFAF

En primera versión:

- abrir ficha RFAF del rival;
- acceder a plantilla desde RFAF;
- abrir RFAF TV / vídeos;
- enlaces externos.

No depender de scraping automático.

---

# 15. CALENDARIO

El calendario debe ser mensual y sencillo.

Solo existen dos tipos de elementos:

- Entrenamiento.
- Partido.

No incluir reuniones, vídeo, recuperación, descanso u otros tipos.

Los partidos existentes deben aparecer automáticamente.

No duplicar partidos mediante otra entidad paralela.

## 15.1 Regla por día

Un día puede contener:

- un partido;
- o un entrenamiento;
- o nada.

No permitir entrenamiento si ese día ya existe un partido.

No permitir múltiples eventos el mismo día en esta fase.

## 15.2 Creación de entrenamiento

Flujo:

`Calendario → Día libre → Añadir entrenamiento`

No hay horarios.

No hay duración total del entrenamiento.

El entrenamiento contiene:

- rival asociado opcional;
- tareas;
- notas;
- fotos/documentos.

Se puede crear, editar y eliminar.

No implementar duplicar entrenamiento.

## 15.3 Rival asociado

Relacionar un entrenamiento con un rival es opcional.

---

# 16. TAREAS DE ENTRENAMIENTO

Las tareas las crea manualmente el entrenador.

No IA en esta fase.

Debe existir una biblioteca de tareas.

## 16.1 Campos de una tarea

- Nombre.
- Objetivo.
- Número general de jugadores.
- Duración de la tarea.
- Descripción.
- Pizarra asociada opcional.

No incluir:

- material;
- espacio/dimensiones;
- categorías;
- jugadores concretos de la plantilla.

## 16.2 Acciones

- crear;
- editar;
- eliminar;
- abrir;
- asociar pizarra.

## 16.3 Buscador

Debe existir un buscador simple por nombre.

No implementar categorías.

## 16.4 Entrenamiento y tareas

Cuando se crea un entrenamiento:

- seleccionar tareas existentes de la biblioteca;
- permitir seleccionar varias;
- permitir ordenarlas.

No implementar “tarea rápida” separada.

---

# 17. PIZARRA TÁCTICA

La pizarra actual ya contiene funcionalidades útiles.

Conservar:

- campo de fondo;
- imagen de fondo;
- vídeo de fondo/reproducción;
- controles de vídeo existentes;
- dibujo libre;
- flechas;
- formas/óvalos/zonas existentes;
- grosores;
- colores;
- goma;
- deshacer;
- limpiar.

## 17.1 Nuevos elementos

Añadir:

- jugadores azules;
- jugadores rojos;
- número editable en cada ficha;
- balón;
- conos;
- porterías;
- miniporterías;
- texto libre sobre el campo.

Los jugadores son genéricos.

No deben estar obligatoriamente vinculados a jugadores reales de la plantilla.

## 17.2 Persistencia

La pizarra debe poder:

- guardarse;
- abrirse;
- editarse;
- renombrarse;
- eliminarse;
- duplicarse.

Debe existir una biblioteca de pizarras.

## 17.3 Relación con tareas

Una tarea puede tener una pizarra asociada.

## 17.4 NO relacionar con análisis rival por ahora

Mantener Pizarra y Análisis Rival separados en esta fase.

## 17.5 Animación futura

Función deseada para una versión futura:

- botón Grabar;
- mover fichas/balón;
- registrar posiciones y tiempo;
- detener;
- reproducir como animación del ejercicio.

No implementar inicialmente.

Pero evitar diseñar el modelo de pizarra de forma que haga imposible añadir animaciones después.

---

# 18. PARTIDOS

Conservar el módulo actual y sus funcionalidades existentes.

Incluye, entre otras:

- creación/gestión de partidos;
- convocatoria;
- alineación;
- partido en directo;
- estadísticas;
- PDFs.

No eliminar ni simplificar arbitrariamente.

---

# 19. PARTIDO EN DIRECTO

Se mantiene como herramienta importante, especialmente para el analista.

Debe seguir funcionando con los datos actuales.

Cualquier modificación debe preservar estadísticas, jugadores, eventos, persistencia y funcionamiento offline.

---

# 20. INFORMES DEL ANALISTA

El analista actualmente genera su informe utilizando ChatGPT fuera de la app.

No integrar IA dentro de AlhendinFC todavía.

Después del partido:

1. analista utiliza datos/acta/convocatoria fuera de la app;
2. genera informe;
3. sube el informe a AlhendinFC.

Los dos usuarios pueden subir, ver y eliminar informes.

Admitir PDF, imágenes y documentos habituales.

El informe debe quedar asociado al partido.

Como el partido conoce al rival, también debe poder consultarse desde la ficha de ese rival sin duplicar físicamente el archivo.

---

# 21. RELACIÓN INFORME → ANÁLISIS DEL RIVAL

La app no interpretará automáticamente el informe.

Migue/analista podrá leer el informe y actualizar manualmente:

`Rivales → Rival → Análisis`

El informe completo se conserva como documentación histórica.

El análisis del rival contiene las conclusiones reutilizables.

---

# 22. ARCHIVOS

Debe existir una solución común y reutilizable para adjuntos.

No programar sistemas independientes para cada módulo.

Tipos:

- imágenes;
- PDF;
- documentos.

Usos:

- entrenamientos;
- rivales;
- partidos;
- informes;
- otras entidades compatibles.

Acciones básicas:

- añadir;
- visualizar;
- eliminar.

## 22.1 Fotos de entrenamientos

Caso de uso clave:

`Calendario → Día → Entrenamiento → Añadir foto`

Puede hacer foto, elegir de galería o adjuntar documento.

La app solo debe guardar, mostrar y borrar.

No OCR.

No IA.

---

# 23. FIREBASE STORAGE

Archivos compartidos deben estar disponibles en ambos dispositivos.

Las rutas locales del dispositivo no sirven como referencia global.

Especial atención a escudos/fotos actuales de rivales si están almacenados mediante URI local.

La migración debe preservar y trasladar correctamente estos recursos.

---

# 24. BACKUPS

Mantener la función de backup manual aunque exista Firebase.

Objetivo:

- Room/local;
- Firebase;
- backup manual.

Tres capas de seguridad.

El backup de datos debe cubrir la información estructurada.

No es obligatorio incluir todos los PDFs/fotos binarios dentro del archivo principal de backup si eso lo hace excesivamente grande.

---

# 25. IA

No integrar IA dentro de la aplicación en esta fase.

ChatGPT seguirá utilizándose externamente para informes/análisis cuando sea necesario.

La arquitectura no debe impedir añadir IA en el futuro.

---

# 26. RFAF / RFAF TV

Primera versión:

- enlaces directos;
- ficha del equipo;
- plantilla en RFAF;
- partidos/RFAF TV;
- YouTube opcional;
- enlaces personalizados.

No scraping obligatorio.

No hacer que funciones críticas dependan de la estructura HTML de RFAF.

---

# 27. BÚSQUEDA

Implementar buscadores donde aporten valor.

Obligatorio inicialmente:

- Tareas → buscar por nombre.

Posibles buscadores útiles si el módulo ya lo necesita:

- Rivales;
- Jugadores;
- Partidos.

No implementar buscador global de toda la app todavía.

---

# 28. PRINCIPIOS DE ARQUITECTURA

## 28.1 No duplicar conceptos

Ejemplos:

- un partido no debe existir duplicado solo para mostrarlo en Calendario;
- un informe no debe duplicarse físicamente para aparecer en Rival;
- un archivo debe utilizar un sistema de adjuntos común;
- una tarea seleccionada en un entrenamiento debe relacionarse con la tarea original.

## 28.2 Fuente única de verdad

Definir claramente qué entidad posee cada dato.

## 28.3 Compatibilidad hacia atrás

Los datos actuales deben sobrevivir.

## 28.4 Offline first

Room sigue siendo esencial.

## 28.5 Cambios pequeños y verificables

Evitar refactors masivos sin necesidad.

---

# 29. REGLAS PARA CURSOR

Antes de modificar código:

1. Leer este archivo.
2. Inspeccionar los archivos relevantes.
3. Explicar brevemente el plan de la fase.
4. Identificar riesgos sobre datos actuales.
5. No tocar módulos fuera del alcance salvo necesidad técnica justificada.

Durante la implementación:

- no borrar funcionalidades existentes;
- no usar soluciones destructivas;
- no cambiar nombres/relaciones de entidades a ciegas;
- no inventar requisitos;
- no adelantarse a fases futuras;
- reutilizar componentes existentes cuando sean sólidos;
- refactorizar solo cuando aporte valor claro;
- mantener Kotlin/Compose/Room.

Al finalizar cada fase:

1. Enumerar archivos modificados.
2. Explicar cambios realizados.
3. Indicar migraciones creadas.
4. Indicar riesgos o limitaciones.
5. Explicar cómo probar manualmente.
6. Ejecutar tests disponibles.
7. Ejecutar build si el entorno lo permite.
8. No afirmar que algo funciona si no se ha podido probar.

---

# 30. GIT Y SEGURIDAD

Antes de cambios grandes:

- commit limpio;
- rama específica;
- backup de datos reales.

Ejemplo:

- `main` = versión estable.
- `develop-alhendin-2` = evolución.

Hacer commits por fase.

---

# 31. ORDEN GENERAL DE IMPLEMENTACIÓN

No ejecutar todo de golpe.

## FASE 0 — Blindaje

- backup;
- auditoría;
- corregir problemas críticos;
- eliminar migraciones destructivas;
- revisar clearAllTables;
- tests básicos.

## FASE 1 — Modelo de datos preparado para sync

- UUID/syncId;
- timestamps;
- metadata;
- deletedAt;
- migraciones no destructivas.

## FASE 2 — Usuarios

- Firebase Authentication;
- cuentas independientes;
- sesión persistente;
- sin registro público.

## FASE 3 — Sincronización

- Firestore;
- Room ↔ cloud;
- offline;
- last-write-wins;
- tombstones;
- primera migración.

## FASE 4 — Storage

- fotos;
- PDFs;
- documentos;
- migración de imágenes locales relevantes.

## FASE 5 — Responsive

- móvil;
- tablet;
- mantener funcionalidad.

## FASE 6 — Inicio / Todos los módulos

- conservar personalización;
- preferencias por usuario;
- Todos los módulos.

## FASE 7 — Rivales ampliado

- resumen;
- análisis;
- archivos;
- enlaces.

## FASE 8 — Calendario + entrenamientos

- calendario mensual;
- partidos automáticos;
- crear entrenamiento en día libre;
- relación opcional con rival;
- tareas;
- notas;
- archivos.

## FASE 9 — Tareas

- biblioteca;
- CRUD;
- buscador;
- pizarra asociada.

## FASE 10 — Pizarra

- persistencia;
- biblioteca;
- fichas azules/rojas;
- números;
- balón;
- conos;
- porterías;
- miniporterías;
- texto.

## FASE 11 — Informes

- adjuntar;
- consultar desde partido;
- consultar desde rival.

## FASE 12 — RFAF / enlaces

- URLs editables;
- apertura externa.

## FASE 13 — Pulido

- consistencia;
- estados vacíos;
- errores;
- rendimiento;
- accesibilidad;
- pruebas de regresión.

---

# 32. PRUEBAS CRÍTICAS OBLIGATORIAS

## Datos

- mismos jugadores antes/después de migración;
- mismas posiciones;
- mismos rivales;
- mismos estadios;
- mismas equipaciones;
- mismos colores;
- mismos escudos/fotos;
- mismos partidos;
- mismas estadísticas.

## Sync

- crear en dispositivo A → aparece en B;
- editar en B → aparece en A;
- eliminar → desaparece en ambos;
- modificar offline → sincroniza al volver conexión;
- conflicto → prevalece último cambio;
- no reaparecen datos eliminados.

## Archivos

- subir foto;
- verla en otro dispositivo;
- eliminar;
- no dejar referencias rotas.

## Calendario

- partidos existentes aparecen;
- día con partido no permite entrenamiento;
- día libre permite uno;
- entrenamiento puede editarse/borrarse.

## Tareas

- crear;
- buscar;
- editar;
- asociar a entrenamiento;
- asociar pizarra.

## Partido en directo

- no romper flujo actual;
- estadísticas siguen siendo correctas;
- funciona sin conexión.

---

# 33. OBJETIVO FINAL

AlhendinFC 2.0 debe convertirse en la herramienta central del cuerpo técnico del Alhendín CF.

Migue debe poder:

- abrir Calendario;
- entrar en un día;
- crear entrenamiento;
- asociar rival;
- seleccionar tareas;
- guardar notas;
- adjuntar fotos;
- revisar meses después lo trabajado.

Debe poder:

- consultar rivales;
- abrir RFAF;
- abrir RFAF TV;
- mantener un análisis táctico vivo;
- consultar informes históricos.

Debe poder:

- crear tareas;
- dibujarlas en pizarra;
- reutilizarlas.

El analista debe poder:

- utilizar partido en directo;
- gestionar estadísticas;
- subir informes;
- consultar la misma información compartida.

Todo debe:

- sincronizarse;
- seguir funcionando offline;
- conservar los datos reales actuales;
- funcionar correctamente en móvil y tablet;
- mantener la identidad del Alhendín CF.

---

# 34. RESTRICCIÓN FINAL

Este documento es la referencia funcional principal.

Si durante una fase aparece una decisión que contradice este documento:

**NO improvisar.**

Detener esa parte, explicar el conflicto y solicitar decisión antes de realizar un cambio irreversible o estructural.
