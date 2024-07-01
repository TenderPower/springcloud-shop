package net.xdclass.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.OSSConfig;
import net.xdclass.service.FileService;
import net.xdclass.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private OSSConfig ossConfig;

    /**
     * 阿里云OSS java 将用户的图像上传到OSS中
     * （当成模板）
     *
     * @param file
     * @return 当文件上传成功后，返回图片在OSS中的路径 否则null
     */
    @Override
    public String uploadUserImg(MultipartFile file) {
//       获取从OSsConfig类中获取相关配置
        String bucketName = ossConfig.getBucketName();
        String endpoint = ossConfig.getEndpoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();
//        创建OSS对象
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

//        MultipartFile对象可以获取文件各种信息
//        获取原生文件名，xxx.jpg
        String originalFilename = file.getOriginalFilename();

//        JDK(8)的日期格式
        LocalDateTime ldt = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");

//        拼装路径，将前端要上传的图片存放到OSS中指定的路径 userImage/yyyy/MM/dd/uuid.jpg
//        在oss上创建文件夹userImage路径（当然，也可以用API进行创建，看文档吧）
        String folder = dtf.format(ldt);
//        将原文件名以uuid的形式进行命名
        String fileName = CommonUtil.generateUUID();
//        获取原文件名的文件类型
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
//        拼接路径

        String newFileName = "userImage" +"/"+ folder + "/" + fileName + extension;

//        将图像文件存放到阿里云OSS中
        try {
            PutObjectResult result = ossClient.putObject(bucketName, newFileName, file.getInputStream());
            //返回访问路径
            if (null != result) {
                //https://xdclass-2024shop-img.oss-cn-beijing.aliyuncs.com/userImage/xxxxxx.jpg
                String imgUrl = "https://" + bucketName + "." + endpoint + "/" + newFileName;
                return imgUrl;
            }
        } catch (IOException e) {
            log.error("上传头像失败:{}", e);
        } finally {
//            关闭OSS服务
            ossClient.shutdown();
        }
        return null;
    }
}
