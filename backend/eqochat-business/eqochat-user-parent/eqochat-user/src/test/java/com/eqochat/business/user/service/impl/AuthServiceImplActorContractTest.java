package com.eqochat.business.user.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.chat.api.session.UserSessionApi;
import com.eqochat.business.user.api.dto.response.UserInfoResponse;
import com.eqochat.business.user.api.service.UserProfileService;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.framework.security.JwtTokenUtil;
import com.eqochat.framework.sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplActorContractTest {

    @Mock
    UserProfileService userProfileService;
    @Mock
    JwtTokenUtil jwtTokenUtil;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    SmsSender smsSender;
    @Mock
    UserSessionApi userSessionApi;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;

    @Test
    void getUserInfoUsesCanonicalSubjectPointsAndCreditProfile() {
        UserProfile user = user(7L, 50);
        when(userProfileService.getById(7L)).thenReturn(user);
        when(subjectDirectoryApi.getSubject(SubjectRef.human(7L)))
                .thenReturn(SubjectSummaryResponse.builder()
                        .id(7L)
                        .type(SubjectType.HUMAN)
                        .points(425)
                        .creditScore(612)
                        .build());

        UserInfoResponse response = service().getUserInfo(7L);

        assertThat(response.getPoints()).isEqualTo(425);
        assertThat(response.getCreditScore()).isEqualTo(612);
    }

    @Test
    void getUserInfoDoesNotFallbackPointsToLegacyCreditScore() {
        UserProfile user = user(8L, 50);
        when(userProfileService.getById(8L)).thenReturn(user);
        when(subjectDirectoryApi.getSubject(SubjectRef.human(8L))).thenReturn(null);

        UserInfoResponse response = service().getUserInfo(8L);

        assertThat(response.getPoints()).isZero();
        assertThat(response.getCreditScore()).isEqualTo(575);
    }

    private AuthServiceImpl service() {
        return new AuthServiceImpl(
                userProfileService,
                jwtTokenUtil,
                passwordEncoder,
                redisTemplate,
                smsSender,
                userSessionApi,
                subjectDirectoryApi
        );
    }

    private static UserProfile user(Long id, Integer creditScore) {
        return UserProfile.builder()
                .id(id)
                .did("did:eqochat:user:" + id)
                .phone("1390000000" + id)
                .nickname("Human " + id)
                .status(UserProfile.UserStatus.ACTIVE)
                .creditScore(creditScore)
                .build();
    }
}
