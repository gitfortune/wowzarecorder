package com.hnradio.wowzarecorder;

import com.hnradio.wowzarecorder.bean.ChannelBean;
import lombok.extern.slf4j.Slf4j;

/**
 * 录制线程
 */
@Slf4j
public class RecorderRunnable implements Runnable {


    @Override
    public void run() {
        log.info("线程执行");

    }
}
