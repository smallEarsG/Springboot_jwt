package org.example.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitConfiguration {
    @Bean("emailQueue")  // 创建队列
    public Queue emailQueue(){
        return QueueBuilder
                .durable("email") //持久化队列名称email
                .build();
    }
}
