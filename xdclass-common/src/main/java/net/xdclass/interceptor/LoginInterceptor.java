package net.xdclass.interceptor;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.model.LoginUser;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JWTUtil;
import net.xdclass.util.JsonData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义 登录时的请求拦截器
 * 方便验证前端是否携带正确的token
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    public static ThreadLocal<LoginUser> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        获取token ： token是通过http的head 穿过来的 or 通过url穿过来的
        String accessToken = request.getHeader("token");
        if (accessToken == null) {
            accessToken = request.getParameter("token");
        }
//        获取ip：
        String ip = CommonUtil.getIpAddr(request);
//        判断Token
        if (StringUtils.isNotBlank(accessToken)) {
//            不为空
            Claims claims = JWTUtil.checkJWT(accessToken, ip);
            if (claims == null) {
//          -----未登录 (将未成功登录的信息，以json的形式传回给前端)
                CommonUtil.sendJsonMessage(response, JsonData.buildResult(BizCodeEnum.ACCOUNT_UNLOGIN));
                return false;
            }
//          -----登录
            long userId = Long.valueOf(claims.get("id").toString());
            String headImg = (String) claims.get("head_img");
            String name = (String) claims.get("name");
            String mail = (String) claims.get("mail");

//            使用@Builder进行生成对象
            LoginUser loginUser = LoginUser.builder().id(userId).name(name).mail(mail).headImg(headImg).build();

//           ---传递登录用户的信息（两种方式：1.attribute传递； 2.threadLocal传递）
//           ---1 通过attribute传递
//            request.setAttribute("loginUser", loginUser);

//           ---2 threadLocal传递 好处：同个线程下，共享变量，
            threadLocal.set(loginUser);

            return true;
        }
//       Token为空
        CommonUtil.sendJsonMessage(response, JsonData.buildResult(BizCodeEnum.ACCOUNT_UNLOGIN));
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
