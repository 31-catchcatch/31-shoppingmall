package com.shoppingmall.domain.file.service;

import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * POST /api/v1/files/upload 담당.
 *
 * TODO(실제 연동 필요): 지금 인프라(was-01)에 S3 같은 오브젝트 스토리지가 없어서
 * was-01 로컬 디스크에 저장하고, WebConfig 에서 /uploads/** 경로로 정적 서빙하도록 했다.
 * 나중에 S3/CDN 붙이게 되면 이 클래스 구현만 갈아끼우면 되고, 컨트롤러/DTO는 그대로 써도 된다.
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:/uploads}")
    private String baseUrl;

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024; // 20MB, application.yml의 multipart 설정과 동일하게

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        // 날짜별 폴더링 + UUID 파일명으로 충돌/한글파일명 문제 방지
        String datePath = LocalDate.now().toString().replace("-", "/");
        String storedFilename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Path.of(uploadDir, datePath);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFilename);
            file.transferTo(targetPath);

            log.info("파일 저장 완료: {}", targetPath);
            return baseUrl + "/" + datePath + "/" + storedFilename;
        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
