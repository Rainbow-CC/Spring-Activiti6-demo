package com.activiti6.delegate;


import lombok.Data;
import lombok.Getter;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.FixedValue;
import org.springframework.stereotype.Service;

/**
 * 对应流程任务中的自动任务, 委托类填写全限定名
 */
@Data
@Service
public class MyTaskDelegate implements JavaDelegate {

    // Getter 和 Setter
    private FixedValue fieldName;
    @Override
    public void execute(DelegateExecution execution) {
        // 这里编写自动任务的具体业务逻辑
        System.out.println("自动任务执行中...");

        // 获取流程变量
        String processVariable = (String) execution.getVariable("fieldName");
        System.out.println("流程变量值：" + processVariable);

        // 可以在这里调用其他服务或业务逻辑
        doSomeBusinessLogic();
    }

    private void doSomeBusinessLogic() {
        // 你的业务逻辑代码
        System.out.println("执行一些业务逻辑...");
    }


}
