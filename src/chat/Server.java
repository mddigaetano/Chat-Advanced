package chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * This class lets you create the server side of a point-to-point chat.
 * There's no actual difference between the client and the server, but
 * the connection method.
 * 
 * @author Amedeo
 */
public class Server {
    
    /**
     * This is the server IP. It will be used by the Client
     */
    public static final String IP = "192.168.56.101";   //IP del server

    /**
     * This is the server listening port. It will be used by the Client
     */
    public static final int PORT = 3939;    //porta di ascolto del server
    
    private ServerSocket ssock; //socket che attendera' connessioni
    private Socket sock;    //socket della connessione avvenuta
    private boolean link;   //se il collegamento e' andato a buon fine sara' impostata a true
    
    private Server(){
        this.link = false;  //verra' settata true se e solo se andra' TUTTO a buon fine
        
        try {
            //istanzio il socket in ascolto e imposto un timeout di 10 secondi
            this.ssock = new ServerSocket(Server.PORT);
            this.ssock.setSoTimeout(10000);
            
            System.out.println("Listening...");
            
            //attendo una connessione
            this.sock = this.ssock.accept();    
            System.out.println("Linked!");
            
            //tutto andato a buon fine
            this.link = true;
        } catch (SocketTimeoutException ste){
            System.err.println("Time's over");
        } catch (IOException ioe) {
            System.err.println("Couldn't create listening socket");
        }
    }
    
    private boolean isLinked(){
        return this.link;
    }
    
    
    private void close(){
        
        //chuidere prima il socket della connessione...
        //ma se non esiste il socket in ascolto non puo' esistere quello della connessione!
        if(ssock != null){
            if(sock != null){
                try {
                    sock.close();
                } catch (IOException ioe) {
                    System.err.println("Couldn't close connection socket");
                }
            }
            
            try {
                ssock.close();
            } catch (IOException ioe) {
                System.err.println("Couldn't close listening socket");
            }
            
        }
    }
    
    private InputStream getInputStream(){
        try {
            return sock.getInputStream();
        } catch (IOException ioe) {
            System.err.println("Input Socket is not linked");
        }
        return null;
    }
    
    private OutputStream getOutputStream(){
        try {
            return sock.getOutputStream();
        } catch (IOException ex) {
            System.err.println("Output Socket is not linked");
        }
        return null;
    }
    
    /**
     * It will create a new Server instance, check if it went well, and eventually
     * start the chat, terminating it afterwards.
     * 
     * @param args
     */
    public static void main(String[] args){
        
        Server server = new Server();
        
        //se la connessione esiste senza problemi
        if(server.isLinked()){
            //inizio la chat
            new Chat(server.getInputStream(), server.getOutputStream(), true);
        }
        
        //chiudo il server
        server.close();
        
    }
    
}
