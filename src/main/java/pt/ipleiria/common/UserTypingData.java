package pt.ipleiria.common;

// Container para os dois campos
public class UserTypingData {
    public DecodedData usernameData;
    public DecodedData passwordData;
    public DeviceInfo deviceInfo;
    public boolean validated;

    public UserTypingData() {
    }

    public UserTypingData(DecodedData usernameData, DecodedData passwordData, DeviceInfo deviceInfo, boolean validated) {
        this.usernameData = usernameData;
        this.passwordData = passwordData;
        this.deviceInfo = deviceInfo;
        this.validated = validated;
    }
}