package net.xdclass.service;

import net.xdclass.enums.SendCodeEnum;
import net.xdclass.request.UserRegisterRequest;
import net.xdclass.util.JsonData;
import org.springframework.stereotype.Service;

public interface NotifyService {
    /**
     * 发送验证码
     *
     * @param sendCodeEnum
     * @param to
     * @return
     */
    JsonData sendCode(SendCodeEnum sendCodeEnum, String to);


    /**
     * 验证验证码
     *
     * @param sendCodeEnum
     * @param to
     * @param code
     * @return
     */
    boolean checkCode(SendCodeEnum sendCodeEnum, String to, String code);
}
