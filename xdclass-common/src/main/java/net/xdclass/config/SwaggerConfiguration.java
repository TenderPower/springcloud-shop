package net.xdclass.config;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.*;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;


//这个是swagger 的配置文件，后期使用 可以直接复制黏贴即可
@Component//配置类 需要让spring 扫描
@Data
@EnableOpenApi
public class SwaggerConfiguration {

    /*
     * 对C端用户的接口文档
     * */
    @Bean
    public Docket webApiDoc() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("用户端接口文档")
                .pathMapping("/")
//                定义是否开启Swagger, 上线是要关闭的
                .enable(true)
//                配置文档的元信息
                .apiInfo(apiInfo())
                .select()
//                扫描一下包
                .apis(RequestHandlerSelectors.basePackage("net.xdclass"))
                //正则匹配请求路径，并分配至当前分组
                .paths(PathSelectors.ant("/api/**"))
//                开始构建
                .build()
//              自定义 http的请求头，比如登录的token令牌
                //新版swagger3.0配置
//                全局的request参数设置
                .globalRequestParameters(getGlobalRequestParameters())
//                全局的response设置
                .globalResponses(HttpMethod.GET,getGlobalResponseMessage())
                .globalResponses(HttpMethod.POST,getGlobalResponseMessage());
    }


    /*
     * 对管理端的接口文档
     * */
    @Bean
    public Docket adminApiDoc() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("管理端接口文档")
                .pathMapping("/")
//                定义是否开启Swagger, 上线是要关闭的
                .enable(true)
//                配置文档的元信息
                .apiInfo(apiInfo())
                .select()
//                扫描一下包
                .apis(RequestHandlerSelectors.basePackage("net.xdclass"))
                //正则匹配请求路径，并分配至当前分组
                .paths(PathSelectors.ant("/admin/**"))
//                开始构建
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("YGK电商平台")
                .description("微服务接口文档")
                .contact(new Contact("YGk", "https://xxxx.xxx", "954337256@qq.com"))
                .version("v1.0")
                .build();
    }

    /*
     * 生成全局通用参数，支持配置多个询问参数
     *  */
    private List<RequestParameter> getGlobalRequestParameters() {
        List<RequestParameter> parameters = new ArrayList<>();
        parameters.add(new RequestParameterBuilder()
                .name("token")
                .description("登录令牌")
                .in(ParameterType.HEADER)
                .query(q -> q.model(m -> m.scalarModel(ScalarType.STRING)))
                .required(false)
                .build());

//        这里可以在添加多个请求头的参数
//        parameters.add(new RequestParameterBuilder()
//                .name("version")
//                .description("版本号")
//                .required(true)
//                .in(ParameterType.HEADER)
//                .query(q -> q.model(m -> m.scalarModel(ScalarType.STRING)))
//                .required(false)
//                .build());
        return parameters;
    }


    /*
     * 配置全局通用的响应信息， Response
     * */
    private List<Response> getGlobalResponseMessage() {
        List<Response> responseList = new ArrayList<>();
        responseList.add(new ResponseBuilder().code("4xx").description("请求错误，根据code和msg进行检查").build());
        return responseList;
    }

}
