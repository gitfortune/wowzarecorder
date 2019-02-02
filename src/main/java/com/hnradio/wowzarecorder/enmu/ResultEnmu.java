package com.hnradio.wowzarecorder.enmu;

import lombok.Getter;

@Getter
public enum ResultEnmu {

    SLEEP_FAIL(1,"休眠线程被中断"),
    HAINA_FAIL(2,""),
    ;


    private int code;

    private String msg;

    ResultEnmu(int code, String msg){
        this.code = code;
        this.msg = msg;
    }
}
