# keycloak-proxy-idp

Keycloak intergate with JustAuth

To build:

`mvn clean package`

To install the social wechat work one has to:

* Add the jar to the Keycloak server (create `providers` folder if needed):
  * `$ cp target/keycloak-proxy-idp-{x.y.z}.jar _KEYCLOAK_HOME_/providers/`

* Add config page templates to the Keycloak server:
  * `$ cp themes/base/admin/resources/partials/realm-identity-provider-proxy-idp.html _KEYCLOAK_HOME_/themes/base/admin/resources/partials/`
  * `$ cp themes/base/admin/resources/partials/realm-identity-provider-proxy-idp-ext.html _KEYCLOAK_HOME_/themes/base/admin/resources/partials/`
  * `$ cp themes/base/admin/messages/admin-messages_en.properties _KEYCLOAK_HOME_/themes/base/admin/messages/`

based on  https://github.com/yanfeiwuji/keycloak-wework
