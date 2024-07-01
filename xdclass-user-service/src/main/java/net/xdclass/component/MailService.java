package net.xdclass.component;

public interface MailService {
    /**
     * 发送邮箱
     * @param to
     * @param subject
     * @param content
     */
    void sedMail(String to, String subject, String content);
}
