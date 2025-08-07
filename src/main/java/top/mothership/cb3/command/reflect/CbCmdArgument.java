package top.mothership.cb3.command.reflect;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CbCmdArgument {
    /**
     * 该参数的前缀字符，只能从CbCmdPrefix中取
     * @return 该参数的前缀字符
     */
    char value() default ' ';

    boolean required() default false;
}
