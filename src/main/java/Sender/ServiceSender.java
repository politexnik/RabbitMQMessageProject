package Sender;

import com.rabbitmq.client.BuiltinExchangeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The class is handle Sender and providing console user interface
 * Application can send messages and files to exchanges with topics
 */
public class ServiceSender {
    private static final String HELP_MESSAGE = "\\exchangeName\n+ to set it" +
            "\\routingKey to set\n" +
            "\\file to send file\n" +
            "\\help" +
            "\\exit";
    private String exchangeName;
    private String message;
    private String routingKey;
    private BufferedReader br;
    private Sender sender;

    public static void main(String[] args) {
        try {
            new ServiceSender().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        //Preparations for sending
        sender = Sender.buildLocalSender();
        br = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Welcome. Type \\help to get a HELP message\n" +
                "First, type the Exchange name: ");

        exchangeName = br.readLine();
        sender.openConnections();
        //declaring exchange in case it's not existing
        sender.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC);

        //1st time setting routingKey. It can be changed in runtime
        System.out.print("Last step before messaging. Type routing key: ");
        routingKey = br.readLine();

        System.out.println("Ok, then just type a message to server. To exit type \\exit.");
        mark:
        while (true) {
            message = br.readLine();
            //switching and routing service commands
            switch (message) {
                case "\\exit":
                    exit();
                    break mark;
                case "\\help":
                    System.out.println(HELP_MESSAGE);
                    continue;
                case "\\exchangeName":
                    setExchangeName();
                    continue;
                case "\\routingKey":
                    setRoutingKey();
                    continue;
                case "\\file":
                    //in case file sending going in another thread
                    System.out.println("Type file path:");
                    String fileName = br.readLine();
                    new Thread(()->{
                        try {
                            sender.sendFile(exchangeName, routingKey, fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    continue;
            }
            //if there is no service command - send a message
            sender.sendMessage(exchangeName, routingKey, (message + "\n").getBytes(StandardCharsets.UTF_8), null);
        }
    }
    // i added some methods for code simplification
    private void setRoutingKey() throws IOException {
        System.out.println("Set the routingKey:");
        routingKey = br.readLine();
    }

    private void exit() throws IOException {
        System.out.println("Stopping...");
        sender.closeConnections();
        br.close();
    }

    private void setExchangeName() throws IOException {
        System.out.println("Set the ExchangeName:");
        exchangeName = br.readLine();
        sender.exchangeDeclare(exchangeName,BuiltinExchangeType.TOPIC);
    }
}
