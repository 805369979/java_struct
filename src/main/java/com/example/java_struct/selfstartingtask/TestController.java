package com.example.java_struct;

import com.example.java_struct.selfstartingtask.SelfRunThreadPoolManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class jj {


    @GetMapping("/start")
    public void hh(){
        System.out.println("开始执行");
        SelfRunThreadPoolManager.reStart();
    }

    @GetMapping("/stop")
    public void hhf(){
        System.out.println("停止执行");
        SelfRunThreadPoolManager.stop();
    }
}
