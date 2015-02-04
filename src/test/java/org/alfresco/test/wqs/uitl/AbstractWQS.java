/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 * This file is part of Alfresco
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.test.wqs.uitl;


import org.alfresco.po.share.AlfrescoVersion;
import org.alfresco.po.share.SharePage;
import org.alfresco.po.share.ShareUtil;
import org.alfresco.po.share.util.ShareTestProperty;
import org.alfresco.test.AlfrescoTests;
import org.alfresco.webdrone.HtmlPage;
import org.alfresco.webdrone.WebDrone;
import org.alfresco.webdrone.WebDroneImpl;
import org.alfresco.webdrone.exception.PageException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.ITestResult;
import org.testng.annotations.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class includes: Abstract class holds all common methods for WQS tests
 *
 * @author Oana Caciuc
 */
public abstract class AbstractWQS implements AlfrescoTests
{
    private static Log logger = LogFactory.getLog(AbstractWQS.class);
    protected static ApplicationContext ctx;
    protected static String shareUrl;
    protected static String wcmqs;

    protected static String DEFAULT_FREENET_USER;
    protected static String DEFAULT_PREMIUMNET_USER;
    protected static final String DEFAULT_PASSWORD = "password";

    // Test Related Folders
    public static final String SLASH = File.separator;
    private static final String SRC_ROOT = System.getProperty("user.dir") + SLASH;
    protected static final String DATA_FOLDER = SRC_ROOT + "testdata" + SLASH;
    private static String RESULTS_FOLDER;
    protected String testName;
    protected WebDrone drone;
    public static long maxWaitTime = 30000;
    public static long refreshDuration = 25000;

    // Constants
    protected static final String SITE_VISIBILITY_PUBLIC = "public";

    protected static final String SITE_WEB_QUICK_START_DASHLET = "site-wqs";

    // Test Run and Users Info: This is now a part of test.properties
    protected static String username;
    protected static String password;
    protected static String DOMAIN_FREE;
    protected static String DOMAIN_PREMIUM;
    protected static String DOMAIN_HYBRID;

    protected static String ADMIN_USERNAME;
    protected static String ADMIN_PASSWORD;
    protected static String DEFAULT_USER;
    protected static String UNIQUE_TESTDATA_STRING;

    protected static AlfrescoVersion alfrescoVersion;
    protected static Map<WebDrone, ShareTestProperty> dronePropertiesMap = new HashMap<WebDrone, ShareTestProperty>();
    protected static ShareTestProperty testProperties;
    private static WqsTestProperty wqsTestProperties;

    Map<String, WebDrone> droneMap = new HashMap<String, WebDrone>();

    @BeforeSuite(alwaysRun = true)
    @Parameters({"contextFileName"})
    public static void setupContext(@Optional("wqs-context.xml") String contextFileName)
    {
        List<String> contextXMLList = new ArrayList<String>();
        contextXMLList.add(contextFileName);
        ctx = new ClassPathXmlApplicationContext(contextXMLList.toArray(new String[contextXMLList.size()]));
        testProperties = (ShareTestProperty) ctx.getBean("shareTestProperties");
        wqsTestProperties = (WqsTestProperty) ctx.getBean("wqsProperties");
        wcmqs = wqsTestProperties.getWcmqs();
        shareUrl = testProperties.getShareUrl();
        username = testProperties.getUsername();
        password = testProperties.getPassword();
        alfrescoVersion = testProperties.getAlfrescoVersion();
        DOMAIN_FREE = wqsTestProperties.getDomainFree();
        DOMAIN_PREMIUM = wqsTestProperties.getDomainPremium();
        DOMAIN_HYBRID = wqsTestProperties.getDomainHybrid();
        DEFAULT_USER = wqsTestProperties.getDefaultUser();
        UNIQUE_TESTDATA_STRING = wqsTestProperties.getUniqueTestDataString();
        ADMIN_USERNAME = wqsTestProperties.getAdminUsername();
        ADMIN_PASSWORD = wqsTestProperties.getAdminPassword();


        DEFAULT_FREENET_USER = DEFAULT_USER + "@" + DOMAIN_FREE;
        DEFAULT_PREMIUMNET_USER = DEFAULT_USER + "@" + DOMAIN_PREMIUM;

        logger.info("Target URL: " + shareUrl);
        logger.info("Alfresco Version: " + alfrescoVersion);
    }

    public void setup() throws Exception
    {
        drone = (WebDrone) ctx.getBean("webDrone");
        droneMap.put("std_drone", drone);
        dronePropertiesMap.put(drone, testProperties);
        maxWaitTime = ((WebDroneImpl) drone).getMaxPageRenderWaitTime();
    }


    @AfterClass(alwaysRun = true)
    public void tearDown()
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("shutting web drone");
        }
        // Close the browser
        for (Map.Entry<String, WebDrone> entry : droneMap.entrySet())
        {
            try
            {
                if (entry.getValue() != null)
                {
                    try
                    {
                        ShareUtil.logout(entry.getValue());
                    } catch (Exception e)
                    {
                        logger.error("If it's tests associated with admin-console-summary-page. it's normal. If not - we have a problem.");
                    }
                    entry.getValue().quit();
                    logger.info(entry.getKey() + " closed");
                    logger.info("[Suite ] : End of Tests in: " + this.getClass().getSimpleName());
                }
            } catch (Exception e)
            {
                logger.error("Failed to close previous instance of brower:" + entry.getKey(), e);
            }
        }
    }

    public void savePageSource(String methodName) throws IOException
    {
        for (Map.Entry<String, WebDrone> entry : droneMap.entrySet())
        {
            if (entry.getValue() != null)
            {
                String htmlSource = ((WebDroneImpl) entry.getValue()).getDriver().getPageSource();
                File file = new File(RESULTS_FOLDER + methodName + "_" + entry.getKey() + "_Source.html");
                FileUtils.writeStringToFile(file, htmlSource);
            }
        }
    }

    /**
     * Helper to Take a ScreenShot. Saves a screenshot in target folder
     * <RESULTS_FOLDER>
     *
     * @param methodName String This is the Test Name / ID
     * @throws IOException if error
     */
    public void saveScreenShot(String methodName) throws IOException
    {
        for (Map.Entry<String, WebDrone> entry : droneMap.entrySet())
        {
            if (entry.getValue() != null)
            {
                File file = entry.getValue().getScreenShot();
                File tmp = new File(RESULTS_FOLDER + methodName + "_" + entry.getKey() + ".png");
                FileUtils.copyFile(file, tmp);
            }
        }
        // try
        // {
        // saveOsScreenShot(methodName);
        // }
        // catch (AWTException e)
        // {
        // logger.error("Not able to take the OS screen shot: " + e.getMessage());
        // }
    }


    @BeforeMethod
    protected String getMethodName(Method method)
    {
        String methodName = method.getName();
        logger.info("[Test: " + methodName + " ]: START");
        return methodName;
    }

    /**
     * Helper returns the test / methodname. This needs to be called as the 1st
     * step of the test. Common Test code can later be introduced here.
     *
     * @return String testcaseName
     */
    public static String getTestName()
    {
        String testID = Thread.currentThread().getStackTrace()[2].getMethodName();
        return getTestName(testID);
    }

    /**
     * Helper returns the test / methodname. This needs to be called as the 1st
     * step of the test. Common Test code can later be introduced here.
     *
     * @return String testcaseName
     */
    public static String getTestName(String testID)
    {
        return (testID.substring(testID.lastIndexOf("_")).replace("_", alfrescoVersion + "-"));
    }

    @AfterMethod
    public void logTestResult(ITestResult result)

    {
        logger.info("[Test: " + result.getMethod().getMethodName() + " ]: " + result.toString().toUpperCase());
    }

    /**
     * Helper to consistently get the userName in the specified domain, in the
     * desired format.
     *
     * @param testID String Name of the test for uniquely identifying / mapping
     *               test data with the test
     * @return String userName
     */
    protected static String getUserNameForDomain(String testID, String domainName)
    {
        String userName;
        if (domainName.isEmpty())
        {
            domainName = DOMAIN_FREE;
        }
        // ALF: Workaround needs toLowerCase to be added. to be removed when
        // jira is fixed
        userName = String.format("user%s@%s", testID, domainName).toLowerCase();

        return userName;
    }


    /**
     * Helper to consistently get the Site Name.
     *
     * @param testID String Name of the test for uniquely identifying / mapping
     *               test data with the test
     * @return String sitename
     */
    public static String getSiteName(String testID)
    {
        String siteName;

        siteName = String.format("Site%s%s", UNIQUE_TESTDATA_STRING, testID);

        return siteName;
    }


    /**
     * Helper to consistently get the filename.
     *
     * @param partFileName String Part Name of the file for uniquely identifying /
     *                     mapping test data with the test
     * @return String fileName
     */
    protected static String getFileName(String partFileName)
    {
        String fileName;

        fileName = String.format("File%s-%s", UNIQUE_TESTDATA_STRING, partFileName);

        return fileName;
    }

    /**
     * Checks if driver is null, throws UnsupportedOperationException if so.
     *
     * @param driver WebDrone Instance
     * @throws UnsupportedOperationException if driver is null
     */
    protected static void checkIfDriverNull(WebDrone driver)
    {
        if (driver == null)
        {
            throw new UnsupportedOperationException("WebDrone is required");
        }
    }

    /**
     * Common method to wait for the next solr indexing cycle.
     *
     * @param driver      WebDrone Instance
     * @param waitMiliSec Wait duration in milliseconds
     */
    @SuppressWarnings("deprecation")
    protected static HtmlPage webDriverWait(WebDrone driver, long waitMiliSec)
    {
        if (waitMiliSec <= 0)
        {
            waitMiliSec = maxWaitTime;
        }
        logger.info("Waiting For: " + waitMiliSec / 1000 + " seconds");
        /*
         * try { Thread.sleep(waitMiliSec); //driver.refresh(); }
         * catch(InterruptedException ie) { throw new
         * RuntimeException("Wait interrupted / timed out"); }
         */
        driver.waitFor(waitMiliSec);
        return getSharePage(driver);
    }

    /**
     * Return the {@link WebDrone} Configured starting of test.
     *
     * @return {@link WebDrone}
     */
    public WebDrone getDrone()
    {
        return drone;
    }


    public String getShareUrl()
    {
        return testProperties.getShareUrl();
    }

    /**
     * Checks if the current page is share page, throws PageException if not.
     *
     * @param driver WebDrone Instance
     * @return SharePage
     * @throws PageException if the current page is not a share page
     */
    public static SharePage getSharePage(WebDrone driver)
    {
        checkIfDriverNull(driver);
        try
        {
            HtmlPage generalPage = driver.getCurrentPage().render(refreshDuration);
            return (SharePage) generalPage;
        } catch (PageException pe)
        {
            throw new PageException("Can not cast to SharePage: Current URL: " + driver.getCurrentUrl());
        }
    }

}