package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Account;
import org.example.entity.vo.request.ConfirmResetVo;
import org.example.entity.vo.request.EmailRegisterVo;
import org.example.entity.vo.request.EmailResetVo;
import org.example.mapper.AccountMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;


public interface AccountService extends IService<Account> , UserDetailsService {
    Account findAccountByNameOrEmail(String txt);
    String registerEmailVerifyCode(String type,String email,String ip);
    String registerEmailAccount(EmailRegisterVo registerVo);
    String resetConfirm(ConfirmResetVo vo);
    String resetEmailAccountPassword(EmailResetVo ov);
}
