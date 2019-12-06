import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Uploader extends Thread {
    private Peer peer;
    byte[] buf;

    ServerSocket serverSocket;
    Socket connectionSocket;
    DataInputStream inputClient;
    DataOutputStream outputClient;

    public Uploader(Peer peer) throws IOException {
        this.peer = peer;
        buf = new byte[Peer.ChunkSize];
        serverSocket = new ServerSocket(peer.PortNum);
    }

    public void run(){
        while(true){
            try{
                connectionSocket = serverSocket.accept();
                System.out.println(peer.PeerNum + " Server connect : " + connectionSocket.getPort() );
                connectionSocket.setSoTimeout(10000);
                inputClient = new DataInputStream(connectionSocket.getInputStream());
                outputClient = new DataOutputStream(connectionSocket.getOutputStream());

                if (peer.checkChunk != null){
                    outputClient.writeInt(peer.checkChunk.length);
                    int check = inputClient.readInt();
                    if (check == peer.checkChunk.length){
                        boolean loop = true;
                        while (loop){
                            peer.useCheckChunk();
                            outputClient.write(peer.checkChunk);
                            peer.releaseCheckChunk();

                            for (int i = 0 ; i < 3 ; i++){
                                int ClientWant = inputClient.readInt();
                                if (ClientWant == -1){
                                    if (i == 0)
                                        loop = false;
                                } else {
                                    peer.useFile();
                                    peer.randomFile.seek(ClientWant * Peer.ChunkSize);
                                    int length = peer.randomFile.read(buf);
                                    peer.releaseFile();
                                    System.out.println(peer.PeerNum + " send Data " + ClientWant + " to " + connectionSocket.getPort() );
                                    outputClient.write(buf, 0 , length);
                                    System.out.println(peer.PeerNum + " send finish");
                                }
                            }
                            if (loop)
                                Thread.sleep(5000);
                        }
                    } else {
                        System.out.println("Error : check ");
                    }
                    //inputClient.close();
                    //outputClient.close();

                    // connectionSocket.close();
                } else {
                    outputClient.writeInt(-1);
                }
                inputClient.close();
                outputClient.close();

                connectionSocket.close();
                System.out.println(peer.PeerNum + " Server disconnect " + connectionSocket.getPort());
            } catch(SocketException e ){
                System.out.println(peer.PeerNum + " Server disconnect " + connectionSocket.getPort());
            }
            catch ( SocketTimeoutException e ){
                System.out.println("TIME OUT!!! " + peer.PeerNum + " Server disconnect " + connectionSocket.getPort());
            }
            catch (Exception e){
                if(!connectionSocket.isClosed()) {
                    try {
                        connectionSocket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
