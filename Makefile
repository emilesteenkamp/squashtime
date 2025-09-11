deploy-reserve-court-function:
	gcloud run deploy reserve-court \
      --image=europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court:latest \
      --region=europe-west4 \
      --allow-unauthenticated

docker-run-reserve-court-function:
	docker run -p8080:8080 europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court:latest

docker-build-and-run-reserve-court-function:
	./gradlew :squashtime:entrypoint:googlecloudfunction:jibDockerBuil
	docker run -p8080:8080 europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court:latest
