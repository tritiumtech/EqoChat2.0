package com.eqochat.business.world.api.service;

/**
 * 供其他业务模块查询 World 侧统计（避免跨模块直接依赖 Mapper）。
 */
public interface WorldPostStatsApi {

    long countByAuthor(Long authorId, String authorType);
}
