package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.UserContext;
import com.eqochat.dto.request.CreateWorldPostRequest;
import com.eqochat.dto.response.WorldPostResponse;
import com.eqochat.dto.response.WorldShareLinkResponse;
import com.eqochat.dto.response.WorldTopicResponse;
import com.eqochat.world.WorldService;
import com.eqochat.world.WorldUploadService;
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
    public ApiResponse<List<WorldPostResponse>> listPosts(@RequestParam(required = false) String sort,
                                                         @RequestParam(required = false) Long cursorId,
                                                         @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(worldService.listFeed(UserContext.requireCurrentUser(), sort, cursorId, limit));
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
    public ApiResponse<List<WorldTopicResponse>> listTopics(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(worldService.listTopics(UserContext.requireCurrentUser(), limit));
    }

    @GetMapping("/topics/{name}/posts")
    public ApiResponse<List<WorldPostResponse>> listTopicPosts(@PathVariable String name,
                                                               @RequestParam(required = false) Long cursorId,
                                                               @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(worldService.listTopicPosts(UserContext.requireCurrentUser(), name, cursorId, limit));
    }

    @PostMapping("/posts/{postId}/upvote")
    public ApiResponse<Map<String, Object>> toggleUpvote(@PathVariable Long postId) {
        boolean upvoted = worldService.toggleUpvote(UserContext.requireCurrentUser(), postId);
        return ApiResponse.success(Map.of("upvoted", upvoted));
    }

    @PostMapping("/topics/{name}/follow")
    public ApiResponse<Map<String, Object>> toggleFollow(@PathVariable String name) {
        boolean following = worldService.toggleTopicFollow(UserContext.requireCurrentUser(), name);
        return ApiResponse.success(Map.of("following", following));
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
}
