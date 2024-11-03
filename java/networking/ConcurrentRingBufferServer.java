package networking;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import datastructures.ConcurrentRingBuffer;

public class ConcurrentRingBufferServer {

  private GenericDataStructureHttpServer<ConcurrentRingBuffer<String>> handler;

  public ConcurrentRingBufferServer(int port, int capacity) throws IOException {
    Map<String, String> methods = new HashMap<>();
    methods.put("offer", "POST");
    methods.put("poll", "GET");
    methods.put("isEmpty", "GET");
    methods.put("isFull", "GET");
    ConcurrentRingBuffer<String> ringBuffer = new ConcurrentRingBuffer<>(1000);
    this.handler = new GenericDataStructureHttpServer<ConcurrentRingBuffer<String>>(ringBuffer, port, methods);
  }

  public void start() { handler.start(); }
  public static void main(String[] args) throws IOException {
    try {
      ConcurrentRingBufferServer server = new ConcurrentRingBufferServer(8086, 1000);
      server.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
