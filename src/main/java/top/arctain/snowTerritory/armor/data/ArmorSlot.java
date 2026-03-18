package top.arctain.snowTerritory.armor.data;

public class ArmorSlot {

    private final String id;
    private final String slotKey;
    private final String templateId;

    public ArmorSlot(String id, String slotKey, String templateId) {
        this.id = id;
        this.slotKey = slotKey;
        this.templateId = templateId;
    }

    public String getId() {
        return id;
    }

    public String getSlotKey() {
        return slotKey;
    }

    public String getTemplateId() {
        return templateId;
    }
}

