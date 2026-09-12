/**
 * Asigna custom claims Firebase para Supabase Storage (Third-Party Auth).
 *
 * Merge: lee custom claims actuales y añade/actualiza SOLO:
 *   role = authenticated
 *   alhendin_workspaces = [...]
 * El resto de claims existentes se conserva.
 *
 * DEV (por defecto):
 *   role = authenticated
 *   alhendin_workspaces = ["alhendin-dev"]
 *
 * Uso (local, una vez):
 *   cd tools
 *   npm install
 *   set GOOGLE_APPLICATION_CREDENTIALS=C:\ruta\serviceAccount.json
 *   node set-firebase-authenticated-role.mjs UID_MIGUE UID_ANALISTA
 *
 * Workspaces extra (futuro):
 *   node set-firebase-authenticated-role.mjs --workspaces=alhendin-dev,alhendin UID1 UID2
 *
 * No commitear el JSON de service account. No contiene passwords.
 * Después: cada usuario debe refrescar el Firebase ID token o volver a iniciar
 * sesión. La app fuerza un refresh la primera vez que usa Storage.
 */
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";

function parseArgs(argv) {
  let workspaces = ["alhendin-dev"];
  const uids = [];
  for (const raw of argv) {
    const arg = raw.trim();
    if (!arg) continue;
    if (arg.startsWith("--workspaces=")) {
      const parsed = arg
        .slice("--workspaces=".length)
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean);
      if (parsed.length === 0) {
        console.error("--workspaces no puede estar vacío");
        process.exit(1);
      }
      workspaces = parsed;
    } else if (arg.startsWith("-")) {
      console.error(`Argumento no reconocido: ${arg}`);
      process.exit(1);
    } else {
      uids.push(arg);
    }
  }
  return { uids, workspaces };
}

const { uids, workspaces } = parseArgs(process.argv.slice(2));
if (uids.length === 0) {
  console.error("Uso: node set-firebase-authenticated-role.mjs [--workspaces=alhendin-dev] UID [UID...]");
  process.exit(1);
}
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error("Define GOOGLE_APPLICATION_CREDENTIALS apuntando al JSON de service account (fuera de Git).");
  process.exit(1);
}

initializeApp({ credential: applicationDefault() });
const auth = getAuth();

for (const uid of uids) {
  const user = await auth.getUser(uid);
  const existing = { ...(user.customClaims ?? {}) };
  const preserved = Object.keys(existing).filter(
    (key) => key !== "role" && key !== "alhendin_workspaces"
  );
  const merged = {
    ...existing,
    role: "authenticated",
    alhendin_workspaces: workspaces
  };
  await auth.setCustomUserClaims(uid, merged);
  console.log(
    `OK ${uid} (${user.email ?? "sin email"}): role=authenticated alhendin_workspaces=${JSON.stringify(workspaces)}` +
      (preserved.length ? ` preserved=[${preserved.join(", ")}]` : "")
  );
}

console.log("Listo. Que cada usuario refresque el ID token o vuelva a iniciar sesión.");
