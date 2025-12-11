package top.arctain.snowTerritory.enderstorage.config;

public class ProgressionLevel {

    private final String permission;
    private final int value;

    public ProgressionLevel(String permission, int value) {
        this.permission = permission;
        this.value = value;
    }

    public String getPermission() {
        return permission;
    }

    public int getValue() {
        return value;
    }
}

