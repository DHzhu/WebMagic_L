/**  
 * @Title: SzseSeleniumDownloader.java
 * @Package us.codecraft.webmagic.downloader.selenium
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-15
 */
package us.codecraft.webmagic.downloader.selenium;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

/**
 * ClassName: SzseSeleniumDownloader 
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-15
 */
public class PicSeleniumDownloader implements Downloader, Closeable {

	private volatile WebDriverPool webDriverPool;

	private Log logger = LogFactory.getLog(getClass());

	private int sleepTime = 0;

	private int poolSize = 1;
	
	private int pageSize = 10;
	

	@SuppressWarnings("unused")
	private static final String DRIVER_PHANTOMJS = "phantomjs";
	
	private String content;

	
	public PicSeleniumDownloader setPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	/**
	 * Constructor without any filed. Construct PhantomJS browser
	 * 
	 * @author bob.li.0718@gmail.com
	 */
	public PicSeleniumDownloader() {
		
	}

	/**
	 * set sleep time to wait until load success
	 *
	 * @param sleepTime sleepTime
	 * @return this
	 */
	public PicSeleniumDownloader setSleepTime(int sleepTime) {
		this.sleepTime = sleepTime;
		return this;
	}

	@Override
	public Page download(Request request, Task task) {
		checkInit();
		WebDriver webDriver;
		try {
			webDriver = webDriverPool.get();
		} catch (InterruptedException e) {
			logger.warn("interrupted", e);
			return null;
		}
		
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("----------------------------------------------------------------");
		logger.info("start processing page:" + request.getUrl());
		
		Page page = new Page();
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);
		
		String url = request.getUrl();
		if(url.matches("(?i).*?\\/([^\\/]*?)\\.(jpg|png|jpeg)$")){
        	page.setRawText("");
        	webDriverPool.returnToPool(webDriver);
        	return page;
        }
		
		//System.out.println(request.getUrl());
		webDriver.get(request.getUrl());
		WebDriver.Options manage = webDriver.manage();
		Site site = task.getSite();
		if (site.getCookies() != null) {
			for (Map.Entry<String, String> cookieEntry : site.getCookies()
					.entrySet()) {
				Cookie cookie = new Cookie(cookieEntry.getKey(),
						cookieEntry.getValue());
				manage.addCookie(cookie);
			}
		}
		
		String urlPage = "1";
		if(url.matches("(?i).*?page\\=(\\d+)$")){
			urlPage = url.replaceAll("(?i).*?page\\=(\\d+)", "$1");
		}
		int pageCount = Integer.valueOf(urlPage);
		
		//取内容页中正文部分
		if(pageCount <= pageSize){
			try{
				java.util.List<WebElement> lists = webDriver.findElements(By.xpath("//div[@class='zm-invite-pager']//span/a"));
				WebElement btnElement = lists.get(lists.size() - 1);
				String clickStr = btnElement.getText();
				if(btnElement != null && clickStr.endsWith("下一页")){
					pageCount ++;
					request.putExtra("NextPage", "?page=" + pageCount);
				}
				java.util.List<WebElement> listC = webDriver.findElements(By.xpath("//div[@class='zm-item-fav']//textarea[@class='content']"));
				for(WebElement element : listC){
					content += element.getAttribute("value");
				}
			}catch(Exception e){
				
			}
		}
		
		page.setRawText(content);
		webDriverPool.returnToPool(webDriver);
		return page;
	}

	private void checkInit() {
		if (webDriverPool == null) {
			synchronized (this) {
				webDriverPool = new WebDriverPool(poolSize);
			}
		}
	}

	@Override
	public void setThread(int thread) {
		this.poolSize = thread;
	}

	@Override
	public void close() throws IOException {
		webDriverPool.closeAll();
	}
}