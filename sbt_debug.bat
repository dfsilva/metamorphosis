set BUILD_NUMBER=108
set SBT_OPTS=-Xms512M -Xmx2048M -Xss1M -XX:+CMSClassUnloadingEnabled -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
java %SBT_OPTS% -jar sbt-launch.jar