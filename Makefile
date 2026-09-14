# Dev shortcuts. See CLAUDE.md for what each command does.
COMPOSE = docker compose -f docker/docker-compose.yml

.PHONY: up down backend web dev test package check

up:            ## start postgres and minio
	$(COMPOSE) up -d

down:          ## stop them
	$(COMPOSE) down

# SKIP_FE keeps frontend-maven-plugin out of dev and test runs; it would
# run npm ci and wipe web/node_modules under a live ng serve.
SKIP_FE = -Dskip.npm -Dskip.installnodenpm

backend:       ## spring boot on :8080, local profile
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local $(SKIP_FE)

web:           ## angular on :4200, proxies /api to :8080
	cd web && npm start

dev: up        ## backend and web together; ctrl-c stops both
	$(MAKE) -j2 backend web

test: up       ## all tests incl. testcontainers
	./mvnw verify $(SKIP_FE)

package:       ## one jar with the angular build inside
	./mvnw package -DskipTests

check: test package ## part "done when": tests green, jar serves health
	@java -jar target/*.jar --spring.profiles.active=local & pid=$$!; \
	for i in $$(seq 1 30); do curl -sf localhost:8080/actuator/health && break; sleep 2; done; \
	echo; kill $$pid
