create table if not exists request_log (
  id bigint primary key auto_increment,
  user_id varchar(64),
  method varchar(16) not null,
  path varchar(512) not null,
  request_body text,
  response_body text,
  status_code int not null,
  duration_ms bigint not null,
  ip varchar(64),
  created_at datetime not null default current_timestamp,
  index idx_request_log_user_id (user_id),
  index idx_request_log_path (path),
  index idx_request_log_created_at (created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
