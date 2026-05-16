insert into users (
  username,
  password_hash,
  enabled,
  created_at,
  updated_at
)
select
  'demo',
  '$2y$10$NWdd7X01kXSvQMCYKgjfpujxSBVsnY/croLh3O8KsrG8Qb0YkWZWa',
  true,
  current_timestamp,
  current_timestamp
where not exists (
  select 1 from users where username = 'demo'
);
