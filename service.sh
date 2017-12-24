#!/bin/bash
#
# service.sh - can start/stop/restart the zim annotation helper
#

LOG_FILE=/tmp/zim-annotationhelper.log
PID_FILE=/tmp/zim-annotationhelper.pid
PROJECTS=$HOME/Projects

cd /

function status()
{
	if [ -r "$PID_FILE" ]; then
		PID=$(cat $PID_FILE)
		if grep -q java /proc/$PID/cmdline ; then
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
	if status ; then
		echo 1>&2 "ERROR: Already running..."
		return
	fi

	# We copy the jar out of projects, so we don't get hung up on that flaky mountpoint...
	JAR=$PROJECTS/zim-annotationhelper/out/artifacts/zim_annotationhelper_jar/zim-annotationhelper.jar
	JAR2=/tmp/zim-annotationhelper.jar
	cp -v "$JAR" "$JAR2"

	D1=/usr/share/java/dbus-java
	D2=/usr/lib/java
	nohup java -cp $D1/dbus.jar:$D2/unix.jar:$D2/debug-disable.jar -Djava.library.path=/usr/lib64/libmatthew-java -jar "$JAR2" >> $LOG_FILE 2>&1 &
	echo -n "$!" > "$PID_FILE"
	sleep 0.5
	status
}

function stop()
{
	if [ -r "$PID_FILE" ]; then
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
	if status ; then
		stop
	fi
	sleep "${1:-1}"
	start
}

set -vexu
"$@"

