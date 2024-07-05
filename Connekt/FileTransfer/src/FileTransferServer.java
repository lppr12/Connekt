import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
public class FileTransferServer {
    public static final int PORT = 8888;
    private static final String SERVER_DIRECTORY = "server_files/";
    public static String getServerDirectory() {
        return SERVER_DIRECTORY;
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running and waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.in = new ObjectInputStream(clientSocket.getInputStream());
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void run() {
        try {
            String command = (String) in.readObject();

            if (command.equals("UPLOAD")) {
                handleUpload();
            } else if (command.equals("DOWNLOAD")) {
                handleDownload();
            } else if (command.equals("LIST_FILES")) {
                sendFileList();
            }
            else if (command.equals("CONNECTED")) {
                handleConnected();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleConnected() {
        System.out.println("Client connected: " + clientSocket.getInetAddress());
    }

     private void handleUpload() throws IOException, ClassNotFoundException {
        String fileName = (String) in.readObject();
        long fileSize = (long) in.readObject();
        Path filePath = Paths.get(FileTransferServer.getServerDirectory(), fileName);

        try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[1000000];
            int bytesRead;

            while (fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }

            System.out.println("File uploaded: " + fileName);
        }
    }

    private void handleDownload() throws IOException, ClassNotFoundException {
    String fileName = (String) in.readObject();
    Path filePath = Paths.get(FileTransferServer.getServerDirectory(), fileName);

    if (Files.exists(filePath)) {
        long fileSize = Files.size(filePath);
        out.writeObject(fileSize);

        try (InputStream fileIn = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[1000000];
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("File sent: " + fileName);
        }
    } else {
        out.writeObject(-1L); // Signal that the file does not exist
    }
}


    private void sendFileList() throws IOException {
        List<String> files = Arrays.stream(new File(FileTransferServer.getServerDirectory()).list())
                .collect(Collectors.toList());
        out.writeObject(files);
    }
}

