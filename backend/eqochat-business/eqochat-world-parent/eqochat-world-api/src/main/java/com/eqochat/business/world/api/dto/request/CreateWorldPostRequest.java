package com.eqochat.business.world.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateWorldPostRequest {

    @NotNull
    private Long actorSubjectId;

    @NotNull
    private SubjectType actorSubjectType;

    @Size(max = 8000)
    private String content;

    /**
     * TEXT | IMAGE | VIDEO
     */
    @Size(max = 20)
    private String mediaType;

    @Size(max = 1024)
    private String imageUrl;

    @Size(max = 1024)
    private String videoUrl;

    @Size(max = 50)
    private List<@Valid MentionedSubject> mentionedSubjects;

    @Data
    public static class MentionedSubject {
        @NotNull
        private Long subjectId;

        @NotNull
        private SubjectType subjectType;
    }
}
