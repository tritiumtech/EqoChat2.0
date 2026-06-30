package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.contact.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
    
    @Select("SELECT * FROM friend_request WHERE requester_id = #{requesterId} AND requester_type = #{requesterType} AND del_token = '0' ORDER BY create_time DESC")
    List<FriendRequest> findByRequesterSubject(
            @Param("requesterId") Long requesterId,
            @Param("requesterType") SubjectType requesterType);
    
    @Select("SELECT * FROM friend_request WHERE recipient_id = #{recipientId} AND recipient_type = #{recipientType} AND del_token = '0' ORDER BY create_time DESC")
    List<FriendRequest> findByRecipientSubject(
            @Param("recipientId") Long recipientId,
            @Param("recipientType") SubjectType recipientType);
    
    @Select("SELECT * FROM friend_request WHERE requester_id = #{requesterId} AND requester_type = #{requesterType} AND recipient_id = #{recipientId} AND recipient_type = #{recipientType} AND status = 'PENDING' AND del_token = '0' LIMIT 1")
    Optional<FriendRequest> findPendingRequest(
            @Param("requesterId") Long requesterId,
            @Param("requesterType") SubjectType requesterType,
            @Param("recipientId") Long recipientId,
            @Param("recipientType") SubjectType recipientType);
    
    @Select("SELECT * FROM friend_request WHERE recipient_id = #{recipientId} AND recipient_type = #{recipientType} AND status = 'PENDING' AND del_token = '0' ORDER BY create_time DESC")
    List<FriendRequest> findPendingByRecipientSubject(
            @Param("recipientId") Long recipientId,
            @Param("recipientType") SubjectType recipientType);
    
    @Select("SELECT COUNT(*) FROM friend_request WHERE recipient_id = #{recipientId} AND recipient_type = #{recipientType} AND status = 'PENDING' AND del_token = '0'")
    long countPendingByRecipientSubject(
            @Param("recipientId") Long recipientId,
            @Param("recipientType") SubjectType recipientType);
}
