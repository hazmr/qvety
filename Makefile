# Dev shortcuts. See CLAUDE.md for what each command does.
COMPOSE = docker compose -f docker/docker-compose.yml

# Keep frontend-maven-plugin out of dev and test runs; it would run
# npm ci and wipe web/node_modules under a live ng serve.
SKIP_FE = -Dskip.npm -Dskip.installnodenpm

.PHONY: up down test backend web

up:            ## postgres, minio, backend :8080, web :4200; ctrl-c stops the servers
	$(COMPOSE) up -d
	$(MAKE) -j2 backend web

down:          ## stop whatever holds :8080 and :4200, then the containers
	-fuser -k 8080/tcp 4200/tcp 2>/dev/null
	$(COMPOSE) down --remove-orphans

test:          ## all tests; testcontainers brings its own postgres
	./mvnw verify $(SKIP_FE)

backend:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local $(SKIP_FE)

web:
	cd web && npm start
