package com.example.demo.shared.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        ObjectMapper apiObjectMapper = new ObjectMapper();
        apiObjectMapper.registerModule(new JavaTimeModule());
        apiObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(apiObjectMapper));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(apiObjectMapper));
    }
}
