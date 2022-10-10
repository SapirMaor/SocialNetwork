package net.api.bidi;

import net.srv.Client;

public class BidiProtocol implements BidiMessagingProtocol<String>{
    private int connectionId;
    private ConnectionsImpl connections;
    private boolean terminate = false;
    public static final String[] badWords = {"war", "Trump", "gnome", "assignment", "spl"};
    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = ConnectionsImpl.getInstance();
    }

    @Override
    public void process(String message) {
        String[] strings = message.split(" ");
        Client c;
        Short op = Short.parseShort(strings[0]);
        switch (op) {
            case 1: // register
                    if(connections.register(new Client(strings[1], strings[2], strings[3]))){
                        connections.send(connectionId,"10 1");
                    }
                    else{
                        connections.send(connectionId, "11 1");
                    }
                break;
            case 2: //Login
                if(strings[3].equals("1")){
                    c = connections.getClient(strings[1]);
                    if(c == null || !c.login(strings[2])){
                        connections.send(connectionId, "11 2");
                    } else {
                        c.setConnectionID(connectionId);
                        connections.send(connectionId, "10 2");
                        //send backlog
                        String[] backlog = c.getBackLog();
                        for (String str : backlog) {
                            connections.send(connectionId, str);
                        }
                    }
                } else {
                    connections.send(connectionId, "11 2");
                }
                break;
            case 3: // logout
                c = connections.getClientByID(connectionId);
                if(c != null && c.logout()){
                    connections.send(connectionId,"10 3");
                    c.setConnectionID(-1);
                } else {
                    connections.send(connectionId, "11 3");
                }
                connections.removeHandler(connectionId);
                break;
            case 4: // follow/unfollow
                c = connections.getClientByID(connectionId);
                if(c!= null && c.isLoggedIn()){
                    boolean success;
                    if(strings[1].equals("0")) {
                        success = c.follow(connections.getClient(strings[2]));
                    } else{
                        success = c.unfollow(connections.getClient(strings[2]));
                    }
                    if(success){
                        connections.send(connectionId,"10 4 " + strings[2]);
                    } else {
                        connections.send(connectionId,"11 4 ");
                    }
                } else {
                    connections.send(connectionId,"11 4");
                }
                break;
            case 5: // post message
                c = connections.getClientByID(connectionId);
                if(c != null && c.isLoggedIn()){ //send to followers
                    c.incrementPost();
                    c.saveMessage(strings[1]);
                    for(Client client : c.getFollowers()) {
                        if (client.isLoggedIn()) {
                            connections.send(client.getConnectionID(), 9 + " " + 1 + " " + c.getUsername() + " " + message.substring(2));
                        } else{
                            client.backlog( 9 + " " + 1 + " " + c.getUsername() + " " + message.substring(2));
                        }
                    }
                    connections.send(connectionId,"10 5");
                    int index = message.substring(2).indexOf("@");
                    String str = message.substring(2);
                    while (index >= 0) { //send to @'s
                        String name = "";
                        if (str.indexOf(" ") != -1) {
                            name = str.substring(index + 1, str.indexOf(" ", index+1));
                        } else {
                            name = str.substring(index + 1);
                        }
                        str = str.substring(str.indexOf("@") + 1);
                        if (str.indexOf(" ") != -1) {
                            str = str.substring(str.indexOf(" ") + 1);
                        }
                        index = str.indexOf("@");

                        Client client = connections.getClient(name);
                        if(client != null) {
                            if (client.isLoggedIn()) {
                                connections.send(client.getConnectionID(), 9 + " " + 1 + " " + c.getUsername() + " " + message.substring(2));
                            } else {
                                client.backlog(9 + " " + 1 + " " + c.getUsername() + " " + message.substring(2));
                            }
                        }
                    }
                } else{
                    connections.send(connectionId,"11 5");
                }
                break;
            case 6: //PM message
                String censored = filter(message.substring(2+strings[1].length()));
                c = connections.getClientByID(connectionId);
                Client recipient = connections.getClient(strings[1]);
                if(c != null && c.isFollowing(recipient)) {
                    if (recipient.isLoggedIn()) {
                        c.saveMessage(9 + " " + 0 + " " + c.getUsername() + " " + censored);
                        connections.send(recipient.getConnectionID(), 9 + " " + 0 + " " + c.getUsername() + /*" " +*/censored);
                    } else{
                        c.saveMessage(9 + " " + 0 + " " + c.getUsername() + " " +censored);
                        recipient.backlog(9 + " " + 0 + " " + c.getUsername() + " " + censored);
                    }
                    connections.send(connectionId,"10 6");
                }else {
                    connections.send(connectionId, "11 6");
                }
                break;
            case 7: //logstat
                c = connections.getClientByID(connectionId);
                if(c != null && c.isLoggedIn()) {
                    Client[] cl = connections.getLoggedInUsers();
                    String multiAck = "";
                    for (Client client : cl) {
                        multiAck+= "10 7 " + client.getAge() + " " + client.getPosts() + " " + client.getNumFollowers() + " " + client.getNumFollowing() + "\0";
                    }
                    connections.send(connectionId,multiAck.substring(0,multiAck.length()-1));
                } else{
                    connections.send(connectionId, "11 7");
                }
                break;
            case 8: // stat
                boolean isError = false;
                String[] usernames = strings[1].split("\\|");
                c = connections.getClientByID(connectionId);
                if(c!=null && c.isLoggedIn()){
                    String multiAck= "";
                    for (String username: usernames) {
                        Client client = connections.getClient(username);
                        if(client!= null){
                            multiAck+= "10 8 " + client.getAge() + " " + client.getPosts() + " " + client.getNumFollowers() + " " + client.getNumFollowing() + "\0";
                        } else {
                            multiAck = "11 8";
                            connections.send(connectionId,multiAck);
                            isError = true;
                            break;
                        }
                    }
                    if(!isError)
                        connections.send(connectionId,multiAck.substring(0,multiAck.length()-1));
                } else {
                    connections.send(connectionId, "11 8");
                }
                break;
            case 12: // block
                Client client = connections.getClient(strings[1]);
                if(client != null){
                    connections.getClientByID(connectionId).block(client);
                    connections.send(connectionId, "10 12");
                } else {
                    connections.send(connectionId,"11 12");
                }
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return terminate;
    }

    public static String filter(String msg){
        String words[] = msg.split(" ");
        String str="";
        for (int i = 0; i < words.length; i++) {
            for(String filter : badWords){
                String noPunc = words[i].replaceAll("[().!?,]","");
                if(noPunc.equals(filter)){
                    words[i] = words[i].replace(filter,"<filtered>");
                    break;
                }
            }
            str+= words[i];
            if(i!= words.length -1){
                str+= " ";
            }
        }
        return str;
    }
}
