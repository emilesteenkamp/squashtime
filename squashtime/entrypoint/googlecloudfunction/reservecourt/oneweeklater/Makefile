deploy-function:
	gcloud run deploy reserve-court-one-week-later \
      --image=europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court-one-week-later:latest \
      --region=europe-west4 \
      --allow-unauthenticated

docker-run-function:
	docker run -p8080:8080 europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court-one-week-later:latest

docker-build-and-run-function:
	gradle jibDockerBuild
	docker run -p8080:8080 europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court-one-week-later:latest
