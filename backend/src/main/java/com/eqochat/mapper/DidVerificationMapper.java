package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.DidVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DidVerificationMapper extends BaseMapper<DidVerification> {
    
    @Select("SELECT * FROM did_verification WHERE did = #{did} AND del_token = '0' ORDER BY create_time DESC")
    List<DidVerification> findByDid(@Param("did") String did);
    
    @Select("SELECT * FROM did_verification WHERE did = #{did} AND verification_type = #{type} AND del_token = '0' ORDER BY verified_at DESC LIMIT 1")
    Optional<DidVerification> findLatestByDidAndType(
            @Param("did") String did, 
            @Param("type") String type);
    
    @Select("SELECT * FROM did_verification WHERE verifier_id = #{verifierId} AND del_token = '0' ORDER BY create_time DESC")
    List<DidVerification> findByVerifierId(@Param("verifierId") Long verifierId);
    
    @Select("SELECT * FROM did_verification WHERE expires_at < NOW() AND del_token = '0'")
    List<DidVerification> findExpired();
    
    @Select("SELECT COUNT(*) FROM did_verification WHERE did = #{did} AND del_token = '0'")
    long countByDid(@Param("did") String did);
}
