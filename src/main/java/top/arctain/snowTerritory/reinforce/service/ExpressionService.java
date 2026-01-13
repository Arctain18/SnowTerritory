package top.arctain.snowTerritory.reinforce.service;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.Map;

/**
 * 表达式解析服务
 * 使用exp4j解析函数表达式
 */
public class ExpressionService {

    /**
     * 解析并计算表达式
     * @param expression 表达式字符串
     * @param variables 变量映射
     * @return 计算结果，如果解析失败返回0
     */
    public double evaluate(String expression, Map<String, Double> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return 0;
        }

        try {
            ExpressionBuilder builder = new ExpressionBuilder(expression);
            
            // 添加所有变量
            if (variables != null) {
                for (String varName : variables.keySet()) {
                    builder.variable(varName);
                }
            }
            
            Expression exp = builder.build();
            
            // 设置变量值
            if (variables != null) {
                for (Map.Entry<String, Double> entry : variables.entrySet()) {
                    exp.setVariable(entry.getKey(), entry.getValue());
                }
            }
            
            return exp.evaluate();
        } catch (Exception e) {
            MessageUtils.logError("表达式解析失败: " + expression + " - " + e.getMessage());
            return 0;
        }
    }

    /**
     * 验证表达式是否有效
     * @param expression 表达式字符串
     * @return 是否有效
     */
    public boolean isValid(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        try {
            new ExpressionBuilder(expression).build();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
