# Nova backend — convenience targets that pin the build to Java 21.
#
# Nova's Maven build enforces Java 21 (see the maven-enforcer rule in pom.xml).
# Many machines default to a newer JDK, so these targets resolve a Java 21 home
# at call time and export it as JAVA_HOME for that single command only. Nothing
# on your machine or shell is modified.
#
# JDK 21 is located in this order:
#   1. An already-correct JAVA_HOME (if it points at a 21.x JDK)
#   2. Homebrew's keg:  brew --prefix openjdk@21
#   3. macOS java_home: /usr/libexec/java_home -v 21  (only if it is really 21.x)
#
# Usage:
#   make run      # start the app (equivalent to: mvn spring-boot:run)
#   make test     # run the test suite
#   make compile  # compile only
#   make verify   # full verify (compile + test + checks)
#   make clean    # mvn clean
#   make java     # print the resolved Java 21 home (for debugging)

MVN ?= mvn

# Resolve a Java 21 home without touching global config.
JAVA21_HOME := $(shell \
	if [ -n "$$JAVA_HOME" ] && "$$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "21'; then \
		echo "$$JAVA_HOME"; \
	elif command -v brew >/dev/null 2>&1 && brew --prefix openjdk@21 >/dev/null 2>&1; then \
		echo "$$(brew --prefix openjdk@21)"; \
	elif H="$$(/usr/libexec/java_home -v 21 2>/dev/null)" && "$$H/bin/java" -version 2>&1 | grep -q 'version "21'; then \
		echo "$$H"; \
	fi)

# Fail fast with a clear message if no Java 21 was found.
define REQUIRE_JAVA21
	@if [ -z "$(JAVA21_HOME)" ]; then \
		echo "error: no Java 21 JDK found."; \
		echo "  Install one with:  brew install openjdk@21"; \
		echo "  Or set JAVA_HOME to a 21.x JDK before running make."; \
		exit 1; \
	fi
endef

.PHONY: run test compile verify clean java

run:
	$(REQUIRE_JAVA21)
	JAVA_HOME="$(JAVA21_HOME)" $(MVN) spring-boot:run

test:
	$(REQUIRE_JAVA21)
	JAVA_HOME="$(JAVA21_HOME)" $(MVN) test

compile:
	$(REQUIRE_JAVA21)
	JAVA_HOME="$(JAVA21_HOME)" $(MVN) compile

verify:
	$(REQUIRE_JAVA21)
	JAVA_HOME="$(JAVA21_HOME)" $(MVN) verify

clean:
	$(REQUIRE_JAVA21)
	JAVA_HOME="$(JAVA21_HOME)" $(MVN) clean

java:
	$(REQUIRE_JAVA21)
	@echo "Java 21 home: $(JAVA21_HOME)"
	@"$(JAVA21_HOME)/bin/java" -version
