package applicationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import static constants.DispatcherConstants.DISPATCHER_IP;
import static constants.DispatcherConstants.DISPATCHER_SERVER_PORT;

@SpringBootApplication
public class Main {

    public static String ip;
    public static int restPort;
    public static int port;
    public static String server_name;


    public static void main(String[] args) {
        if (args.length != 0) {
            server_name = args[0];
            port = Integer.parseInt(args[1]);
            restPort = Integer.parseInt(args[2]);
        } else {
            server_name = "Application server Alpha";
            port = 1200;
            restPort = 1600;
        }
        System.setProperty("server.port", String.valueOf(restPort));
        SpringApplication.run(Main.class, args);

        try {
            ip = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ApplicationServer server = ApplicationServer.getInstance();
        server.init(server_name, ip, port, restPort, DISPATCHER_IP, DISPATCHER_SERVER_PORT);
        Lobby.getInstance().setName(server_name);

    }


}
