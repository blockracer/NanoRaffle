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

@WebSocket
public class WebSocketHandler {

    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
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
                // Add any other code you want to execute every 100 seconds
            }
        }, 10000, 10000);
        // Schedule the task to run at a fixed rate of 100 seconds
	try {
		Ws.balanceChecker(session);
	}
	catch(URISyntaxException e) {
		e.printStackTrace();
	}
	catch(InterruptedException e) {
		e.printStackTrace();
	}
	catch(IOException e) {
		e.printStackTrace();
	}
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        System.out.println("Got: " + message);   // Print message
        session.getRemote().sendString(message); // and send it back
    }

}



