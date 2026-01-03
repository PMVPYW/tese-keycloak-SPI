package pt.ipleiria.common;

public class DeviceInfo {

    public String browser;
    public Boolean isMobile;
    public Boolean hasMotion;
    public ScreenDimensions screenDimensions;
    public String platform;
    public String language;
    public String userAgent;

    public DeviceInfo(String browser, Boolean isMobile, Boolean hasMotion, ScreenDimensions screenDimensions, String platform, String language, String userAgent) {
        this.browser = browser;
        this.isMobile = isMobile;
        this.hasMotion = hasMotion;
        this.screenDimensions = screenDimensions;
        this.platform = platform;
        this.language = language;
        this.userAgent = userAgent;
    }

    public DeviceInfo() {
    }
}