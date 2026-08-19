package com.shoppingmall.global.validation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * [1-1 조치] 리치텍스트 전용 화이트리스트 정제기.
 *
 * <p>허용 목록에 없는 태그·속성·프로토콜은 전부 제거된다.
 * (script, iframe, on* 이벤트 핸들러, javascript: 스킴 등이 자동 제거됨)
 *
 * <p>상품 상세 설명처럼 서식 태그가 실제로 필요한 필드에만 사용한다.
 * HTML 이 필요 없는 필드는 {@link NoHtml} 로 아예 차단하는 편이 낫다.
 */
public final class HtmlSanitizer {

    private static final Safelist POLICY = Safelist.basic()
            .addTags("h1", "h2", "h3", "img")
            .addAttributes("img", "src", "alt", "width", "height")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https");

    private HtmlSanitizer() {
    }

    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        // prettyPrint(false) 로 원문 줄바꿈·공백을 보존한다
        Document.OutputSettings output = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, "", POLICY, output);
    }
}
