package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.DidDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DidDocumentMapper extends BaseMapper<DidDocument> {
    
    @Select("SELECT * FROM did_document WHERE did = #{did} AND del_token = '0' LIMIT 1")
    Optional<DidDocument> findByDid(@Param("did") String did);
    
    @Select("SELECT * FROM did_document WHERE controller_id = #{controllerId} AND del_token = '0'")
    List<DidDocument> findByControllerId(@Param("controllerId") Long controllerId);
    
    @Select("SELECT * FROM did_document WHERE did_method = #{method} AND del_token = '0'")
    List<DidDocument> findByMethod(@Param("method") String method);
    
    @Select("SELECT * FROM did_document WHERE is_active = true AND del_token = '0'")
    List<DidDocument> findAllActive();
    
    @Select("SELECT EXISTS(SELECT 1 FROM did_document WHERE did = #{did} AND del_token = '0')")
    boolean existsByDid(@Param("did") String did);
}
