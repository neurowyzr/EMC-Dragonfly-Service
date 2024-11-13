#!/bin/bash

docker compose -f cicd/infra-docker-compose.yml -f cicd/infra-docker-compose.pr.yml down -v
rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi

rm -rf temp
