mvn clean package
docker build --no-cache . -t edgora/keycloak:11.0.2
docker push  edgora/keycloak:11.0.2
docker tag  edgora/keycloak:11.0.2  registry.cn-beijing.aliyuncs.com/edgora-oss/keycloak:11.0.2
docker push registry.cn-beijing.aliyuncs.com/edgora-oss/keycloak:11.0.2