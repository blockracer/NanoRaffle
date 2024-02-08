package raffle;

import java.net.URISyntaxException;
import java.net.URI;
import uk.oczadly.karl.jnano.websocket.NanoWebSocketClient;
import java.lang.InterruptedException;
import uk.oczadly.karl.jnano.websocket.topic.TopicConfirmation;
import uk.oczadly.karl.jnano.websocket.topic.TopicUnconfirmedBlocks;
import uk.oczadly.karl.jnano.model.block.Block;
import uk.oczadly.karl.jnano.model.NanoAmount;
import java.math.BigInteger;
import java.math.BigDecimal;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
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
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;

import raffle.WebSocketHandler;
import raffle.Distribute;
import raffle.Main;


public class Ws {
	public static void balanceChecker(Session session) throws URISyntaxException, InterruptedException, IOException {

                String firstBal = Distribute.getBalance();
                                try {
                                        BigDecimal balanceDec = new BigDecimal(firstBal).movePointLeft(30).stripTrailingZeros();
                                        firstBal = balanceDec.toString();
                                        Main.globalBalance = firstBal;
                                        session.getRemote().sendString(firstBal);
                                }
                                catch (IOException e) {
                                        e.printStackTrace();
                                }

                // Register a topic listener (in this case, using a lambda function)
                Main.ws.getTopics().topicConfirmedBlocks().registerListener((message, context) -> {
                        System.out.println("found something in balance checker!!");
                        Block block = message.getBlock();
                        JsonObject blockJson = block.toJsonObject();
                        String subtype = blockJson.get("subtype").getAsString();
                                String balance = Distribute.getBalance();
                                try {
                                        BigDecimal balanceDec = new BigDecimal(balance).movePointLeft(30).stripTrailingZeros();
                                        balance = balanceDec.toString();
                                        Main.globalBalance = balance;
                                        session.getRemote().sendString(balance);
                                }
                                catch (IOException e) {
                                        e.printStackTrace();
                                }
                                catch (WebSocketException e) {
                                        System.out.println("session closed");
                                }
                });
                // Subscribe to the confirmed blocks topic, and specify filters and configuration
                boolean subscribed = Main.ws.getTopics().topicConfirmedBlocks().subscribeBlocking(
                        new TopicConfirmation.SubArgs()
                                .includeElectionInfo() // Include election info in the messages
                                .filterAccounts("nano_38sbai751batgzspmtf4x3bky7pcd3br19upemzbtb9jafaj4pdgbpo4phr5")
                        );

        }





	public static void webSocket() throws URISyntaxException, InterruptedException {
		//URI uri = new URI("wss://ws.nanoriver.io");
		//NanoWebSocketClient ws = new NanoWebSocketClient(uri);
		//ws.setObserver(new Observer());

		// Attempt to connect to the WebSocket
		String compareAmount = "1";
		// Register a topic listener (in this case, using a lambda function)
		Main.ws.getTopics().topicConfirmedBlocks().registerListener((message, context) -> {
			System.out.println("recieved something!");
 			System.out.println(message.getHash());                                      // Print the block hash
			Block block = message.getBlock();
			JsonObject blockJson = block.toJsonObject();
			String subtype = blockJson.get("subtype").getAsString();
			System.out.println(subtype);
			System.out.println(blockJson);
			String amount = message.getAmount().getAsNano().toString();

			if (subtype.equals("send") && amount.equals(compareAmount)) {

				String entryAddress = blockJson.get("account").getAsString();

				//String entryAddress = blockJson.get("link_as_account").getAsString();

				System.out.println("valid");
				//get account

				//write to entries.json file
				//Path resourcesPath = Path.of("src", "main", "resources");
				Path filePath = Paths.get("/home/server-admin/javaProjects/rafflePages/entries.json");
				//String strFilePath = filePath.toString();
				String strFilePath = "/home/server-admin/javaProjects/rafflePages/entries.json";



					try {
                        			// Read the content of the file
                        			FileReader reader = new FileReader(strFilePath);
						if(Files.size(filePath) > 0) {

							//get last entry

                        				JsonArray entriesArray = JsonParser.parseReader(reader).getAsJsonArray();
							int lastIndex =  entriesArray.size() - 1; 
							JsonElement lastElement =  entriesArray.get(lastIndex);
							JsonObject lastObj = lastElement.getAsJsonObject();
							String lastEntry = lastObj.get("entry").getAsString();
							//convert to int and add 1
							int lastEntryInt = Integer.parseInt(lastEntry);
							lastEntryInt =  lastEntryInt+1;


							JsonObject entryObject = new JsonObject();
							entryObject.addProperty("entry", String.valueOf(lastEntryInt));
							entryObject.addProperty("address", entryAddress);
							entriesArray.add(entryObject);
							//write to file
							try(PrintWriter writer = new PrintWriter(new File(strFilePath))) {
                                				writer.write(entriesArray.toString());
                                			System.out.println("File written to entries.json");
                        				} catch (FileNotFoundException e) {
                                				e.printStackTrace();
                       					}
						}
						else {
							//write a new jsonarray entry
							JsonObject firstObj = new JsonObject();
							firstObj.addProperty("entry", "1");
							firstObj.addProperty("address", entryAddress);
							JsonArray firstArray = new JsonArray();
							firstArray.add(firstObj);
							//write to file
							try(PrintWriter writer = new PrintWriter(new File(strFilePath))) {
                                				writer.write(firstArray.toString());
                                			System.out.println("clean file written to entries.json");
                        				} catch (FileNotFoundException e) {
                                				e.printStackTrace();
                       					}

						}



                        			reader.close();
                			} catch (IOException | IllegalStateException e) {
                        			System.out.println("caught an error");

                			}

			}
			else {
				System.out.println("not valid");

			}



		});


		// Subscribe to the confirmed blocks topic, and specify filters and configuration
		boolean subscribed = Main.ws.getTopics().topicConfirmedBlocks().subscribeBlocking(
        		new TopicConfirmation.SubArgs()
                		.includeElectionInfo() // Include election info in the messages
                		.filterAccounts("nano_38sbai751batgzspmtf4x3bky7pcd3br19upemzbtb9jafaj4pdgbpo4phr5")
			);

		// Print subscription status
		System.out.println(subscribed ? "Subscribed to topic!" : "Could not subscribe to topic!");
	}
}

