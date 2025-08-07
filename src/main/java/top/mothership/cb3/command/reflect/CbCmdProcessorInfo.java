package top.mothership.cb3.command.reflect;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CbCmdProcessorInfo {
    private String className;
    private String methodName;
    private Class<?> parameterType;
    private Object parameter;
}
