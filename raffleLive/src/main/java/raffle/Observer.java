package raffle;

import uk.oczadly.karl.jnano.websocket.WsObserver;
import raffle.Ws;
import java.net.URISyntaxException;
import java.lang.InterruptedException;
import raffle.Main;

public class Observer implements WsObserver {
    @Override
    public void onOpen(int httpStatus) {
        System.out.println("WebSocket connected!");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket disconnected!");
	//retry connection
	try { 

		if (!Main.ws.connect()) {
            		// Connection failed
            		System.err.println("Could not connect to WebSocket!");
		}

		//Ws.webSocket();

	}
	catch(InterruptedException e) {
		e.printStackTrace();
	}
    }

    @Override
    public void onSocketError(Exception ex) {
        ex.printStackTrace();

    }
}
