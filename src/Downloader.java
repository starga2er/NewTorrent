import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Downloader implements Runnable{
    Peer peer;
    byte[] buf;
    Socket clientSocket;
    byte[] uploaderBitMap;

    DataInputStream inputServer;
    DataOutputStream outputServer;

    public Downloader(Peer peer){
        this.peer = peer;
        buf = new byte[Peer.ChunkSize];
        clientSocket = null;
        uploaderBitMap =null;
        inputServer = null;
        outputServer = null;
    }

    @Override
    public void run() {
        int nowPeer = 0;
        while (!peer.downloadAll){
            try{
                while (true) {
                    clientSocket = null;
                    inputServer = null;
                    outputServer = null;
                    try {
                        nowPeer = peer.useFriendList();
                        clientSocket = new Socket(peer.friendsList[nowPeer].peerIP, peer.friendsList[nowPeer].peerPort);
                        clientSocket.setSoTimeout(10000);
                        break;
                    } catch ( Exception e ){
                        System.out.println(" Error ");
                        // peer.releaseFriendList(nowPeer);
                    }
                }

                while (clientSocket.isClosed()) {
                    System.out.println("wait");
                }
                System.out.println(peer.PeerNum + " Client connect : " + clientSocket.getPort() + " / " + clientSocket.getLocalPort());
                inputServer = new DataInputStream(clientSocket.getInputStream());
                outputServer = new DataOutputStream(clientSocket.getOutputStream());
                int check = inputServer.readInt();

                if (check != -1){
                    if (uploaderBitMap ==null)
                        uploaderBitMap = new byte[check];
                    peer.useCheckChunk();
                    if (peer.checkChunk == null){
                        peer.checkChunk = new byte[check];
                    }
                    peer.releaseCheckChunk();

                    outputServer.writeInt(check);

                    boolean loop = true;
                    while (loop){
                        inputServer.read(uploaderBitMap);

                        peer.useCheckChunk();
                        int[] map = peer.compareCheckChunk(uploaderBitMap);
                        peer.releaseCheckChunk();

                        for (int i = 0 ; i < 3 ; i++){
                            outputServer.writeInt(map[i]);
                            if (map[i] != -1) {

                                int length = inputServer.read(buf);

                                peer.useFile();
                                peer.randomFile.seek(map[i] * Peer.ChunkSize);
                                peer.randomFile.write(buf, 0 , length);
                                peer.releaseFile();

                                peer.useCheckChunk();
                                peer.SetUpChunk(map[i]);
                                peer.releaseCheckChunk();
                            }
                        }
                        if (map[0] == -1)
                            loop =false;
                    }
                    boolean All = true;
                    peer.useCheckChunk();
                    for(int i = 0 ; i < peer.checkChunk.length ; i++){
                        if (peer.checkChunk[i] != 2){
                            All = false;
                            break;
                        }
                    }
                    peer.downloadAll = All;
                    peer.releaseCheckChunk();


                }
                inputServer.close();
                outputServer.close();
                clientSocket.close();
                System.out.println(peer.PeerNum + " Client disconnect " + clientSocket.getLocalPort());

            } catch ( SocketException e ){
                System.out.println(peer.PeerNum + " Client disconnect " + clientSocket.getLocalPort());
            } catch ( SocketTimeoutException e ){
                System.out.println("TIME OUT!!! " + peer.PeerNum + " Client disconnect " + clientSocket.getLocalPort());
                if (!clientSocket.isClosed()){
                    try {
                        clientSocket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            catch (Exception e){
                System.out.println(peer.PeerNum + " | error " + clientSocket.getLocalPort() + " / ");
                e.printStackTrace();
                try {
                    if (clientSocket.isConnected())
                        clientSocket.close();
                } catch (IOException ex) {
                    System.out.println(" BIG ERROR ");
                }

            } finally {
                // peer.releaseFriendList(nowPeer);
            }
        }
        System.out.println((peer.PeerNum + " is download all!"));
    }

}
