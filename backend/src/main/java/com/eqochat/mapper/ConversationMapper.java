package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
