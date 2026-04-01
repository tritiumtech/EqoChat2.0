/**
 * 用户会话管理模块
 * 
 * 负责管理用户的登录会话，实现单设备登录功能：
 * - 每个用户只能有一个有效的登录会话
 * - 新登录会自动使旧会话失效
 * - 通过 Redis 存储会话映射关系
 * 
 * @author EqoChat Team
 */
package com.eqochat.service;
