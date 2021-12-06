ADB = ${ANDROID_HOME}/platform-tools/adb
EMULATOR = ${ANDROID_HOME}/tools/emulator
GRADLEW = ./gradlew --daemon --parallel

ifdef ComSpec	 # Windows
  # Use `/` for all paths, except `.\`
  ADB := $(subst \,/,${ADB})
  EMULATOR := $(subst \,/,${EMULATOR})
  GRADLEW := $(subst /,\,${GRADLEW})
endif

.PHONY: default build clean test deploy uninstall emulator kill logs force
.PHONY: all build-all deploy-all uninstall-all

default:
	@ADB='${ADB}' ./scripts/build_and_maybe_deploy
all: clean build-all deploy-all

force: build uninstall
	adb install -r build/outputs/apk/cht-gateway-SNAPSHOT-medic-debug.apk

build:
	${GRADLEW} assembleMedicDebug
build-all:
	${GRADLEW} assembleDebug

clean:
	rm -rf src/main/assets/
	rm -rf build/

test:
	${GRADLEW} clean check test

e2e:
	${GRADLEW} assemble connectedCheck

emulator:
	nohup ${EMULATOR} -avd test -wipe-data > emulator.log 2>&1 &
	${ADB} wait-for-device

logs:
	${ADB} logcat CHTGateway:V AndroidRuntime:E '*:S' | tee android.log

deploy:
	${GRADLEW} installMedicDebug
deploy-all: build-all
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
	${GRADLEW} --stacktrace check test assemble
	./scripts/start_emulator
	${GRADLEW} connectedCheck
