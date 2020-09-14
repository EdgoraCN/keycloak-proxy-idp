package org.keycloak.social.proxy;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class ProxyProviderConfig extends IdentityProviderModel{
    protected static final Logger logger = Logger.getLogger(ProxyProviderConfig.class);

    public ProxyProviderConfig(IdentityProviderModel model) {

        super(model);
    }

    public ProxyProviderConfig() {
        super();
    }

    public String getProxyUrl() {
        return getConfig().get("proxyUrl");
    }

    public void setProxyUrl(String proxyUrl) {
        getConfig().put("proxyUrl", proxyUrl);
    }

    public String getExtraConfig() {
        return getConfig().get("extraConfig");
    }

    public String setExtraConfig(String extraConfig) {
        return getConfig().put("extraConfig", extraConfig);
    }

    public Map<String, Object> getExtraConfigMap() {
        if (this.getExtraConfig()!=null&&this.getExtraConfig().trim().length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            try {
                return mapper.readValue(this.getExtraConfig(), typeRef);
            } catch (Exception e) {
                logger.error("parse config failed",e);
                return new HashMap<String,Object>();
            }
        } else {
            return new HashMap<String,Object>();
        }
    }
    public String getIdpType() {
        return getConfig().get("idpType");
    }

    public String setIdpType(String idpType) {
        return getConfig().put("idpType" ,idpType);
    }

    public String getAuthorizationUrl() {
        return getConfig().get("authorizationUrl");
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        getConfig().put("authorizationUrl", authorizationUrl);
    }

    public String getTokenUrl() {
        return getConfig().get("tokenUrl");
    }

    public void setTokenUrl(String tokenUrl) {
        getConfig().put("tokenUrl", tokenUrl);
    }

    public String getUserInfoUrl() {
        return getConfig().get("userInfoUrl");
    }

    public void setUserInfoUrl(String userInfoUrl) {
        getConfig().put("userInfoUrl", userInfoUrl);
    }

    public String getClientId() {
        return getConfig().get("clientId");
    }

    public void setClientId(String clientId) {
        getConfig().put("clientId", clientId);
    }

    

    public void setClientAuthMethod(String clientAuth) {
        getConfig().put("clientAuthMethod", clientAuth);
    }

    public String getClientSecret() {
        return getConfig().get("clientSecret");
    }

    public void setClientSecret(String clientSecret) {
        getConfig().put("clientSecret", clientSecret);
    }

    public String getDefaultScope() {
        return getConfig().get("defaultScope");
    }

    public void setDefaultScope(String defaultScope) {
        getConfig().put("defaultScope", defaultScope);
    }

    public boolean isLoginHint() {
        return Boolean.valueOf(getConfig().get("loginHint"));
    }

    public void setLoginHint(boolean loginHint) {
        getConfig().put("loginHint", String.valueOf(loginHint));
    }
    


    public boolean isUiLocales() {
        return Boolean.valueOf(getConfig().get("uiLocales"));
    }

    public void setUiLocales(boolean uiLocales) {
        getConfig().put("uiLocales", String.valueOf(uiLocales));
    }

    public String getPrompt() {
        return getConfig().get("prompt");
    }

    public String getForwardParameters() {
        return getConfig().get("forwardParameters");
    }

    public void setForwardParameters(String forwardParameters) {
       getConfig().put("forwardParameters", forwardParameters);
    }

}
