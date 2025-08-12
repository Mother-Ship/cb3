package top.mothership.cb3.command.argument;


import lombok.Data;
import top.mothership.cb3.command.constant.CbCmdPrefix;
import top.mothership.cb3.command.reflect.CbCmdArgument;

@Data
public class MyBPCommandArg {
    @CbCmdArgument(CbCmdPrefix.COLON)
    private String mode;
}
