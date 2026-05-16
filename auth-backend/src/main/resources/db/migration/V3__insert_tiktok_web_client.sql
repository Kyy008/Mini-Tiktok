insert into oauth2_registered_client (
  id,
  client_id,
  client_id_issued_at,
  client_secret,
  client_secret_expires_at,
  client_name,
  client_authentication_methods,
  authorization_grant_types,
  redirect_uris,
  post_logout_redirect_uris,
  scopes,
  client_settings,
  token_settings
)
select
  'tiktok-web',
  'tiktok-web',
  current_timestamp,
  null,
  null,
  'Mini-Tiktok Web',
  'none',
  'authorization_code',
  'http://localhost:5173/oauth/callback',
  '',
  'video:read,video:write,video:like',
  '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
  '{"settings.token.access-token-time-to-live":"PT2H","settings.token.authorization-code-time-to-live":"PT5M","settings.token.reuse-refresh-tokens":true}'
where not exists (
  select 1 from oauth2_registered_client where client_id = 'tiktok-web'
);
