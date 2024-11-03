package networking;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TCPServer {
  private static final int PORT = 12345;
  private static final int THREAD_POOL_SIZE = 10;

  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
      System.out.println("TCP Server is running and listening on port " + PORT);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        threadPool.execute(new ClientHandler(clientSocket));
      }
    }
  }

  private static class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
      this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          System.out.println("Received: " + inputLine);
          out.println(inputLine); // Echo back to the client
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          clientSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static class TCPClient {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private static final int NUM_CLIENTS = 10;
    private static final int NUM_MESSAGES = 100;
    private static final int MESSAGE_DELAY_MS = 50;

    public static void main(String[] args) throws InterruptedException {
      ExecutorService threadPool = Executors.newFixedThreadPool(NUM_CLIENTS);

      for (int i = 0; i < NUM_CLIENTS; i++) {
        threadPool.execute(new ClientTask(i));
      }

      threadPool.shutdown();
      threadPool.awaitTermination(10, TimeUnit.MINUTES);
    }

    private static class ClientTask implements Runnable {
      private final int clientId;

      public ClientTask(int clientId) {
        this.clientId = clientId;
      }

      @Override
      public void run() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
          for (int i = 0; i < NUM_MESSAGES; i++) {
            String message = "Client " + clientId + " message " + i;
            out.println(message);
            String response = in.readLine();
            System.out.println("Client " + clientId + " received: " + response);
            Thread.sleep(MESSAGE_DELAY_MS); // Simulate delay
          }
        } catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static class StressTest {
    public static void main(String[] args) throws InterruptedException {
      System.out.println("Starting TCP Server...");

      new Thread(() -> {
        try {
          TCPServer.main(args);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();

      // Give server some time to start
      Thread.sleep(2000);

      long startTime = System.nanoTime();

      System.out.println("Starting TCP Clients...");
      TCPClient.main(args);

      long endTime = System.nanoTime();

      System.out.println("Stress test completed.");
      System.out.println("Elapsed time: " + (endTime - startTime) / 1_000_000 + " ms");
    }
  }
}
