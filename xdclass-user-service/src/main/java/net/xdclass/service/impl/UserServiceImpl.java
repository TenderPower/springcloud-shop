package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.SendCodeEnum;
import net.xdclass.feign.CouponFeignService;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.UserMapper;
import net.xdclass.model.LoginUser;
import net.xdclass.model.UserDO;
import net.xdclass.request.NewUserRequest;
import net.xdclass.request.UserLoginRequest;
import net.xdclass.request.UserRegisterRequest;
import net.xdclass.service.NotifyService;
import net.xdclass.service.UserService;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JWTUtil;
import net.xdclass.util.JsonData;
import net.xdclass.vo.UserVO;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.json.Json;

import javax.print.attribute.standard.JobSheets;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CouponFeignService couponFeignService;

    /**
     * 用户注册
     * <p>
     * 邮箱验证码验证
     * 密码加密 --- 使用MD5+盐 （当然，你也可以双重MD5 、 双重MD5+盐）
     * 账号唯一性检查(TODO)
     * 插入数据库
     * 新注册用户福利发放(TODO)
     *
     * @param userRegisterRequest
     * @return
     */
    @Override
//    TM入口service的方法增加 @GlobalTransactional 注解（使用的是Seata）
//    @GlobalTransactional
    public JsonData register(UserRegisterRequest userRegisterRequest) {

        boolean checkCode = false;
//        1.邮箱验证码验证
        if (StringUtils.isNotBlank(userRegisterRequest.getMail())) {
            checkCode = notifyService.checkCode(SendCodeEnum.USER_REGISTER, userRegisterRequest.getMail(), userRegisterRequest.getCode());
        }
        if (!checkCode) {
            return JsonData.buildResult(BizCodeEnum.CODE_ERROR);
        }

//        2.注册用户
        UserDO userDO = new UserDO();
//        BeanUtils.copyProperties 是一个 Apache Commons Lang 库中的一个工具类方法，
//        用于将一个 JavaBean 对象的属性值复制到另一个 JavaBean 对象中。
//        具体来说，该方法会遍历源对象的所有属性，并将其属性值复制到目标对象的对应属性中。如果源对象和目标对象的属性名称或类型不匹配，则会忽略该属性。
        BeanUtils.copyProperties(userRegisterRequest, userDO);

        userDO.setCreateTime(new Date());
//        userDO.setSlogan("加油");

//        3.设置密码 ------ 密码加密 ---- 密码存储常用方式中的 MD5+加盐
//        3.1 生成密钥
        userDO.setSecret("$1$" + CommonUtil.getStringNumRandom(8));
//        3.2 密码 + 加盐(密钥)
        String cryptPwd = Md5Crypt.md5Crypt(userRegisterRequest.getPwd().getBytes(), userDO.getSecret());
//        3.3 密码
        userDO.setPwd(cryptPwd);

//        4.账号唯一性检查
        if (checkUnique(userDO.getMail())) {
            //        5.插入数据库
            int row = userMapper.insert(userDO);
            log.info("row:{},注册成功：{}", row, userDO.toString());

            //        用户初始化成功后，发放福利 TODO
            userRegisterInitTask(userDO);
            //        模拟异常
//            int b = 1 / 0;
            return JsonData.buildSuccess();
        } else {
            return JsonData.buildResult(BizCodeEnum.ACCOUNT_REPEAT);
        }


    }

    /**
     * 用户登录
     * <p>
     * 1.根据Mail 从数据库中找有没有该记录 （使用QueryWrapper）
     * 2.有的话，使用密钥+用户的明文密码， 进行加密
     * 3.将加密好的密文 与 数据库中存放的密文进行匹配
     *
     * @param userLoginRequest
     * @return
     */
    @Override
    public JsonData login(UserLoginRequest userLoginRequest, String ip) {
//        1.根据Mail 从数据库中找有没有该记录 （使用QueryWrapper）
        List<UserDO> list = userMapper.selectList(new QueryWrapper<UserDO>().eq("mail", userLoginRequest.getEmail()));

        if (list != null && list.size() == 1) {
            //已经注册
            //        2.有的话，使用密钥+用户的明文密码， 进行加密
            UserDO userDO = list.get(0);
            String cryptPwd = Md5Crypt.md5Crypt(userLoginRequest.getPwd().getBytes(), userDO.getSecret());
            //        3.将加密好的密文 与 数据库中存放的密文进行匹配
            if (cryptPwd.equals(userDO.getPwd())) {
//                登录成功，生成Token TODO
                LoginUser loginUser = LoginUser.builder().build();
                BeanUtils.copyProperties(userDO, loginUser);
                loginUser.setIp(ip);
                String token = JWTUtil.generateJWT(loginUser);
                return JsonData.buildSuccess(token);
            } else {
                return JsonData.buildResult(BizCodeEnum.ACCOUNT_PWD_ERROR);
            }
        } else {
//            未注册
            return JsonData.buildResult(BizCodeEnum.ACCOUNT_UNREGISTER);
        }
    }

    /**
     * 查询用户详情
     *
     * @return
     */
    @Override
    public UserVO findUserDetail() {
//        获取拦截器中的ThreadLocal 以此 得到LoginUser
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        UserDO userDO = userMapper.selectOne(new QueryWrapper<UserDO>().eq("id", loginUser.getId()));

//        将DO 转为 VO ：1.隐藏敏感信息 2、VO作为后端想给前端展示的数据
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userDO, userVO);
        return userVO;
    }

    /**
     * 检查用户账号的唯一性
     *
     * @param mail
     * @return
     */
    private boolean checkUnique(String mail) {
//        QueryWrapper 是 MyBatis Plus 中的一个"查询"封装器，用于封装 SQL "查询条件"
//        提供了一系列的链式调用方法，用于构建 SQL 查询语句
//        使用 QueryWrapper 构建查询条件后，可以将其传递给 MyBatis Plus 的查询方法，例如 selectList、selectOne、selectCount 等，以实现对数据库的查询操作。
        QueryWrapper queryWrapper = new QueryWrapper<UserDO>().eq("mail", mail);
        List<UserDO> list = userMapper.selectList(queryWrapper);
        return list.size() > 0 ? false : true;
    }

    /**
     * 用户注册，初始化福利信息 TODO
     * 利用用户微服务的Feign接口调用优惠券微服务
     * 实现了微服务之间的通信
     * @param userDO
     */
    private void userRegisterInitTask(UserDO userDO) {
        NewUserRequest newUserRequest = new NewUserRequest();
        newUserRequest.setName(userDO.getName());
        newUserRequest.setUserId(userDO.getId());
        JsonData jsonData = couponFeignService.addNewUserCoupon(newUserRequest);
//        服务A调用服务B， 服务B发生异常，由于全局异常处理的存在（@ControllerAdvice）, seata 无法拦截到B服务的异常， 而是收到正常的Json返回值
//        从而导致分布式事务未生效
//        程序代码各自判断RPC响应码是否正常，再抛出异常
        if (jsonData.getCode() != 0) {
            throw new RuntimeException("发放新用户注册优惠券失败");
        }
        log.info("发放新用户注册优惠券:{}， 结果：{}", newUserRequest.toString(), jsonData.toString());
    }
}
