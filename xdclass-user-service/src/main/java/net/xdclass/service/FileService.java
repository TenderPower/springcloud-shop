package net.xdclass.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

//在这里不写@Service
public interface FileService {
    public String uploadUserImg(MultipartFile file);
}
