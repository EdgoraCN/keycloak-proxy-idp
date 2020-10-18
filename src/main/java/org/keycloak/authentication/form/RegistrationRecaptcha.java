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

package org.keycloak.authentication.form;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.common.util.ServerCookie;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationRecaptcha implements FormAction, FormActionFactory, ConfiguredProvider {
    public static final String G_CAPTCHA_CODE = "captcha_code";
    public static final String G_CAPTCHA_ID = "captcha_id";
    public static final String CAPTCHA_REFERENCE_CATEGORY = "easycaptcha";
    public static final String CAPTCHA_COOKIE_KEY = "easy_captcha";
    public static final String SITE_KEY = "site.key";
    public static final String SITE_SECRET = "secret";
    public static final String EASY_CAPTCHA_URL = "easyCaptchaUrl";
    public static final String EASY_CAPTCHA_TIMEOUT = "easyCaptchaTimeout";
    private static final Logger logger = Logger.getLogger(RegistrationRecaptcha.class);

    public static final String PROVIDER_ID = "edgora-registration-captcha-action";

    @Override
    public String getDisplayType() {
        return "EasyCaptcha";
    }

    @Override
    public String getReferenceCategory() {
        return CAPTCHA_REFERENCE_CATEGORY;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }
    protected String getCookie(FormContext context,String name) {
        Cookie cookie = context.getHttpRequest().getHttpHeaders().getCookies().get(name);
        if(cookie!=null){
            return cookie.getValue();
        } else {
            return null;
        }
    }
    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
        String userLanguageTag = context.getSession().getContext().resolveLocale(context.getUser()).toLanguageTag();
        if (captchaConfig == null || captchaConfig.getConfig() == null || Validation.isBlank(captchaConfig.getConfig().get(EASY_CAPTCHA_URL))) {
            form.addError(new FormMessage(null, Messages.EASY_CAPTCHA_NOT_CONFIGURED));
            return;
        }
        String siteKey = captchaConfig.getConfig().get(SITE_KEY);
        String secret = captchaConfig.getConfig().get(SITE_SECRET);
        String timeout = captchaConfig.getConfig().get(EASY_CAPTCHA_TIMEOUT);

        form.setAttribute("easyCaptchaRequired", true);
        form.setAttribute("easyCaptchaSiteKey", siteKey);
        HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        String id =  getCookie(context,CAPTCHA_COOKIE_KEY);
        logger.info("captcha id="+id);
        if(Validation.isBlank(id)){
            id = UUID.randomUUID().toString().replace("-", "");
            setCookie(context, CAPTCHA_COOKIE_KEY, id);
        }
        HttpGet get = new HttpGet(getCaptchaDomain(context.getAuthenticatorConfig()) + "/api/admin/start/"+id+"/"+(Validation.isBlank(timeout)?"120":timeout));
        get.addHeader("secret",secret);
        try {
            HttpResponse response = httpClient.execute(get);
            InputStream content = response.getEntity().getContent();
            try {
                logger.info("start captcha with id "+id);
            } finally {
                content.close();
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }
        form.setAttribute(G_CAPTCHA_ID, id);
        form.addScript(getCaptchaDomain(captchaConfig) + "/static/captcha.min.js");
    }

    protected void setCookie(FormContext context, String name, String value) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int maxCookieAge = 60 * 60 * 24 * 30; // 30 days
        URI uri = context.getUriInfo().getBaseUriBuilder().path("realms").path(context.getRealm().getName()).build();
        addCookie(context, name, value,
                uri.getRawPath(),
                null, null,
                maxCookieAge,
                false, true);
    }


    public void addCookie(FormContext context, String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly) {
        org.jboss.resteasy.spi.HttpResponse response = context.getSession().getContext().getContextObject(org.jboss.resteasy.spi.HttpResponse.class);
        StringBuffer cookieBuf = new StringBuffer();
        ServerCookie.appendCookieValue(cookieBuf, 1, name, value, path, domain, comment, maxAge, secure, httpOnly, null);
        String cookie = cookieBuf.toString();
        response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, cookie);
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        boolean success = false;
        context.getEvent().detail(Details.REGISTER_METHOD, "form");

        String code = formData.getFirst(G_CAPTCHA_CODE);
        String id = getCookie(context, CAPTCHA_COOKIE_KEY);
        if (!Validation.isBlank(code)&&!Validation.isBlank(id)) {
            AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
            String secret = captchaConfig.getConfig().get(SITE_SECRET);
            success = validateRecaptcha(context, success,id ,code, secret);
        }
        if (success) {
            context.success();
        } else {
            errors.add(new FormMessage(null, Messages.EASY_CAPTCHA_FAILED));
            formData.remove(G_CAPTCHA_CODE);
            formData.remove(G_CAPTCHA_ID);
            context.error(Errors.INVALID_REGISTRATION);
            context.validationError(formData, errors);
            context.excludeOtherErrors();
            return;
        }
    }

    private String getCaptchaDomain(AuthenticatorConfigModel config) {
        String base_url = Optional.ofNullable(config)
                .map(configModel -> configModel.getConfig())
                .map(cfg -> cfg.get(EASY_CAPTCHA_URL))
                .orElse("");
        if (Validation.isBlank(base_url)) {
            return "https://cc.hroze.org";
        }
        return base_url;
    }

    protected boolean validateRecaptcha(ValidationContext context, boolean success,String id,String code, String secret) {
        HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        HttpGet get = new HttpGet(getCaptchaDomain(context.getAuthenticatorConfig()) + "/api/admin/verify/"+id+"/"+code);
        get.addHeader("secret",secret);
        get.addHeader("remoteip",context.getConnection().getRemoteAddr());
        try {
            HttpResponse response = httpClient.execute(get);
            InputStream content = response.getEntity().getContent();
            try {
                Map json = JsonSerialization.readValue(content, Map.class);
                Object val = json.get("success");
                success = Boolean.TRUE.equals(val);
            } finally {
                content.close();
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }
        return success;
    }

    @Override
    public void success(FormContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }


    @Override
    public void close() {

    }

    @Override
    public FormAction create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Adds Google Recaptcha button.  Recaptchas verify that the entity that is registering is a human.  This can only be used on the internet and must be configured after you add it.";
    }

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(SITE_KEY);
        property.setLabel("Recaptcha Site Key");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Google Recaptcha Site Key");
        CONFIG_PROPERTIES.add(property);
        property = new ProviderConfigProperty();
        property.setName(SITE_SECRET);
        property.setLabel("Recaptcha Secret");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Google Recaptcha Secret");
        CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(EASY_CAPTCHA_URL);
        property.setLabel("captcha url");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("the base url of easy captcha base url");
        CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(EASY_CAPTCHA_TIMEOUT);
        property.setLabel("captcha timeout");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("the timeout for easy captcha");
        CONFIG_PROPERTIES.add(property);

        
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }
}