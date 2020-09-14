package org.keycloak.social.proxy;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * User attribute mapper.
 *
 */

public class ProxyUserAttributeMapper extends AbstractIdentityProviderMapper {
    private static final String[] cp = new String[] { ProxyIdentityProviderFactory.PROVIDER_ID };

    @Override
    public String[] getCompatibleProviders() {
        return cp;
    }

    @Override
    public String getId() {
        return "proxy-idp-user-attribute-mapper";
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // 更新用户信息
        Stream.of(ProxyIdentityProvider.userInfoKeys).forEach(i -> {
            user.setSingleAttribute(i, i);
        });
    }

    @Override
    public String getDisplayCategory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDisplayType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHelpText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // TODO Auto-generated method stub
        return null;
    }
}
