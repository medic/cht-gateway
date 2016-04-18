ADB = ${ANDROID_HOME}/platform-tools/adb
EMULATOR = ${ANDROID_HOME}/tools/emulator
GRADLEW = ./gradlew

ifdef ComSpec	 # Windows
  # Use `/` for all paths, except `.\`
  ADB := $(subst \,/,${ADB})
  EMULATOR := $(subst \,/,${EMULATOR})
  GRADLEW := $(subst /,\,${GRADLEW})
endif

.PHONY: default assets clean deploy emulator kill logs prod

default: clean deploy logs
prod: clean deploy

clean:
	rm -rf src/main/assets/
	rm -rf build/

emulator:
	nohup ${EMULATOR} -avd test -wipe-data > emulator.log 2>&1 &
	${ADB} wait-for-device

logs:
	${ADB} shell logcat | tee android.log

deploy:
	${GRADLEW} --daemon --parallel installDebug

kill:
	pkill -9 emulator64-arm


.PHONY: demo-server

demo-server:
	npm start
