package com.shoppingmall;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * was-01(외부 Tomcat 10.1)에 WAR로 배포할 때 필요한 진입점.
 * ./gradlew bootWar 로 만든 ROOT.war 를
 * /opt/tomcat/webapps/ROOT.war 로 교체 배포한다.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ShoppingmallBackendApplication.class);
    }
}
