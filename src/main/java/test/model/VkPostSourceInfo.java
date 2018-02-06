package test.model;

public class VkPostSourceInfo {
    public Integer ownerId;
    public Integer offset;
    public Integer count;

    public VkPostSourceInfo() {
    }

    public VkPostSourceInfo(Integer ownerId, int offset, Integer count) {
        this.ownerId = ownerId;
        this.offset = offset;
        this.count = count;
    }
}
