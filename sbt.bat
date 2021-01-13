set BUILD_NUMBER=108
set SBT_OPTS=-Xms512M -Xmx2G -Xss2M -XX:+CMSClassUnloadingEnabled
java %SBT_OPTS% -jar sbt-launch.jar