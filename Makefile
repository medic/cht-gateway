ADB = ${ANDROID_HOME}/platform-tools/adb
EMULATOR = ${ANDROID_HOME}/tools/emulator
GRADLE = ./gradlew
flavor = Medic
flavor_lower = $(shell echo ${flavor} | tr '[:upper:]' '[:lower:]')

ifdef ComSpec	 # Windows
  # Use `/` for all paths, except `.\`
  ADB := $(subst \,/,${ADB})
  EMULATOR := $(subst \,/,${EMULATOR})
  GRADLEW := $(subst /,\,${GRADLE} --daemon --parallel)
endif

.PHONY: default assemble clean lint test deploy uninstall emulator kill logs force
.PHONY: all assemble-all assemble-release deploy-all uninstall-all

default:
	@ADB='${ADB}' ./scripts/assemble_and_maybe_deploy
all: clean assemble-all deploy-all

force: assemble uninstall
	adb install -r build/outputs/apk/${flavor_lower}/debug/cht-gateway-SNAPSHOT-${flavor_lower}-debug.apk

assemble:
	${GRADLE} --daemon --parallel assemble${flavor}Debug
assemble-all:
	${GRADLE} --daemon --parallel assembleDebug

assemble-release:
	${GRADLE} --daemon --parallel assembleRelease

clean:
	rm -rf src/main/assets/
	rm -rf build/

lint:
	${GRADLE} --daemon --parallel androidCheckstyle

test: lint
	IS_GENERIC_FLAVOUR=false \
	IS_MEDIC_FLAVOUR=true \
		${GRADLE} --daemon --parallel test${flavor}DebugUnitTest

test-ui: assemble
	${GRADLE} --daemon --parallel connectedGenericDebugAndroidTest

emulator:
	nohup ${EMULATOR} -avd test -wipe-data > emulator.log 2>&1 &
	${ADB} wait-for-device

logs:
	${ADB} logcat CHTGateway:V AndroidRuntime:E '*:S' | tee android.log

deploy:
	${GRADLE} --daemon --parallel install${flavor}Debug
deploy-all: assemble-all
	find build/outputs/apk -name \*-debug.apk | \
		xargs -n1 ${ADB} install -r

uninstall:
	adb uninstall medic.gateway.alert
uninstall-all: uninstall
	adb uninstall medic.gateway.alert.generic

kill:
	pkill -9 emulator64-arm


.PHONY: demo-server

demo-server:
	npm install && npm start


.PHONY: avd changelog stats ci

avd:
	nohup android avd > /dev/null &

stats:
	./scripts/project_stats

changelog:
	./scripts/changelog

ci: stats
	IS_GENERIC_FLAVOUR=false \
	IS_MEDIC_FLAVOUR=true \
		${GRADLE} --daemon --parallel --stacktrace androidCheckstyle testMedicDebugUnitTest assemble
	${GRADLE} --daemon --parallel connectedCheck

version:
	${GRADLE} --version
