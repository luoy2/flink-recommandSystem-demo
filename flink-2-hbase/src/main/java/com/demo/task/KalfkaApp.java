package com.demo.task;

import com.demo.map.LogMapFunction;
import com.demo.util.Property;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.api.common.functions.MapFunction;
import java.util.Properties;

/**
 * 日志 -> Hbase
 *
 * @author XINZE
 */
public class KalfkaApp {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Properties properties = Property.getKafkaProperties("log");


        System.out.println(properties);
        DataStreamSource<String> dataStream = env.addSource(new FlinkKafkaConsumer<String>("con", new SimpleStringSchema(), properties));
//        dataStream.map(new LogMapFunction());
        dataStream.rebalance().map(new MapFunction<String, String>() {
            private static final long serialVersionUID = -6867736771747690202L;

            @Override
            public String map(String value) throws Exception {
                return "Kafka and Flink1 says: " + value;
            }
        }).print();

        env.execute("stout");
    }
}
