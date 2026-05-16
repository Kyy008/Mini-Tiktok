create table if not exists video_like (
  id bigint primary key auto_increment,
  user_id varchar(64) not null,
  video_id bigint not null,
  created_at datetime not null default current_timestamp,
  unique key uk_video_like_user_video (user_id, video_id),
  index idx_video_like_video_id (video_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
