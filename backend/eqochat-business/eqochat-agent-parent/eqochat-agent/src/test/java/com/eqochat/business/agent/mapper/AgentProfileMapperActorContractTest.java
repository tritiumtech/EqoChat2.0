package com.eqochat.business.agent.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AgentProfileMapperActorContractTest {

    @Test
    void activeLookupQueriesIgnoreRetiredAgentProfiles() throws Exception {
        assertThat(selectSql("findByDid", String.class)).contains("del_token = '0'");
        assertThat(selectSql("findByName", String.class)).contains("del_token = '0'");
        assertThat(selectSql("findActiveByOwnerId", Long.class)).contains("status = 'ACTIVE'", "del_token = '0'");
        assertThat(selectSql("countActiveByOwnerId", Long.class)).contains("status = 'ACTIVE'", "del_token = '0'");
    }

    private static String selectSql(String methodName, Class<?> parameterType) throws Exception {
        Method method = AgentProfileMapper.class.getMethod(methodName, parameterType);
        Select select = method.getAnnotation(Select.class);
        assertThat(select).isNotNull();
        return String.join(" ", select.value());
    }
}
