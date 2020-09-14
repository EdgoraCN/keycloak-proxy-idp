package org.keycloak.social.proxy.dto;

public class ProxyIdpConfig {

    private String alias;
    /**
     * idp proxy type
     */
    private String idpType;
    /**
     * idp setting
     */
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String alipayPublicKey;
    private String stackOverflowKey;
    private String agentId;
    private String codingGroupName;
    /**
     * proxy setting
     */
    private String proxyHost;
    private String proxyType;
    private Integer proxyPort;
    private Integer proxyTimeout;

    public String getIdpType() {
        return idpType;
    }

    public void setIdpType(String idpType) {
        this.idpType = idpType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public String getStackOverflowKey() {
        return stackOverflowKey;
    }

    public void setStackOverflowKey(String stackOverflowKey) {
        this.stackOverflowKey = stackOverflowKey;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getCodingGroupName() {
        return codingGroupName;
    }

    public void setCodingGroupName(String codingGroupName) {
        this.codingGroupName = codingGroupName;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Integer getProxyTimeout() {
        return proxyTimeout;
    }

    public void setProxyTimeout(Integer proxyTimeout) {
        this.proxyTimeout = proxyTimeout;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

}
