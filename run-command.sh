#!/bin/bash

# run kafka-cluster
docker-compose up kafka-cluster

#open new terminal and run on docker landoop

docker run --rm -it -v "$(pwd)":/tutorial --net=host landoop/fast-data-dev bash

