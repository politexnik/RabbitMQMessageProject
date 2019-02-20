package Sender;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.nio.file.Paths.get;

/**
 * Core class of application uses RabbitMQ API
 */
public class Sender {
    private static final Logger log = LogManager.getLogger("Sender");
    private ConnectionFactory factory;
    private Channel channel;
    private Connection connection;

    private Sender() {
        factory = new ConnectionFactory();
    }

    public static Sender buildLocalSender(){
        Sender sender = new Sender();
        sender.factory.setHost("localhost");
        return sender;
    }

    /**
     * This are parameters for connection to RabbitMQ Server
     * @param userName
     * @param password
     * @param virtualHost
     * @see <a href="https://www.rabbitmq.com/api-guide.html#connecting">rabbitMQ.com</a>
     * @param hostName
     * @param portNumber    Use 5672 for regular connections, 5671 for connections that use TLS
     */

    public static Sender buildSender(String userName, String password, String virtualHost, String hostName, int portNumber){
        Sender sender = new Sender();
        sender.factory.setUsername(userName);
        sender.factory.setPassword(password);
        sender.factory.setVirtualHost(virtualHost);
        sender.factory.setHost(hostName);
        sender.factory.setPort(portNumber);

        return sender;
    }

    public void sendMessage(String exchangeName, String routingKey, byte[] messageByteArr, AMQP.BasicProperties basicProperties) throws IOException{
        channel.basicPublish(exchangeName, routingKey, basicProperties, messageByteArr);
    }


     //Method can send short files only because of reading it in byte array. For sendind big files needs to be improved
    public void sendFile(String exchangeName, String routingKey, String fileName) throws IOException {
        Path file = Paths.get(fileName);
        byte[] messageByteArr = Files.readAllBytes(file);
        String fileShortName = file.getFileName().toString();
        Map<String, Object> hashMap = new HashMap<>();

        hashMap.put("fileName", fileShortName);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("file")
                .headers(hashMap)
                .build();
        channel.basicPublish(exchangeName, routingKey, properties, messageByteArr);
        log.info("Send file: " + fileName);
    }

    public void openConnections(){
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void closeConnections(){
        try {
            channel.close();
            connection.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void exchangeDeclare(String exchangeName, BuiltinExchangeType exchangeType) {
        try {
            channel.exchangeDeclare(exchangeName, exchangeType);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }


}
