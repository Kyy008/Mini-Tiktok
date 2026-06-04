delete from oauth2_authorization_consent
where registered_client_id in (
  select id from oauth2_registered_client where client_id = 'tiktok-web'
);

delete from oauth2_authorization
where registered_client_id in (
  select id from oauth2_registered_client where client_id = 'tiktok-web'
);

delete from oauth2_registered_client
where client_id = 'tiktok-web';
