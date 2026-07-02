package com.eqochat.business.world.service.impl;

import com.eqochat.business.world.api.service.WorldPostStatsApi;
import com.eqochat.business.world.mapper.WorldPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorldPostStatsApiImpl implements WorldPostStatsApi {

    private final WorldPostMapper worldPostMapper;

    @Override
    public long countByAuthor(Long authorId, String authorType) {
        return worldPostMapper.countByAuthor(authorId, authorType);
    }
}
