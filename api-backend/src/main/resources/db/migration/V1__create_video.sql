create table if not exists video (
  id bigint primary key auto_increment,
  title varchar(128) not null,
  file_hash varchar(128) not null,
  uploader_id varchar(64) not null,
  deleted boolean not null default false,
  created_at datetime not null default current_timestamp,
  index idx_video_uploader_id (uploader_id),
  index idx_video_deleted_created (deleted, created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
