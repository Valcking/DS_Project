package com.kuleuven.distributedsystems.applicationserver.rest.controllers;

import classes.Coordinate;
import classes.ResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuleuven.distributedsystems.applicationserver.rest.VirtualClient;
import com.kuleuven.distributedsystems.applicationserver.rest.VirtualClientManager;
import exceptions.InvalidMoveException;
import exceptions.NotInGameException;
import exceptions.NotYourTurnException;
import exceptions.UserNotLoggedInException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.rmi.RemoteException;

import static classes.ResponseType.NOK;
import static classes.ResponseType.OK;

@RestController
@RequestMapping(value = "memory/game")
public class GameRestController {

    private static VirtualClientManager clientManager = VirtualClientManager.getInstance();

    /*
     * REST METHODS
     * */

    @RequestMapping(value = "leaveGame", produces = "application/json")
    public ResponseMessage leaveGame(String token) {
        ResponseMessage responseMessage = null;

        try {
            VirtualClient client = clientManager.getClient(token);
            client.leaveGame();
            responseMessage = new ResponseMessage(OK, "Left the game successfully");
        } catch (UserNotLoggedInException | NotInGameException e) {
            responseMessage = new ResponseMessage(NOK, e.getMessage());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return responseMessage;
    }

    @RequestMapping(value = "readyUp", produces = "application/json")
    public ResponseMessage readyUp(@RequestParam String token) {

        ResponseMessage responseMessage = null;

        VirtualClient client = null;
        try {
            client = clientManager.getClient(token);
            client.readyUp();
            responseMessage = new ResponseMessage(OK, "Player set to ready.");

        } catch (UserNotLoggedInException | NotInGameException e) {
            responseMessage = new ResponseMessage(NOK, e.getMessage());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return responseMessage;
    }

    //Polling method
    @RequestMapping(value = "getGameState", produces = "application/json")
    public ResponseMessage getGameState(@RequestParam String token, @RequestParam int from) {
        ResponseMessage responseMessage = null;

        try {
            VirtualClient client = clientManager.getClient(token);
            Object o = client.getGameController().getActions(from);
            responseMessage = new ResponseMessage(OK, "Game data:", o);
        } catch (UserNotLoggedInException e) {
            responseMessage = new ResponseMessage(NOK, e.getMessage());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return responseMessage;
    }

    @RequestMapping(value = "move", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseMessage move(@RequestParam String token, @RequestBody String string) {
        ResponseMessage responseMessage = null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            VirtualClient client = clientManager.getClient(token);
            Coordinate c = mapper.readValue(string, Coordinate.class);
            Object value = client.recordMove(c);
            responseMessage = new ResponseMessage(OK, "Move has been processed", value);
        } catch (UserNotLoggedInException | InvalidMoveException | NotYourTurnException e) {
            responseMessage = new ResponseMessage(NOK, e.getMessage());
        } catch (IOException e) {
            responseMessage = new ResponseMessage(NOK, "Something went wrong!");
            e.printStackTrace();
        }

        return responseMessage;
    }
}