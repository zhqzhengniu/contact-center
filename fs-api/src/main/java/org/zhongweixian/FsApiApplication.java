package org.zhongweixian;


import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import io.minio.MinioClient;
import org.cti.cc.util.SnowflakeIdWorker;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.zhongweixian.cc.cache.CacheService;
import org.zhongweixian.cc.command.GroupHandler;
import org.zhongweixian.cc.fs.FsListen;
import org.zhongweixian.cc.tcp.TcpServer;
import org.zhongweixian.cc.websocket.WebSocketManager;

import java.io.Serializable;


@EnableDiscoveryClient
@EnableEncryptableProperties
@MapperScan("org.cti.cc.mapper")
@SpringBootApplication
public class FsApiApplication implements CommandLineRunner, ApplicationListener<ContextClosedEvent> {
    private Logger logger = LoggerFactory.getLogger(FsApiApplication.class);

    @Autowired
    private WebSocketManager webSocketManager;

    @Autowired
    private TcpServer tcpServer;

    @Autowired
    private FsListen fsListen;

    @Autowired
    private GroupHandler groupHandler;

    @Autowired
    private CacheService cacheService;

    @Value("${spring.instance.id}")
    private String instanceId;

    @Value("${spring.cloud.nacos.server-addr}")
    private String nacosAddr;

    @Autowired
    private LoadBalancerClient loadBalancerClient;


    @Bean
    public RedisTemplate<String, Serializable> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Serializable> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    @Bean
    public MinioClient minioClient(@Value("${minio.endpoint:}") String endpoint, @Value("${minio.access.key:}") String accessKey, @Value("${minio.secret.key:}") String secretKey) {
        try {
            return new MinioClient.Builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Bean
    public SnowflakeIdWorker snowflakeIdWorker() {
        return new SnowflakeIdWorker(0, 0);
    }


    public static void main(String[] args) {
        SpringApplication.run(FsApiApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        cacheService.initCompany();
        webSocketManager.start();
        tcpServer.start();
        fsListen.start();
    }


    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        cacheService.stop();
        webSocketManager.stop();
        tcpServer.stop();
        fsListen.stop();
        groupHandler.stop();
    }

    @Bean
    public RestTemplate restTemplate(@Value("${cdr.notify.connectTimeout:200}") Integer connectTimeout,
                                     @Value("${cdr.notify.readTimeout:500}") Integer readTimeout) {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(connectTimeout);
        simpleClientHttpRequestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(simpleClientHttpRequestFactory);
    }
}
