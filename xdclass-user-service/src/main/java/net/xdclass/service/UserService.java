package net.xdclass.service;

import net.xdclass.request.UserLoginRequest;
import net.xdclass.request.UserRegisterRequest;
import net.xdclass.util.JsonData;
import net.xdclass.vo.UserVO;

public interface UserService {

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    JsonData register(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param userLoginRequest
     * @return
     */
    JsonData login(UserLoginRequest userLoginRequest, String ip);

    /**
     * 查询用户详情
     * @return
     */
    UserVO findUserDetail();
}
