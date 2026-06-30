package com.eqochat.business.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    @Select("""
            SELECT *
            FROM notification
            WHERE recipient_id = #{recipientId}
              AND recipient_type = #{recipientType}
              AND del_token = '0'
            ORDER BY create_time DESC
            """)
    List<Notification> findByRecipient(@Param("recipientId") Long recipientId,
                                        @Param("recipientType") String recipientType);
    
    @Select("""
            SELECT *
            FROM notification
            WHERE recipient_id = #{recipientId}
              AND recipient_type = #{recipientType}
              AND is_read = false
              AND del_token = '0'
            ORDER BY create_time DESC
            """)
    List<Notification> findUnreadByRecipient(@Param("recipientId") Long recipientId,
                                             @Param("recipientType") String recipientType);
    
    @Select("""
            SELECT *
            FROM notification
            WHERE recipient_id = #{recipientId}
              AND recipient_type = #{recipientType}
              AND notification_type = #{type}
              AND del_token = '0'
            ORDER BY create_time DESC
            """)
    List<Notification> findByRecipientAndType(
            @Param("recipientId") Long recipientId, 
            @Param("recipientType") String recipientType,
            @Param("type") String type);
    
    @Select("""
            SELECT COUNT(*)
            FROM notification
            WHERE recipient_id = #{recipientId}
              AND recipient_type = #{recipientType}
              AND is_read = false
              AND del_token = '0'
            """)
    long countUnreadByRecipient(@Param("recipientId") Long recipientId,
                                @Param("recipientType") String recipientType);
    
    @Select("""
            SELECT *
            FROM notification
            WHERE id = #{notificationId}
              AND recipient_id = #{recipientId}
              AND recipient_type = #{recipientType}
              AND del_token = '0'
            LIMIT 1
            """)
    Notification findByIdForRecipient(@Param("notificationId") Long notificationId,
                                      @Param("recipientId") Long recipientId,
                                      @Param("recipientType") String recipientType);

    @Update("""
            UPDATE notification
            SET is_read = true,
                read_at = NOW()
            WHERE id = #{notificationId}
              AND recipient_id = #{recipientId}
              AND recipient_type = #{recipientType}
              AND is_read = false
              AND del_token = '0'
            """)
    int markReadForRecipient(@Param("notificationId") Long notificationId,
                             @Param("recipientId") Long recipientId,
                             @Param("recipientType") String recipientType);

    @Update("""
            UPDATE notification
            SET is_read = true,
                read_at = NOW()
            WHERE recipient_id = #{recipientId}
              AND recipient_type = #{recipientType}
              AND is_read = false
              AND del_token = '0'
            """)
    int markAllAsRead(@Param("recipientId") Long recipientId,
                      @Param("recipientType") String recipientType);
}
