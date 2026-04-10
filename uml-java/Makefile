JAVAC=javac
JAVA=java
SRC_DIR=src/main/java
SRC=$(SRC_DIR)/uml4java/*.java
OUT=out

all: build

build:
	mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(SRC)

run: build
	$(JAVA) -cp $(OUT) uml4java.Uml4Java $(ARGS)

mvn-build:
	mvn package

mvn-run:
	$(JAVA) -jar target/uml4java-1.0-SNAPSHOT.jar $(ARGS)

clean:
	rm -rf $(OUT) target

.PHONY: all build run mvn-build mvn-run clean
