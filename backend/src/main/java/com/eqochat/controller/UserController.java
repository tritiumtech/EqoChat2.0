package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.BizException;
import com.eqochat.common.UserContext;
import com.eqochat.dto.response.UserPublicProfileResponse;
import com.eqochat.dto.response.UserSearchResponse;
import com.eqochat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 通过账号搜索用户（精准匹配，自动识别类型）
     */
    @GetMapping("/search")
    public ApiResponse<UserSearchResponse> searchUserByAccount(
            @RequestParam String keyword
    ) {
        Long currentUserId = UserContext.requireCurrentUser();
        
        // 依次尝试 ID、手机号、邮箱三种类型
        String[] types = {"id", "phone", "email"};
        
        for (String type : types) {
            try {
                return ApiResponse.success(userService.searchUserByAccount(currentUserId, type, keyword));
            } catch (Exception e) {
                // 继续尝试下一种类型
                log.debug("Search by type {} failed: {}", type, e.getMessage());
            }
        }
        
        // 所有类型都失败，返回用户不存在
        throw BizException.of("user.not_found");
    }

    /**
     * 获取用户公开资料（非好友视角）
     */
    @GetMapping("/{userId}/public")
    public ApiResponse<UserPublicProfileResponse> getUserPublicProfile(@PathVariable Long userId) {
        Long currentUserId = UserContext.requireCurrentUser();
        return ApiResponse.success(userService.getUserPublicProfile(currentUserId, userId));
    }
}
