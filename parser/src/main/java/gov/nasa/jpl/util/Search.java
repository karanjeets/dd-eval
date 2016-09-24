package gov.nasa.jpl.util;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A utility class to query external search engines
 */
public class Search {

	private static final String FFX_HOST = "localhost";
	private static final Integer FFX_TIMEOUT_SECS = 180;
	private static final Integer PAGE_TIMEOUT_SECS = 10;
	private static final String JSOUP_USER_AGENT = "mozilla";
	
	public static final String GOOGLE_RELATED = "related:";
	
	private static FirefoxProfile ffxProfile;
	private static FirefoxBinary ffxBinary;
	private static DesiredCapabilities capabilities;
	
	private static Integer COUNTER = 0;
	
	
	static {
		ProfilesIni allProfiles = new ProfilesIni();
		ffxProfile = allProfiles.getProfile("default");
		ffxProfile.setPreference(FirefoxProfile.ALLOWED_HOSTS_PREFERENCE, FFX_HOST);
		ffxBinary = new FirefoxBinary();
		ffxBinary.setTimeout(TimeUnit.SECONDS.toMillis(FFX_TIMEOUT_SECS));
		
		capabilities = new DesiredCapabilities();
		capabilities.setJavascriptEnabled(true);
		capabilities.setCapability("takeScreenshot", false);
		capabilities.setCapability("phantomjs.page.settings.userAgent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1");
	}
	
	
	/**
	 * Search google for a specific term and get the topN urls back: It also filters any urls that belong to google
	 * @param query
	 * @return
	 * @throws InterruptedException
	 */
	public static LinkedHashSet<String> google(String query, Integer topN) throws InterruptedException{
		
		System.out.println("Googling Query: " + query);
		
		LinkedHashSet<String> urls = new LinkedHashSet<String>();;
		WebDriver driver = new FirefoxDriver(ffxBinary, ffxProfile);
		//WebDriver driver = new PhantomJSDriver(capabilities);
		driver.manage().timeouts().pageLoadTimeout(PAGE_TIMEOUT_SECS, TimeUnit.SECONDS);
		boolean badRequest = false;
		
		try {
			driver.get("http://www.google.com");
			Thread.sleep(3000);
			driver.findElement(By.id("lst-ib")).sendKeys(query);
			driver.findElement(By.id("lst-ib")).sendKeys(Keys.RETURN);
			WebDriverWait wait = new WebDriverWait(driver, 30);
		    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("rso")));
			Thread.sleep(10000);
		}
		catch(Exception e) {
			if(e instanceof TimeoutException) {
				System.out.println("Timeout Exception Raised. Processing whatever loaded so far...");
			}
			else {
				e.printStackTrace();
				badRequest = true;
			}
		}
		finally {
			try {
				if(!badRequest) {
					int start = 0;
					int pages = 1;
					String googleUrl = driver.getCurrentUrl();
					
					//driver.navigate().to(googleUrl + "&start=" + start + "&num=100");
					//Thread.sleep(3000);
					
					int googleLinks = driver.findElements(By.cssSelector("[valign='top'] > td")).size();
					if(googleLinks != 0)
						pages = driver.findElements(By.cssSelector("[valign='top'] > td")).size() - 2;
					int page = 1;
					System.out.println(pages);
					while(urls.size() < topN && page <= pages) {
						WebElement linkDiv = driver.findElement(By.id("rso"));
						List<WebElement> anchorTags = linkDiv.findElements(By.tagName("a"));
						for( WebElement anchorTag: anchorTags){
							// If contains the word google then dont take it
							if (anchorTag.getAttribute("href") != null
									&& !anchorTag.getAttribute("href").contains("books.google.com")
									&& !anchorTag.getAttribute("href").contains("www.google.com")
									&& !anchorTag.getAttribute("href").endsWith(".pdf")
									&& !anchorTag.getAttribute("href").contains("webcache.googleusercontent.com")) {
								urls.add(anchorTag.getAttribute("href"));
								if(urls.size() == topN)
									break;
							}
						}
						
						// Go to Next page if limit not reached
						if(urls.size() < topN && page < pages) {
							start += 10;
							//System.out.println(googleUrl + "&start=" + start);
							driver.navigate().to(googleUrl + "&start=" + start);
							Thread.sleep(3000);
						}
						else
							break;
						page++;
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				if(driver != null) {
					driver.close();
					driver.quit();
				}
			}
		}
		
		System.out.println("Search Completed");
		
		return urls; 
	} 
	
	/**
	 * Search google for a specific term and get the result urls back: It also filters any urls that belong to google
	 * @param query
	 * @return
	 * @throws InterruptedException
	 */
	public static LinkedHashSet<String> google(String query) throws InterruptedException {
		return google(query, 10);
	}
	
	/**
	 * Get the page text using jsoup. If it fails then use selenium-firefox to fetch the rendered page
	 * @param url
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws BoilerpipeProcessingException 
	 */
	public static String getPageText(String url) throws InterruptedException, IOException, BoilerpipeProcessingException {
		System.out.println("Fetching: " + url);
		
		Response response;
		Document htmlContent;
		String text = "";
		boolean badRequest = false;
		
		// Change to false if boilerPipe is not required
		boolean boilerPipe = false;
		
		try {
			// Fetch page using JSOUP
			response = Jsoup.connect(url).userAgent(JSOUP_USER_AGENT).followRedirects(true).execute();
			htmlContent = response.parse();
			if(boilerPipe)
				text = ArticleExtractor.INSTANCE.getText(htmlContent.html());
			else
				text = htmlContent.getElementsByTag("body").text();
			//System.out.println(text);
			//Cosine.tokenizeintoBOW(Cosine.STOP_WORDS, text, LanguageProcessing.posTagger);
		}
		catch(Exception e) {
			
			System.out.println("Exception Raised while fetching content using JSOUP. Trying Selenium now...");
			
			// Fetch page using Selenium
			WebDriver driver = new FirefoxDriver(ffxBinary, ffxProfile);
			//WebDriver driver = new PhantomJSDriver(capabilities);
			driver.manage().timeouts().pageLoadTimeout(PAGE_TIMEOUT_SECS, TimeUnit.SECONDS);
			
			try {
				driver.get(url);
			}
			catch(Exception se) {
				if(se instanceof TimeoutException) {
					System.out.println("Timeout Exception Raised. Processing whatever loaded so far...");
				}
				else {
					se.printStackTrace();
					badRequest = true;
				}
			}
			finally {
				try {
					if(!badRequest) {
						if(boilerPipe)
							text = ArticleExtractor.INSTANCE.getText(driver.getPageSource());
						else
							text = driver.findElement(By.tagName("body")).getText();
						
						//Cosine.tokenizeintoBOW(Cosine.STOP_WORDS, text, LanguageProcessing.posTagger);
					}
				}
				catch(Exception se1) {
					se1.printStackTrace();
				}
				finally {
					if(driver != null) {
						try {
							driver.close();
							driver.quit();
						}
						catch(Exception se2) {
							System.out.println("Exception on closing the browser!!");
							se2.printStackTrace();
						}
					}
				}
			}
		}
		return new String(text.getBytes("UTF-8"));
	}
	
	/**
	 * Gives back a map of <Url, Text> for a given list of urls provides
	 * @param urls
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static LinkedHashMap<String,String> getAllPageTexts(LinkedHashSet<String> urls) throws InterruptedException, IOException{
		LinkedHashMap<String,String> text=new LinkedHashMap<String,String>();
		
		for (String url: urls){
			try{ 
				text.put(url, getPageText(url));
			}
			catch(Exception e){
				System.out.println("Problem fetching: " + url);
				e.printStackTrace();
			}
		}
		return text;
	}
	
	
	public static LinkedHashMap<String, String> googleAndFetch(String query, Integer topN) {
		System.out.println("Googling Query: " + query);
		COUNTER++;
		if(COUNTER % 50 == 0) {
			try {
				System.out.println(COUNTER + " Queries Executed. Waiting for 10 minutes...");
				Thread.sleep(600000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		LinkedHashSet<String> urls = new LinkedHashSet<String>();
		LinkedHashMap<String,String> urlcontent = new LinkedHashMap<String,String>();
		WebDriver driver = null;
		boolean badRequest = false;
		try {
			driver = new FirefoxDriver(ffxBinary, ffxProfile);
			//driver = new PhantomJSDriver(capabilities);
			driver.manage().timeouts().pageLoadTimeout(PAGE_TIMEOUT_SECS, TimeUnit.SECONDS);
		
		
			driver.get("http://www.google.com");
			Thread.sleep(3000);
			driver.findElement(By.id("lst-ib")).sendKeys(query);
			driver.findElement(By.id("lst-ib")).sendKeys(Keys.RETURN);
			WebDriverWait wait = new WebDriverWait(driver, 30);
		    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("rso")));
			Thread.sleep(6000);
		}
		catch(Exception e) {
			if(e instanceof TimeoutException) {
				System.out.println("Timeout Exception Raised. Processing whatever loaded so far...");
			}
			else {
				e.printStackTrace();
				badRequest = true;
			}
		}
		finally {
			try {
				if(!badRequest) {
					int start = 0;
					int pages = 1;
					String googleUrl = driver.getCurrentUrl();
					
					//driver.navigate().to(googleUrl + "&start=" + start + "&num=100");
					//Thread.sleep(3000);
					
					int googleLinks = driver.findElements(By.cssSelector("[valign='top'] > td")).size();
					System.out.println("Google Links: " + googleLinks);
					if(googleLinks != 0)
						pages = driver.findElements(By.cssSelector("[valign='top'] > td")).size() - 2;
					int page = 1;
					System.out.println(pages);
					while(urls.size() < topN && page <= pages) {
						WebElement linkDiv = driver.findElement(By.id("rso"));
						List<WebElement> anchorTags = linkDiv.findElements(By.tagName("a"));
						for( WebElement anchorTag: anchorTags){
							// If contains the word google then dont take it
							/*if (anchorTag.getAttribute("href") != null
									&& !anchorTag.getAttribute("href").contains("books.google.com")
									&& !anchorTag.getAttribute("href").contains("www.google.com")
									&& !anchorTag.getAttribute("href").endsWith(".pdf")
									&& !anchorTag.getAttribute("href").contains("webcache.googleusercontent.com")) {*/
							
							if (anchorTag.getAttribute("href") != null
									&& !anchorTag.getAttribute("href").contains("google")
									&& !anchorTag.getAttribute("href").endsWith(".pdf")
									&& !anchorTag.getAttribute("href").contains("wikipedia")) {
								String content = getPageText(anchorTag.getAttribute("href"));
								//System.out.println("URL " + anchorTag.getAttribute("href"));
								//System.out.println("Content " + content);
								if(content == null || content.trim().isEmpty())
									continue;
								System.out.println(anchorTag.getAttribute("href"));
								urls.add(anchorTag.getAttribute("href"));
								//System.out.println("URLs Size " + urls.size());
								urlcontent.put(anchorTag.getAttribute("href"), content);
								if(urls.size() == topN)
									break;
							}
						}
						
						// Go to Next page if limit not reached
						if(urls.size() < topN && page < pages) {
							start += 10;
							//System.out.println(googleUrl + "&start=" + start);
							driver.navigate().to(googleUrl + "&start=" + start);
							Thread.sleep(3000);
						}
						else
							break;
						page++;
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				if(driver != null) {
					try {
						driver.close();
						driver.quit();
					}
					catch(Exception e) {
					  e.printStackTrace();
					}
				}
			}
		}
		
		System.out.println("Search Completed");
		
		return urlcontent; 
	}
	
	
	public static LinkedHashMap<String, String> googleAndFetch(String query) {
		return googleAndFetch(query, 10);
	}
	
	public static void main(String[] args) throws InterruptedException, IOException, BoilerpipeProcessingException {
		//LinkedHashMap<String, String> urls = googleAndFetch("Please find and list all the ads in Rochester, NY posted with indicators of multiple individuals being advertised.", 5);
		//System.out.println(urls.size());
		//for(String url: urls.keySet())
		//	System.out.println(url);
		
		
		FileInputStream is = null;
		BufferedReader br = null;
		FileWriter writer = null;
		BufferedWriter bufferedWriter = null;
		String url;
		
		try {
			is = new FileInputStream("/Users/asitangm/Desktop/JPL/files/model-urls.txt");
			br = new BufferedReader(new InputStreamReader(is));
			writer = new FileWriter("/Users/asitangm/Desktop/JPL/files/model-url-content.txt", true);
			bufferedWriter = new BufferedWriter(writer);
			
			while((url = br.readLine()) != null) {
				//Document doc = Jsoup.parse(Jsoup.connect(url).userAgent("Mozilla").get().toString());
				bufferedWriter.append("1\t" + getPageText(url).replaceAll("\\n", " ") + "\n");
				//bufferedWriter.append(doc.text().replaceAll("\\n", " ") + "\n");
				System.out.println("Processed: " + url);
			}
		}
		finally {
			br.close();
			is.close();
			bufferedWriter.close();
			writer.close();
		}
		
		
		//searchGoogle("simple search");
		//System.out.println(getPageText("https://simple.wikipedia.org/wiki/List_of_United_States_cities_by_population"));
	}
	
}
