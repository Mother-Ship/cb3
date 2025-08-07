package top.mothership.cb3.command.aop;

import top.mothership.cb3.command.constant.ContextDataEnum;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NeedContextData {
    ContextDataEnum[] value();
}
