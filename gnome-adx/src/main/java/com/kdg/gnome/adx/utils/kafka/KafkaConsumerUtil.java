package com.kdg.gnome.adx.utils.kafka;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kdg.gnome.adx.utils.CacheUtils;
import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 自定义简单Kafka消费者， 使用高级API
 * Created by hbwang on 12/21.
 */
public class KafkaConsumerUtil implements Runnable {

    private static final Logger logger = LogManager.getLogger("ES_OUT_INFO");
    /**
     * Kafka数据消费对象
     */
    private ConsumerConnector consumer;

    /**
     * Kafka Topic名称
     */
    private String topic;

    /**
     * 线程数量，一般就是Topic的分区数量
     */
    private int numThreads;

    /**
     * 线程池
     */
    private ExecutorService executorPool;

    /**
     * 构造函数
     *
     * @param topic      Kafka消息Topic主题
     * @param numThreads 处理数据的线程数/可以理解为Topic的分区数
     * @param zookeeper  Kafka的Zookeeper连接字符串
     * @param groupId    该消费者所属group ID的值
     */
    public KafkaConsumerUtil(String topic, int numThreads, String zookeeper, String groupId) {
        // 1. 创建Kafka连接器
        this.consumer = Consumer.createJavaConsumerConnector(createConsumerConfig(zookeeper, groupId));
        // 2. 数据赋值
        this.topic = topic;
        this.numThreads = numThreads;
    }

    @Override
    public void run() {
        // 1. 指定Topic
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(this.topic, this.numThreads);

        // 2. 指定数据的解码器
        StringDecoder keyDecoder = new StringDecoder(new VerifiableProperties());
        StringDecoder valueDecoder = new StringDecoder(new VerifiableProperties());

        // 3. 获取连接数据的迭代器对象集合
        /**
         * Key: Topic主题
         * Value: 对应Topic的数据流读取器，大小是topicCountMap中指定的topic大小
         */
        Map<String, List<KafkaStream<String, String>>> consumerMap = this.consumer.createMessageStreams(topicCountMap, keyDecoder, valueDecoder);

        // 4. 从返回结果中获取对应topic的数据流处理器
        List<KafkaStream<String, String>> streams = consumerMap.get(this.topic);

        // 5. 创建线程池
        this.executorPool = Executors.newFixedThreadPool(this.numThreads);

        // 6. 构建数据输出对象
        int threadNumber = 0;
        for (final KafkaStream<String, String> stream : streams) {
            this.executorPool.submit(new ConsumerKafkaStreamProcesser(stream, threadNumber));
            threadNumber++;
        }
    }

    public void shutdown() {
        // 1. 关闭和Kafka的连接，这样会导致stream.hashNext返回false
        if (this.consumer != null) {
            this.consumer.shutdown();
        }

        // 2. 关闭线程池，会等待线程的执行完成
        if (this.executorPool != null) {
            // 2.1 关闭线程池
            this.executorPool.shutdown();

            // 2.2. 等待关闭完成, 等待五秒
            try {
                if (!this.executorPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.info("kafka consumer. Timed out waiting for consumer threads to shut down, exiting uncleanly!!");
                }
            } catch (InterruptedException e) {
                logger.error("kafka consumer error. Interrupted during shutdown, exiting uncleanly!!");
            }
        }

    }

    /**
     * 根据传入的zk的连接信息和groupID的值创建对应的ConsumerConfig对象
     *
     * @param zookeeper zk的连接信息，类似于：<br/>
     *                  hadoop-senior01.ibeifeng.com:2181,hadoop-senior02.ibeifeng.com:2181/kafka
     * @param groupId   该kafka consumer所属的group id的值， group id值一样的kafka consumer会进行负载均衡
     * @return Kafka连接信息
     */
    private ConsumerConfig createConsumerConfig(String zookeeper, String groupId) {
        // 1. 构建属性对象
        Properties prop = new Properties();
        // 2. 添加相关属性
        prop.put("group.id", groupId); // 指定分组id
        prop.put("zookeeper.connect", zookeeper); // 指定zk的连接url
        prop.put("zookeeper.sync.time.ms", "200");
        prop.put("auto.commit.interval.ms", "100");
        prop.put("zookeeper.session.timeout.ms", "5000");
        prop.put("zookeeper.connection.timeout.ms", "10000");
        prop.put("zookeeper.sync.time.ms", "2000");
//        prop.put("enable.auto.commit", "false");
//        prop.put("auto.offset.reset", "earliest");
//        prop.put("session.timeout.ms", "3000");
//        prop.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
//        prop.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // 3. 构建ConsumerConfig对象
        return new ConsumerConfig(prop);
    }


    /**
     * Kafka消费者数据处理线程
     */
    public static class ConsumerKafkaStreamProcesser implements Runnable {
        // Kafka数据流
        private KafkaStream<String, String> stream;
        // 线程ID编号
        private int threadNumber;

        public ConsumerKafkaStreamProcesser(KafkaStream<String, String> stream, int threadNumber) {
            this.stream = stream;
            this.threadNumber = threadNumber;
        }

        @Override
        public void run() {
            // 1. 获取数据迭代器
            ConsumerIterator<String, String> iter = this.stream.iterator();
            // 2. 迭代输出数据
            while (iter.hasNext()) {
                // 2.1 获取数据值
                MessageAndMetadata value = iter.next();

                String message = (String) value.message();

                // 2.2 输出
                logger.info("kafka received msg : {}", message);

                try {

                    TrackingMsg msg = GSON.fromJson(message, TrackingMsg.class);
                    if (msg != null && StringUtils.equalsIgnoreCase(msg.k, "a")) {
                        //针对广告主的控制
                        CacheUtils.setCampaignSuspend(msg.v, 2, "kafka计划到量");
                    } else if (msg != null && StringUtils.equalsIgnoreCase(msg.k, "c")) {
                        //针对计划的控制
                        CacheUtils.setCampaignSuspend(msg.v, 1, "kafka 广告主到量");
                    }

                } catch (JsonSyntaxException e) {

                }



            }
            // 3. 表示当前线程执行完成
            logger.info("kafka consumer. Shutdown Thread:" + this.threadNumber);
        }
    }

    private static Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static class TrackingMsg {
        public String   k;
        public String   v;
    }
    public static void main(String[] args) {
        String zookeeper = "172.21.0.31:2181,172.21.0.37:2181,172.21.0.27:2181";
        String groupId = "grouptest";
        String topic = "adx_reachCeiling_110";
        int threads = 1;

        KafkaConsumerUtil example = new KafkaConsumerUtil(topic, threads, zookeeper, groupId);
        new Thread(example).start();

        // 执行10秒后结束
        int sleepMillis = 600000;
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 关闭
        example.shutdown();
    }
}