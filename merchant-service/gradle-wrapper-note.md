# Gradle wrapper
The wrapper JAR could not be bundled (offline build environment). Generate it once with a
local Gradle install (8.5+):

    gradle wrapper --gradle-version 8.9

Or build directly without the wrapper:

    gradle clean bootJar
