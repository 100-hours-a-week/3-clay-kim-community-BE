package kr.kakaotech.community.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 연결 설정
        template.setConnectionFactory(connectionFactory);

        // JavaTimeModule 등록된 ObjectMapper 생성
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO 8601 포맷 유지

        // 커스텀 ObjectMapper로 직렬화기 생성
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Key는 문자열로
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value는 JSON 직렬화로 (Jackson)
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        // enableTransactionSupport를 끄면 @Transactional 내에서도 Redis 읽기가 즉시 반환됨
        // 켜면 MULTI/EXEC로 큐잉되어 get()이 null을 반환하는 문제 발생
        template.afterPropertiesSet();

        return template;
    }
}
