#!/bin/bash
function create_kafka_topic {
    $KAFKA_HOME/bin/kafka-topics.sh --create --if-not-exists --zookeeper zookeeper:2181 --replication-factor 1 --partitions 1 --topic $1
}
function send_messages_to_kafka {
    msg=$(generator_message)
    echo $msg
    echo -e $msg | $KAFKA_HOME/bin/kafka-console-producer.sh --broker-list kafka:9093 --topic $TOPIC
}
function rand {
    min=$1
    max=$(($2-$min+1))
    echo $(shuf -i $min-$max -n 1)
}
function generator_message {
    uid=$(rand 1 999);
    product_id=$(rand 1 999);
    timestamp=`date '+%s'`;
    action=$(rand 1 3);
    msg=$uid","$product_id","$timestamp","$action;
    echo $msg
}

TOPIC="con"
create_kafka_topic $TOPIC
while true
do
 send_messages_to_kafka
 sleep 0.5
done
