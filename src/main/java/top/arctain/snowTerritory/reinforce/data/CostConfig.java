package top.arctain.snowTerritory.reinforce.data;

import lombok.Getter;
import lombok.Setter;

/**
 * 消耗配置数据类
 */
@Getter
@Setter
public class CostConfig {

    private boolean enabled = false;
    private String baseExpression = "";
    private String levelMultiplier = "1";

    /**
     * 检查配置是否有效
     */
    public boolean isValid() {
        return enabled && !baseExpression.isEmpty() && !levelMultiplier.isEmpty();
    }
}
