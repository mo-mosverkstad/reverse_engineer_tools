CC=gcc
CFLAGS=-Wall -O2

ifeq ($(OS),Windows_NT)
	TARGET=uml4java.exe
	RM=del /Q
else
	TARGET=uml4java
	RM=rm -f
endif

all: $(TARGET)

$(TARGET): uml4java.c
	$(CC) $(CFLAGS) -o $(TARGET) uml4java.c

clean:
	$(RM) $(TARGET)

.PHONY: all clean
