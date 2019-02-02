package com.hnradio.wowzarecorder.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * 节目
 */
@Data
public class ProgramBean implements Serializable {

    private static final long serialVersionUID = -7957088706858910913L;

    private long id;

    private long channelId;

    private String name; //节目名称

    private boolean willSplit;

    private String starttime; //开始时间

    private String endtime; //结束时间

    private String signa; //节目识别码

    private String filePath;



}
