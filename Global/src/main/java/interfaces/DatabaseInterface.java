package interfaces;

import exceptions.InvalidCredentialsException;
import exceptions.UserAlreadyExistsException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DatabaseInterface extends Remote {

    public void createNewUser(String username, String password) throws RemoteException, UserAlreadyExistsException;

    public String createToken(String username, String password) throws RemoteException, InvalidCredentialsException;

    public boolean checkCredentials(String username, String password) throws RemoteException;

    public boolean isTokenValid(String username, String token) throws RemoteException;

    public byte[] getTheme(int id) throws RemoteException;


}