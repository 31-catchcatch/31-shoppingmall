package com.shoppingmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing // BaseTimeEntity 의 created_at / updated_at 자동 관리
@SpringBootApplication
@EnableScheduling
public class ShoppingmallBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingmallBackendApplication.class, args);
    }
}
