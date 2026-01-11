package pt.ipleiria.FreemanModel;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;
import pt.ipleiria.common.KeystrokeHistoryEntryDTO;
import pt.ipleiria.common.UserTypingData;
import pt.ipleiria.common.UserUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FreemanAuthenticator implements ConditionalAuthenticator {

    private static final Logger logger = Logger.getLogger(FreemanAuthenticator.class);
    private final FreemanModel freemanModel = new FreemanModel();
    private static final double RISK_THRESHOLD = 1.5;

    @Override
    public boolean matchCondition(AuthenticationFlowContext authenticationFlowContext) {
        UserModel user = authenticationFlowContext.getUser();

        if (user == null) {
            return false;
        }

        UserTypingData lastTypingData = UserUtils.getDecodedDataToEvaluate(user);

        GlobalStatsManager.getInstance().loadGlobalStats(authenticationFlowContext.getSession(), authenticationFlowContext.getRealm(), lastTypingData.deviceInfo.isMobile);


        List<UserTypingData> userTypingData = UserUtils.getDecodedData(user, lastTypingData.deviceInfo.isMobile);

        logger.info("User has : " + String.valueOf(userTypingData.size() + " registers."));

        if (userTypingData.size() < 2) {
            if (lastTypingData != null) {
                GlobalStatsManager.getInstance().addToStats(lastTypingData);
            }
            return true;
        }

        double riskScore = freemanModel.calculateRiskScore(lastTypingData, userTypingData);

        logger.infof("Freeman Analysis - User: %s | Score: %.4f | Threshold: %.1f",
                user.getUsername(), riskScore, RISK_THRESHOLD);

        GlobalStatsManager.getInstance().addToStats(lastTypingData);

        //TODO --> update validated from last login data (possibly after otp or if riskscore < threshold)

        // 6. DECISÃO
        // Se Score > Threshold -> True (Ativa o OTP no sub-fluxo)
        return riskScore > RISK_THRESHOLD;
    }

    protected void setLastLoginAsValidated(UserModel user) {
        String attributeKey = "keystroke_history_decoded";
        List<String> currentHistory = user.getAttributeStream(attributeKey)
                .collect(Collectors.toCollection(ArrayList::new));

        if (currentHistory.size() < 1) {
            return;
        }
        KeystrokeHistoryEntryDTO entry = null;
        try {
            entry = JsonSerialization.readValue(currentHistory.getLast(), KeystrokeHistoryEntryDTO.class);

            if (entry == null) {
                return;
            }
            entry.validated = true;
            String updatedJson = JsonSerialization.writeValueAsString(entry);

            currentHistory.set(currentHistory.size() - 1, updatedJson);
            user.setAttribute(attributeKey, currentHistory);

        } catch (IOException e) {
            logger.error("Falha ao deserializar entrada de histórico: " + e.getMessage());
        }
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {

    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.success();
    }
}
