package com.byeolgyeol.saju;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SajuApplication {
    public static void main(String[] args) {
        // AWS 서버는 기본 시간이 UTC(한국보다 9시간 느림)라서 한국 시간으로 고정한다.
        // → 저장 시각 · 관리자 화면 시각 · "오늘의 운세"가 바뀌는 시점(자정)이 모두 한국 기준이 됨
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(SajuApplication.class, args);
    }
}
