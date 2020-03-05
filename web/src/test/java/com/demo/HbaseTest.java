package com.demo;


import com.demo.client.HbaseClient;
import com.demo.service.UserScoreService;
import com.demo.util.Property;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback
public class HbaseTest {

    @Autowired
    UserScoreService userScoreService;

    @Test
    public void testHbase() throws IOException {
        userScoreService.calUserScore("1");
    }


    @Test
    public void testHbaseClient() throws IOException {
        String data = HbaseClient.getData("user", "1", "color", "red");
        System.out.println(data);
    }

    @Test
    public void testCreateTable() throws IOException {
        HbaseClient.createTable("test","test01");
    }

    @Test
    public void testListTable() throws Exception {
        System.setProperty("hadoop.home.dir", "C:\\Hadoop");
        Configuration conf = HBaseConfiguration.create();
//        conf.set("hbase.rootdir", Property.getStrValue("hbase.rootdir"));
        conf.set("hbase.zookeeper.quorum", Property.getStrValue("hbase.zookeeper.quorum"));
        conf.set("hbase.zookeeper.property.client", "2181");
        conf.set("hbase.client.retries.number", Integer.toString(0));
        conf.set("zookeeper.session.timeout", Integer.toString(600));
        conf.set("zookeeper.recovery.retry", Integer.toString(0));
//        conf.set("hbase.client.scanner.timeout.period", Property.getStrValue("hbase.client.scanner.timeout.period"));
//        conf.set("hbase.rpc.timeout", Property.getStrValue("hbase.rpc.timeout"));
        try{
            Connection connection = ConnectionFactory.createConnection(conf);
            Admin admin = connection.getAdmin();
            HBaseAdmin.checkHBaseAvailable(conf);
            HTableDescriptor[] tableDescriptors = admin.listTables();
            for (HTableDescriptor tableDescriptor : tableDescriptors) {
                System.out.println("Table Name:"+ tableDescriptor.getNameAsString());
            }
            admin.close();
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
