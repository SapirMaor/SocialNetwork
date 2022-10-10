package net.srv;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

public class Client {
    public final String username;
    private final String password;
    public final LocalDate birthday;
    private boolean status; // connection status
    private int posts = 0;
    private final LinkedList<Client> following;
    private final LinkedList<Client> followers;
    private final LinkedList<Client> blocked;
    private final LinkedList<String> messages;
    private int connectionID = -1;
    private LinkedList<String> backlog;

    public Client(String username, String password, String birthday){
        this.username = username;
        this.password = password;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        this.birthday =  LocalDate.parse(birthday, dtf);
        following = new LinkedList<>();
        followers = new LinkedList<>();
        blocked = new LinkedList<>();
        messages = new LinkedList<>();
        backlog = new LinkedList<>();
    }

    /**
     *
     * @return login status
     */
    public boolean isLoggedIn(){
        return status;
    }

    /**
     * increments posts
     */
    public void incrementPost(){
        posts++;
    }

    /**
     * Checks if the password given is equals to the clients passwords and updates status.
     * @param password : login password given by the user.
     * @return true: connection successful, false: connection failed.
     */
    public boolean login(String password){
        if(status)
            return false;
        if(password.equals(this.password)){
            status = true;
            return true;
        }
        return false;
    }

    /**
     * attempts to log out user
     * @return true: logout successful, false: user was not logged in.
     */
    public boolean logout(){
        if(!status){
            return false;
        }
        status = false;
        return true;
    }

    /**
     * 'this' will follow the given client.
     * @param client: the client to follow.
     * @return true: following successful, false: already following.
     */
    public boolean follow(Client client){
        if(client == null || (following.contains(client) || blocked.contains(client) || client.blocked.contains(this))){
            return false;
        }
        following.add(client);
        client.followers.add(this);
        return true;
    }

    /**
     * 'this' will unfollow the given client.
     * @param client: the client to unfollow.
     * @return true: unfollowing successful, false: already not following.
     */
    public boolean unfollow(Client client){
        if(!following.contains(client)){
            return false;
        }
        following.remove(client);
        client.followers.remove(this);
        return true;
    }
    //<editor-fold desc="logStat">
    /**
     *
     * @return age of client
     */
    public int getAge(){
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    /**
     *
     * @return number of posts
     */
    public int getPosts(){
        return posts;
    }

    /**
     *
     * @return number of clients 'this' follows
     */
    public int getNumFollowers(){
        return  followers.size();
    }

    /**
     *
     * @return number of clients 'this' is following
     */
    public int getNumFollowing(){
        return  following.size();
    }
    //</editor-fold>

    /**
     *
     * @param client to block
     */
    public void block(Client client){
        blocked.add(client);
        followers.remove(client);
        following.remove(client);
        client.followers.remove(this);
        client.following.remove(this);
    }

    public int getConnectionID() {
        return connectionID;
    }

    public void setConnectionID(int connectionID) {
        this.connectionID = connectionID;
    }

    public String getUsername() {
        return username;
    }

    public LinkedList<Client> getFollowing() {
        return following;
    }

    public LinkedList<Client> getFollowers() {
        return followers;
    }
    public boolean isFollowing(Client client) {
        if (client != null)
            return following.contains(client);
        return false;
    }
    public void backlog(String msg){
        backlog.add(msg);
    }
    public String[] getBackLog(){
        String[] back = backlog.toArray(new String[backlog.size()]);
        backlog.clear();
        return back;
    }
    public void saveMessage(String msg){
        messages.add(msg);
    }
}
