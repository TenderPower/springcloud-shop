package net.xdclass.controller;

import com.google.code.kaptcha.Producer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.SendCodeEnum;
import net.xdclass.service.NotifyService;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 开发一个Controller类测试验证的功能
 */

@Api(tags = "通知模块")
@RestController
@RequestMapping("/api/notify/v1")
@Slf4j
public class NotifyController {

    @Autowired
    private Producer captchaProducer;//这是captcha的工具类

    //    注入Redis的工具类
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NotifyService notifyService;
    /**
     * 临时使用10分钟有效，方便测试
     */
    private static final long CAPTCHA_CODE_EXPIRED = 60 * 1000 * 10;


    @ApiOperation("获取图像的验证码")
    @GetMapping("captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        String captchaText = captchaProducer.createText();
        log.info("图像验证码{}", captchaText);

//        将验证码存起来(ps:存储起来没必要永久，可以设置过期时间）
//        getCaptchaKey(request) 表示
        redisTemplate.opsForValue().set(getCaptchaKey(request), captchaText, CAPTCHA_CODE_EXPIRED, TimeUnit.MILLISECONDS);


        BufferedImage bufferedImage = captchaProducer.createImage(captchaText);
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            ImageIO.write(bufferedImage, "jpg", outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            log.error("获取图像验证码异常:{}", e);
        }

    }

    @ApiOperation("发送验证码")
    @GetMapping("send_code")
    public JsonData sendRegisterCode(@RequestParam(value = "to", required = true) String to,
                                     @RequestParam(value = "captcha", required = true) String captcha,
                                     HttpServletRequest request) {
//        获取根据key暂存在Redis中对应的图形验证码
        String key = getCaptchaKey(request);
        String cacheCaptcha = redisTemplate.opsForValue().get(key);

//        匹配图形验证码是否一样
        if (captcha != null && cacheCaptcha != null && cacheCaptcha.equalsIgnoreCase(captcha)) {
//            成功
            redisTemplate.delete(key);
            JsonData jsonData = notifyService.sendCode(SendCodeEnum.USER_REGISTER, to);
            return jsonData;
        } else {
//            失败
            return JsonData.buildResult(BizCodeEnum.CODE_CAPTCHA);
        }
    }


    /**
     * 获取Redis缓存的key
     * 存储和读取Redis时都要有个key
     * key规范：业务划分，冒号隔离 user-service:captcha:xxxx
     *
     * 可以将其当成工具函数
     * @param request
     * @return
     */
    private String getCaptchaKey(HttpServletRequest request) {
        String ip = CommonUtil.getIpAddr(request);
        //利用浏览器中的指纹来作为redis中key的一部分
        String userAgent = request.getHeader("User-Agent");

//       构造key（ip+浏览器的指纹）
        String key = "user-service:captcha" + CommonUtil.MD5(ip + userAgent);

        log.info("ip={}", ip);
        log.info("userAgent={}", userAgent);
        log.info("key={}", key);
        return key;
    }
}
