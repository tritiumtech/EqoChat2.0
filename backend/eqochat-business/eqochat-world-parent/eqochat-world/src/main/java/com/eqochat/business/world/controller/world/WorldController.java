package com.eqochat.business.world.controller.world;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.PageResponse;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.world.api.dto.request.CreateWorldPostRequest;
import com.eqochat.business.world.api.dto.request.CreateWorldPostReplyRequest;
import com.eqochat.business.world.api.dto.response.WorldPostReplyResponse;
import com.eqochat.business.world.api.dto.response.WorldPostResponse;
import com.eqochat.business.world.api.dto.response.WorldShareLinkResponse;
import com.eqochat.business.world.api.dto.response.WorldTopicResponse;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.world.WorldService;
import com.eqochat.business.world.WorldUploadService;
import com.eqochat.framework.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/world")
@RequiredArgsConstructor
public class WorldController {

    private final WorldService worldService;
    private final WorldUploadService worldUploadService;

    @GetMapping("/posts")
    public ApiResponse<PageResponse<WorldPostResponse>> listPosts(@RequestParam(required = false) String sort,
                                                         @RequestParam(required = false) Long cursorId,
                                                         @RequestParam(required = false) Integer limit,
                                                         @RequestParam(required = false) Long viewerSubjectId,
                                                         @RequestParam(required = false) String viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef viewer = requireExplicitSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(worldService.listFeed(principalHumanId, viewer, sort, cursorId, limit));
    }

    /**
     * 某好友最近发布的动态（需互为好友）。
     */
    /**
     * Canonical subject author timeline. authorType must be HUMAN/AGENT/SYSTEM.
     */
    @GetMapping("/subjects/{authorType}/{authorId}/posts")
    public ApiResponse<PageResponse<WorldPostResponse>> listPostsBySubjectAuthor(@PathVariable String authorType,
                                                                                 @PathVariable Long authorId,
                                                                                 @RequestParam(required = false) Long cursorId,
                                                                                 @RequestParam(required = false) Integer limit,
                                                                                 @RequestParam(required = false) Long viewerSubjectId,
                                                                                 @RequestParam(required = false) String viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef viewer = requireExplicitSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(worldService.listPostsByAuthor(
                principalHumanId, viewer, new SubjectRef(authorId, parseSubjectType(authorType, "world.author.type.invalid")), cursorId, limit));
    }

    @PostMapping("/posts")
    public ApiResponse<WorldPostResponse> createPost(@Valid @RequestBody CreateWorldPostRequest request) {
        return ApiResponse.success(worldService.createPost(UserContext.requireCurrentUser(), request));
    }

    @GetMapping("/posts/{postId}/share-link")
    public ApiResponse<WorldShareLinkResponse> shareLink(@PathVariable Long postId) {
        return ApiResponse.success(worldService.shareLink(postId));
    }

    @GetMapping("/topics")
    public ApiResponse<PageResponse<WorldTopicResponse>> listTopics(@RequestParam(required = false) Integer limit,
                                                           @RequestParam(required = false) Long cursorId,
                                                           @RequestParam(required = false) Long viewerSubjectId,
                                                           @RequestParam(required = false) String viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef viewer = requireExplicitSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(worldService.listTopics(principalHumanId, viewer, limit, cursorId));
    }

    @GetMapping("/topics/{name}/posts")
    public ApiResponse<PageResponse<WorldPostResponse>> listTopicPosts(@PathVariable String name,
                                                               @RequestParam(required = false) Long cursorId,
                                                               @RequestParam(required = false) Integer limit,
                                                               @RequestParam(required = false) Long viewerSubjectId,
                                                               @RequestParam(required = false) String viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef viewer = requireExplicitSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(worldService.listTopicPosts(principalHumanId, viewer, name, cursorId, limit));
    }

    @GetMapping("/mentions")
    public ApiResponse<PageResponse<WorldPostResponse>> listMentionedMe(@RequestParam(required = false) Long cursorId,
                                                                @RequestParam(required = false) Integer limit,
                                                                @RequestParam(required = false) Long viewerSubjectId,
                                                                @RequestParam(required = false) String viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef viewer = requireExplicitSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(worldService.listMentionedMe(principalHumanId, viewer, cursorId, limit));
    }

    @GetMapping("/my-posts")
    public ApiResponse<PageResponse<WorldPostResponse>> listMyPosts(@RequestParam(required = false) Long cursorId,
                                                            @RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) Long viewerSubjectId,
                                                            @RequestParam(required = false) String viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef author = requireExplicitSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(worldService.listMyPosts(principalHumanId, author, cursorId, limit));
    }

    @PostMapping("/posts/{postId}/upvote")
    public ApiResponse<Map<String, Object>> toggleUpvote(@PathVariable Long postId,
                                                         @RequestParam(required = false) Long actorSubjectId,
                                                         @RequestParam(required = false) String actorSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef actor = requireExplicitSubject(actorSubjectId, actorSubjectType);
        boolean upvoted = worldService.toggleUpvote(principalHumanId, actor, postId);
        return ApiResponse.success(Map.of("upvoted", upvoted));
    }

    @PostMapping("/topics/{name}/follow")
    public ApiResponse<Map<String, Object>> toggleFollow(@PathVariable String name,
                                                         @RequestParam(required = false) Long actorSubjectId,
                                                         @RequestParam(required = false) String actorSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef actor = requireExplicitSubject(actorSubjectId, actorSubjectType);
        boolean following = worldService.toggleTopicFollow(principalHumanId, actor, name);
        return ApiResponse.success(Map.of("following", following));
    }

    @PostMapping("/posts/{postId}/replies")
    public ApiResponse<Map<String, Object>> createReply(@PathVariable Long postId,
                                                         @Valid @RequestBody CreateWorldPostReplyRequest request) {
        int replyCount = worldService.createReply(UserContext.requireCurrentUser(), postId, request);
        return ApiResponse.success(Map.of("replyCount", replyCount));
    }

    @PostMapping("/replies/{replyId}/upvote")
    public ApiResponse<Map<String, Object>> toggleReplyUpvote(@PathVariable Long replyId,
                                                              @RequestParam(required = false) Long actorSubjectId,
                                                              @RequestParam(required = false) String actorSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef actor = requireExplicitSubject(actorSubjectId, actorSubjectType);
        boolean upvoted = worldService.toggleReplyUpvote(principalHumanId, actor, replyId);
        return ApiResponse.success(Map.of("upvoted", upvoted));
    }

    @GetMapping("/posts/{postId}/replies")
    public ApiResponse<List<WorldPostReplyResponse>> listReplies(@PathVariable Long postId,
                                                                 @RequestParam(required = false) Long cursorId,
                                                                 @RequestParam(required = false) Integer limit,
                                                                 @RequestParam(required = false) Long viewerSubjectId,
                                                                 @RequestParam(required = false) String viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef viewer = requireExplicitSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(worldService.listReplies(principalHumanId, viewer, postId, cursorId, limit));
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file,
                                                     HttpServletRequest request) throws IOException {
        String url = worldUploadService.storeAndBuildAbsoluteUrl(file, request);
        return ApiResponse.success(Map.of("url", url));
    }

    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws IOException {
        Path path = worldUploadService.resolveStoredFile(filename);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        String ct = Files.probeContentType(path);
        MediaType mediaType = ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(resource);
    }

    private static SubjectRef requireExplicitSubject(Long subjectId, String subjectType) {
        if (subjectId == null || subjectType == null) {
            throw BizException.of("world.actor.invalid");
        }
        return new SubjectRef(subjectId, parseSubjectType(subjectType, "world.actor.invalid"));
    }

    private static SubjectType parseSubjectType(String subjectType, String errorCode) {
        try {
            return SubjectType.from(subjectType);
        } catch (RuntimeException ex) {
            throw BizException.of(errorCode);
        }
    }
}
