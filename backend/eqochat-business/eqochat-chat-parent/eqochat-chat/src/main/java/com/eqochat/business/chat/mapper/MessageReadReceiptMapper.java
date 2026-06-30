package com.eqochat.business.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.entity.MessageReadReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MessageReadReceiptMapper extends BaseMapper<MessageReadReceipt> {
    
    @Select("SELECT * FROM message_read_receipt WHERE message_id = #{messageId} AND del_token = '0'")
    List<MessageReadReceipt> findByMessageId(@Param("messageId") Long messageId);
    
    @Select("""
            SELECT * FROM message_read_receipt
            WHERE reader_id = #{readerId}
              AND reader_type = #{readerType}
              AND del_token = '0'
            """)
    List<MessageReadReceipt> findByReader(
            @Param("readerId") Long readerId,
            @Param("readerType") SubjectType readerType);
    
    @Select("""
            SELECT * FROM message_read_receipt
            WHERE message_id = #{messageId}
              AND reader_id = #{readerId}
              AND reader_type = #{readerType}
              AND del_token = '0'
            LIMIT 1
            """)
    Optional<MessageReadReceipt> findByMessageAndReader(
            @Param("messageId") Long messageId, 
            @Param("readerId") Long readerId,
            @Param("readerType") SubjectType readerType);
    
    @Select("SELECT COUNT(*) FROM message_read_receipt WHERE message_id = #{messageId} AND del_token = '0'")
    long countByMessageId(@Param("messageId") Long messageId);
}
