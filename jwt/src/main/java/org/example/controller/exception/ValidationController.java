package org.example.controller.exception;

import jakarta.validation.ValidationException;
import org.example.entity.RustBean;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ValidationController {
    @ExceptionHandler(ValidationException.class) //ExceptionHandler  处理特定类型的异常
    public RustBean<Void> validateException(ValidationController exception){

        return  RustBean.failure(400,"请求参数有误");
    }

}
