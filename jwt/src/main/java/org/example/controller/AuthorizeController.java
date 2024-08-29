package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.example.entity.RustBean;
import org.example.entity.vo.request.ConfirmResetVo;
import org.example.entity.vo.request.EmailRegisterVo;
import org.example.entity.vo.request.EmailResetVo;
import org.example.service.AccountService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthorizeController {
    @Resource
    AccountService service;

    @GetMapping("/ask-code")
    public RustBean<Void> askVerifyCode(@RequestParam @Email String email,
                                        @RequestParam @Pattern(regexp = "(register|reset)") String type

    , HttpServletRequest request){

      return this.messageHandle(() -> service.registerEmailVerifyCode(type,email,request.getRemoteAddr()))

    }
    @PostMapping("/register")
    public RustBean<Void> register(@RequestBody @Valid EmailRegisterVo vo){
//        return this.messageHandle(() ->service.registerEmailAccount(vo));
        return messageHandle(vo ,service::registerEmailAccount);
    }

    @PostMapping("/resetConfig")
    public RustBean<Void> resetConfirm(@RequestBody @Valid ConfirmResetVo vo){
//        return  this.messageHandle(()->service.resetConfirm(vo));
        return messageHandle(vo ,service::resetConfirm);
    }

    @PostMapping("/resetPassword")
    public  RustBean<Void> resetPassword(@RequestBody @Valid EmailResetVo vo){
        return  this.messageHandle(vo ,service::resetEmailAccountPassword);
    }
    private <T>  RustBean<Void> messageHandle(T vo, Function<T,String> function){
        return  messageHandle(()-> function.apply(vo));
    }
    private  RustBean<Void>  messageHandle(Supplier<String> action){
        String message = action.get();
        return message ==  null?RustBean.success() : RustBean.failure(400,message);
    }
}
