package pt.ipleiria.KeyStrokeStorer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.jboss.logging.Logger;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PasswordChangeListener implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(PasswordChangeListener.class);
    private final KeycloakSession session;

    public PasswordChangeListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getType();

        boolean credentialUpdate = EventType.UPDATE_CREDENTIAL.equals(event.getType()); //UPDATE credential is trigered for any update (otp, webaut, ...)

        if (eventType.equals(EventType.RESET_PASSWORD) || eventType.equals(EventType.UPDATE_PASSWORD) || credentialUpdate) {
            if (credentialUpdate) {
                String credentialType = event.getDetails().get("credential_type");
                if (!credentialType.equalsIgnoreCase("password")) {
                    return;
                }
            }
            RealmModel realm = session.getContext().getRealm();

            deleteUserKeystrokeData(realm, event.getUserId());
        } else if (eventType.equals(EventType.LOGIN)){
            updateLastKeystrokeToValidated(event.getUserId(), event.getRealmId());
        }
    }

    private void updateLastKeystrokeToValidated(String userId, String realmId) {
        RealmModel realm = session.realms().getRealm(realmId);
        UserModel user = session.users().getUserById(realm, userId);

        if (user == null) return;

        String attributeKey = "keystroke_history_decoded";

         List<String> history = user.getAttributeStream(attributeKey)
                .collect(Collectors.toCollection(ArrayList::new));

        if (history.isEmpty()) return;

        int lastIndex = history.size() - 1;
        String lastEntryJson = history.getLast();

        try {
            ObjectNode node = (ObjectNode) JsonSerialization.mapper.readTree(lastEntryJson);

            node.put("validated", true);

            history.set(lastIndex, JsonSerialization.writeValueAsString(node));

            user.setAttribute(attributeKey, history);

            logger.info("Keystroke Dynamics VALIDADO com sucesso para user: " + user.getUsername());

        } catch (IOException e) {
            logger.error("Falha ao validar keystroke JSON", e);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (ResourceType.USER.equals(event.getResourceType())
                && OperationType.ACTION.equals(event.getOperationType())) {

            if (event.getResourcePath() != null && event.getResourcePath().endsWith("/reset-password")) {
                String[] pathParts = event.getResourcePath().split("/");

                if (pathParts.length >= 2) {
                    String userId = pathParts[1];

                    // 4. Obter o Realm correto (importante usar o ID do evento, pois o admin pode estar noutro realm)
                    RealmModel realm = session.realms().getRealm(event.getRealmId());

                    deleteUserKeystrokeData(realm, userId);
                }
            }
        }
    }

    private void deleteUserKeystrokeData(RealmModel realm, String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user != null) {
            user.removeAttribute("keystroke_history_decoded");
        }
    }

    @Override
    public void close() {

    }
}
