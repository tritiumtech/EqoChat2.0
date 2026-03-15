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
 * - del_token: 删除标记 (0=未删除, UUID=已删除)
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    
    private static final String DEL_TOKEN_NOT_DELETED = "0";
    
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
        this.strictInsertFill(metaObject, "delToken", String.class, DEL_TOKEN_NOT_DELETED);
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
     * 生成删除标记UUID
     */
    public static String generateDelToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 检查是否已删除
     */
    public static boolean isDeleted(String delToken) {
        return delToken != null && !DEL_TOKEN_NOT_DELETED.equals(delToken);
    }
}
