package com.hnradio.wowzarecorder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "recorder")
public class RecorderProperties {

    /**
     * ffmpeg路径
     */
    public String ffmpeg;

    /**
     * 制文件临时存储目录
     */
    public String temp;

    /**
     * 录制文件存储目录
     */
    public String storage;

    /**
     * userName:passWord@ip:端口
     */
    public String urlPrefix;
}
