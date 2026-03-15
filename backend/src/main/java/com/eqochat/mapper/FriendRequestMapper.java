package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
    
    @Select("SELECT * FROM friend_request WHERE requester_id = #{requesterId} AND del_token = '0' ORDER BY create_time DESC")
    List<FriendRequest> findByRequesterId(@Param("requesterId") Long requesterId);
    
    @Select("SELECT * FROM friend_request WHERE recipient_id = #{recipientId} AND del_token = '0' ORDER BY create_time DESC")
    List<FriendRequest> findByRecipientId(@Param("recipientId") Long recipientId);
    
    @Select("SELECT * FROM friend_request WHERE requester_id = #{requesterId} AND recipient_id = #{recipientId} AND status = 'PENDING' AND del_token = '0' LIMIT 1")
    Optional<FriendRequest> findPendingRequest(
            @Param("requesterId") Long requesterId, 
            @Param("recipientId") Long recipientId);
    
    @Select("SELECT * FROM friend_request WHERE recipient_id = #{recipientId} AND status = 'PENDING' AND del_token = '0' ORDER BY create_time DESC")
    List<FriendRequest> findPendingByRecipientId(@Param("recipientId") Long recipientId);
    
    @Select("SELECT COUNT(*) FROM friend_request WHERE recipient_id = #{recipientId} AND status = 'PENDING' AND del_token = '0'")
    long countPendingByRecipientId(@Param("recipientId") Long recipientId);
}
