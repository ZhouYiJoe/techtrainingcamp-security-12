package com.catchyou.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.catchyou.pojo.Log;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogMapper extends BaseMapper<Log> {
}
