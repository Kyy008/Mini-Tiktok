package com.minitiktok.api.mapper;

import com.minitiktok.api.dto.VideoRecommendationVO;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minitiktok.api.entity.Video;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {
    /**
     * 使用编写在注解中的动态 SQL 实现推荐核心逻辑：
     * 1. 过滤未删除的视频 (deleted = 0)
     * 2. 使用 NOT EXISTS 过滤当前用户已观看视频
     * 3. 统计每个视频的点赞数作为排序依据
     * 4. 计算当前用户是否点赞
     * 5. 按点赞数降序、创建时间降序排序
     */
    @SuppressWarnings("SqlResolve") // 禁用 IDE 对该方法内注解 SQL 的强行检查
    @Select("<script>" +
            "SELECT v.id, v.title, v.created_at AS createdAt, " +
            "       (SELECT COUNT(*) FROM video_like WHERE video_id = v.id) AS likeCount, " +
            "       CASE WHEN (SELECT COUNT(*) FROM video_like WHERE video_id = v.id AND user_id = #{userId}) > 0 THEN 1 ELSE 0 END AS liked, " +
            "       CONCAT('/api/videos/', v.id, '/play') AS playUrl " +
            "FROM video v " +
            "WHERE v.deleted = 0 " +
            "  AND NOT EXISTS (SELECT 1 FROM video_view vv WHERE vv.video_id = v.id AND vv.user_id = #{userId}) " +
            "ORDER BY likeCount DESC, v.created_at DESC " +
            "LIMIT #{size}" +
            "</script>")
    List<VideoRecommendationVO> findRecommendations(@Param("userId") String userId,
                                                    @Param("size") int size);
}
