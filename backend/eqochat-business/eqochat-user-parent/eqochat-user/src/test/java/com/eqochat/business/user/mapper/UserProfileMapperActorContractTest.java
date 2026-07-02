package com.eqochat.business.user.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileMapperActorContractTest {

    @Test
    void profileLookupQueriesIgnoreRetiredHumanAndMirrorProfiles() throws Exception {
        assertThat(selectSql("findByDid", String.class)).contains("del_token = '0'");
        assertThat(selectSql("findByPhone", String.class)).contains("del_token = '0'");
        assertThat(selectSql("findByEmail", String.class)).contains("del_token = '0'");
        assertThat(selectSql("existsByDid", String.class)).contains("del_token = '0'");
        assertThat(selectSql("existsByPhone", String.class)).contains("del_token = '0'");
    }

    private static String selectSql(String methodName, Class<?> parameterType) throws Exception {
        Method method = UserProfileMapper.class.getMethod(methodName, parameterType);
        Select select = method.getAnnotation(Select.class);
        assertThat(select).isNotNull();
        return String.join(" ", select.value());
    }
}
