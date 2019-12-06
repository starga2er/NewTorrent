import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Peer {
    public static int ChunkSize = 1024 * 10;

    byte[] checkChunk;
    boolean isLockChuck;
    boolean isLockFile;
    File downloadFile;
    RandomAccessFile randomFile;
    boolean downloadAll;
    int PeerNum;
    int PortNum;

    InfoPeer[] friendsList;
    volatile int friendsClock;

    public Peer(int number, String fileName, String fileType, InfoPeer[] list) throws FileNotFoundException {
        PeerNum = number;
        isLockChuck = isLockFile = false;
        downloadFile = new File(fileName + number + "."+ fileType);
        if (downloadFile.exists()){
            downloadAll = true;
            checkChunk = new byte[((int)downloadFile.length()-1) / ChunkSize + 1];
            Arrays.fill(checkChunk, (byte) 2);
        } else {
            downloadAll = false;
            checkChunk = null;
        }
        int i = 0;
        int j = 0;
        friendsClock = -1;
        friendsList = new InfoPeer[4];
        for (; i< 5 ; i++){
            if (PeerNum == i){
                PortNum = list[i].peerPort;
            } else {
                friendsList[j] = list[i];
                j++;
            }
        }
        randomFile = new RandomAccessFile(downloadFile, "rw");
    }

    public synchronized void useFile() throws InterruptedException {
        while (isLockFile){
            wait();
        }
        isLockFile = true;
    }

    public synchronized void releaseFile() {
        isLockFile = false;
        notifyAll();
    }

    public synchronized void useCheckChunk() throws InterruptedException {
        while (isLockChuck){
            wait();
        }
        isLockChuck = true;
    }

    public synchronized void releaseCheckChunk() {
        isLockChuck = false;
        notifyAll();
    }

    public int[] compareCheckChunk(byte[] otherCheckChunk) {
        if (isLockChuck){
            int num = 0;
            int[] ret = new int[3];
            for (int i = 0 ; i < this.checkChunk.length && num != 3; i++){
                if (otherCheckChunk[i] == (byte) 2 && this.checkChunk[i] == (byte)0){
                    ret[num] = i;
                    this.checkChunk[i] = (byte)1;
                    num++;
                }
            }

            for (; num < 3; num++) {
                ret[num] = -1;
            }

            System.out.println(PeerNum + " : " + ret[0] + "/" + ret[1] + "/" + ret[2] + "/");
            return ret;

        } else {
            System.out.println("Thread Error : Compare");
            return new int[3];
        }
    }

    public void SetUpChunk(int index){
        if (isLockChuck) {
            if (checkChunk[index] == (byte) 1)
                checkChunk[index] = (byte) 2;
            else
                System.out.println("error checkChunk");
        } else
            System.out.println("Thread Error : SetUp");
    }

    public synchronized int useFriendList() {
        friendsClock = (friendsClock+1)%4;
        return friendsClock;
    }

}


class InfoPeer {
    public String peerIP;
    public int peerPort;

    public InfoPeer(String peerIP, int peerPort){
        this.peerIP = peerIP;
        this.peerPort = peerPort;
    }
}