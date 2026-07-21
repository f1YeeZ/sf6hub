package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hubdemo.entity.ComboLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ComboLikeMapper extends BaseMapper<ComboLike> {
    @Select("SELECT pg_advisory_xact_lock(hashtextextended(CONCAT(#{comboId}, ':', #{userId}), 0))")
    void lockInteraction(@Param("comboId") Long comboId, @Param("userId") Long userId);
}
