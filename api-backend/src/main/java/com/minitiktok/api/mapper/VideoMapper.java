package com.minitiktok.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minitiktok.api.entity.Video;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {
}
