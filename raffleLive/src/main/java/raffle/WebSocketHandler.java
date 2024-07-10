package raffle;

import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.URISyntaxException;
import java.lang.InterruptedException;
import java.io.IOException;

import raffle.Ws;
import raffle.Main;

@WebSocket
public class WebSocketHandler {

    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
        Main.balanceSessions.add(session);
	Timer timer = new Timer();

	timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run()  {
                // Replace this with your custom logic
                // Assuming 'session' is an instance variable or passed as a parameter
		try {
                	session.getRemote().sendString(Main.globalBalance);
		}
		catch(IOException e) {
			e.printStackTrace();

		}
		catch(WebSocketException e) {
			//print to see connection spam
			//System.out.println("websocketHandler session closed");

		}
                // Add any other code you want to execute every 100 seconds
            }
        }, 30000, 30000);
        // Schedule the task to run at a fixed rate of 100 seconds
	try {
		Ws.firstBalance(session);
	}
	catch(Exception e) {
		e.printStackTrace();
	}
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
        Main.balanceSessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        System.out.println("Got: " + message);   // Print message
        session.getRemote().sendString(message); // and send it back
    }

}



