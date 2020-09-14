FROM jboss/keycloak:11.0.2
COPY $PWD/target/keycloak-proxy-idp-11.0.2.jar $JBOSS_HOME/standalone/deployments/keycloak-proxy-idp-11.0.2.jar
COPY $PWD/themes/base/admin/resources/partials/realm-identity-provider-proxy-idp.html $JBOSS_HOME/themes/base/admin/resources/partials/realm-identity-provider-proxy-idp.html
COPY $PWD/themes/base/admin/resources/partials/realm-identity-provider-proxy-idp-ext.html $JBOSS_HOME/themes/base/admin/resources/partials/realm-identity-provider-proxy-idp-ext.html
COPY $PWD/themes/base/admin/messages/admin-messages_en.properties $JBOSS_HOME/themes/base/admin/messages/admin-messages_en.properties