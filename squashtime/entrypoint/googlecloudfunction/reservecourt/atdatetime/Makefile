deploy-function:
	gcloud run deploy reserve-court-at-date-time \
      --image=europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court-at-date-time:latest \
      --region=europe-west4 \
      --allow-unauthenticated

docker-run-function:
	docker run -p8080:8080 europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court-at-date-time:latest

docker-build-and-run-function:
	gradle jibDockerBuild
	docker run -p8080:8080 europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court-at-date-time:latest
