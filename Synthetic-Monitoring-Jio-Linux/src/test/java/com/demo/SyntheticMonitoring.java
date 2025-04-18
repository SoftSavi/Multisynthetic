package com.demo;

	import com.fasterxml.jackson.core.type.TypeReference;
	import com.fasterxml.jackson.databind.ObjectMapper;
	import io.restassured.RestAssured;
	import io.restassured.response.Response;
    import org.apache.logging.log4j.LogManager;
    import org.openqa.selenium.*;
	import org.openqa.selenium.NoSuchElementException;
	import org.openqa.selenium.chrome.ChromeDriver;
	import org.openqa.selenium.chrome.ChromeOptions;
	import org.openqa.selenium.support.ui.ExpectedConditions;
	import org.openqa.selenium.support.ui.WebDriverWait;
	import org.quartz.*;
	import java.time.Duration;
	import javax.crypto.Cipher;
	import javax.crypto.spec.SecretKeySpec;
	import javax.mail.*;
	import javax.mail.internet.InternetAddress;
	import javax.mail.internet.MimeMessage;
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.IOException;
	import java.util.*;
	import java.util.concurrent.TimeUnit;
	import org.apache.logging.log4j.Logger;
	

public class SyntheticMonitoring {

		public static WebDriver driver;
		private static Logger logger = LogManager.getLogger(SyntheticMonitoring.class);
		private static final String SECRET_KEY = "pm#dds$rxz4jhsdt"; // AES-128 key (16 characters)
		private static String selectedBrowser = "chrome";

		 public static void main(String[] args) throws InterruptedException {
		        try {
		            System.out.println("Starting URL monitoring.....");

		            // Monitor URLs
		            SyntheticMonitoring syntheticMonitoring = new SyntheticMonitoring();
		            syntheticMonitoring.monitorUrls();

		        } catch (Exception e) {
		            e.printStackTrace();
		        }																																																																																																		
		    }
		 
		 public void monitorUrls() throws InterruptedException, IOException {
			    System.out.println("Using browser: " + selectedBrowser);
			    

			    initializeDriver(selectedBrowser);
			    
			    ObjectMapper objectMapper = new ObjectMapper();
			    List<Map<String, Object>> urlDataList;

			    try {
			        urlDataList = objectMapper.readValue(new File("/elkapp/app/savitri/ERPNextjson.json"),
			                new TypeReference<List<Map<String, Object>>>() {
			                });
			    } catch (Exception e) {
			        logger.error("Error reading URL data: " + e.getMessage());
			        return; // Exit if file read fails
			    }

			    // Sequentially process each URL entry
			    for (Map<String, Object> entry : urlDataList) {
			        processEntry(entry);
			    }

			    driver.quit();
			    
			}



		private static void initializeDriver(String browser) {
			if (browser.equalsIgnoreCase("chrome")) {
				System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver"); // Path to ChromeDriver on
																								// Linux
				ChromeOptions options = new ChromeOptions();
				options.addArguments("--headless"); //Run Chrome in headless mode on Linux if no GUI is available
				options.addArguments("--no-sandbox"); //Required for Linux
				options.addArguments("--disable-dev-shm-usage"); // Overcomes limited resource problems on Linux
				options.addArguments("--disable-gpu"); // Applicable for Linux environments
				options.addArguments("--disable-notifications"); // Disables notifications
				options.addArguments("--disable-popup-blocking"); // Disables popup blocking, if needed
				options.addArguments("--start-maximized"); // Starts the browser maximized
				driver = new ChromeDriver(options);
			}

		}

		private static void processEntry(Map<String, Object> entry) throws InterruptedException {
		    logger.info("Starting processing for URL: " + entry.get("url")); // Start log for processing

		    String baseUrl = (String) entry.get("url");
		    String applicationName = (String) entry.get("applicationName");

		    Number expectedResponseTimeNumber = (Number) entry.get("expectedResponseTime");
		    double expectedResponseTime = expectedResponseTimeNumber != null ? expectedResponseTimeNumber.doubleValue() : 0.0;

		    int expectedResponseCode = (int) entry.get("expectedResponseCode");
		    @SuppressWarnings("unchecked")
		    List<String> expectedResponse = (List<String>) entry.get("expectedResponse");

		    RestAssured.baseURI = baseUrl;

		    StringBuilder emailBody = new StringBuilder();
		    boolean alertTriggered = false;

		    try {
		        // Set a maximum time for the page to load (e.g., 30 seconds)
		        driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
		        driver.get(RestAssured.baseURI);  // Navigate to the URL
		        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		        driver.manage().window().maximize();

		        logger.info("Navigating to URL: " + baseUrl);

		        boolean requiresCredentials = entry.containsKey("requiresCredentials") && (boolean) entry.get("requiresCredentials");
		        if (requiresCredentials) {
		            loadAndUseCredentials(entry); // Handle credentials if necessary
		        }

		        // Allow page to load completely
		        Thread.sleep(5000);

		        String currentUrl = driver.getCurrentUrl();
		        long responseTime = getResponseTime(currentUrl);
		        int responseCode = getResponseCode(currentUrl);

		        logger.info("URL: " + currentUrl);

		        // Build the email body
		        emailBody.append("=========================\n")
		                .append("Alert Notification\n")
		                .append("=========================\n")
		                .append("Timestamp: ").append(new Date()).append("\n")
		                .append("Application Name: ").append(applicationName).append("\n")
		                .append("Alerts for the URL: ").append(currentUrl).append("\n")
		                .append("Expected Response Code: ").append(expectedResponseCode).append("\n")
		                .append("Actual Response Code: ").append(responseCode).append("\n")
		                .append("Response Time: ").append(responseTime).append(" ms\n")
		                .append("-------------------------------------------------\n")
		                .append("Event Breakdown:\n");

		        // Check response time
		        if (responseTime > expectedResponseTime) {
		            logger.info("Response Time: " + responseTime);
		            logger.info("Response Time Status: " + "Exceeded expected range.");
		            emailBody.append("The response time exceeded the expected time.\n");
		            alertTriggered = true;
		        } else {
		            logger.info("Response Time: " + responseTime);
		            logger.info("Response Time Status: " + "Within expected range.");
		        }

		        // Check response code
		        if (responseCode != expectedResponseCode) {
		            logger.info("Expected Response Code: " + expectedResponseCode);
		            logger.info("Actual Response Code: " + responseCode);
		            logger.warn("Response Code Status: " + "Does not match expected code.");

		            emailBody.append("The response code did not match the expected code.\n" +
		                    "Expected Response Code: " + expectedResponseCode + "\n" +
		                    "Actual Response Code: " + responseCode + "\n");
		            alertTriggered = true;
		        } else {
		            logger.info("Expected Response Code: " + expectedResponseCode);
		            logger.info("Actual Response Code: " + responseCode);
		            logger.warn("Response Code Status: " + "Matches expected code.");
		        }

		        // Check expected content
		        String pageSource = driver.getPageSource();
		        boolean isMatchFound = false;
		        for (String expectedName : expectedResponse) {
		            if (pageSource.contains(expectedName)) {
		                logger.info("Expected Content Status: " + expectedName);
		                isMatchFound = true;
		                break;
		            }
		        }

		        if (!isMatchFound) {
		            logger.warn("Expected Content Status: " + "No names are present in the page's HTML content.");
		            emailBody.append("None of the expected content was found on the page.\n");
		            alertTriggered = true;
		        }

		    } catch (TimeoutException e) {
		        logger.error("Error: " + "Timeout while trying to load URL " + baseUrl + ". Exception " + e.getMessage());

		        // Build the email body in the desired format
		        emailBody.append("=========================\n")
		                .append("Alert Notification\n")
		                .append("=========================\n")
		                .append("Timestamp: ").append(new Date()).append("\n")
		                .append("Application Name: ").append(applicationName).append("\n")
		                .append("Alerts for the URL: ").append(baseUrl).append("\n")
		                .append("Expected Response Code: ").append(expectedResponseCode).append("\n")
		                .append("Actual Response Code: N/A (Timeout)\n")
		                .append("Response Time: N/A (Timeout)\n")
		                .append("-------------------------------------------------\n")
		                .append("Event Breakdown:\n")
		                .append("Error: Timeout occurred while trying to load the URL ").append(baseUrl).append("\n");

		        alertTriggered = true;

		    } catch (WebDriverException e) {
		        logger.error("Error: " + "Error accessing URL " + baseUrl + ". Exception " + e.getMessage());

		        // Build the email body in the desired format
		        emailBody.append("=========================\n")
		                .append("Alert Notification\n")
		                .append("=========================\n")
		                .append("Timestamp: ").append(new Date()).append("\n")
		                .append("Application Name: ").append(applicationName).append("\n")
		                .append("Alerts for the URL: ").append(baseUrl).append("\n")
		                .append("Expected Response Code: ").append(expectedResponseCode).append("\n")
		                .append("Actual Response Code: N/A (Error accessing URL)\n")
		                .append("Response Time: N/A (Error accessing URL)\n")
		                .append("-------------------------------------------------\n")
		                .append("Event Breakdown:\n")
		                .append("Error: Unable to access the URL ").append(baseUrl).append("\n")
		                .append("Exception Message: ").append(e.getMessage()).append("\n");

		        alertTriggered = true;

		    } finally {
		        logger.info("Finished processing for URL: " + entry.get("url")); // End log for processing
		        // Send the email if any alert is triggered, either due to page issues or exceptions
		        if (alertTriggered) {
		            sendEmail("Email Status: " + "Critical Error Notification ", emailBody.toString());
		        } else {
		            logger.info("Email Status: N/A");  // Log when no email is sent
		        }
		    }
		}

		

		private static long getResponseTime(String currentUrl) {
			long startTime = System.currentTimeMillis();
			RestAssured.given().get(currentUrl);
			return System.currentTimeMillis() - startTime;
		}

		private static int getResponseCode(String currentUrl) {
			Response res = RestAssured.given().get(currentUrl);
			return res.getStatusCode();
		}

		private static void loadAndUseCredentials(Map<String, Object> entry) {
			try {
				String loginType = (String) entry.getOrDefault("loginType", "singleStep");
				if (!entry.containsKey("credentials")) {
					logger.error("No credentials provided for a URL that requires login."); 
					return;
				}

				@SuppressWarnings("unchecked")
				Map<String, Object> credentials = (Map<String, Object>) entry.get("credentials");
				String encryptedUsername = (String) credentials.get("encrypted.username");
				String encryptedPassword = (String) credentials.get("encrypted.password");

				String decryptedUsername = decrypt(encryptedUsername, SECRET_KEY);
				String decryptedPassword = decrypt(encryptedPassword, SECRET_KEY);

				switch (loginType.toLowerCase()) {
					case "singlestep":
						inputCredentials(credentials, decryptedUsername, decryptedPassword);
						break;
					case "multistep":
						inputNextButtonCredentials(credentials, decryptedUsername, decryptedPassword);
						break;
					case "threestep": // New case for three-step login
						threeStepLogin(credentials, decryptedUsername, decryptedPassword);
						break;    
					default:
						logger.warn("Unknown login type, defaulting to single-step.");
						inputCredentials(credentials, decryptedUsername, decryptedPassword);
				}
			} catch (Exception e) {
				logger.error("Error loading credentials: " + e.getMessage()); 
			}
		}

		private static void inputCredentials(Map<String, Object> credentialLocators, String username, String password) {
			
			try {
				WebElement usernameField = driver.findElement(getLocator(credentialLocators, "usernameLocator"));
				usernameField.sendKeys(username);


				WebElement passwordField = driver.findElement(getLocator(credentialLocators, "passwordLocator"));
				passwordField.sendKeys(password);


				WebElement loginButton = driver.findElement(getLocator(credentialLocators, "submitLocator"));
				loginButton.click();


			} catch (NoSuchElementException e) {
				logger.error("Element not found during login: " + e.getMessage());
			}
		}

		private static void inputNextButtonCredentials(Map<String, Object> credentialLocators, String username, String password) {
			try {
				WebElement usernameField = driver.findElement(getLocator(credentialLocators, "usernameLocator"));
				usernameField.sendKeys(username);


				WebElement nextButton = driver.findElement(getLocator(credentialLocators, "nextLocator"));
				nextButton.click();


				Thread.sleep(2000); // Wait for the password field

				WebElement passwordField = driver.findElement(getLocator(credentialLocators, "passwordLocator"));
				passwordField.sendKeys(password);


				WebElement loginButton = driver.findElement(getLocator(credentialLocators, "submitLocator"));
				loginButton.click();


			} catch (NoSuchElementException | InterruptedException e) {
				logger.error("Element not found during multi-step login: " + e.getMessage());
			}
		}
		
		private static void threeStepLogin(Map<String, Object> credentialLocators, String username, String password) {
			try {
				// Click the initial "Click here to login" button
				WebElement clickHereButton = driver.findElement(getLocator(credentialLocators, "clickHereLocator"));
				clickHereButton.click();

	 
				// Wait for the username field to become visible
				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // Use Duration for the timeout
				wait.until(ExpectedConditions.visibilityOfElementLocated(getLocator(credentialLocators, "usernameLocator")));
			   
				// Enter the username
				WebElement usernameField = driver.findElement(getLocator(credentialLocators, "usernameLocator"));
				usernameField.sendKeys(username);


				// Enter the password
				WebElement passwordField = driver.findElement(getLocator(credentialLocators, "passwordLocator"));
				passwordField.sendKeys(password);


				// Click the login button
				WebElement loginButton = driver.findElement(getLocator(credentialLocators, "submitLocator"));
				loginButton.click();


			} catch (NoSuchElementException e) {
				logger.error("Element not found during three-step login: " + e.getMessage());
			}
		}

		
		
		@SuppressWarnings("unchecked")
		private static By getLocator(Map<String, Object> credentialLocators, String locatorType) {
			Map<String, String> locatorDetails = (Map<String, String>) credentialLocators.get(locatorType);
			String locatorValue = locatorDetails.get("value");
			String locatorMethod = locatorDetails.get("type");

			switch (locatorMethod) {
				case "xpath":
					return By.xpath(locatorValue);
				case "id":
					return By.id(locatorValue);
				case "css":
					return By.cssSelector(locatorValue);
				default:
					throw new IllegalArgumentException("Invalid locator method: " + locatorMethod);
			}
		}


		private static String decrypt(String encryptedData, String secretKey) {
			try {
				SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.DECRYPT_MODE, keySpec);
				byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
				return new String(decryptedBytes);
			} catch (Exception e) {
				logger.error("Error decrypting data: " + e.getMessage());
				return null;
			}
		}

		private static void sendEmail(String subject, String body) {
		    boolean emailTriggered = false;
		    boolean emailSentSuccessfully = false;

		    try {
		        // Load email credentials from properties file
		        Properties emailProps = new Properties();
		        try (FileInputStream input = new FileInputStream("/elkapp/app/savitri/credentials.properties")) {
		            emailProps.load(input);
		        }

		        // Plain text email ID
		        String username = emailProps.getProperty("email.username");

		        // Encrypted email password
		        String encryptedPassword = emailProps.getProperty("encrypted.email.password");
		        String smtpHost = emailProps.getProperty("smtp.host");
		        // Extract admin emails as a comma-separated list
		        String adminEmails = emailProps.getProperty("admin.email");

		        if (username == null || encryptedPassword == null || smtpHost == null || adminEmails == null) {
		            logger.info("Email Status: N/A | One or more email credentials are missing or null. Please check the properties file.");
		            return;
		        }

		        // Decrypt email password
		        String password = decrypt(encryptedPassword, SECRET_KEY);

		        // Set up email properties
		        Properties props = new Properties();
		        props.put("mail.smtp.auth", "true");
		        props.put("mail.smtp.starttls.enable", "true");
		        props.put("mail.smtp.host", smtpHost);
		        props.put("mail.smtp.port", "587");

		        // Add timeout properties
		        props.put("mail.smtp.connectiontimeout", "5000"); // 5 seconds
		        props.put("mail.smtp.timeout", "5000");           // 5 seconds
		        props.put("mail.smtp.writetimeout", "5000");      // 5 seconds

		        // Create session with the decrypted credentials
		        Session session = Session.getInstance(props, new Authenticator() {
		            protected PasswordAuthentication getPasswordAuthentication() {
		                return new PasswordAuthentication(username, password);
		            }
		        });

		        // Create and send email
		        Message message = new MimeMessage(session);
		        message.setFrom(new InternetAddress(username));

		        // Parse and set multiple recipients
		        InternetAddress[] recipientAddresses = InternetAddress.parse(adminEmails);
		        message.setRecipients(Message.RecipientType.TO, recipientAddresses);

		        message.setSubject(subject);
		        message.setText(body);

		        Transport.send(message);  // Attempt to send email
		        emailTriggered = true;
		        emailSentSuccessfully = true;  // Set to true when email is successfully sent
		    } catch (Exception e) {
		        emailTriggered = true;  // Email was triggered but an error occurred
		        emailSentSuccessfully = false;
		        logger.error("Email Status: Failed | Error: " + e.getMessage());  // Log the error
		    } finally {
		        // Log the status based on the conditions
		        if (!emailTriggered) {
		            logger.info("Email Status: N/A");  // No email attempt was made
		        } else if (emailSentSuccessfully) {
		            logger.info("Email Status: Success");  // Email successfully sent
		        } else {
		            logger.info("Email Status: Failed");  // Email failed to send
		        }
		    }
		}


		
		public static class UrlMonitoringJob implements Job {
			@Override
			public void execute(JobExecutionContext context) throws JobExecutionException {
				SyntheticMonitoring syntheticMonitoring = new SyntheticMonitoring();
				try {
					syntheticMonitoring.monitorUrls();
				} catch (InterruptedException | IOException e) {
					logger.error("Job execution failed: " + e.getMessage());
				}
			}
		}
	}
