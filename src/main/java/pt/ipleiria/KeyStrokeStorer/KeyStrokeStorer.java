package pt.ipleiria.KeyStrokeStorer;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import pt.ipleiria.common.UserTypingData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KeyStrokeStorer implements Authenticator {

    private static final Logger logger = Logger.getLogger(KeyStrokeStorer.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        UserModel user = context.getUser();

        if (user != null) {
            try {
                // Descodificar
                UserTypingData decodedData = TypingPatternDecoder.decodeFromFormData(formData);

                if (decodedData.usernameData != null || decodedData.passwordData != null) {
                    // Criar JSON
                    ObjectNode historyEntry = JsonSerialization.createObjectNode();
                    historyEntry.put("timestamp", System.currentTimeMillis());
                    historyEntry.put("date_readable", java.time.Instant.now().toString());
                    historyEntry.put("validated", false);

                    if (decodedData.usernameData != null) {
                        historyEntry.set("username_metrics", JsonSerialization.mapper.valueToTree(decodedData.usernameData));
                    }

                    if (decodedData.passwordData != null) {
                        historyEntry.set("password_metrics", JsonSerialization.mapper.valueToTree(decodedData.passwordData));
                    }

                    if (decodedData.deviceInfo != null) {
                        historyEntry.set("device_info", JsonSerialization.mapper.valueToTree(decodedData.deviceInfo));
                    }

                    String jsonString = JsonSerialization.writeValueAsString(historyEntry);

                    String attributeKey = "keystroke_history_decoded";

                    // Obter valores atuais (stream -> list mutável)
                    List<String> currentHistory = user.getAttributeStream(attributeKey)
                            .collect(Collectors.toCollection(ArrayList::new));

                    // Adicionar o novo registo
                    currentHistory.add(jsonString);

                    // Guardar a lista atualizada no utilizador
                    user.setAttribute(attributeKey, currentHistory);

                    logger.info("Histórico descodificado salvo para: " + user.getUsername());
                }

            } catch (IOException e) {
                logger.error(e.getMessage());
                logger.error("Erro ao serializar dados de keystroke.");
            }
        }

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}