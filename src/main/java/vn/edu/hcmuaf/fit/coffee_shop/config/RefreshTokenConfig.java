package vn.edu.hcmuaf.fit.coffee_shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class RefreshTokenConfig {

    @Value("${refresh.token.expiration}")
    private Long expiration;
}