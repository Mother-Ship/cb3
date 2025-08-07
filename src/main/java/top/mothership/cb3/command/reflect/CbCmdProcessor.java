package top.mothership.cb3.command.reflect;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CbCmdProcessor {
    /**
     * 能处理的命令前缀
     * @return
     */
    String[] value() default "";

    /**
     * 是否由命令处理器自己处理参数，适用于参数前缀处理不了的扩展场景
     * @return
     */
    boolean rawParameter() default false;
}
