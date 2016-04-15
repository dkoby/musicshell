####################################
#
#
####################################
export SH = /bin/bash
export CC = gcc
export LD = gcc
export AR = ar

DEBUG := 1

ifdef DEBUG
    export CFLAGS += -g
else
    export CFLAGS += -O2
endif
export CFLAGS += -Wall

ifndef DEBUG
    export LDFLAGS += -s
endif

#export DEPFILE = depfile.mk
####################################
#
#
####################################
DIRS += fft

.PHONY: all clean
all:
	$(SH) foreach.sh $@ $(DIRS)

clean:
	$(SH) foreach.sh $@ $(DIRS)
	make -C samples clean

.PHONY: depend
#depend:
#	$(SH) foreach.sh $@ $(DIRS)


