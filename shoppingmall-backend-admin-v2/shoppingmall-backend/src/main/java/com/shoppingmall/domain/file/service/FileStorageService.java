package com.shoppingmall.domain.file.service;

import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.validation.UploadFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * POST /api/v1/files/upload 담당.
 *
 * <p><b>[2-1 조치]</b> 기존에는 크기(≤20MB)와 빈 파일만 검사하고 확장자를 사용자 입력에서
 * 그대로 가져와 .jsp / .php / .html 업로드가 가능했다. 이제 UploadFileValidator 로
 * 확장자·MIME·매직바이트를 3중 검증하고, <b>저장 파일명의 확장자는 서버가 확정한 값</b>만 쓴다.
 *
 * <p>TODO(실제 연동 필요): 지금 인프라(was-01)에 S3 같은 오브젝트 스토리지가 없어서
 * 로컬 디스크에 저장하고, WebConfig 에서 /uploads/** 경로로 정적 서빙하도록 했다.
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
    private static final long MIN_FILE_SIZE = 100L;              // [2-1] 지나치게 작은 파일 차단

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_FILE_SIZE || file.getSize() < MIN_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // [2-1 조치] 확장자·MIME·매직바이트 3중 검증 후, 서버가 확장자를 확정한다.
        //            사용자가 보낸 파일명·확장자는 이 시점 이후 일절 사용하지 않는다.
        String extension = UploadFileValidator.validateAndResolveExtension(file);
        String storedFilename = UUID.randomUUID() + "." + extension;

        // 날짜별 폴더링 + UUID 파일명으로 충돌/한글파일명 문제 방지
        String datePath = LocalDate.now().toString().replace("-", "/");

        try {
            Path baseDir = Path.of(uploadDir).toAbsolutePath().normalize();
            Path targetDir = baseDir.resolve(datePath).normalize();

            // 경로 이탈 방어 (이중 안전장치)
            if (!targetDir.startsWith(baseDir)) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFilename);
            file.transferTo(targetPath);

            // [5-1 조치] 원본 파일명은 로그에 남기지 않는다
            log.info("파일 저장 완료: {}", storedFilename);
            return baseUrl + "/" + datePath + "/" + storedFilename;

        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
