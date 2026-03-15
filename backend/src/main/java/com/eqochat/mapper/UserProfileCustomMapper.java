package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.UserProfile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 自定义SQL示例（使用XML配置）
 */
public interface UserProfileCustomMapper {
    
    /**
     * 获取用户完整信息（包含统计数据）
     */
    UserProfile selectUserFullInfo(@Param("userId") Long userId);
    
    /**
     * 搜索用户
     */
    // 使用BaseMapper提供的分页方法
}
