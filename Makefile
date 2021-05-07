EXTENSION-LIBRARY-FOLDER-NAME = places-monitor-android

BUILD-ASSEMBLE-LOCATION = ./ci/assemble
ROOT_DIR=$(shell git rev-parse --show-toplevel)

PROJECT_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleProjectName" | cut -d'=' -f2)
AAR_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleAARName" | cut -d'=' -f2)
MODULE_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleName" | cut -d'=' -f2)
LIB_VERSION = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleVersion" | cut -d'=' -f2)
SOURCE_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)
AAR_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)/build/outputs/aar



URL_MAVEN_METADATA = https://jcenter.bintray.com/com/adobe/marketing/mobile/places-monitor/maven-metadata.xml
FILE_NAMES_CONTAINS_VERSION = ./code/gradle.properties ./code/places-monitor-android/src/phone/java/com/adobe/marketing/mobile/PlacesMonitorConstants.java

check-version:
	if curl -H "Accept: application/xml" -H "Content-Type: application/xml" -X GET ${URL_MAVEN_METADATA} | grep -o "<latest>.*</latest>" | grep -o "[0-9]*\.[0-9]*\.[0-9]*" | xargs -Istr grep -o -i "[mouduleVersion|version].*str" ${FILE_NAMES_CONTAINS_VERSION}; then exit 1; else exit 0; fi

create-ci: clean
		(mkdir -p ci)

clean:
	(rm -rf ci)
	(rm -rf $(AAR_FILE_DIR))
	(./code/gradlew -p code clean)

ci-build: create-ci
	(echo $(PROJECT_NAME))
	(echo $(AAR_NAME))
	(echo $(MODULE_NAME))
	(echo $(LIB_VERSION))
	(echo $(SOURCE_FILE_DIR))
	(echo $(AAR_FILE_DIR))
		(mkdir -p ci/assemble)
		(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhone)
		# (mv $(AAR_FILE_DIR)/$(EXTENSION-LIBRARY-FOLDER-NAME)-phone-release.aar  $(AAR_FILE_DIR)/$(MODULE_NAME)-release-$(LIB_VERSION).aar)
		# (cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build $(BUILD-ASSEMBLE-LOCATION))

ci-unit-test: create-ci
			(mkdir -p ci/unit-test)
			(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) platformUnitTestJacocoReport)
			(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build ./ci/unit-test/)


ci-javadoc: create-ci
	(mkdir -p ci/javadoc)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) javadocPublic > ci/javadocPublic.log 2>&1)
	(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build ./ci/javadoc)

ci-generate-library-debug:
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneDebug)

ci-generate-library-release:
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneRelease)

build-release:
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} clean lint assemblePhoneRelease)

ci-publish-staging: clean build-release
	  (./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository)

ci-publish-main: clean build-release
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository -Prelease)
