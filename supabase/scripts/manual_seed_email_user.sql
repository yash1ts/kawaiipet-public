-- One-off: create a confirmed email/password user in auth (and profiles via trigger).
--
-- HOW TO RUN
-- 1. Open Supabase Dashboard → SQL Editor for project iutndoomufkwzilxjfhz (or your linked project).
-- 2. Replace v_plain_password below with the password you want to use for the app (then run once).
-- 3. Do NOT commit real passwords into git.
--
-- If this email already exists in Authentication → Users, delete that user first or skip this script.

create extension if not exists "pgcrypto";

do $$
declare
  v_user_id uuid := gen_random_uuid();
  v_email text := 'yashwantsandey2000@gmail.com';
  -- Replace before running:
  v_plain_password text := 'REPLACE_ME_WITH_YOUR_PASSWORD';
  v_encrypted_pw text;
  v_instance_id uuid;
begin
  if v_plain_password = 'REPLACE_ME_WITH_YOUR_PASSWORD' then
    raise exception 'Edit v_plain_password in this script before running.';
  end if;

  if exists (select 1 from auth.users where lower(email) = lower(v_email)) then
    raise exception 'User % already exists. Remove them in Dashboard → Authentication → Users first.', v_email;
  end if;

  v_encrypted_pw := crypt(v_plain_password, gen_salt('bf'));

  select coalesce(
    (select id from auth.instances order by created_at nulls last limit 1),
    '00000000-0000-0000-0000-000000000000'::uuid
  ) into v_instance_id;

  insert into auth.users (
    id,
    instance_id,
    aud,
    role,
    email,
    encrypted_password,
    email_confirmed_at,
    raw_app_meta_data,
    raw_user_meta_data,
    created_at,
    updated_at,
    confirmation_token,
    recovery_token,
    email_change_token_new,
    email_change
  )
  values (
    v_user_id,
    v_instance_id,
    'authenticated',
    'authenticated',
    v_email,
    v_encrypted_pw,
    now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{}'::jsonb,
    now(),
    now(),
    '',
    '',
    '',
    ''
  );

  insert into auth.identities (
    id,
    user_id,
    identity_data,
    provider,
    provider_id,
    last_sign_in_at,
    created_at,
    updated_at
  )
  values (
    gen_random_uuid(),
    v_user_id,
    jsonb_build_object(
      'sub', v_user_id::text,
      'email', v_email,
      'email_verified', true
    ),
    'email',
    v_email,
    now(),
    now(),
    now()
  );

  raise notice 'Created user % (id %). Profile row should exist via on_auth_user_created trigger.', v_email, v_user_id;
end $$;
