package pt.ipleiria.IsolationFlorest;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import pt.ipleiria.common.UserTypingData;
import pt.ipleiria.common.UserUtils;

public class IsolationFlorestAuthenticator implements ConditionalAuthenticator {
    private static final Logger logger = Logger.getLogger(IsolationFlorestAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (user == null) {
            return false;
        }
        BiometricRiskModel isolationForest = new BiometricRiskModel();

        UserTypingData lastLogin = UserUtils.getDecodedDataToEvaluate(user);

        if (lastLogin == null){
            return false;
        }

        double risk = isolationForest.calculateRiskWithIsolationFLoresr(lastLogin, UserUtils.getDecodedData(user, lastLogin.deviceInfo.isMobile));

        logger.info("User has risk of: " + risk);

        //case its not the user, we ask for the next step (OTP code)
        return risk > 0.6;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }
}
