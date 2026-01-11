package pt.ipleiria.common;

import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;
import pt.ipleiria.FreemanModel.FreemanAuthenticator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserUtils {

    private static final Logger logger = Logger.getLogger(FreemanAuthenticator.class);

    public static UserTypingData getDecodedDataToEvaluate(UserModel user) {
        String attributeKey = "keystroke_history_decoded";
        List<String> currentHistory = user.getAttributeStream(attributeKey)
                .collect(Collectors.toCollection(ArrayList::new));

        if (currentHistory.isEmpty()) {
            return null;
        }
        KeystrokeHistoryEntryDTO entry = null;
        try {
            entry = JsonSerialization.readValue(currentHistory.getLast(), KeystrokeHistoryEntryDTO.class);
        } catch (IOException e) {
            logger.error("Falha ao deserializar entrada de histórico: " + e.getMessage());
        }

        return entry != null ? entry.toUserTypingData() : null;
    }

    public static List<UserTypingData> getDecodedData(UserModel user, boolean mobile) {
        String attributeKey = "keystroke_history_decoded";
        List<String> currentHistory = user.getAttributeStream(attributeKey)
                .collect(Collectors.toCollection(ArrayList::new));

        List<UserTypingData> decodedHistory = new ArrayList<>();

        for (String jsonString : currentHistory) {
            try {
                // Converte a String JSON para o Objeto Java
                KeystrokeHistoryEntryDTO entry = JsonSerialization.readValue(jsonString, KeystrokeHistoryEntryDTO.class);
                if (entry.validated && entry.deviceInfo.isMobile == mobile) {
                    decodedHistory.add(entry.toUserTypingData());
                }
            } catch (IOException e) {
                logger.error("Falha ao deserializar entrada de histórico: " + e.getMessage());
            }
        }
        return decodedHistory;
    }
}
