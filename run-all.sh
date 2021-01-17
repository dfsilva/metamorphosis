#!/bin/bash
./sbt docker
docker-compose -f docker-compose-dev.yml -f docker-compose-server.yml up -d