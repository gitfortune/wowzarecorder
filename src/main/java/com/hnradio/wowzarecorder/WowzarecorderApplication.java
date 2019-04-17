package com.hnradio.wowzarecorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.hnradio.wowzarecorder.api"})
@EnableHystrix
public class WowzarecorderApplication {

	public static void main(String[] args) {
		SpringApplication.run(WowzarecorderApplication.class, args);
	}

}

