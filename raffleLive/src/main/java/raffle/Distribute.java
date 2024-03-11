package raffle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import java.util.Random;
import java.io.FileReader;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.io.IOException;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.security.SecureRandom;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import java.math.RoundingMode;


public class Distribute {
	//private static  Path resourcesPath = Path.of("src", "main", "resources");
	private static Path filePath = Paths.get("/home/server-admin/javaProjects/rafflePages/entries.json");
	private static	Path winnersPath = Paths.get("/home/server-admin/javaProjects/rafflePages/winners.json");
	
	public static void distribute() {
		//write to entries.json file
               		//String strFilePath = filePath.toString();
               		String strFilePath = "/home/server-admin/javaProjects/rafflePages/entries.json";

		try {
                	// Read the content of the file
                        FileReader reader = new FileReader(strFilePath);
                       	if(Files.size(filePath) > 0) {
 				JsonArray entriesArray = JsonParser.parseReader(reader).getAsJsonArray();
				//choose random winner

				// Get a random index
        			int randomIndex = getRandomIndex(entriesArray.size());

				JsonElement winnerElement =  entriesArray.get(randomIndex);
				
        			JsonObject winnerObject = winnerElement.getAsJsonObject();

				System.out.println(winnerObject.get("address").getAsString());

				String winnerAccount = winnerObject.get("address").getAsString();

				//get balance of account
				 //HttpClient httpClient = HttpClients.createDefault();

        			// Define the URL you want to send the GET request to
        			//String url = "http://localhost:3333/getpotbalance";

        			// Create an HttpGet object with the URL
        			//HttpGet httpGet = new HttpGet(url);

        			try {
            				// Execute the request and get the response
            				//HttpResponse response = httpClient.execute(httpGet);

            				// Check if the response code is successful (200 OK)
            				//if (response.getStatusLine().getStatusCode() == 200) {
                				// Parse the response content as a JSON string
                				//String jsonResponse = EntityUtils.toString(response.getEntity());

                				// Use Gson to parse the JSON string into a JsonElement
                				//JsonElement jsonElement = JsonParser.parseString(jsonResponse);
						//JsonObject potObj = jsonElement.getAsJsonObject();
                				// Now you can work with the JsonElement as needed. convert to jsonObject with .getAsJsonObjet()
                				//System.out.println("JsonElement: " + jsonElement.toString());

						//BigDecimal potBig = new BigDecimal(potObj.get("balance").getAsString());
						BigDecimal potBig = new BigDecimal(getBalance());

						BigDecimal winnerPayout = potBig.multiply(new BigDecimal("0.90"));
						winnerPayout = winnerPayout.setScale(0, RoundingMode.DOWN);
						String winnerPayoutStr = winnerPayout.toString();
						System.out.println("winner payout: " + winnerPayout);


						BigDecimal donationPayout = potBig.multiply(new BigDecimal("0.025"));
						donationPayout = donationPayout.setScale(0, RoundingMode.DOWN);
						System.out.println("donation payout: " + donationPayout);
						String donationPayoutStr = donationPayout.toString();


						BigDecimal myPayout = potBig.multiply(new BigDecimal("0.075"));
						myPayout = myPayout.setScale(0, RoundingMode.DOWN);
						String myPayoutStr = myPayout.toString();
						System.out.println("my payout: " + myPayout);

						//make the send
						send(winnerAccount, winnerPayoutStr, donationPayoutStr, myPayoutStr);
						System.out.println("payout distributed!");

						//add entry to winners.json
						addWinner(winnerAccount, winnerPayout);


						//clear entries file
						PrintWriter writer = new PrintWriter(new FileWriter(strFilePath, false));
						writer.close();


            				//} 
					//else {
                			//	System.out.println("Failed to retrieve data. Response code: " + response.getStatusLine().getStatusCode());
           				//}
        			} catch (Exception e) {
            				e.printStackTrace();
        			}


			}
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}

	}

	 private static int getRandomIndex(int arrayLength) {
		SecureRandom secureRandom = new SecureRandom();
        	return secureRandom.nextInt(arrayLength);
    	}

	private static void send(String winnerAccount, String winnerPayoutStr, String donationPayoutStr, String myPayoutStr) {
				//get balance of account
				 HttpClient httpClient = HttpClients.createDefault();

        			// Define the URL you want to send the GET request to
        			String url = "https://secret.nanoriver.io/rafflepayout/" + winnerAccount + "/" + winnerPayoutStr + "/" + donationPayoutStr + "/" + myPayoutStr;

        			//String url = "http://127.0.0.1:3333/rafflepayout/" + winnerAccount + "/" + winnerPayoutStr + "/" + donationPayoutStr + "/" + myPayoutStr;

        			// Create an HttpGet object with the URL
        			HttpGet httpGet = new HttpGet(url);

        			try {
            				// Execute the request and get the response
            				HttpResponse response = httpClient.execute(httpGet);

            				// Check if the response code is successful (200 OK)
            				if (response.getStatusLine().getStatusCode() == 200) {
                				// Parse the response content as a JSON string
                				String jsonResponse = EntityUtils.toString(response.getEntity());

                				// Use Gson to parse the JSON string into a JsonElement
                				JsonElement jsonElement = JsonParser.parseString(jsonResponse);
                				// Now you can work with the JsonElement as needed. convert to jsonObject with .getAsJsonObjet()
                				System.out.println("JsonElement: " + jsonElement.toString());

					}

				}	
				catch (Exception e) {
					e.printStackTrace();

				}
	}

	private static void addWinner(String winnerAccount, BigDecimal payout) throws FileNotFoundException, IOException {

		//first check if empty
               	String winnersPathStr = winnersPath.toString();
		JsonArray winnersArray = new JsonArray();
		JsonObject winnerObj = new JsonObject();
		payout = payout.movePointLeft(30);
		payout = payout.stripTrailingZeros();
		winnerObj.addProperty("winner", winnerAccount);
		winnerObj.addProperty("payout", payout.toString());


		//date
		LocalDateTime currentDateTimeUTC = LocalDateTime.now(ZoneOffset.UTC);
     //   	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'UTC'", Locale.ENGLISH);
       		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
        	String time = currentDateTimeUTC.format(formatter);

		winnerObj.addProperty("time", time);

		FileReader reader = new FileReader(winnersPathStr);

	
                if(Files.size(winnersPath) == 0) {
			//add entry directly
			winnersArray.add(winnerObj);

		}
		else {
			//get winners array
			winnersArray = JsonParser.parseReader(reader).getAsJsonArray();
			winnersArray.add(winnerObj);
		}
		try(PrintWriter writer = new PrintWriter(new File(winnersPathStr))) {
              		writer.write(winnersArray.toString());
                } catch (FileNotFoundException e) {
                	e.printStackTrace();
                }


	}

	public static String getBalance() {
				// Define the URL you want to send the GET request to

        			// Create an HttpGet object with the URL
				String zero = "0";
        			try {

        				String url = "https://secret.nanoriver.io/getpotbalance";

        				//String url = "http://127.0.0.1:3333/getpotbalance";

        				HttpGet httpGet = new HttpGet(url);

				 	HttpClient httpClient = HttpClients.createDefault();
            				// Execute the request and get the response
            				HttpResponse response = httpClient.execute(httpGet);

            				// Check if the response code is successful (200 OK)
            				if (response.getStatusLine().getStatusCode() == 200) {
                				// Parse the response content as a JSON string
                				String jsonResponse = EntityUtils.toString(response.getEntity());

                				// Use Gson to parse the JSON string into a JsonElement
                				JsonElement jsonElement = JsonParser.parseString(jsonResponse);
						JsonObject potObj = jsonElement.getAsJsonObject();
						String balance = potObj.get("balance").getAsString();
						return balance;
					}
					else {

                				System.out.println("Failed to retrieve data. Response code: " + response.getStatusLine().getStatusCode());
						return null;
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			return "0";


	}

}
