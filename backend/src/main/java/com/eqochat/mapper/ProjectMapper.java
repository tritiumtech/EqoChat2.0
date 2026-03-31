package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}

