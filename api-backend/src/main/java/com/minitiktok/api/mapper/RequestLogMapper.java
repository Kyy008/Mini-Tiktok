package com.minitiktok.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minitiktok.api.entity.RequestLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLog> {
}
