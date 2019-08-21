package org.bytecamp19.seckill4.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bytecamp19.seckill4.cache.message.CacheMessageListener;
import org.bytecamp19.seckill4.cache.LayeringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.UnknownHostException;

/**
 * Created by LLAP on 2019/8/16.
 * Copyright (c) 2019 LLAP. All rights reserved.
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(CacheRedisCaffeineProperties.class)
public class CacheRedisCaffeineAutoConfiguration {

    private CacheRedisCaffeineProperties cacheRedisCaffeineProperties;

    public CacheRedisCaffeineAutoConfiguration(CacheRedisCaffeineProperties cacheRedisCaffeineProperties) {
        this.cacheRedisCaffeineProperties = cacheRedisCaffeineProperties;
    }

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public LayeringCacheManager cacheManager(RedisTemplate<Object, Object> redisTemplate) {
        return new LayeringCacheManager(cacheRedisCaffeineProperties, redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(name = "stringKeyRedisTemplate")
    public RedisTemplate<Object, Object> stringKeyRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(name = "stringRedisTemplate")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = "stringIntegerRedisTemplate")
    public RedisTemplate<Object, Integer> stringIntegerRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Integer> template = new RedisTemplate<>();
        RedisSerializer<Integer> serializer = new RedisSerializer<Integer>() {
            @Override
            public byte[] serialize(Integer integer) throws SerializationException {
                return integer.toString().getBytes();
            }

            @Override
            public Integer deserialize(byte[] bytes) throws SerializationException {
                return Integer.valueOf(new String(bytes));
            }
        };
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setValueSerializer(serializer);
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<Object, Object> stringKeyRedisTemplate,
                                                                       LayeringCacheManager redisCaffeineCacheManager) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(stringKeyRedisTemplate.getConnectionFactory());
        CacheMessageListener cacheMessageListener = new CacheMessageListener(stringKeyRedisTemplate, redisCaffeineCacheManager);
        redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(cacheRedisCaffeineProperties.getRedis().getTopic()));
        return redisMessageListenerContainer;
    }
}
