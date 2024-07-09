#!/bin/sh
cd docker
docker-compose rm -s -f
docker-compose up -d
