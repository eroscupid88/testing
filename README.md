# testing
# description about the application
-the application take data( JSon data) from twitter user who tweet about trump then sink into new elasticsearch database
-source: twitter
-sink: elasticsearch
-data go to kafka stream using kafka-stream-api ,  make some new nodes and filter some wanted information and finally 
save it into elasticSearch database
 
#how to run an application 

#run command from home directory:

docker-compose up kafka-cluster

#open new terminal then run docker command

docker run --rm -it -v "$(pwd)":/tutorial --net=host landoop/fast-data-dev bash

#create topics using command in docker

kafka-topics --zookeeper 127.0.0.1:2181 --create --topic src-topic --partitions 3 --replication-factor 1

kafka-topics --zookeeper 127.0.0.1:2181 --create --topic out-topic --partitions 3 --replication-factor 1

kafka-topics --zookeeper 127.0.0.1:2181 --create --topic twitter --partitions 3 --replication-factor 1
#run stream application
