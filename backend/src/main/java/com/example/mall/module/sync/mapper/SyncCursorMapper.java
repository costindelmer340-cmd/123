package com.example.mall.module.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.module.sync.entity.SyncCursor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SyncCursorMapper extends BaseMapper<SyncCursor> {
}
