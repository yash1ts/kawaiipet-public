-- Profiles are normally created by on_auth_user_created. Users who signed up before that
-- trigger existed (or were created without firing it) need a backfill.
insert into public.profiles (id)
select u.id
from auth.users u
where not exists (select 1 from public.profiles p where p.id = u.id)
on conflict (id) do nothing;
