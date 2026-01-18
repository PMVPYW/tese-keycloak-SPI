package pt.ipleiria.common;

public class ScreenDimensions {
    public long width;
    public long height;
    public String resolution;

    public ScreenDimensions() {
    }

    public ScreenDimensions(long width, long height) {
        this.width = width;
        this.height = height;
        this.resolution = width + "x" + height;
    }
}
