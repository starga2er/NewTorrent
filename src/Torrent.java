import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Torrent {
    public static void main(String argv[]) throws Exception {
        Peer[] peers = new Peer[5];
        InfoPeer[] peersList = new InfoPeer[5];
        Uploader[] uploadList = new Uploader[5];
        Downloader[] downloadList = new Downloader[5];

        File inputMessage = new File("Input.txt");
        if (!inputMessage.exists()) {
            System.out.println("Not Exists Input.txt");
            return;
        }
        BufferedReader br = new BufferedReader(new FileReader(inputMessage));
        String fileName = br.readLine();
        String fileType = br.readLine();

        for (int i = 0; i < 5; i++) {

            peersList[i] = new InfoPeer(br.readLine() ,Integer.parseInt(br.readLine()));
        }
        for (int i = 0 ; i < 5 ; i++) {
            peers[i] = new Peer(i, fileName, fileType, peersList);
            uploadList[i] = new Uploader(peers[i]);
            uploadList[i].start();
            downloadList[i] = new Downloader(peers[i]);
            for (int j = 0 ; j < 1; j++) // 1모드
                new Thread(downloadList[i]).start();
        }
    }
}
