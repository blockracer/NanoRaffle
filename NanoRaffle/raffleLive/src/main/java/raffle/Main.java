package raffle;

import spark.Spark;
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


import raffle.Ws;
import raffle.Distribute;


public class Main {
	public static String globalBalance;

	public static void main (String[] args) throws URISyntaxException, InterruptedException, IOException  {
		long next24 = 0;

		port(9654);
		staticFiles.externalLocation("/path/to/folder/rafflePages");


		webSocket("/pot", WebSocketHandler.class);


		String strFilePath = "/path/to/file/time.json";

		//staticFiles.location(resourcesPath.toString());

		Spark.init();

		
		JsonObject timeObject = new JsonObject();

		 try {
            		// Read the content of the file
            		FileReader reader = new FileReader(strFilePath);

            		// Parse the content into a JsonObject
            		timeObject = JsonParser.parseReader(reader).getAsJsonObject();

            		// Now you have the JsonObject
            		//System.out.println(timeObject);

            		reader.close();
        	} catch (IOException | IllegalStateException e) {
			System.out.println("caught an error");
			timeObject.addProperty("time", "0");
			System.out.println("set time to 0 before processeing");

        	}

		//check if time is valid(current time is before time)
		long currentTime = Instant.now().getEpochSecond();

		//get the time element as long value
		JsonElement raffleTimeElement = timeObject.get("time");
		long raffleTime = raffleTimeElement.getAsLong();
		System.out.println("raffleTIme: "+ raffleTime);
		long delay = 0;
		long nextRaffleAfter = 0;


		//check if raffle time is after current time
		if(raffleTime > currentTime) {
		//all is good we can proceed
			System.out.println("next raffle is soon");
			delay = raffleTime - currentTime; 


		}
		else {
			raffleTime = currentTime + 604800;
			delay = raffleTime - currentTime;
			String raffleTimeStr = String.valueOf(raffleTime);
			JsonObject nextRaffleTimeObj = new JsonObject();
			nextRaffleTimeObj.addProperty("time", raffleTimeStr);
			String nextRaffleStr = nextRaffleTimeObj.toString();  
			//write new jsonObject to file.
			try(PrintWriter writer = new PrintWriter(new File(strFilePath))) {
				writer.write(nextRaffleStr);
			 	System.out.println("File 'time.json' successfully written to resources directory!");
				//raffleTime = Long.parseLong(nextRaffleTimeObj.get("time"));
        		} catch (FileNotFoundException e) {
            			e.printStackTrace();
        		}

		}

			//start schedular
			//long delay = 1000 * (raffleTime - currentTime); 
			//long next24Mil = next24 * 1000;
			System.out.println("delay: " + delay);
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					long currentTime = Instant.now().getEpochSecond();
					//write new time on time.json
					try(PrintWriter writer = new PrintWriter(new File(strFilePath))) {
						JsonObject nextRaffleTimeObj = new JsonObject();
						String nRaffle = String.valueOf((currentTime + 604800));
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
			}, delay*1000, 604800*1000);
		//start jnano websockets
			Ws.webSocket();
			System.out.println("Test");
        }
}

