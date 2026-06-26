package com.example.mall.module.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.module.platform.entity.ExternalAuthToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExternalAuthTokenMapper extends BaseMapper<ExternalAuthToken> {
}
