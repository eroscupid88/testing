import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class TwitterProducer {
    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    // get from developer twitter account
    String consumerKey = "fywls5AYRQQBkXOdmPNzHxH09";
    String consumerSecret =  "8CZpoE3DzCtilUHWQ7ezTZBPa7dFGVWd1bKthwLTZudr2B7hN6";
    String token = "995636485830389760-2BbTF1r33JSa48aBbmjA7FnOY8rxqd3";
    String secret = "ca9VTULHv0xJ7nJzWb1pxQhExtopy6uNddU9A8uXdEM4K";
    List<String> terms = Lists.newArrayList("trump");
    public TwitterProducer(){}
    public void run() {
        logger.info("set up...");
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
        Client client = createTwitterClient(msgQueue);
        // create twitter client
        client.connect();
        // create a kafka producer
        KafkaProducer<String, String> kafkaProducer = createKafkaProducer();
        // loop to send twitter to kafka
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.info("stop application");
            logger.info("shutting down client from twitter");
            client.stop();
            logger.info("shutting producer");
            kafkaProducer.close();
        }));

        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }
            if (msg != null) {
                logger.info(msg);
                kafkaProducer.send(new ProducerRecord<String,String>( "src-topic", null, msg),new Callback(){
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if(e!=null){
                            logger.error("something bad happen", e);
                        }
                    }
                });
            }

        }
        logger.info("end of application!!");
    }
    public Client createTwitterClient(BlockingQueue<String> msgQueue){


        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings and track terms

        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));


        Client hosebirdClient = builder.build();
        return hosebirdClient;
// Attempts to establish a connection.

    }

    public KafkaProducer<String, String> createKafkaProducer(){
        final Logger logger = LoggerFactory.getLogger(TwitterProducer.class);
        String bootStrapServer = "127.0.0.1:9092";
        // create producer properties
        Properties properties = new Properties();

        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootStrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());

        // create safe producer (idempotent)
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG,"all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5");


        //create compression configuration high throughput producer  make a bit of latency and CPU usage

        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20"); // delay to wait to send message as a batch 20ms
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024)); //32KB per batch. send message as a batch to broker-> consumer



        //create producer
        KafkaProducer<String,String> kafkaProducer = new KafkaProducer<String,String>(properties);

        return kafkaProducer;
    }

    public static void main(String[] args) {
        new TwitterProducer().run();


    }
}
