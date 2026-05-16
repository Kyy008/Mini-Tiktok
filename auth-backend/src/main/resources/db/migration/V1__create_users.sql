create table if not exists users (
  id bigint primary key auto_increment,
  username varchar(64) not null,
  password_hash varchar(255) not null,
  enabled boolean not null default true,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint uk_users_username unique (username)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
