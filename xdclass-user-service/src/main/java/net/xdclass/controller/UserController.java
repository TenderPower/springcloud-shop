package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.request.UserLoginRequest;
import net.xdclass.request.UserRegisterRequest;
import net.xdclass.service.FileService;
import net.xdclass.service.UserService;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import net.xdclass.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.spring.web.json.Json;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author ygk
 * @since 2024-04-11
 */
@RestController
@Api(tags = "用户模块")
@RequestMapping("/api/user/v1")
public class UserController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    /**
     * 上传用户头像
     * <p>
     * 默认文件大小 1M,超过会报错
     *
     * @param file
     * @return
     * @RequestPart 用于处理多部分请求（Multipart request）中的一部分数据.
     * 例如，在文件上传场景中，可以使用 @RequestPart 注解来接收文件数据和表单数据。
     * 使用 @RequestPart 注解的方法参数可以是以下几种类型：
     * MultipartFile：接收文件数据。
     * String：接收文本数据。
     * InputStream：接收二进制数据。
     * byte[]：接收二进制数据。
     */
    @ApiOperation("上传用户头像")
    @PostMapping("upload")
    public JsonData uploadUserImg(@ApiParam(value = "文件上传", required = true) @RequestPart("file") MultipartFile file) {
//        将文件上传到阿里云OSS上，并返回url地址（OSS 中的）
        String url = fileService.uploadUserImg(file);

        return url != null ? JsonData.buildSuccess(url) : JsonData.buildResult(BizCodeEnum.FILE_UPLOAD_USER_IMG_FAIL);

    }

    /**
     * 用户注册
     * <p>
     * 因为要接收前端的Json， 所以用@RequestBody来接收
     * (接收前端的JSON，有规范，需要一个request的类）
     *
     * @return
     */
    @ApiOperation("用户注册")
    @PostMapping("register")
    public JsonData register(@ApiParam("前端传给后端的用户注册对象") @RequestBody UserRegisterRequest userRegisterRequest) {

        return userService.register(userRegisterRequest);
    }

    /**
     * 用户登录
     * <p>
     * 将电脑的ip作为JWT负载的一部分 目的：方式JWT令牌Token泄露被恶意使用
     *
     * @return
     */
    @ApiOperation("用户登录")
    @PostMapping("login")
    public JsonData login(@ApiParam("用户登录对象") @RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String ip = CommonUtil.getIpAddr(request);
        return userService.login(userLoginRequest, ip);
    }

    @ApiOperation("个人信息查询")
    @GetMapping("detail")
    public JsonData userDetail() {
//        设置VO对象，是为了信息的安全性，（将后端中相要 透露给 前端看的数据 透露出去）
        UserVO userVO = userService.findUserDetail();
        return JsonData.buildSuccess(userVO);
    }

}

