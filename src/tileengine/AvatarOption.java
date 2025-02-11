package tileengine;

public class AvatarOption {
    private final String name;
    private final String previewPath;
    private final int index;

    public AvatarOption(String name, String previewPath, int index) {
        this.name = name;
        this.previewPath = previewPath;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public int getIndex() {
        return index;
    }
}