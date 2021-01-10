# testing

#how to run an application 
#run command from home directory:

docker-compose up kafka-cluster

#open new terminal then run docker command

docker run --rm -it -v "$(pwd)":/tutorial --net=host landoop/fast-data-dev bash

#create topics using command 

