package Receiver;

import com.rabbitmq.client.BuiltinExchangeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The class is handle Receiver and providing console user interface
 * Application can receive messages and files from exchange with topics
 */
public class ServiceReceiver {
    private static final String HELP_MESSAGE =
            "\\help\n" +
            "\\exit\n" +
            "\\directoryPath for receiving files";
    private String exchangeName;
    private String message;
    private String bindingKey;
    private Receiver receiver;
    private BufferedReader br;

    public static void main(String[] args) {
        try {
            new ServiceReceiver().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        //preparations
        receiver = Receiver.buildLocalReceiver();
        br = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Welcome. Type \\help to get a HELP message,\n" +
                "but first, type the bindingKey: ");
        bindingKey = br.readLine();

        System.out.println("Type directory Path for receiving files");
        receiver.setDirectoryPath(br.readLine());

        receiver.openConnections();
        receiver.startReceiving();

        System.out.println("Set the ExchangeName:");
        exchangeName = br.readLine();
        receiver.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC);
        receiver.queueBind(exchangeName, bindingKey);
        //Setting console to get messages
        receiver.setOutputStream(System.out);

        System.out.println("Start receiving");
        mark:
        while (true) {
            //handling service commands
            message = br.readLine();

            switch (message) {
                case "\\exit":
                    System.out.println("Stopping...");
                    receiver.closeConnections();
                    break mark;
                case "\\help":
                    System.out.println(HELP_MESSAGE);
                    continue;
                case "\\directoryPath":
                    System.out.println("Type directory Path for receiving files");
                    receiver.setDirectoryPath(br.readLine());
            }
        }
    }
}
