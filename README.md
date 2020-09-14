# keycloak-proxy-idp

Keycloak intergate with JustAuth

## quickstart with docker

### start the keycloak with idp proxy enabled

```bash
  docker run -d  --name keycloak --restart=always \
    -e PROXY_ADDRESS_FORWARDING=true \
    -p 8080:8080 \
    edgora/keycloak:11.0.2
```

for details refer [Keycloak offical document](https://hub.docker.com/r/jboss/keycloak/) 

### start redis for cache user

```bash
docker run -d  --name redis --restart=always \
    -v ~/data/redis:/bitnami \
    -e REDIS_PASSWORD=passwd \
    -p 6379:6379 \
bitnami/redis:5.0.8
```

### start keycloak justauth proxy

```bash
docker run -d  --name justauth-proxy --restart=always \
   -e SPINRG_REDIS_HOST=redis \
    -e SPINRG_REDIS_PASSWORD=passwd \
    -e SPINRG_REDIS_PORT=6379 \
    -e SERVER_PORT=8080 \
    -p 8080:8080 \
    edgora/keycloak-justauth-proxy
```

for details refer [keycloak-justauth-proxy](https://github.com/EdgoraCN/keycloak-justauth-proxy.git)

### create proxy idp and set `proxyUrl` to keycloak-justauth-proxy address

## build

`mvn clean package`

To install the social wechat work one has to:

## install 

* Add the jar to the Keycloak server (create `providers` folder if needed):
  * `$ cp target/keycloak-proxy-idp-{x.y.z}.jar _KEYCLOAK_HOME_/providers/`

* Add config page templates to the Keycloak server:

  * `$ cp themes/base/admin/resources/partials/realm-identity-provider-proxy-idp.html _KEYCLOAK_HOME_/themes/base/admin/resources/partials/`
  * `$ cp themes/base/admin/resources/partials/realm-identity-provider-proxy-idp-ext.html _KEYCLOAK_HOME_/themes/base/admin/resources/partials/`
  * `$ cp themes/base/admin/messages/admin-messages_en.properties _KEYCLOAK_HOME_/themes/base/admin/messages/`

## references

* https://github.com/yanfeiwuji/keycloak-wework
* [JustAuth-Demo](https://github.com/justauth/JustAuth-demo)

