package top.mothership.cb3.command.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.constant.ContextDataEnum;

@Aspect
@Slf4j
@Component
public class ContextDataAspect {

    @Around("@annotation(needContextData)")
    public Object fillContextData(ProceedingJoinPoint joinPoint, NeedContextData needContextData) throws Throwable {
        // 在方法执行前填充上下文数据
        fillContextData(needContextData.value());
        
        try {
            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 方法执行后清理上下文数据（如果需要）
            clearContextData();
        }
    }
    
    private void fillContextData(ContextDataEnum[] contextDataEnums) {
        // 上下文数据填充
        for (ContextDataEnum contextDataEnum : contextDataEnums){
            log.info("填充上下文数据: {}", contextDataEnum);
        }
    }
    
    private void clearContextData() {
        // 清理上下文数据
    }
}
