package top.arctain.snowTerritory.utils;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 颜色工具类
 * 处理所有颜色代码转换，包括传统颜色、16进制颜色和渐变
 */
public class ColorUtils {

    /**
     * 转换颜色代码（支持传统颜色代码、16进制颜色和渐变）
     * 
     * 支持格式：
     * - 传统颜色代码: &a (绿色), &l (粗体), &r (重置) 等
     * - 16进制颜色: &{#FFFFFF} 或 <#FFFFFF>
     * - 渐变颜色: 
     *   - <GRADIENT:#FF6600:#FF6600> 或 <g:#1f2fa6:#91c8ff>
     *   - {#314eb5>}Text{#8fdaff<}
     *   - {#314eb5>}Text1{#c294ff<>} Text2{#8fdaff<} (多层渐变)
     * 
     * 示例：
     * - "&aHello &{#FF0000}World" → 绿色Hello + 红色World
     * - "<#00FF00>Green Text" → 绿色文本
     * - "<GRADIENT:#FF0000:#0000FF>Gradient" → 红到蓝渐变
     * 
     * @param text 包含颜色代码的文本
     * @return 转换后的文本（可直接发送给玩家）
     */
    public static String colorize(String text) {
        if (text == null) return "";
        
        // 先处理渐变颜色（需要在16进制颜色之前处理）
        text = processGradients(text);
        
        // 然后处理16进制颜色格式（转换为Minecraft原生格式）
        text = processHexColors(text);
        
        // 最后处理传统颜色代码（&a, &l 等）
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * 处理渐变颜色格式
     * 
     * 支持的格式：
     * - <GRADIENT:#FF6600:#FF6600>Text</GRADIENT> 或 <g:#1f2fa6:#91c8ff>Text</g>
     * - {#314eb5>}Text{#8fdaff<} - 自定义格式，> 表示开始，< 表示结束
     * - {#314eb5>}Text1{#c294ff<>} Text2{#8fdaff<} - 多层渐变，<> 表示中间点
     * 
     * @param text 包含渐变颜色代码的文本
     * @return 转换后的文本
     */
    private static String processGradients(String text) {
        if (text == null || text.isEmpty()) return text;
        
        // 处理标准渐变格式: <GRADIENT:#FF6600:#FF6600>Text</GRADIENT> 或 <g:#1f2fa6:#91c8ff>Text</g>
        text = processStandardGradient(text);
        
        // 处理自定义渐变格式: {#314eb5>}Text{#8fdaff<} 或多层渐变
        text = processCustomGradient(text);
        
        return text;
    }
    
    /**
     * 处理标准渐变格式: <GRADIENT:#FF6600:#FF6600>Text</GRADIENT> 或 <g:#1f2fa6:#91c8ff>Text</g>
     */
    private static String processStandardGradient(String text) {
        // 处理 <GRADIENT:#RRGGBB:#RRGGBB>Text</GRADIENT> 格式
        Pattern pattern = Pattern.compile(
            "<(?:GRADIENT|g):#([0-9A-Fa-f]{6}):#([0-9A-Fa-f]{6})>(.*?)</(?:GRADIENT|g)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String color1 = matcher.group(1);
            String color2 = matcher.group(2);
            String content = matcher.group(3);
            
            String gradientText = applyGradient(content, color1, color2);
            matcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理自定义渐变格式: {#314eb5>}Text{#8fdaff<} 或 {#314eb5>}Text1{#c294ff<>} Text2{#8fdaff<}
     */
    private static String processCustomGradient(String text) {
        if (text == null || text.isEmpty()) return text;
        
        // 使用更精确的正则表达式匹配所有渐变标记
        Pattern pattern = Pattern.compile(
            "\\{#([0-9A-Fa-f]{6})(>|<>|<)\\}(.*?)(?=\\{#|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        
        GradientSegment currentSegment = null;
        
        while (matcher.find()) {
            // 添加匹配前的普通文本
            if (matcher.start() > lastIndex) {
                result.append(text.substring(lastIndex, matcher.start()));
            }
            
            String color = matcher.group(1);
            String marker = matcher.group(2);
            String content = matcher.group(3);
            
            if (marker.equals(">")) {
                // 开始新的渐变段
                if (currentSegment != null && currentSegment.endColor == null) {
                    // 如果之前的段没有结束，先处理它（使用开始颜色作为结束颜色）
                    currentSegment.setEndColor(currentSegment.startColor);
                    result.append(currentSegment.applyGradient());
                }
                currentSegment = new GradientSegment(color, content);
            } else if (marker.equals("<>")) {
                // 中间点，添加到当前段
                if (currentSegment != null) {
                    currentSegment.addMiddlePoint(color, content);
                }
            } else if (marker.equals("<")) {
                // 结束点
                if (currentSegment != null) {
                    currentSegment.setEndColor(color);
                    result.append(currentSegment.applyGradient());
                    currentSegment = null;
                }
                // 结束标记后的内容需要单独添加（因为正则匹配到的content是结束标记后的文本）
                // 注意：这里content是结束标记{#RRGGBB<}之后的文本
                if (content != null && !content.isEmpty()) {
                    result.append(content);
                }
            }
            
            lastIndex = matcher.end();
        }
        
        // 处理未完成的渐变段
        if (currentSegment != null) {
            if (currentSegment.endColor == null) {
                currentSegment.setEndColor(currentSegment.startColor);
            }
            result.append(currentSegment.applyGradient());
        }
        
        // 添加剩余的文本
        if (lastIndex < text.length()) {
            result.append(text.substring(lastIndex));
        }
        
        return result.toString();
    }
    
    /**
     * 渐变段（用于处理自定义格式的渐变）
     */
    private static class GradientSegment {
        String startColor;
        String endColor;
        List<String> middleColors = new ArrayList<>();
        List<String> middleTexts = new ArrayList<>();
        String startText;
        
        GradientSegment(String startColor, String startText) {
            this.startColor = startColor;
            this.startText = startText;
        }
        
        void addMiddlePoint(String color, String text) {
            middleColors.add(color);
            middleTexts.add(text);
        }
        
        void setEndColor(String color) {
            this.endColor = color;
        }
        
        String applyGradient() {
            if (endColor == null) {
                return startText; // 没有结束点，返回原文本
            }
            
            // 构建完整的文本和颜色点
            List<String> allColors = new ArrayList<>();
            List<String> allTexts = new ArrayList<>();
            
            allColors.add(startColor);
            allTexts.add(startText);
            
            for (int i = 0; i < middleColors.size(); i++) {
                allColors.add(middleColors.get(i));
                allTexts.add(middleTexts.get(i));
            }
            
            allColors.add(endColor);
            
            // 合并所有文本
            StringBuilder fullText = new StringBuilder(startText);
            for (String text : middleTexts) {
                fullText.append(text);
            }
            
            // 计算每个颜色点对应的文本位置
            String cleanText = fullText.toString().replaceAll("&[0-9a-fk-or]", "");
            if (cleanText.isEmpty()) {
                return fullText.toString();
            }
            
            // 计算每个颜色点对应的字符位置
            int[] colorPositions = new int[allColors.size()];
            int totalChars = cleanText.length();
            for (int i = 0; i < allColors.size(); i++) {
                colorPositions[i] = (int) Math.round((double) i / (allColors.size() - 1) * (totalChars - 1));
            }
            
            // 应用渐变
            StringBuilder result = new StringBuilder();
            int textIndex = 0;
            
            for (int i = 0; i < fullText.length(); i++) {
                char ch = fullText.charAt(i);
                
                // 跳过颜色代码
                if (ch == '&' && i + 1 < fullText.length()) {
                    char next = fullText.charAt(i + 1);
                    if ("0123456789abcdefklmnor".indexOf(Character.toLowerCase(next)) >= 0) {
                        result.append(ch).append(next);
                        i++;
                        continue;
                    }
                }
                
                // 计算当前字符应该使用的颜色
                int charPos = textIndex;
                if (charPos >= cleanText.length()) {
                    charPos = cleanText.length() - 1;
                }
                
                // 找到字符所在的两个颜色点之间
                int segment = 0;
                for (int j = 0; j < colorPositions.length - 1; j++) {
                    if (charPos >= colorPositions[j] && charPos <= colorPositions[j + 1]) {
                        segment = j;
                        break;
                    }
                }
                if (segment >= colorPositions.length - 1) {
                    segment = colorPositions.length - 2;
                }
                
                // 计算插值
                int startIdx = colorPositions[segment];
                int endIdx = colorPositions[segment + 1];
                double ratio = (endIdx == startIdx) ? 0 : (double)(charPos - startIdx) / (endIdx - startIdx);
                
                String color1 = allColors.get(segment);
                String color2 = allColors.get(segment + 1);
                String interpolatedColor = interpolateColor(color1, color2, ratio);
                
                // 应用颜色
                result.append(hexToMinecraftColor(interpolatedColor));
                result.append(ch);
                
                textIndex++;
            }
            
            return result.toString();
        }
    }
    
    /**
     * 应用渐变到文本（两色渐变）
     */
    private static String applyGradient(String text, String color1, String color2) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 移除颜色代码字符，只计算实际显示字符
        String cleanText = text.replaceAll("&[0-9a-fk-or]", "");
        if (cleanText.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        int textIndex = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            
            // 跳过颜色代码
            if (ch == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if ("0123456789abcdefklmnor".indexOf(Character.toLowerCase(next)) >= 0) {
                    result.append(ch).append(next);
                    i++;
                    continue;
                }
            }
            
            // 计算当前字符的渐变比例
            double ratio = cleanText.length() > 1 ? (double) textIndex / (cleanText.length() - 1) : 0;
            String interpolatedColor = interpolateColor(color1, color2, ratio);
            
            // 应用颜色
            result.append(hexToMinecraftColor(interpolatedColor));
            result.append(ch);
            
            textIndex++;
        }
        
        return result.toString();
    }
    
    /**
     * 颜色插值（从color1到color2，ratio从0到1）
     */
    private static String interpolateColor(String color1, String color2, double ratio) {
        int r1 = Integer.parseInt(color1.substring(0, 2), 16);
        int g1 = Integer.parseInt(color1.substring(2, 4), 16);
        int b1 = Integer.parseInt(color1.substring(4, 6), 16);
        
        int r2 = Integer.parseInt(color2.substring(0, 2), 16);
        int g2 = Integer.parseInt(color2.substring(2, 4), 16);
        int b2 = Integer.parseInt(color2.substring(4, 6), 16);
        
        int r = (int) Math.round(r1 + (r2 - r1) * ratio);
        int g = (int) Math.round(g1 + (g2 - g1) * ratio);
        int b = (int) Math.round(b1 + (b2 - b1) * ratio);
        
        // 确保值在0-255范围内
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        
        return String.format("%02X%02X%02X", r, g, b);
    }
    
    /**
     * 将16进制颜色转换为Minecraft格式: &x&R&R&G&G&B&B
     */
    private static String hexToMinecraftColor(String hex) {
        if (hex == null || hex.length() != 6) {
            return "";
        }
        hex = hex.toUpperCase();
        return "&x&" + hex.charAt(0) + "&" + hex.charAt(1) + 
               "&" + hex.charAt(2) + "&" + hex.charAt(3) + 
               "&" + hex.charAt(4) + "&" + hex.charAt(5);
    }
    
    /**
     * 处理16进制颜色格式
     * 
     * 支持的格式：
     * - &{#FFFFFF} → 转换为 &x&F&F&F&F&F&F
     * - <#FFFFFF> → 转换为 &x&F&F&F&F&F&F
     * 
     * Minecraft 1.16+ 使用 &x&R&R&G&G&B&B 格式表示16进制颜色
     * 其中每个字符前都需要 & 符号
     * 
     * @param text 包含16进制颜色代码的文本
     * @return 转换后的文本
     */
    private static String processHexColors(String text) {
        if (text == null || text.isEmpty()) return text;
        
        // 处理 &{#RRGGBB} 格式
        // 示例: &{#FF0000} → &x&F&F&0&0&0&0 (红色)
        text = text.replaceAll("&\\{#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])\\}", 
            "&x&$1&$2&$3&$4&$5&$6");
        
        // 处理 <#RRGGBB> 格式
        // 示例: <#00FF00> → &x&0&0&F&F&0&0 (绿色)
        text = text.replaceAll("<#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])>", 
            "&x&$1&$2&$3&$4&$5&$6");
        
        return text;
    }

    /**
     * 移除所有颜色代码（包括传统颜色代码、16进制颜色和渐变）
     * 
     * 这个方法会移除：
     * - 传统颜色代码: &a, &l, &r 等
     * - 16进制颜色: &x&R&R&G&G&B&B
     * - 渐变代码: <GRADIENT:...>, <g:...>, {#...>}, {#...<} 等
     * 
     * @param text 包含颜色代码的文本
     * @return 移除颜色代码后的纯文本
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        
        // 先移除传统颜色代码
        text = ChatColor.stripColor(text);
        
        // 移除16进制颜色代码格式: &x&R&R&G&G&B&B (Minecraft 1.16+格式)
        // 匹配 &x 后跟6个 &[0-9A-Fa-f] 的组合
        text = text.replaceAll("&x(&[0-9A-Fa-f]){6}", "");
        
        // 移除渐变代码格式（在colorize之前）: <GRADIENT:#...#:...#>...</GRADIENT> 或 <g:#...#:...#>...</g>
        text = text.replaceAll("<(?:GRADIENT|g):#[0-9A-Fa-f]{6}:#[0-9A-Fa-f]{6}>.*?</(?:GRADIENT|g)>", "");
        
        // 移除自定义渐变格式（在colorize之前）: {#RRGGBB>}, {#RRGGBB<>}, {#RRGGBB<}
        text = text.replaceAll("\\{#[0-9A-Fa-f]{6}(>|<>|<)\\}", "");
        
        // 移除16进制颜色格式（在colorize之前）: &{#RRGGBB} 或 <#RRGGBB>
        text = text.replaceAll("&?\\{#[0-9A-Fa-f]{6}\\}", "");
        text = text.replaceAll("<#[0-9A-Fa-f]{6}>", "");
        
        // 移除所有剩余的 & 符号（可能是颜色代码的一部分）
        text = text.replaceAll("&[0-9a-fk-orx]", "");
        
        return text.trim();
    }
}

