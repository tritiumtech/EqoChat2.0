package com.eqochat.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计字段自动填充处理器
 * 
 * 自动填充字段:
 * - create_time: 创建时间 (TIMESTAMP)
 * - update_time: 更新时间 (TIMESTAMP)  
 * - create_by: 创建人ID
 * - update_by: 更新人ID
 * - del_token: 删除标记 (0=有效, 非0=已删除)
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    
    private static final Long DEL_TOKEN_NOT_DELETED = 0L;
    
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("执行insertFill自动填充...");
        
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = UserContext.getCurrentUserOrSystem();
        
        // 填充 create_time (如果为空)
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        
        // 填充 update_time (如果为空)
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        
        // 填充 create_by - 当前登录用户ID
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        
        // 填充 update_by - 当前登录用户ID
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
        
        // 填充 del_token (如果为空)
        this.strictInsertFill(metaObject, "delToken", Long.class, DEL_TOKEN_NOT_DELETED);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("执行updateFill自动填充...");
        
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = UserContext.getCurrentUserOrSystem();
        
        // 填充 update_time
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        
        // 填充 update_by - 当前登录用户ID
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
    }
    
    /**
     * 生成删除标记（使用时间戳）- 用于自定义逻辑删除场景
     */
    public static Long generateDelToken() {
        return System.currentTimeMillis();
    }
}
