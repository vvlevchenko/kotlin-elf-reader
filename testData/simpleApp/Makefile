CFLAGS=-g -O1

SOURCE=simple.c
OBJECTS=${addsuffix .o, ${basename ${SOURCE}}}
APP=simpleApp
LD=gcc

all:${APP}

clean:
	${RM} ${RMFLAGS} ${APP} ${OBJECTS}

${APP}: ${OBJECTS}
	$(LD) -o $@ $<


.PHONY: clean all
