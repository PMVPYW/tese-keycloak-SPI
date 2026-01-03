package pt.ipleiria.common;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystrokeHistoryEntryDTO {

    @JsonProperty("timestamp")
    public Long timestamp;

    @JsonProperty("date_readable")
    public String dateReadable;

    @JsonProperty("username_metrics")
    public TypingMetricsDTO usernameMetrics;

    @JsonProperty("password_metrics")
    public TypingMetricsDTO passwordMetrics;

    @JsonProperty("device_info")
    public DeviceInfoDTO deviceInfo;

    @JsonProperty("validated")
    public boolean validated;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypingMetricsDTO {
        @JsonProperty("totalTimeMs")
        public Integer totalTimeMs;

        @JsonProperty("events")
        public List<KeystrokeEventDTO> events;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeystrokeEventDTO {
        @JsonProperty("index")
        public Integer index;

        @JsonProperty("seekTime")
        public Integer seekTime;

        @JsonProperty("pressTime")
        public Integer pressTime;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceInfoDTO {
        @JsonProperty("browser")
        public String browser;

        @JsonProperty("isMobile")
        public Boolean isMobile;

        @JsonProperty("hasMotion")
        public Boolean hasMotion;

        @JsonProperty("screenDimensions")
        public ScreenDimensionsDTO screenDimensions;

        @JsonProperty("platform")
        public String platform;

        @JsonProperty("language")
        public String language;

        @JsonProperty("userAgent")
        public String userAgent;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScreenDimensionsDTO {
        @JsonProperty("width")
        public Integer width;

        @JsonProperty("height")
        public Integer height;

        @JsonProperty("resolution")
        public String resolution;
    }

    public UserTypingData toUserTypingData() {
        DecodedData userMetrics = null;
        DecodedData passMetrics = null;
        DeviceInfo devInfo = null;

        // Converter métricas de username
        if (this.usernameMetrics != null) {
            userMetrics = mapToDecodedData(this.usernameMetrics);
        }

        // Converter métricas de password
        if (this.passwordMetrics != null) {
            passMetrics = mapToDecodedData(this.passwordMetrics);
        }

        // Converter info do dispositivo
        if (this.deviceInfo != null) {
            devInfo = mapToDeviceInfo(this.deviceInfo);
        }

        return new UserTypingData(userMetrics, passMetrics, devInfo, this.validated);
    }

    private DecodedData mapToDecodedData(TypingMetricsDTO dto) {
        DecodedData data = new DecodedData();
        data.totalTimeMs = dto.totalTimeMs;

        if (dto.events != null) {
            // Assume que DecodedData tem uma List<KeystrokeEvent>
            data.events = dto.events.stream()
                    .map(e -> {
                        // Instanciar o evento original (ajusta o nome da classe se necessário)
                        return new KeystrokeEvent(e.index, e.seekTime, e.pressTime);
                    })
                    .collect(Collectors.toList());
        } else {
            data.events = new ArrayList<>();
        }
        return data;
    }

    private DeviceInfo mapToDeviceInfo(DeviceInfoDTO dto) {
        DeviceInfo info = new DeviceInfo();
        info.browser = dto.browser;
        info.isMobile = dto.isMobile;
        info.hasMotion = dto.hasMotion;
        info.platform = dto.platform;
        info.language = dto.language;
        info.userAgent = dto.userAgent;

        if (dto.screenDimensions != null) {
            // Instanciar ScreenDimensions original
            info.screenDimensions = new ScreenDimensions(dto.screenDimensions.width, dto.screenDimensions.height);
        }
        return info;
    }
}