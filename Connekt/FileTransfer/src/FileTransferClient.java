import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.*;
import javafx.concurrent.Task;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class FileTransferClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Semaphore semaphore;
    private volatile boolean isPaused;

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void setStatusLabel(Label statusLabel) {
        this.statusLabel = statusLabel;
    }

    public void clearProgressBar() {
        updateProgressBar(0.0);
    }

    public void uploadFile(File file) {
        semaphore = new Semaphore(1); // Initialize semaphore with permits = 1

        Task<Void> uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
                ) {
                    out.writeObject("UPLOAD");
                    out.writeObject(file.getName());
                    out.writeObject(file.length());

                    try (FileInputStream fileIn = new FileInputStream(file)) {
                        byte[] buffer = new byte[1000000];
                        int bytesRead;
                        long totalBytesRead = 0;
                        long fileSize = file.length();

                        while ((bytesRead = fileIn.read(buffer)) != -1) {
                            semaphore.acquire(); // Acquire the permit, blocking if necessary
                            out.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            double progress = (double) totalBytesRead / fileSize;
                            System.out.println("Upload Progress: " + progress); // Debug statement
                            Platform.runLater(() -> updateProgressBar(progress));

                            // Introduce a small delay to allow the UI thread to catch up
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                semaphore.release(); // Release the permit
                            }
                        }

                        if (isPaused) {
                            setStatus("Upload paused");
                        } else {
                            setStatus("File uploaded successfully");
                        }
                    } finally {
                        clearProgressBar(); // Clear progress bar after completion
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        Thread uploadThread = new Thread(uploadTask);
        uploadThread.start();
    }

    public void downloadFile(String fileName, String directory) {
        semaphore = new Semaphore(1); // Initialize semaphore with permits = 1

        Task<Void> downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
                ) {
                    out.writeObject("DOWNLOAD");
                    out.writeObject(fileName);

                    try {
                        long fileSize = (long) in.readObject();

                        if (fileSize != -1L) {
                            // Extract the file name from the provided file path
                            String actualFileName = new File(fileName).getName();

                            // Create directories if they don't exist
                            File saveDirectory = new File(directory);
                            if (!saveDirectory.exists()) {
                                saveDirectory.mkdirs();
                            }

                            try (FileOutputStream fileOut = new FileOutputStream(new File(saveDirectory, actualFileName))) {
                                byte[] buffer = new byte[1000000];
                                int bytesRead;
                                long totalBytesRead = 0;

                                while (fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                                    semaphore.acquire(); // Acquire the permit, blocking if necessary
                                    fileOut.write(buffer, 0, bytesRead);
                                    totalBytesRead += bytesRead;

                                    double progress = (double) totalBytesRead / fileSize;
                                    System.out.println("Download Progress: " + progress);
                                    updateProgressBar(progress);

                                    semaphore.release(); // Release the permit

                                    // Introduce a small delay to allow the UI thread to catch up
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // Send acknowledgment to the server
                                String ack = (String) in.readObject();
                                System.out.println("Acknowledgment: " + ack); // Debug statement

                                if (isPaused) {
                                    setStatus("Download paused");
                                } else {
                                    setStatus("File downloaded successfully");
                                    Platform.runLater(() -> clearProgressBar());
                                }
                            }
                        } else {
                            setStatus("File does not exist on the server");
                        }
                    } catch (ClassNotFoundException | InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        clearProgressBar(); // Clear progress bar after completion
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        Thread downloadThread = new Thread(downloadTask);
        downloadThread.start();
    }

    public void pauseTransfer() {
        // Check if already paused
        if (!isPaused) {
            // Acquire the semaphore permit to pause the transfer
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setStatus("Transfer paused");
            isPaused = true;  // Set the paused flag
        }
    }

    public void resumeTransfer() {
        // Release the semaphore permit to resume the transfer
        semaphore.release();
        setStatus("Resuming transfer");
        isPaused = false;  // Reset the paused flag
    }
    

    private void updateProgressBar(double progress) {
        // Update JavaFX UI components on the JavaFX Application Thread
        Platform.runLater(() -> progressBar.setProgress(progress));
    }

    private void setStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }
}
