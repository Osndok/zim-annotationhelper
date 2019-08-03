#!/bin/bash
#
# service.sh - can start/stop/restart the zim annotation helper
#

# !!!: here, the file is removed
#LOG_FILE=$HOME/tmp/zim-annotationhelper.log
LOG_FILE=/tmp/zim-annotationhelper.log
PID_FILE=$HOME/.local/run/zim-annotationhelper.pid
PROJECTS=$HOME/Projects
PROJECT_DIR=$(dirname $(realpath "$0"))

MAIN_CLASS="meta.works.zim.annotationhelper.Main"

cd /

test -d ~/.local/run || mkdir ~/.local/run

function status()
{
	if [ -r "$PID_FILE" ]
	then
		PID=$(cat $PID_FILE)
		if grep -q java /proc/$PID/cmdline
		then
			echo 2>&1 "process $PID is running"
			return 0
		else
			echo 2>&1 "process $PID is DEAD"
			return 2
		fi
	else
		echo 2>&1 "No pid file: $PID_FILE"
		return 1
	fi
}

function start()
{
	if status
	then
		echo 1>&2 "ERROR: Already running..."
		return
	fi

	# We copy the jar out of projects, so we don't get hung up on that flaky mountpoint...
	JAR=$PROJECT_DIR/out/artifacts/zim_annotationhelper_jar/zim-annotationhelper.jar
	test -e $JAR || JAR=$PROJECT_DIR/target/zim-annotationhelper-1.0-SNAPSHOT-jar-with-dependencies.jar
	test -e $JAR || mvn package
	# Uggh... can't have it deleted: java.util.MissingResourceException: Can't find bundle for base name dbusjava_localized, locale en_US
	#JAR2=$HOME/tmp/zim-annotationhelper.jar
	JAR2=/tmp/zim-annotationhelper.jar
	cp -v "$JAR" "$JAR2"

	mv -f "$LOG_FILE" "${LOG_FILE}.old" || true

	D1=/usr/share/java/dbus-java
	D2=/usr/lib/java
	nohup java -cp $D1/dbus.jar:$D2/unix.jar:$D2/debug-disable.jar:"$JAR2" -Djava.library.path=/usr/lib64/libmatthew-java "$MAIN_CLASS" >> $LOG_FILE 2>&1 &
	echo -n "$!" > "$PID_FILE"
	sleep 0.5
	status
}

function stop()
{
	if [ -r "$PID_FILE" ]
	then
		pkill -F "$PID_FILE" java || true
		#NB: This would kill the service script too.
		#pkill -f zim-annotationhelper || true
		rm -f "$PID_FILE"
	else
		echo 1>&2 "ERROR: Missing/unreadable pid file: $PID_FILE"
		exit 1
	fi
}

function restart()
{
	if status
	then
		stop
	fi
	sleep "${1:-1}"
	start
}

set -vexu
"$@"

