package com.shoppingmall.domain.file.controller;

import com.shoppingmall.domain.file.dto.response.FileUploadResponse;
import com.shoppingmall.domain.file.service.FileStorageService;
import com.shoppingmall.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** API 명세서 "공통/인증 - 파일 - 파일/이미지 업로드 공통" */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.store(file);
        return ResponseEntity.ok(ApiResponse.success(new FileUploadResponse(fileUrl)));
    }
}
