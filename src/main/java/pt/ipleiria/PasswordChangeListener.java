package pt.ipleiria;

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
import org.keycloak.services.resources.admin.UserResource;

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
