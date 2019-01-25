package com.hnradio.wowzarecorder.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/log4j2", produces = "application/json;charset=UTF-8")
public class LogLevelController {

    private static final Logger LOG = LoggerFactory.getLogger(LogLevelController.class);

    @ResponseBody
    @GetMapping(value = "/levels")
    public Map<String, String> show(){
        return  getNowSets();
    }


    @RequestMapping(value = "/levels/{p}/{level}")
    public  Map<String, String> set(@PathVariable("p") String p, @PathVariable("level") String level){

        if(StringUtils.isNotBlank(p)){
            Map<String, String> nowSets = getNowSets();
            if(!nowSets.containsKey(p)){
                throw new RuntimeException("Non-existent loggerConfig : " + p);
            }
            Configurator.setLevel(p, Level.valueOf(level));
        }
        return getNowSets();
    }


    private Map<String, String> getNowSets(){
        Map<String, String> nowSets = new HashMap<>();
        final LoggerContext loggerContext = LoggerContext.getContext(false);
        Map<String, LoggerConfig> config = loggerContext.getConfiguration().getLoggers();
        for(Map.Entry<String, LoggerConfig> logEntry : config.entrySet()){
            if(StringUtils.isNotBlank(logEntry.getKey())){
                nowSets.put(logEntry.getKey(),logEntry.getValue().getLevel().name());
            }
        }
        return nowSets;
    }

}
