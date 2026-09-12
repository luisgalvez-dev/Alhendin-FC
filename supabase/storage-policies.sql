-- Bucket privado + RLS para AlhendinFC (Supabase Storage Free).
-- Pegar en SQL Editor del proyecto Supabase. No usa service_role en el cliente Android.
--
-- Identidad: Firebase Auth third-party (project_id alhendinfc).
-- Claims JWT requeridos (custom claims Firebase, merge en Admin SDK):
--   role = authenticated          → rol Postgres authenticated
--   alhendin_workspaces = ["…"]   → membership del object path
--
-- Path permitido:
--   workspaces/{workspaceId}/attachments/...
-- El {workspaceId} DEBE existir en auth.jwt()->'alhendin_workspaces'.
-- Un usuario con solo ["alhendin-dev"] no puede tocar workspaces/alhendin/...



drop policy if exists "alhendin_files_select" on storage.objects;
drop policy if exists "alhendin_files_insert" on storage.objects;
drop policy if exists "alhendin_files_update" on storage.objects;
drop policy if exists "alhendin_files_delete" on storage.objects;

create policy "alhendin_files_select"
on storage.objects
for select
to authenticated
using (
  bucket_id = 'alhendin-files'
  and coalesce(auth.jwt()->>'role', '') = 'authenticated'
  and coalesce(auth.jwt()->>'iss', '') = 'https://securetoken.google.com/alhendinfc'
  and coalesce(auth.jwt()->>'aud', '') = 'alhendinfc'
  and split_part(name, '/', 1) = 'workspaces'
  and split_part(name, '/', 3) = 'attachments'
  and coalesce(auth.jwt()->'alhendin_workspaces', '[]'::jsonb) ? split_part(name, '/', 2)
);

create policy "alhendin_files_insert"
on storage.objects
for insert
to authenticated
with check (
  bucket_id = 'alhendin-files'
  and coalesce(auth.jwt()->>'role', '') = 'authenticated'
  and coalesce(auth.jwt()->>'iss', '') = 'https://securetoken.google.com/alhendinfc'
  and coalesce(auth.jwt()->>'aud', '') = 'alhendinfc'
  and split_part(name, '/', 1) = 'workspaces'
  and split_part(name, '/', 3) = 'attachments'
  and coalesce(auth.jwt()->'alhendin_workspaces', '[]'::jsonb) ? split_part(name, '/', 2)
);

create policy "alhendin_files_update"
on storage.objects
for update
to authenticated
using (
  bucket_id = 'alhendin-files'
  and coalesce(auth.jwt()->>'role', '') = 'authenticated'
  and coalesce(auth.jwt()->>'iss', '') = 'https://securetoken.google.com/alhendinfc'
  and coalesce(auth.jwt()->>'aud', '') = 'alhendinfc'
  and split_part(name, '/', 1) = 'workspaces'
  and split_part(name, '/', 3) = 'attachments'
  and coalesce(auth.jwt()->'alhendin_workspaces', '[]'::jsonb) ? split_part(name, '/', 2)
)
with check (
  bucket_id = 'alhendin-files'
  and coalesce(auth.jwt()->>'role', '') = 'authenticated'
  and coalesce(auth.jwt()->>'iss', '') = 'https://securetoken.google.com/alhendinfc'
  and coalesce(auth.jwt()->>'aud', '') = 'alhendinfc'
  and split_part(name, '/', 1) = 'workspaces'
  and split_part(name, '/', 3) = 'attachments'
  and coalesce(auth.jwt()->'alhendin_workspaces', '[]'::jsonb) ? split_part(name, '/', 2)
);

create policy "alhendin_files_delete"
on storage.objects
for delete
to authenticated
using (
  bucket_id = 'alhendin-files'
  and coalesce(auth.jwt()->>'role', '') = 'authenticated'
  and coalesce(auth.jwt()->>'iss', '') = 'https://securetoken.google.com/alhendinfc'
  and coalesce(auth.jwt()->>'aud', '') = 'alhendinfc'
  and split_part(name, '/', 1) = 'workspaces'
  and split_part(name, '/', 3) = 'attachments'
  and coalesce(auth.jwt()->'alhendin_workspaces', '[]'::jsonb) ? split_part(name, '/', 2)
);
