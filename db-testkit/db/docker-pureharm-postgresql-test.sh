#!/usr/bin/env bash
# Script that setups and runs a postgres 11.0 database inside a docker container.
# You can run this script via terminal using `sh docker-postgresql.sh`

# Running the script for the first time pulls a postgres image from docker repository.
# If a container with the same name already exists, running this script will start/restart the container.
# Postgres default configuration is specified by the parameters below.
# Docker is required in order for this script to work !

# parameters #

CONTAINER_NAME=postgres_pureharm_test   # Name of the docker container used to run postgres
EXPOSED_PORT=20010                      #this is the port on the host machine; most likely you want to change this one.
INTERNAL_PORT=5432                      #this is the default port on which postgresql starts on within the container.
DB_NAME=pureharm_test
DB_USER=pureharmony
DB_PASS=pureharmony

# actual script #
if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
	if [ ! "$(docker ps -aq -f name=$CONTAINER_NAME -f status=exited)" ]; then
		echo "Stopping postgres container"
		docker stop $CONTAINER_NAME
	fi
	echo "Starting postgres container"
	docker start $CONTAINER_NAME
else
	echo "Creating & starting postgres container"
	docker run -d \
		--name $CONTAINER_NAME \
		-p $EXPOSED_PORT:$INTERNAL_PORT \
		-e POSTGRES_DB=$DB_NAME \
		-e POSTGRES_USER=$DB_USER \
		-e POSTGRES_PASSWORD=$DB_PASS \
		postgres:11.3
fi