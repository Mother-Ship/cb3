package top.mothership.cb3.command.reflect;


import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.constant.CbCmdPrefix;
import top.mothership.cb3.command.context.DataContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Component
@Slf4j
public class CbCmdProcessorRegistry {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private CbCmdProcessorManager cbCmdProcessorManager;

    @PostConstruct
    public void checkRoutingAmbiguous() {

    }

    @PostConstruct
    public void checkArgumentAmbiguous() {

    }

    public CbCmdProcessorInfo getProcessorInfo(String message) {

        String commandName = getCommandName(message);

        if (commandName == null) {
            // 没有感叹号的情况
            return null;
        }
        DataContext.setCommand(commandName);

        Method method = cbCmdProcessorManager.getByCommandName(commandName);

        CbCmdProcessor processorInfo = AnnotationUtils.findAnnotation(method, CbCmdProcessor.class);

        var argumentText = getStringParameterByText(message, commandName);

        //如果命令标明了要自己处理参数，而不是依赖注解，则假定命令的入参为String类型
        if (processorInfo != null && processorInfo.rawParameter()) {
            return CbCmdProcessorInfo.builder()
                    .className(method.getDeclaringClass().getName())
                    .methodName(method.getName())
                    .parameterType(String.class)
                    .parameter(argumentText)
                    .build();
        }

        //否则从命令文本中解析出参数对象
        Class<?> parameterClz = method.getParameterTypes()[0];

        Object parameter = getParameterByText(parameterClz, argumentText);

        return CbCmdProcessorInfo.builder()
                .className(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterType(parameterClz)
                .parameter(parameter)
                .build();
    }

    @SneakyThrows
    public void invokeProcessor(CbCmdProcessorInfo info) {
        Class<?> clz = Class.forName(info.getClassName());
        Object o = applicationContext.getBean(clz);
        Method m = clz.getMethod(info.getMethodName(), info.getParameterType());
        m.invoke(o, info.getParameter());
    }

    /**
     * 拆解出命令名称
     * 0. 如果没有感叹号 直接返回
     * 1. 取第一个感叹号 和 第一个空格之间的文本，如果没有空格就取到文本最后
     * 2. 如果是sudo，则取第一个感叹号 到第二个空格之间的文本
     *
     * @param message
     * @return
     */
    private String getCommandName(String message) {

        int start = StringUtils.indexOfAny(message, '!', '！');
        if (start == -1) return null;

        int end = message.contains(" ") ? StringUtils.indexOf(message, " ") : message.length();
        String commandName = message.substring(start + 1, end);

        if (Objects.equals("sudo", commandName)) {
            end = StringUtils.ordinalIndexOf(message, " ", 2);
            end = end == -1 ? message.length() : end;
            commandName = message.substring(start, end);
        }

        return commandName;
    }

    /**
     * 拆解字符串格式命令参数
     * 消息替换掉中英文感叹号，再替换掉命令名称和sudo，再去掉开头的空格
     *
     * @param message
     * @return
     */
    @SneakyThrows
    private String getStringParameterByText(String message, String commandName) {

        return message.replace("！", "")
                .replace("!", "")
                .replace("sudo", "")
                .replace(commandName, "")
                .trim();
    }

    /**
     * 拆解命令参数
     * 1. 检查所有参数前缀在命令中出现的次数，多余一次直接报错，强制要求命令自己处理参数
     * 2. 切割出每个参数前缀<->空格之间的字符串，设置到对应注解的字段中
     *
     * @param parameterType
     * @param text
     * @return
     */
    @SneakyThrows
    private Object getParameterByText(Class<?> parameterType, String text) {

        Object param = parameterType.getDeclaredConstructor().newInstance();
        // 如果解析出来的参数文本是空的，直接用默认构造函数创建对象返回
        if (StringUtils.isEmpty(text)) {
            return param;
        }

        List<CbCmdArgument> annotations = Arrays.stream(parameterType.getDeclaredFields())
                .map(field -> field.getAnnotation(CbCmdArgument.class)).toList();

        List<Character> prefixList = annotations.stream()
                .map(a -> getValidPrefix(a.value(), text))
                .toList();

        Map<Character, String> prefixTextMap = split(text, prefixList);

        for (Field declaredField : parameterType.getDeclaredFields()) {
            CbCmdArgument argumentAnnotation = declaredField.getAnnotation(CbCmdArgument.class);
            Character prefix = argumentAnnotation.value();
            String value = prefixTextMap.get(prefix);
            if (value != null) {
                declaredField.setAccessible(true);
                declaredField.set(param, value);
            }
        }

        return param;

    }

    private Character getValidPrefix(Character prefix, String text) {
        if (CbCmdPrefix.isNothing(prefix)) {
            return prefix;
        }

        String[] split = StringUtils.split(text, String.valueOf(prefix), 2);

        if (split[1].indexOf(prefix) != -1) {
            log.warn("命令解析错误：命令{}中参数前缀{}重复", text, prefix);
            throw new RuntimeException("命令解析错误：参数前缀重复");
        }
        return prefix;
    }

    private Map<Character, String> split(String s, List<Character> separatorList) {
        Map<Character, String> result = new HashMap<>(separatorList.size());
        for (Character currentSeparator : separatorList) {

            //如果是空前缀的参数，则取文本开头 到第一个非空前缀之间的内容，再去除头尾空格
            if (CbCmdPrefix.isNothing(currentSeparator)) {
                int end = findNextSeparator(s, separatorList, currentSeparator);
                result.put(currentSeparator, s.substring(0, end).trim());
                continue;
            }

            //如果是普通前缀的参数，则遍历字符串，找到当前前缀 和字符串顺序内下一个前缀之间的内容，再去除头尾空格
            int i = 0;
            while (i < s.length()) {
                if (currentSeparator.equals(s.charAt(i))) {
                    int start = i + 1;
                    int end = findNextSeparator(s, separatorList, currentSeparator);
                    result.put(currentSeparator, s.substring(start, end).trim());
                }
                i++;
            }
        }
        return result;
    }

    private int findNextSeparator(String s, List<Character> separator, Character currentSeparator) {
        int index = s.indexOf(currentSeparator);

        int j = 0;
        while (j < s.length()) {
            if (separator.contains(s.charAt(j))
                    && !CbCmdPrefix.isNothing(s.charAt(j))
                    && j > index) {
                return j;
            }
            j++;
        }

        //没有找到下一个分隔符，则返回最后一个字符的位置
        return j;
    }

}
