package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    @Select("SELECT * FROM notification WHERE recipient_id = #{recipientId} AND del_token = '0' ORDER BY create_time DESC")
    List<Notification> findByRecipientId(@Param("recipientId") Long recipientId);
    
    @Select("SELECT * FROM notification WHERE recipient_id = #{recipientId} AND is_read = false AND del_token = '0' ORDER BY create_time DESC")
    List<Notification> findUnreadByRecipientId(@Param("recipientId") Long recipientId);
    
    @Select("SELECT * FROM notification WHERE recipient_id = #{recipientId} AND notification_type = #{type} AND del_token = '0' ORDER BY create_time DESC")
    List<Notification> findByRecipientIdAndType(
            @Param("recipientId") Long recipientId, 
            @Param("type") String type);
    
    @Select("SELECT COUNT(*) FROM notification WHERE recipient_id = #{recipientId} AND is_read = false AND del_token = '0'")
    long countUnreadByRecipientId(@Param("recipientId") Long recipientId);
    
    @Update("UPDATE notification SET is_read = true, read_at = NOW() WHERE recipient_id = #{recipientId} AND is_read = false AND del_token = '0'")
    int markAllAsRead(@Param("recipientId") Long recipientId);
}
