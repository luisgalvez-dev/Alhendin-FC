# Firebase (AlhendinFC)

Authentication (email/contraseña) + Firestore. **No Storage** en esta fase.

- Flavor **dev** → workspace `alhendin-dev`
- Flavor **stable** → workspace `alhendin` (aún no usar en pruebas)

## Emulador (manual)

Desde la raíz del repo, con Firebase CLI:

```
firebase emulators:start --only auth,firestore
```

Reglas: `firebase/firestore.rules`. Índices: `firebase/firestore.indexes.json` (vacío: las queries listan colecciones enteras).

Los tests de reglas (`FirestoreRulesEmulatorTest`) están `@Ignore` hasta ejecutar el Emulator Suite a mano.

## Consola

1. Crear proyecto Firebase (no se inventa aquí el projectId).
2. Activar Authentication → Email/Password (sin registro público en la app).
3. Crear usuarios Migue y Analista en Authentication.
4. Firestore: documentos `workspaces/alhendin-dev/members/{uid}` (y `alhendin` en producción).
5. Copiar `google-services.json` de la app Android `com.luis.alhendinfc.dev` a `app/src/dev/google-services.json`. Stable no lleva JSON hasta registrar `com.luis.alhendinfc`; en ese flavor la app compila y muestra “Firebase no configurado”.
