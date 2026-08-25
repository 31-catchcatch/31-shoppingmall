package com.shoppingmall.global.validation;

import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * [2-1 조치] 업로드 파일 3중 검증.
 *
 * <ol>
 *   <li>확장자 화이트리스트 (대소문자 무시, 이중 확장자·끝점 우회 차단)</li>
 *   <li>선언된 Content-Type 화이트리스트</li>
 *   <li>실제 매직바이트(시그니처)와 확장자 일치 여부</li>
 * </ol>
 *
 * <p>세 가지를 모두 통과해야 저장한다. 저장 파일명의 확장자는 사용자 입력이 아니라
 * 이 클래스가 확정한 값을 쓴다.
 *
 * <p>.svg 는 허용하지 않는다 — 이미지처럼 보이지만 &lt;script&gt; 를 담을 수 있어
 * 저장형 XSS 벡터가 된다.
 */
public final class UploadFileValidator {

    /** 업무상 필요한 이미지 형식만 허용한다. 확장자를 추가할 때는 MAGIC 에도 함께 등록할 것. */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    /** 확장자 -> 파일 선두 시그니처. 하나라도 일치하면 통과. */
    private static final Map<String, byte[][]> MAGIC = Map.of(
            "jpg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
            "jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
            "png", new byte[][]{{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}},
            "gif", new byte[][]{{'G', 'I', 'F', '8', '7', 'a'}, {'G', 'I', 'F', '8', '9', 'a'}},
            // WebP 는 RIFF 뒤 8~11 byte 의 "WEBP" 까지 확인한다 (matchesMagic 참고)
            "webp", new byte[][]{{'R', 'I', 'F', 'F'}});

    private UploadFileValidator() {
    }

    /**
     * @return 검증을 통과한 정규화 확장자 (소문자, 점 없음). 저장 파일명은 이 값으로 만든다.
     */
    public static String validateAndResolveExtension(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());

        // 1) 확장자 화이트리스트
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }

        // 2) 선언 Content-Type 화이트리스트
        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }

        // 3) 실제 매직바이트 검증 (선언값 위장 차단)
        if (!matchesMagic(file, extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_CONTENT);
        }

        return extension;
    }

    /**
     * 마지막 점 뒤만 확장자로 인정한다.
     * <pre>
     *   "evil.jpg.jsp" -> "jsp"  (이중 확장자 차단)
     *   "evil.php."    -> 예외    (끝점 트릭 차단)
     *   "EVIL.PNG"     -> "png"  (대소문자 무시)
     * </pre>
     */
    private static String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        // 경로 구분자가 섞여 들어오면 파일명만 취한다
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);

        // 널바이트 등 제어문자 차단
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
            }
        }

        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean matchesMagic(MultipartFile file, String extension) {
        byte[][] signatures = MAGIC.get(extension);
        if (signatures == null) {
            return false;
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);

            for (byte[] signature : signatures) {
                if (startsWith(head, signature)) {
                    if ("webp".equals(extension)) {
                        return head.length >= 12
                                && head[8] == 'W' && head[9] == 'E'
                                && head[10] == 'B' && head[11] == 'P';
                    }
                    return true;
                }
            }
            return false;

        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_FILE_CONTENT);
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
