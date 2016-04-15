####################
#
####################
CC = gcc
LD = gcc
####################
#
####################
JNI_CLASS = KissFFT
JNI_PACKAGE = mshell.jni.fft
JNI_DIR = $(BUILD_DIR)/jni/fft

C_FILES += KissFFT.c
C_FILES += kiss_fft.c

C_OBJS  = $(foreach obj,$(C_FILES),$(patsubst %c,$(JNI_DIR)/%o, $(obj)))
OBJS += $(C_OBJS)

TARGET = $(JNI_DIR)/libfft.so

####################
#
####################
CFLAGS += -Wall
CFLAGS += -D_REENTRANT
CFLAGS += -D_POSIX_C_SOURCE 
CFLAGS += -I$(JAVA_HOME)/include
CFLAGS += -I$(JAVA_HOME)/include/linux
CFLAGS += -I$(JNI_DIR)
#
# kiss_fft
#
CFLAGS += -DFIXED_POINT=32 

LDFLAGS += -fPIC
LDFLAGS += -shared
####################
#
####################
.PHONY: all clean
all: compile

$(JNI_DIR)/%.o: %.c
	$(CC) -c $(CFLAGS) -o $@ $^

.PHONY: compile jheader
compile: jheader $(TARGET)

jheader: $(JNI_CLASS).java
	mkdir -p $(JNI_DIR)
	javac -d $(CLASSES_DIR) $^
	javah -cp $(CLASSES_DIR) -o $(JNI_DIR)/$(JNI_CLASS).h $(JNI_PACKAGE).$(JNI_CLASS)

$(TARGET): $(OBJS)
	$(LD) $(LDFLAGS) -o $@ $^

clean:
	rm -rf $(OBJS) $(TARGET) $(JNI_DIR)

