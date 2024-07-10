package raffle;

import spark.Spark;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import static spark.Spark.*;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.io.FileReader;
import java.time.Instant;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.lang.InterruptedException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import uk.oczadly.karl.jnano.websocket.NanoWebSocketClient;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;

import raffle.Ws;
import raffle.Distribute;

public class Main {
	public static List<String> repeatCheck = new ArrayList<>();
	private static final ConcurrentHashMap<String, AtomicLong> requestCounters = new ConcurrentHashMap<>();
    public static String globalBalance;
    public static final List<Session> balanceSessions = new ArrayList<>();
    private static final Map<String, Long> websocketConnectionTimestamps = new HashMap<>();
    private static final long WEBSOCKET_RATE_LIMIT_INTERVAL_MS = 60000; // 1 minute
    private static final int MAX_WEBSOCKET_CONNECTIONS_PER_INTERVAL = 60;

    public static URI uri;
    public static NanoWebSocketClient ws;

    public static void main(String[] args) throws URISyntaxException, InterruptedException, IOException {

        port(1234);

        staticFiles.externalLocation("/home/server-admin/javaProjects/rafflePages");
        uri = new URI("ws://127.0.0.1:7894");
        ws = new NanoWebSocketClient(uri);
	ws.setObserver(new Observer());
	Ws.balanceChecker();
        if (!ws.connect()) {
            // Connection failed
            System.err.println("Could not connect to WebSocket!");
        }
	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        	// Define the task to be executed
        Runnable task = () -> {
            		// Your method logic goes here
			repeatCheck.clear();
        };

        // Schedule the task to run every 30 minutes with an initial delay of 0 seconds
        scheduler.scheduleAtFixedRate(task, 0, 30, TimeUnit.MINUTES);

        long next24 = 0;

        webSocket("/pot", WebSocketHandler.class);

	Spark.before((request, response) -> {
		String clientIpAddress = request.headers("CF-Connecting-IP");

            long currentTimeMillis = System.currentTimeMillis();
            long currentTimeMinute = currentTimeMillis / (60 * 1000); // convert to minutes

            // Construct a key based on client IP and current minute
            String key = clientIpAddress + "_" + currentTimeMinute;

            // Get or create aimport java.util.concurrent.ConcurrentHashMap;
            AtomicLong counter = requestCounters.computeIfAbsent(key, k -> new AtomicLong(0));

            // Increment the counter
            long currentCount = counter.incrementAndGet();

            // Check if the limit is exceeded
            if (currentCount > 60) {
                if (currentTimeMinute > (Long.parseLong(key.split("_")[1]) + 1)) {
                    // Reset the counter as 1 minute has passed
                    counter.set(1);
                } else {
                    // Limit exceeded, halt the request with a 429 (Too Many Requests) status
                    response.status(429);
                    response.body("Rate limit exceeded. Please try again later.");
                    halt(429, "Rate limit exceeded, try again later");
                }
            }
		String host = request.host();
            	String mainDomain = extractMainDomain(host);

            	// Replace "allowedDomain" with the main domain you want to allow
            	if (!"nanoriver.io".equals(mainDomain)) {
                	halt(403, "Forbidden: Direct access to this server is not allowed.");
		}
		
        });

	get("/", (req, res) -> {
		String htmlContent = new String(Files.readAllBytes(Paths.get("/home/server-admin/javaProjects/rafflePages/landing.html")));
		res.type("text/html");
		return htmlContent;
        });





   //     ipAddress("[::1]");


        Spark.before("/pot", (request, response) -> {
            String ipAddress = request.headers("CF-Connecting-IP");

            if (isWebSocketRateLimited(ipAddress)) {
                Spark.halt(429, "WebSocket rate limit exceeded");
            }
        });

        String strFilePath = "/home/server-admin/javaProjects/rafflePages/time.json";

        JsonObject timeObject = new JsonObject();

        try {
            // Read the content of the file
            FileReader reader = new FileReader(strFilePath);

            // Parse the content into a JsonObject
            timeObject = JsonParser.parseReader(reader).getAsJsonObject();

            // Now you have the JsonObject
            // System.out.println(timeObject);

            reader.close();
        } catch (IOException | IllegalStateException e) {
            System.out.println("caught an error");
            timeObject.addProperty("time", "0");
            System.out.println("set time to 0 before processing");
        }

        // check if time is valid (current time is before time)
        long currentTime = Instant.now().getEpochSecond();

        // get the time element as a long value
        JsonElement raffleTimeElement = timeObject.get("time");
        long raffleTime = raffleTimeElement.getAsLong();
        System.out.println("raffleTime: " + raffleTime);
        long delay = 0;

        // check if raffle time is after current time
        if (raffleTime > currentTime) {
            // all is good we can proceed
            System.out.println("next raffle is soon");
            delay = raffleTime - currentTime;
        } else {
            raffleTime = currentTime + 432000;
            delay = raffleTime - currentTime;
            String raffleTimeStr = String.valueOf(raffleTime);
            JsonObject nextRaffleTimeObj = new JsonObject();
            nextRaffleTimeObj.addProperty("time", raffleTimeStr);
            String nextRaffleStr = nextRaffleTimeObj.toString();
            // write new jsonObject to file.
            try (PrintWriter writer = new PrintWriter(new File(strFilePath))) {
                writer.write(nextRaffleStr);
                System.out.println("File 'time.json' successfully written to resources directory!");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // start scheduler
        System.out.println("delay: " + delay);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                long currentTime = Instant.now().getEpochSecond();
                // write new time on time.json
                try (PrintWriter writer = new PrintWriter(new File(strFilePath))) {
                    JsonObject nextRaffleTimeObj = new JsonObject();
                    String nRaffle = String.valueOf((currentTime + 432000));
                    nextRaffleTimeObj.addProperty("time", nRaffle);
                    String nextRaffleStr = nextRaffleTimeObj.toString();
                    writer.write(nextRaffleStr);
                    System.out.println("File 'time.json' successfully written");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                System.out.println("distributing!");
                Distribute.distribute();
            }
        }, delay * 1000, 432000 * 1000);

        // start jnano websockets
        Ws.webSocket();
    }

    private static boolean isWebSocketRateLimited(String ipAddress) {
        long currentTime = System.currentTimeMillis();

        if (!websocketConnectionTimestamps.containsKey(ipAddress)) {
            websocketConnectionTimestamps.put(ipAddress, currentTime);
            return false;
        }

        long lastConnectionTime = websocketConnectionTimestamps.get(ipAddress);

        if (currentTime - lastConnectionTime > WEBSOCKET_RATE_LIMIT_INTERVAL_MS) {
            // Reset the counter if the interval has passed
            websocketConnectionTimestamps.put(ipAddress, currentTime);
            return false;
        }

        // Check if the number of WebSocket connections exceeds the limit
        long connectionCount = websocketConnectionTimestamps.entrySet().stream()
                .filter(entry -> currentTime - entry.getValue() <= WEBSOCKET_RATE_LIMIT_INTERVAL_MS)
                .count();

        return connectionCount >= MAX_WEBSOCKET_CONNECTIONS_PER_INTERVAL;
    }
    private static String extractMainDomain(String host) {
        try {
            URL url = new URL("http://" + host);
            String[] parts = url.getHost().split("\\.");
            if (parts.length > 1) {
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return host; // Return the original host if extraction fails
    }

}

