package com.qintongxue.opensource.opensource_qintongxue.selfstartingtask;

import org.springframework.stereotype.Component;

@Component
public class Operator {
    // 任务操作主执行方法
    public void execute(Long id){
        System.out.println("任务处理中.....");
    }
}
