create database if not exists mini_tiktok_auth
  default character set utf8mb4
  collate utf8mb4_unicode_ci;

create database if not exists mini_tiktok_api
  default character set utf8mb4
  collate utf8mb4_unicode_ci;

grant all privileges on mini_tiktok_auth.* to 'mini_tiktok'@'%';
grant all privileges on mini_tiktok_api.* to 'mini_tiktok'@'%';

flush privileges;
