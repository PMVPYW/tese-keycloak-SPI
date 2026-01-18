package pt.ipleiria.IsolationFlorest;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class IsolationFlorestAuthenticatorFactory implements ConditionalAuthenticatorFactory {
    @Override
    public ConditionalAuthenticator getSingleton() {
        return new pt.ipleiria.IsolationFlorest.IsolationFlorestAuthenticator();
    }

    @Override
    public String getDisplayType() {
        return "Isolation Florest Authenticator";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Isolation FLorest Authentication";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "isolationFlorestAuth";
    }
}
