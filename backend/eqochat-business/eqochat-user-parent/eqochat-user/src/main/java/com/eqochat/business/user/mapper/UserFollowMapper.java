package com.eqochat.business.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.user.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
    
    @Select("SELECT * FROM user_follow WHERE follower_id = #{followerId} AND del_token = '0'")
    List<UserFollow> findByFollowerId(@Param("followerId") Long followerId);
    
    @Select("SELECT * FROM user_follow WHERE following_id = #{followingId} AND del_token = '0'")
    List<UserFollow> findByFollowingId(@Param("followingId") Long followingId);
    
    @Select("SELECT * FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId} AND del_token = '0' LIMIT 1")
    Optional<UserFollow> findByFollowerAndFollowing(
            @Param("followerId") Long followerId, 
            @Param("followingId") Long followingId);
    
    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{followerId} AND del_token = '0'")
    long countFollowing(@Param("followerId") Long followerId);
    
    @Select("SELECT COUNT(*) FROM user_follow WHERE following_id = #{followingId} AND del_token = '0'")
    long countFollowers(@Param("followingId") Long followingId);
    
    @Select("SELECT EXISTS(SELECT 1 FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId} AND del_token = '0')")
    boolean isFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}
