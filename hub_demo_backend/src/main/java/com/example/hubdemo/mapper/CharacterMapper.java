package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hubdemo.entity.CharacterInfo;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CharacterMapper extends BaseMapper<CharacterInfo> {
    @Select("SELECT avatar FROM characters WHERE avatar IS NOT NULL AND avatar <> ''")
    List<String> selectReferencedAvatarUrls();
}
