package net.api.bidi;

import net.srv.Client;
import net.srv.ConnectionHandler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl  implements Connections<String>{
    private ConcurrentHashMap<String, Client> clients;
    private ConcurrentHashMap<Integer, ConnectionHandler> connections;
    private static class SingletonHolder {
        private static ConnectionsImpl instance = new ConnectionsImpl();
    }
    public static ConnectionsImpl getInstance(){
        return SingletonHolder.instance;
    }
    private ConnectionsImpl(){
        if(connections == null){
            connections = new ConcurrentHashMap<>();
        }
        if(clients == null){
            clients = new ConcurrentHashMap<>();
        }
    }

    @Override
    public boolean send(int connectionId, String msg) {
        ConnectionHandler handler = connections.get(connectionId);
        try {
            handler.send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void broadcast(String msg) {
        //TODO implement
    }

    @Override
    public void disconnect(int connectionId) {
        removeHandler(connectionId);
    }
    public boolean register(Client client){
        if(clients.containsKey(client.getUsername())){
            return false;
        }
        clients.put(client.getUsername(), client);
        return true;
    }
    public boolean register(ConnectionHandler handler){
        if(connections.containsKey(handler.getID())){
            return false;
        }
        connections.put(handler.getID(), handler);
        return true;
    }
    public Client getClient(String username){
        if(clients.containsKey(username))
            return clients.get(username);
        return null;
    }
    public Client getClientByID(int id){
        for (Map.Entry<String,Client> username: clients.entrySet()) {
            if(id ==username.getValue().getConnectionID())
                return username.getValue();
        }
        return null;
    }
    public Client[] getLoggedInUsers(){
        LinkedList<Client> cl= new LinkedList<>();
        for (Map.Entry<String,Client> username: clients.entrySet()) {
            if(username.getValue().isLoggedIn())
                cl.add(username.getValue());
        }
        return cl.toArray(new Client[cl.size()]);
    }
    public void removeHandler(int connectionID){
        if(connections.contains(connectionID)) {
            try {
                connections.get(connectionID).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connections.remove(connectionID);
        }
    }
}












