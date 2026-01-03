package pt.ipleiria.KeyStrokeStorer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pt.ipleiria.common.*;

public class TypingPatternDecoder {

    /**
     * Entrada principal
     */
    public static UserTypingData decodeFromFormData(jakarta.ws.rs.core.MultivaluedMap<String, String> formData) {
        UserTypingData result = new UserTypingData();

        String usernamePattern = formData.getFirst("typingdna_username_sametext");
        String passwordPattern = formData.getFirst("typingdna_password_sametext");
        String deviceStringData = formData.getFirst("typingdna_device_info");

        if (usernamePattern != null && !usernamePattern.isBlank()) {
            result.usernameData = parsePattern(usernamePattern);
        }

        if (passwordPattern != null && !passwordPattern.isBlank()) {
            result.passwordData = parsePattern(passwordPattern);
        }

        if (deviceStringData != null && !deviceStringData.isBlank()) {
            result.deviceInfo = parseDeviceInfo(deviceStringData);
        }

        return result;
    }

    private static DeviceInfo parseDeviceInfo(String deviceStringInfo){
        JsonObject devJSON = JsonParser.parseString(deviceStringInfo).getAsJsonObject();
        DeviceInfo info = new DeviceInfo();
        if (devJSON.has("browser")) {
            info.browser = devJSON.get("browser").getAsString();
        }
        if (devJSON.has("isMobile")) {
            info.isMobile = devJSON.get("isMobile").getAsBoolean();
        }
        if (devJSON.has("hasMotion")) {
            info.hasMotion = devJSON.get("hasMotion").getAsBoolean();
        }
        if (devJSON.has("hasMotion")) {
            info.hasMotion = devJSON.get("hasMotion").getAsBoolean();
        }
        if (devJSON.has("screen")) {
            JsonObject screenInfo = devJSON.get("screen").getAsJsonObject();
            long witdth = screenInfo.get("width").getAsLong();
            long height = screenInfo.get("height").getAsLong();
            info.screenDimensions = new ScreenDimensions(witdth, height);
        }
        if (devJSON.has("platform")) {
            info.platform = devJSON.get("platform").getAsString();
        }
        if (devJSON.has("language")) {
            info.language = devJSON.get("language").getAsString();
        }
        if (devJSON.has("userAgent")) {
            info.userAgent = devJSON.get("userAgent").getAsString();
        }
        return info;
    }

    /**
     * Parse focada apenas em metadados biométricos
     */
    private static DecodedData parsePattern(String patternStr) {
        DecodedData output = new DecodedData();

        // Separar eventos pelo pipe "|"
        String[] parts = patternStr.split("\\|");

        // Loop começa em 1 para saltar o cabeçalho técnico
        for (int i = 1; i < parts.length; i++) {
            String segment = parts[i];

            // Parar se encontrar caracteres de controlo de movimento
            if (segment.isEmpty() || segment.startsWith("#") || segment.startsWith("/")) {
                break;
            }

            String[] val = segment.split(",");

            if (val.length >= 3) {
                try {
                    // val[0] é o charCode -> IGNORAMOS COMPLETAMENTE (Privacidade)
                    int seekTime = Integer.parseInt(val[1]); // tempo de voo a partir da tecla anterior
                    int pressTime = Integer.parseInt(val[2]);

                    // O índice é determinado pela ordem de inserção
                    int currentIndex = output.events.size();

                    output.events.add(new KeystrokeEvent(currentIndex, seekTime, pressTime));
                    output.totalTimeMs += (seekTime + pressTime);

                } catch (NumberFormatException e) {
                    // Ignora silenciosamente erros de parse para não quebrar o fluxo
                }
            }
        }

        return output;
    }
}