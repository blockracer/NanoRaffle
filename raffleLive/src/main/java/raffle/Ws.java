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
import uk.oczadly.karl.jnano.rpc.exception.RpcException;

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
import uk.oczadly.karl.jnano.util.workgen.OpenCLWorkGenerator;
import uk.oczadly.karl.jnano.util.workgen.WorkGenerator;
import uk.oczadly.karl.jnano.util.workgen.FutureWork;
import uk.oczadly.karl.jnano.util.workgen.GeneratedWork;
import uk.oczadly.karl.jnano.model.HexData;
import uk.oczadly.karl.jnano.model.work.WorkDifficulty;
import uk.oczadly.karl.jnano.rpc.request.wallet.RequestSend;
import uk.oczadly.karl.jnano.rpc.RpcQueryNode;
import uk.oczadly.karl.jnano.rpc.request.node.RequestFrontiers;
import uk.oczadly.karl.jnano.rpc.response.ResponseMultiAccountFrontiers;
import uk.oczadly.karl.jnano.model.NanoAccount;
import uk.oczadly.karl.jnano.rpc.response.ResponseBlockHash;
import uk.oczadly.karl.jnano.model.work.WorkSolution;
import uk.oczadly.karl.jnano.util.workgen.OpenCLWorkGenerator.OpenCLInitializerException;
import uk.oczadly.karl.jnano.rpc.exception.RpcInvalidArgumentException;

import com.talanlabs.avatargenerator.eightbit.EightBitAvatar;
import com.talanlabs.avatargenerator.Avatar;
import java.util.Base64;


public class Ws {

	public static void firstBalance(Session session) {
				String firstBal = Distribute.getBalance();
                                try {
                                        BigDecimal balanceDec = new BigDecimal(firstBal).movePointLeft(30).stripTrailingZeros();
                                        firstBal = balanceDec.toString();
                                        Main.globalBalance = firstBal;
					//broadCastMessage(firstBal);
                                    	session.getRemote().sendString(firstBal);
                                }
                                catch (IOException e) {
                                        e.printStackTrace();
                                }

	}
	public static void balanceChecker() throws URISyntaxException, InterruptedException, IOException {

                
                // Register a topic listener (in this case, using a lambda function)
                Main.ws.getTopics().topicConfirmedBlocks().registerListener((message, context) -> {
                        Block block = message.getBlock();
                        JsonObject blockJson = block.toJsonObject();
                        //System.out.println("found something in balance checker!! " + blockJson);
                        String subtype = blockJson.get("subtype").getAsString();
                                String balance = Distribute.getBalance();
                                try {
                                        BigDecimal balanceDec = new BigDecimal(balance).movePointLeft(30).stripTrailingZeros();
                                        //balance = balanceDec.toString();
                                        Main.globalBalance = balanceDec.toString();
                                       // session.getRemote().sendString(balance);
				       broadcastMessage();
                                }
                                catch (WebSocketException e) {
				//	e.printStackTrace();
                                 //       System.out.println("session closed");
                                }
                });
                // Subscribe to the confirmed blocks topic, and specify filters and configuration
                boolean subscribed = Main.ws.getTopics().topicConfirmedBlocks().subscribeBlocking(
                        new TopicConfirmation.SubArgs()
                                .includeElectionInfo() // Include election info in the messages
                                .filterAccounts("nano_1iroza4zsyt95uk6ucwhe1nwbe5q7g87gxfhcyuoetfkz5jmac8mtfwwoac4")
                        );

        }





	public static void webSocket() throws URISyntaxException, InterruptedException {

		RpcQueryNode rpc = new RpcQueryNode("[::1]", 7076);

		//URI uri = new URI("wss://ws.nanoriver.io");
		//NanoWebSocketClient ws = new NanoWebSocketClient(uri);
		//ws.setObserver(new Observer());

		// Attempt to connect to the WebSocket
		//String compareAmount = "1";
		BigDecimal compareAmount = new BigDecimal("1.0");
		// Register a topic listener (in this case, using a lambda function)
		Main.ws.getTopics().topicConfirmedBlocks().registerListener((message, context) -> {
			System.out.println("recieved something!");

 			System.out.println(message.getHash());                                      // Print the block hash
			Block block = message.getBlock();
			JsonObject blockJson = block.toJsonObject();
			HexData newHash = message.getHash();
			String blockString = new Gson().toJson(blockJson);
			String subtype = blockJson.get("subtype").getAsString();
			String theAccount = blockJson.get("account").getAsString();
			System.out.println("the account in confirmed found: " + theAccount + " subtype" + subtype);
			String previous = blockJson.get("previous").getAsString();
			System.out.println(blockJson);
			System.out.println(subtype);
			String strAmount = message.getAmount().toRawString();
			BigDecimal rawDec = new BigDecimal(strAmount).movePointLeft(30).stripTrailingZeros();
			strAmount = rawDec.toPlainString();	
			System.out.println("the amount: " + strAmount);	
			BigDecimal amount = new BigDecimal(strAmount);	
			int comparisonResult = amount.compareTo(compareAmount);
			boolean matchFound = false;

			String entryAddress2 = blockJson.get("account").getAsString();
			 for (String str : Main.repeatCheck) {
            			if (str.equalsIgnoreCase(previous)) {
                			System.out.println("Match found: " + str);
					matchFound = true;
				}
			}

					String entryAddress = blockJson.get("account").getAsString();

					if (subtype.equals("send") && comparisonResult >= 0 && matchFound == false && !entryAddress.equals("nano_1iroza4zsyt95uk6ucwhe1nwbe5q7g87gxfhcyuoetfkz5jmac8mtfwwoac4")) {

						System.out.println("check passed, adding to entriestest.json");

						String blockAccount = blockJson.get("account").getAsString();


						try {
						//manually check frontier againt previous
						RequestFrontiers frontiers = new RequestFrontiers(blockAccount, 1);
                        			ResponseMultiAccountFrontiers frontierResponse =  rpc.processRequest(frontiers);
                        			//byte[] accountBytes = source.getBytes();
                        			NanoAccount nanoAccount = NanoAccount.parse(blockAccount);
                        			HexData blockHash = frontierResponse.getFrontierBlockHash(nanoAccount);
						if(!blockHash.equals(newHash)) {

							System.out.println("Block ACCOUNT " + blockAccount);			
							System.out.println("NEW HASH: " + newHash);			
							System.out.println("BLOCK FRONTIER: " + blockHash);			
							return;
						}
						}
						catch (IOException e) {
							e.printStackTrace();
						}
						catch (RpcException e) {
							e.printStackTrace();
						}
						
						int firstDigit;
						
						if(strAmount.contains(".")) {
    							// Get substring before the decimal point and parse to integer
   							 firstDigit = Integer.parseInt(strAmount.substring(0, strAmount.indexOf('.')));
						} else {
    							// Parse the entire string as it doesn't contain a decimal point
   							 firstDigit = Integer.parseInt(strAmount);
						}

					

						  //firstDigit = Character.getNumericValue(strAmount.charAt(0));

						 // loop through and add
						 for (int i = 0; i < firstDigit; i++) {

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
							//get base64 of avatar
							String avatarBytes = getAvatarBase64(entryAddress);
							entryObject.addProperty("avatar", avatarBytes);
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
							System.out.println("Adding new ENTRY");

							System.out.println("Adding new ENTRY");
							//write a new jsonarray entry
							JsonObject firstObj = new JsonObject();
							firstObj.addProperty("entry", "1");
							firstObj.addProperty("address", entryAddress);
							String avatarBytes = getAvatarBase64(entryAddress);
							firstObj.addProperty("avatar", avatarBytes);
							JsonArray firstArray = new JsonArray();
							firstArray.add(firstObj);
							System.out.println(firstArray);
							System.out.println(firstArray);
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
						e.printStackTrace();

                		}
				}

				Main.repeatCheck.add(blockString);

			}
			else {
				System.out.println("not valid");
				System.out.println("subtype: " + subtype);
				System.out.println("comparison result: " + comparisonResult);
				System.out.println("Match Found? " + matchFound);
				System.out.println("account: " + entryAddress);
			}




		});


		// Subscribe to the confirmed blocks topic, and specify filters and configuration
		boolean subscribed = Main.ws.getTopics().topicConfirmedBlocks().subscribeBlocking(
        		new TopicConfirmation.SubArgs()
                		.includeElectionInfo() // Include election info in the messages
                		.filterAccounts("nano_1iroza4zsyt95uk6ucwhe1nwbe5q7g87gxfhcyuoetfkz5jmac8mtfwwoac4")
			);

		// Print subscription status
		System.out.println(subscribed ? "Subscribed to topic!" : "Could not subscribe to topic!");
	}
	
	  public static String getAvatarBase64(String entryAddress) {
                String extractedDigits = entryAddress.replaceAll("[^\\d]+", "");
                String lastSixCharacters = extractedDigits.substring(Math.max(extractedDigits.length() - 6, 0));
                System.out.println(lastSixCharacters);


                //String firstSixDigits = extractedDigits.substring(0, Math.min(6, extractedDigits.length()));
                long avatarId = Long.valueOf(lastSixCharacters).longValue();

                Avatar avatar = EightBitAvatar.newMaleAvatarBuilder().build();
                byte[] avatarBytes = avatar.createAsPngBytes(avatarId);
                String avatarString =  Base64.getEncoder().encodeToString(avatarBytes);
                return avatarString;

        }
	public static void broadcastMessage() {
        	for (Session session : Main.balanceSessions) {
            		try {
				//System.out.println("broadcasing to session: " + Main.globalBalance);
                		session.getRemote().sendString(Main.globalBalance);
           		 } catch (Exception e) {
                		e.printStackTrace();
            		}
        	}
    	}
}

