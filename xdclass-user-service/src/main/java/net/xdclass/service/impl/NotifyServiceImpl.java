package net.xdclass.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.component.MailService;
import net.xdclass.constant.CacheKey;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.SendCodeEnum;
import net.xdclass.service.NotifyService;
import net.xdclass.util.CheckUtil;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import org.apache.commons.lang3.StringUtils;
import org.mockito.internal.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {
    @Autowired
    private MailService mailService;

    @Autowired
    private StringRedisTemplate redisTemplate;
    /**
     * 验证码标题
     */
    private static final String SUBJECT = "XDCLASS验证码";

    /**
     * 验证码的内容
     */
    private static final String Content = "您的验证码为%s,有效时间为60秒，打死也不要告诉任何人";

    /**
     * 验证码10分钟有效
     */
    private static final long CODE_EXPIRED = 60 * 1000 * 10;

    /**
     * 向目标用户 发送验证码
     * 功能：
     * 1.向用户的手机 or 邮箱进行发送 验证码
     * 2.具备了邮箱验证码防刷机制（具备了两种）
     * 2.1增加Redis存储，发送的时候设置下额外的key，并且60秒后过期（）
     * 2.2基于原先的key拼装时间戳
     *
     * @param sendCodeEnum
     * @param to
     * @return
     */

    @Override
    public JsonData sendCode(SendCodeEnum sendCodeEnum, String to) {

//        对Redis 设置接收验证码 的key
        String cacheKey = String.format(CacheKey.CHECK_CODE_KEY, sendCodeEnum.name(), to);
//        从Redis中获取key对应的值
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);

//        如果不为空，则判断是否60秒内重复发送
        if (StringUtils.isNotBlank(cacheValue)) {
//            判断是否重复发送
            long ttl = Long.parseLong(cacheValue.split("_")[1]);
//            当前时间戳-验证码发送时间戳，如果小于60秒，则不给重复发送
            long time = CommonUtil.getCurrentTimeStamp() - ttl;
            if (time < 1000 * 60) {
                log.info("重复发送验证码，时间间隔为:{}秒", time / 1000);
                return JsonData.buildResult(BizCodeEnum.CODE_LIMITED);
            }

        }
//      如果cacheValue 虽然不为空，但是已经过了60秒，就可以重新获取新的验证码
        /**
         * 注册验证码防刷方案---基于原先的key拼装时间戳
         * 拼接验证码 验证码_时间戳
         */
        String code = CommonUtil.getRandomCode(6);
        String value = code + "_" + CommonUtil.getCurrentTimeStamp();
        redisTemplate.opsForValue().set(cacheKey, value, CODE_EXPIRED, TimeUnit.MILLISECONDS);
        if (CheckUtil.isEmail(to)) {
//            发送邮箱验证码
            mailService.sedMail(to, SUBJECT, String.format(Content, code));
            return JsonData.buildSuccess();
        } else if (CheckUtil.isPhone(to)) {
//            短信验证码
            //TODO
        }
//        就是接收方的邮箱or手机号格式不对
        return JsonData.buildResult(BizCodeEnum.CODE_TO_ERROR);
    }

    /**
     * 验证邮箱验证码是否一致
     *
     * @param sendCodeEnum
     * @param to
     * @param code
     * @return
     */
    @Override
    public boolean checkCode(SendCodeEnum sendCodeEnum, String to, String code) {
//        构造缓存的key（Redis 中的)
        String cacheKey = String.format(CacheKey.CHECK_CODE_KEY, sendCodeEnum.name(), to);
//        获取key对应的value
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);

        if (StringUtils.isNotBlank(cacheValue)) {
//            从value中获取缓存的邮箱验证码
            String cacheCode = cacheValue.split("_")[0];
            if (cacheCode.equals(code)) {
//                邮箱验证码通过； 删除验证码
                redisTemplate.delete(cacheKey);
                return true;
            }
        }
        return false;
    }
}
