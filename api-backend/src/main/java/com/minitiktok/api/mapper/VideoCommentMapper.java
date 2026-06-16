package com.minitiktok.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minitiktok.api.entity.VideoComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoCommentMapper extends BaseMapper<VideoComment> {
}
