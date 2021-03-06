package applicationserver;

import classes.GameInfo;
import classes.ThemeInfo;
import exceptions.*;
import interfaces.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Lobby extends UnicastRemoteObject implements LobbyInterface {

    private static Lobby instance;
    private static int idCounter = 0;

    static {
        try {
            instance = new Lobby();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Game> liveGames = new HashMap<>();
    private Map<String, Game> backupLiveGames = new HashMap<>();

    private String name;

    private ApplicationServerInterface applicationServer;
    private DatabaseInterface db;
    private ServerDispatcherInterface dispatch;


    private Lobby() throws RemoteException {
    }

    private String createID() throws RemoteException{
        return applicationServer.getName() + idCounter++;
    }

    public static Lobby getInstance() {
        return instance;
    }

    public Lobby init(ApplicationServerInterface applicationServer, DatabaseInterface db, ServerDispatcherInterface dispatch) {
        this.applicationServer = applicationServer;
        this.db = db;
        this.dispatch = dispatch;
        return this;
    }

    public GameInterface makeNewGame(String name, int x, int y, int max_players, ClientInterface client, int theme_id)
            throws RemoteException, InvalidSizeException, InvalidCredentialsException, AlreadyPresentException, ThemeNotLargeEnoughException {

        return makeNewGame(createID(), name, x, y, max_players, client, theme_id, false);
    }

    public GameInterface makeNewGame(String id, String name, int x, int y, int max_players, ClientInterface client, int theme_id, boolean backup)
            throws RemoteException, InvalidSizeException, InvalidCredentialsException, AlreadyPresentException, ThemeNotLargeEnoughException {

        if (!isValidPlayer(client)) {
            throw new InvalidCredentialsException();
        }
        if (db.getTheme(theme_id).getSize() < x * y / 2) {
            throw new ThemeNotLargeEnoughException();
        }

        GameInterface newGameInterface = null;
        Game newGame = null;
        /*
        * Bij het aanmaken van een new game moet eerst gechecked worden of de gevraagde game bij de server kan.
        * Zo niet wordt een nieuwe app server opgestart
        * */
        if (!applicationServer.canFit(max_players) && !backup){
            //Vraag een appserver op die wel plaats heeft voor de game.
            //De dispatcher start als nodig een nieuwe appserver op.
            ApplicationServerInterface newAppServer = dispatch.getApplicationServerByFreeSlots(max_players);
            client.transferTo(newAppServer);
            newGameInterface = newAppServer.getLobby().makeNewGame(name, x, y, max_players, client, theme_id);
        } else {
            //validate username and token in gameclientinterface

            newGame = new Game(name, x, y, max_players, id , client, this, theme_id, backup);
            newGame.addPlayer(client);


            if (!backup) {
                liveGames.put(id, newGame);
                System.out.println("INFO: new game initialised [id:" + id + "]");
                ((ApplicationServer) applicationServer).reduceFreeSlots(max_players);
                db.addGame(newGame.getGameInfo());
                //dispatch.broadCastLobby(this);
            } else {
                backupLiveGames.put(id, newGame);
            }
        }

        System.out.println();
        System.out.println("Current games:");
        System.out.println("\tLive Games:");
        for (Map.Entry<String, Game> entry : liveGames.entrySet()) {
            System.out.println("\t\t" + entry.getValue());
        }
        System.out.println("\tBackup Games:");
        for (Map.Entry<String, Game> entry : backupLiveGames.entrySet()) {
            System.out.println("\t\t" + entry.getValue());
        }
        System.out.println();

        if (newGameInterface!=null) return newGameInterface;
        else return newGame;
    }

    //TODO: add spectator

    public synchronized GameInterface joinGame(String gameId, ClientInterface client)
            throws GameFullException, GameStartedException, RemoteException, InvalidCredentialsException, GameNotFoundException, AlreadyPresentException {
        //validate username and token in gameclientinterface
        if (!isValidPlayer(client)) {
            throw new InvalidCredentialsException();
        }

        Game game = liveGames.get(gameId);
        if (game == null) {
            throw new GameNotFoundException();
        }
        if (game.isStarted()) {
            throw new GameStartedException();
        }
        if (game.getMax_players() > game.getPlayerQueue().size()) {
            game.addPlayer(client);
            return game;
        } else throw new GameFullException();
    }

    public GameInterface spectateGame(String gameId, ClientInterface client) throws InvalidCredentialsException, GameNotFoundException {
        if (!isValidPlayer(client)) {
            throw new InvalidCredentialsException();
        }

        Game game = liveGames.get(gameId);
        if (game == null) {
            throw new GameNotFoundException();
        }
        game.addSpectator(client);
        return game;
    }

    public List<GameInfo> getAllLiveGames() throws RemoteException {
        return db.getAllGames();
    }

    public void terminateGame(GameInterface game) throws RemoteException {
        liveGames.remove(game.getId());
        //We kunnen dit doen omdat game id's server gebonden zijn
        backupLiveGames.remove(game.getId());
        db.removeGame(game.getGameInfo());
        ((ApplicationServer)applicationServer).addFreeSlots(game.getMax_players());

        System.out.println("INFO: game [id:" + game.getId() + "] was finished");

        try {
            checkShouldShutDown();
        } catch (InvalidCredentialsException | AlreadyPresentException e) {
            e.printStackTrace();
        }
    }

    public void checkShouldShutDown() throws RemoteException, InvalidCredentialsException, AlreadyPresentException {

        /*
         * When a game completes we check if the server can be shut down
         * */
        if (liveGames.isEmpty() && backupLiveGames.isEmpty()) {

            //Markeer het server paar als onbeschikbaar in de dispatcher
            boolean proceed = dispatch.markApplicationServerPairUnavailable(applicationServer);
            if (proceed) {
                //Verhuis alle geconnecteerde clients
                List<ClientInterface> clients = applicationServer.getAppLogin().getConnectedClients();

                for (ClientInterface client : clients) {
                    client.transferTo(dispatch.getApplicationServer());
                }

                //Sluit de servers af
                System.exit(0);
            }
        }
    }

    public List<byte[]> getPictures(int themeId) throws RemoteException {
        return db.getPictures(themeId);
    }

    public byte[] getPicture(int themeId, int pictureIndex) throws RemoteException {
        return db.getPicture(themeId, pictureIndex);
    }

    public boolean isValidPlayer(ClientInterface client) {
        try {
            return dispatch.isTokenValid(client.getUsername(), client.getToken());
        } catch (RemoteException re) {
            re.printStackTrace();
        }
        return false;

    }

    public List<ThemeInfo> getThemes() {
        try {
            return db.getThemes();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lobby)) return false;
        if (!super.equals(o)) return false;
        Lobby lobby = (Lobby) o;
        return Objects.equals(applicationServer, lobby.applicationServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), applicationServer);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatabaseInterface getDb() {
        return db;
    }
}
