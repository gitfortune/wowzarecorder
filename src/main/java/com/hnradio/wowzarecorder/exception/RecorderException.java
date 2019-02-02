package com.hnradio.wowzarecorder.exception;

import com.hnradio.wowzarecorder.enmu.ResultEnmu;
import lombok.Data;

@Data
public class RecorderException extends RuntimeException {

    private int code;

    public RecorderException(ResultEnmu resultEnmu){
        super(resultEnmu.getMsg());

        this.code = resultEnmu.getCode();

    }

    public RecorderException(int code, String msg){
        super(msg);
        this.code = code;
    }
}
