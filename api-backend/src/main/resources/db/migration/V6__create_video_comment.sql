create table if not exists video_comment (
  id bigint primary key auto_increment,
  video_id bigint not null,
  user_id varchar(64) not null,
  username_snapshot varchar(64) not null,
  content varchar(1000) not null,
  created_at datetime not null default current_timestamp,
  index idx_video_comment_video_created (video_id, created_at),
  index idx_video_comment_user_id (user_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
