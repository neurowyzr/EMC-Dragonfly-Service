#!/bin/bash

mkdir temp
sed -i 's:/var/lib/mysql/:'`pwd`'/temp/mysql/:' cicd/infra-docker-compose.yml

docker compose -f cicd/infra-docker-compose.yml -f cicd/infra-docker-compose.pr.yml up -d --quiet-pull
