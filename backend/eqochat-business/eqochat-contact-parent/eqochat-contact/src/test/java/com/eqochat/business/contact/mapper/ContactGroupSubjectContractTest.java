package com.eqochat.business.contact.mapper;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.contact.entity.GroupMember;
import com.eqochat.business.contact.entity.GroupProfile;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ContactGroupSubjectContractTest {

    @Test
    void groupProfileOwnsSubjectAwareOwnerType() throws Exception {
        assertThat(GroupProfile.class.getDeclaredField("ownerType").getType()).isEqualTo(SubjectType.class);

        Method method = GroupProfileMapper.class.getMethod("findByOwner", Long.class, SubjectType.class);
        Select select = method.getAnnotation(Select.class);

        assertThat(select.value()).anySatisfy(sql -> assertThat(sql)
                .contains("owner_id = #{ownerId}")
                .contains("owner_type = #{ownerType}"));
        assertThat(GroupProfileMapper.class.getMethod("findByOwner", SubjectRef.class)
                .getReturnType()).isEqualTo(List.class);
    }

    @Test
    void groupMemberOwnsSubjectAwareMemberType() throws Exception {
        assertThat(GroupMember.class.getDeclaredField("memberId").getType()).isEqualTo(Long.class);
        assertThat(GroupMember.class.getDeclaredField("memberType").getType()).isEqualTo(SubjectType.class);

        Method findByMember = GroupMemberMapper.class.getMethod("findByMember", Long.class, SubjectType.class);
        Method findByGroupAndMember = GroupMemberMapper.class.getMethod(
                "findByGroupAndMember", Long.class, Long.class, SubjectType.class);
        Method isMember = GroupMemberMapper.class.getMethod("isMember", Long.class, Long.class, SubjectType.class);

        assertThat(findByMember.getAnnotation(Select.class).value()).anySatisfy(sql -> assertThat(sql)
                .contains("user_id = #{memberId}")
                .contains("member_type = #{memberType}"));
        assertThat(findByGroupAndMember.getAnnotation(Select.class).value()).anySatisfy(sql -> assertThat(sql)
                .contains("group_id = #{groupId}")
                .contains("user_id = #{memberId}")
                .contains("member_type = #{memberType}"));
        assertThat(isMember.getAnnotation(Select.class).value()).anySatisfy(sql -> assertThat(sql)
                .contains("user_id = #{memberId}")
                .contains("member_type = #{memberType}"));
        assertThat(GroupMemberMapper.class.getMethod("findByGroupAndMember", Long.class, SubjectRef.class)
                .getReturnType()).isEqualTo(Optional.class);
    }
}
