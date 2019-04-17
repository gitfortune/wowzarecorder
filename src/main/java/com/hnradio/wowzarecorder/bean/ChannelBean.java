package com.hnradio.wowzarecorder.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 频率
 */
@Data
public class ChannelBean implements Serializable {

    private static final long serialVersionUID = 3895893841535243928L;

    private long id;

    private String name;  //频率名称

    private boolean willSplit;  //是否要切片,默认false

    private String streamName;  //要切片的流名称[和wowza管理员确认]

    private List<ProgramBean> programs; //包含的节目信息

    /**
     * 指明当前节目的流是在wowoza的哪个Applications下
     */
    private String app;

}
