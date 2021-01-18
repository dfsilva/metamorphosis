set SBT_OPTS=-Xms512M -Xmx2G -Xss2M -XX:+CMSClassUnloadingEnabled
java %SBT_OPTS% -jar sbt-launch.jar docker
docker-compose -f docker-compose-dev.yml -f docker-compose-server.yml up -d