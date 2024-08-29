package org.example.listener;

import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RabbitListener(queues = "email") // 绑定要监听的队列
public class MailQueueListener {
    @Resource
    JavaMailSender sender;
    @Value("${spring.mail.username}")
    String username;

    @RabbitHandler  // 被标记的方法用来接收消息队列发送来的参数
    public void sendMailMessage(Map<String, Object> data){
        String email = data.get("email").toString();
        Integer code = (Integer) data.get("code");
        String type = data.get("type").toString();
        SimpleMailMessage message = switch (type){
            case "register" -> createMessage("欢迎注册","您的邮箱验证码为"+code,email);
            case "reset" -> createMessage("重置密码","您的邮箱验证码为"+code,email);
            default -> null;
        };
        if (message == null) return;
        sender.send(message);
    }
    private SimpleMailMessage createMessage(String title,String content, String email){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);
        message.setText(content);
        message.setTo(email);
        message.setFrom(username);
        return  message;
    }
}
