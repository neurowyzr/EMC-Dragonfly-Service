Online [reference](https://docs.docker.com/compose/compose-file/compose-file-v3/) of docker-compose

This service leverages containers for running automated tests. Steps to start the containers locally are as follows:
1. Ensure that docker is running using `docker info`.
2. Start the dependent containers using `docker-compose -f cicd/infra-docker-compose.yml up -d`
3. Start the service and test as necessary.
4. Stop the containers using `docker-compose -f cicd/infra-docker-compose.yml down -v`
