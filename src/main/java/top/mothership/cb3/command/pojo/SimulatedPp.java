package top.mothership.cb3.command.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算界面展示的模拟PP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedPp {

    /**
     * 如果FC的PP
     */
    private Double fcPp;

    /**
     * 如果SS的PP
     */
    private Double ssPp;

    /**
     * 98% acc的PP
     */
    private Double acc98Pp;

    /**
     * 95% acc的PP
     */
    private Double acc95Pp;
}
