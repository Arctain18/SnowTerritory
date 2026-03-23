package top.arctain.snowTerritory.armor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArmorSetArgParser {

    private static final Pattern WEIGHT_SUFFIX = Pattern.compile("^([^\\[]+)\\[([^\\]]+)\\]$");

    private ArmorSetArgParser() {
    }

    public record Parsed(String setId, int[] qualityWeights) {
        public boolean hasQualityWeights() {
            return qualityWeights != null;
        }
    }

    public static Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Parsed("", null);
        }
        String trimmed = raw.trim();
        Matcher m = WEIGHT_SUFFIX.matcher(trimmed);
        if (!m.matches()) {
            return new Parsed(trimmed, null);
        }
        String setId = m.group(1).trim();
        String inner = m.group(2).trim();
        if (setId.isEmpty()) {
            return new Parsed(trimmed, null);
        }
        List<Integer> nums = new ArrayList<>();
        for (String part : inner.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            try {
                nums.add(Integer.parseInt(p));
            } catch (NumberFormatException e) {
                return new Parsed(setId, new int[0]);
            }
        }
        if (nums.size() != 3) {
            return new Parsed(setId, new int[0]);
        }
        return new Parsed(setId, new int[]{nums.get(0), nums.get(1), nums.get(2)});
    }

    public static String tabCompletePrefix(String partial) {
        if (partial == null) {
            return "";
        }
        int i = partial.indexOf('[');
        if (i < 0) {
            return partial;
        }
        return partial.substring(0, i);
    }
}
