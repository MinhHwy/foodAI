package com.foodai.backend.controller;

import com.foodai.backend.dto.request.ConfirmRecognitionItemRequest;
import com.foodai.backend.dto.request.CreateRecognitionSessionRequest;
import com.foodai.backend.dto.response.ApiResponse;
import com.foodai.backend.dto.response.RecognitionResponse;
import com.foodai.backend.service.CurrentFoodUser;
import com.foodai.backend.service.RecognitionService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BE-07. All endpoints require the authenticated user's verified Principal. */
@RestController
@RequestMapping("/api/v1/recognitions")
public class RecognitionController {
    private final RecognitionService service;
    private final CurrentFoodUser users;
    public RecognitionController(RecognitionService service, CurrentFoodUser users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<RecognitionResponse>> createSession(
            @Valid @RequestBody CreateRecognitionSessionRequest request, Principal principal) {
        RecognitionResponse response = service.create(request, users.require(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recognition session created.", response));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<RecognitionResponse> get(@PathVariable Long sessionId, Principal principal) {
        return ApiResponse.success("Recognition loaded.", service.get(sessionId, users.require(principal)));
    }

    @PatchMapping("/{sessionId}/items/{itemId}/review")
    public ApiResponse<RecognitionResponse> review(@PathVariable Long sessionId,
            @PathVariable Long itemId, @Valid @RequestBody ConfirmRecognitionItemRequest request,
            Principal principal) {
        return ApiResponse.success("Recognition item reviewed.",
                service.confirm(sessionId, itemId, request, users.require(principal)));
    }
}
