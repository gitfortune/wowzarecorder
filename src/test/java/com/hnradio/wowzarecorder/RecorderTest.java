package com.hnradio.wowzarecorder;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class RecorderTest {

    @Autowired
    RecorderTask recorder;

    @Test
    public void tt() {
        recorder.RecorderExecutor();
    }

    @Test
    @Scheduled(cron = "00 23 16 * * ?")
    public void test1(){
        log.info("AAAAAAAAAA");
    }
}