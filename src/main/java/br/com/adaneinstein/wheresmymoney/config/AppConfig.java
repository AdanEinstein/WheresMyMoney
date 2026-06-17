package br.com.adaneinstein.wheresmymoney.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
}
