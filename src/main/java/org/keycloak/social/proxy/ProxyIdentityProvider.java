/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.social.proxy;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.social.proxy.dto.AuthResponse;
import org.keycloak.social.proxy.dto.AuthUser;

public class ProxyIdentityProvider extends AbstractIdentityProvider<ProxyProviderConfig> implements SocialIdentityProvider<ProxyProviderConfig>{

    protected static final Logger logger = Logger.getLogger(ProxyIdentityProvider.class);

    protected static ObjectMapper mapper = new ObjectMapper();

    public static final String DEFAULT_SCOPE = "";
    public static CloseableHttpClient HTTP_CLIENT;
    public static String CACHE_NAME = "proxy-idp";

    public static String[] userInfoKeys = new String[] { "userid", "blog", "source", "email", "avatar", "remark",
            "gender", "company", "location" };
    private String proxyUrl;
    private String idpType;
    private Map<String, Object> extraConfig = new HashMap<>();
    private String authorizeUrl;
    private String registerURL;
    private String renderURL;
    private String loginURL;
    private String refreshURL;
    private String revokeURL;
    private static final Map<String,String> LOGIN_CACHE = new HashMap<>();

    private HttpClient getHttpClient() {
        if (HTTP_CLIENT == null) {
            RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000)
                    .setConnectionRequestTimeout(10000).build();
            HTTP_CLIENT = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
        }
        return HTTP_CLIENT;
    }
    private void register() {
        String cachedUrl = LOGIN_CACHE.get(this.getConfig().getAlias());
        if(cachedUrl!=null&&!cachedUrl.trim().equals("")){
            this.authorizeUrl = cachedUrl;
            return;
        }
        Map<String, Object> config = new HashMap<>();
        config.put("idpType", this.idpType);
        config.put("clientId", getConfig().getClientId());
        config.put("clientSecret", getConfig().getClientSecret());
        config.put("scope", getConfig().getDefaultScope());
        config.putAll(this.extraConfig);
        config.put("alias",getConfig().getAlias());
        try {
            String responseBody = SimpleHttp.doPost(this.registerURL, this.getHttpClient()).json(config).asString();
            AuthResponse<Map<String, Object>> res = SimpleHttp.mapper.readValue(responseBody,AuthResponse.class);
            logger.info("registry response=" + responseBody);
            if(res.ok()) {
                this.authorizeUrl = res.getData().get("authorizeUrl").toString();
                LOGIN_CACHE.put(this.getConfig().getAlias(),this.authorizeUrl);
            }
        } catch (Exception e) {
            logger.error("register  proxy idp failed", e);
        }
    }

    private Map<String,Object> login(String redirectUri,MultivaluedMap<String, String> map) {
        final UriBuilder uriBuilder = UriBuilder.fromUri(this.loginURL + "/" + this.getConfig().getAlias());
        Map<String,String> data = new HashMap<>();
        data.put("redirectUri", redirectUri);
        for (String key : map.keySet()) {
            data.put(key, map.getFirst(key));
        }
        String uri = uriBuilder.build().toString();
        logger.info("login_uri=" + uri);
        try {
            String  responseBody= SimpleHttp.doPost(uri, this.getHttpClient()).json(data).asString();
            AuthResponse<Map<String,Object>> res = SimpleHttp.mapper.readValue(responseBody,AuthResponse.class);
            logger.info("login response=" + SimpleHttp.mapper.writeValueAsString(res));
            if (res.ok()) {
                return res.getData();
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("login with proxy idp failed", e);
        }
        return null;
    }

    public ProxyIdentityProvider(KeycloakSession session, ProxyProviderConfig config) {
        super(session, config);
        this.proxyUrl = config.getProxyUrl();
        this.idpType = config.getIdpType();
        this.loginURL = this.proxyUrl + "/auth/login";
        this.renderURL = this.proxyUrl + "/auth/render";
        this.refreshURL = this.proxyUrl + "/auth/refresh";
        this.revokeURL = this.proxyUrl + "/auth/revoke";
        this.registerURL = this.proxyUrl + "/auth/register";
        this.extraConfig = config.getExtraConfigMap();
        this.register();
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(callback, realm, event);
    }
	public static void storeUserProfileForMapper(BrokeredIdentityContext user, Map<String, Object> userData) {
		user.getContextData().put("authUser", userData);
	}
    protected BrokeredIdentityContext extractIdentityFromProfile(AuthUser user,Map<String, Object> userData) {
        BrokeredIdentityContext identity = new BrokeredIdentityContext(user.getUuid());
        identity.setBrokerUserId(this.idpType + "." + user.getUuid());
        identity.setUsername(user.getUsername());
        identity.setModelUsername(user.getUsername());
        identity.setFirstName(user.getNickname()!=null?user.getNickname():"");
        identity.setLastName("");
        identity.setEmail(user.getEmail()!=null?user.getEmail():"");
        identity.setUserAttribute(this.idpType + ".userid", user.getUuid());
        identity.setUserAttribute(this.idpType + ".email", user.getEmail());
        identity.setUserAttribute(this.idpType + ".avatar", user.getAvatar());
        identity.setUserAttribute(this.idpType + ".company", user.getCompany());
        identity.setUserAttribute(this.idpType + ".source", user.getSource());
        identity.setUserAttribute(this.idpType + ".blog", user.getBlog());
        identity.setUserAttribute(this.idpType + ".location", user.getLocation());
        identity.setUserAttribute(this.idpType + ".remark", user.getRemark());
        identity.setUserAttribute(this.idpType + ".gender", user.getGender());
        storeUserProfileForMapper(identity, userData);
        return identity;

    }

    public BrokeredIdentityContext getFederatedIdentity(AuthUser user,Map<String, Object> userData) {
        Object at = user.getToken().get("accessToken");
        if (at == null) {
            throw new IdentityBrokerException("No access token available");
        }
        String accessToken = at.toString();
        BrokeredIdentityContext context = extractIdentityFromProfile(user,userData);
        // JsonNode profile = SimpleHttp.mapper.readTree("{}");
        context.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
        return context;
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        try {
            this.register();
            String url = this.authorizeUrl.replace("{state}", request.getState().getEncoded()).replace("http://redirectUri",
                    request.getRedirectUri());
            URI authorizationUrl = URI.create(url);
            logger.info("auth url " + authorizationUrl.toString());
            return Response.seeOther(authorizationUrl).build();
        } catch (Exception e) {
            logger.error("performLogin failed", e);
            throw new IdentityBrokerException("Could not create authentication request.", e);
        }
    }

    protected class Endpoint {
        protected AuthenticationCallback callback;
        protected RealmModel realm;
        protected EventBuilder event;

        @Context
        protected KeycloakSession session;

        @Context
        protected ClientConnection clientConnection;

        @Context
        protected HttpHeaders headers;

        @Context
        protected UriInfo uriInfo;

        public Endpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            this.callback = callback;
            this.realm = realm;
            this.event = event;
        }
        public AuthUser toAuthUser(Map<String, Object> userData) {
            Field[] fields = AuthUser.class.getDeclaredFields();
            AuthUser authUser = new AuthUser();
            for (int i = 0; i < fields.length; i++) {
                String element = fields[i].getName();
                //String propertyType = fields[i].getType().getName();
                fields[i].setAccessible(true);
                Object value = userData.get(element);
                if (value!=null) {
                    try {
                            fields[i].set(authUser, value);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        logger.error("convert to AuthUser failed",e);
                    }
                }
            }
            return authUser;
        }
        @GET
        public Response authResponse(@Context UriInfo uriInfo, @Context Request request,
                @Context HttpHeaders httpHeaders) {
            MultivaluedMap<String, String> map = uriInfo.getQueryParameters(true);
            logger.info("params=" + map);
            String state = map.getFirst(AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_STATE);
            URI uri = uriInfo.getAbsolutePath();
            AuthUser user =null;
            Map<String, Object> userData = login(uri.toString(),map);
            if (userData == null) {
                logger.info("broker login failed," +userData);
                // if (error.equals(ACCESS_DENIED)) {
                // return callback.cancelled(state);
                // } else {
                event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                return callback.error(state, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                // }
            }
            user = toAuthUser(userData);
            BrokeredIdentityContext federatedIdentity = getFederatedIdentity(user,userData);
            //org.keycloak.services.resources.IdentityBrokerService;
            //org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext.
            federatedIdentity.setIdpConfig(getConfig());
            federatedIdentity.setIdp(ProxyIdentityProvider.this);
            federatedIdentity.setCode(state);
            event.event(EventType.LOGIN);
            // return ErrorPage.error(session, null,
            // Response.Status.BAD_GATEWAY,Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            return callback.authenticated(federatedIdentity);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            BrokeredIdentityContext context) {
        // 更新用户信息
        Stream.of(userInfoKeys).forEach(i -> {
            String value = context.getUserAttribute(idpType + "." + i);
            user.setSingleAttribute(idpType + "." + i, value != null ? value : "N/A");
        });
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        // TODO Auto-generated method stub
        return null;
    }
    public String getDefaultScopes(){
        return "";
    }
}
