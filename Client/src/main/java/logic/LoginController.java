package logic;

import exceptions.InvalidCredentialsException;
import interfaces.AppLoginInterface;
import interfaces.DispatcherInterface;
import interfaces.LobbyInterface;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ui.AlertBox;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LoginController {

    public TextField input_username;
    public PasswordField input_password;
    public Button button_signin;
    public Button button_register;



    public LoginController(){
    }

    public void login(ActionEvent actionEvent){
        Registry registry;
        try {
            String username = input_username.getText();
            String password = input_password.getText();

            //Dispatcher opzoeken
            registry = LocateRegistry.getRegistry(Main.DISPATCH_IP, Main.DISPATCH_PORT);
            DispatcherInterface dispatch = (DispatcherInterface) registry.lookup("dispatcher_service");

            //Token opvragen
            String token = "";
            //Nieuwe token opvragen als de oude ouder dan 24u is
            if(!dispatch.isTokenValid(username, token)){
                token = dispatch.requestNewToken(username, password);
            }

            Client client = Client.getInstance();

            AppLoginInterface app_login = dispatch.getApplicationServer();

            LobbyInterface lobby = app_login.clientLogin(username, token);


            client.setUsername(username);
            client.setToken(token);
            client.setDispatch(dispatch);
            client.setApp_login(app_login);
            client.setLobby(lobby);

            //Naar lobby scherm gaan
            SceneController.getInstance().showLobbyScene();

            //Tijdelijke code

        } catch (RemoteException | NotBoundException re) {
            AlertBox.display("Connection problems", "Cannot contact dispatcher");
            re.printStackTrace();
        }
        catch(InvalidCredentialsException ice){
            //ice.printStackTrace();
            AlertBox.display("Login not successful.", "Username and/or password are wrong.");
            //TODO: error op scherm laten verschijnen;
        }

    }

    public void register(ActionEvent actionEvent) {
        SceneController.getInstance().showRegisterScene();
    }


}