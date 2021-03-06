package org.alfresco.test.wqs.share;

import org.alfresco.po.share.ShareUtil;
import org.alfresco.po.share.dashlet.SiteWebQuickStartDashlet;
import org.alfresco.po.share.dashlet.WebQuickStartOptions;
import org.alfresco.po.share.enums.Dashlets;
import org.alfresco.po.share.site.CustomiseSiteDashboardPage;
import org.alfresco.po.share.site.CustomizeSitePage;
import org.alfresco.po.share.site.SiteDashboardPage;
import org.alfresco.po.share.site.SitePageType;
import org.alfresco.po.share.site.datalist.DataListPage;
import org.alfresco.po.share.site.datalist.items.VisitorFeedbackRow;
import org.alfresco.po.share.site.datalist.lists.VisitorFeedbackList;
import org.alfresco.po.share.site.document.DocumentLibraryPage;
import org.alfresco.po.share.util.SiteUtil;
import org.alfresco.po.share.wqs.*;
import org.alfresco.test.FailedTestListener;
import org.alfresco.test.util.SiteService;
import org.alfresco.test.wqs.uitl.AbstractWQS;
import org.apache.log4j.Logger;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.springframework.social.alfresco.api.entities.Site;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.List;

@Listeners(FailedTestListener.class)
public class WqsDLIntegrationTests extends AbstractWQS
{
    private static final Logger logger = Logger.getLogger(WqsDLIntegrationTests.class);
    String newsName;
    String wcmqsURL;
    String siteName;

    public static final String INDEX_HTML = "slide1.html";

    @Override
    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception
    {
        super.setup();
        testName = this.getClass().getSimpleName();

        siteName = getSiteName(testName) + System.currentTimeMillis();
        logger.info("Start tests:" + testName);

        wcmqsURL = wcmqs;
        logger.info(" wcmqs url : " + wcmqsURL);
    }

    @BeforeMethod(alwaysRun = true, groups = {"WQS"})
    public void testSetup() throws Exception
    {
        super.setup();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown()
    {
        try
        {
            drone.quit();
        } catch (UnreachableBrowserException e)
        {
            logger.error("Browser communication failed.");
        }
    }

    @Test(groups = {"DataPrepWQS", "EnterpriseOnly"})
    public void dataPrep() throws Exception
    {
        // Login

        ShareUtil.loginAs(drone, shareUrl, ADMIN_PASSWORD, ADMIN_PASSWORD);

        // Create Site
        SiteService siteService = (SiteService) ctx.getBean("siteService");
        siteService.create(ADMIN_USERNAME, ADMIN_PASSWORD, DOMAIN_FREE, siteName, "", Site.Visibility.PUBLIC);

        SiteDashboardPage siteDashboardPage = (SiteDashboardPage) SiteUtil.openSiteDashboard(drone, siteName);

        CustomiseSiteDashboardPage customiseSiteDashboardPage = siteDashboardPage.getSiteNav().selectCustomizeDashboard().render();
        siteDashboardPage = customiseSiteDashboardPage.addDashlet(Dashlets.WEB_QUICK_START, 1).render();

        SiteWebQuickStartDashlet wqsDashlet = siteDashboardPage.getDashlet(SITE_WEB_QUICK_START_DASHLET).render();
        wqsDashlet.selectWebsiteDataOption(WebQuickStartOptions.FINANCE);
        wqsDashlet.clickImportButtton();
        wqsDashlet.waitForImportMessage();

        // Data Lists component is added to the site
        CustomizeSitePage customizeSitePage = siteDashboardPage.getSiteNav().selectCustomizeSite().render();
        List<SitePageType> addPageTypes = new ArrayList<SitePageType>();
        addPageTypes.add(SitePageType.DATA_LISTS);
        customizeSitePage.addPages(addPageTypes).render();

        // Site Dashboard is rendered with Data List link
        SiteUtil.openSiteDashboard(drone, siteName).render();

        ShareUtil.logout(drone);
    }

    /**
     * AONE-5593:Verify correct displaying of comments in Share Data lists
     */
    @Test(groups = {"WQSShare", "EnterpriseOnly"})
    public void AONE_5593() throws Exception
    {
        String visitorName = "name " + getTestName();
        String visitorEmail = getTestName() + "@" + DOMAIN_FREE;
        String visitorWebsite = "website " + getTestName();
        String visitorComment = "Comment by " + visitorName;

        // --- Prec Step 4 ---
        // --- Step action ---
        // Any blog post is opened;
        drone.navigateTo(wcmqsURL);
        WcmqsHomePage wcmqsHomePage = new WcmqsHomePage(drone);
        wcmqsHomePage.render();
        wcmqsHomePage.selectMenu("blog");

        WcmqsBlogPage blogsPage = new WcmqsBlogPage(drone);
        blogsPage.render();
        String blogName = "blog3.html";
        blogsPage.clickBlogNameFromShare(blogName);

        WcmqsLoginPage wcmqsLoginPage = new WcmqsLoginPage(drone);
        wcmqsLoginPage.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        // --- Step 1 ---
        // --- Step action ---
        // Create any comment for post with valid data;
        // --- Expected results ---
        // Post is created;
        WcmqsBlogPostPage blogPostPage = new WcmqsBlogPostPage(drone);
        blogPostPage.render();
        blogPostPage.setVisitorName(visitorName);
        blogPostPage.setVisitorEmail(visitorEmail);
        blogPostPage.setVisitorWebsite(visitorWebsite);
        blogPostPage.setVisitorComment(visitorComment);
        blogPostPage.clickPostButton();
        Assert.assertTrue(blogPostPage.isAddCommentMessageDisplay(), "Comment was not posted.");

        // --- Step 2 ---
        // --- Step action ---
        // Login in Alfresco Share as admin;
        // --- Expected results ---
        // Admin is logged in Alfresco Share;
        ShareUtil.loginAs(drone, shareUrl, ADMIN_USERNAME, ADMIN_PASSWORD);

        // --- Step 3 ---
        // --- Step action ---
        // Open WCMQS site's dashboard;
        // --- Expected results ---
        // Site's dashboard is opened;
        DocumentLibraryPage docLibPage = SiteUtil.openSiteFromSearch(drone, siteName).getSiteNav().selectSiteDocumentLibrary().render();

        // --- Step 4 ---
        // --- Step action ---
        // Click "Data lists" link;
        // --- Expected results ---
        // Data lists page is opened;
        DataListPage dataListPage = docLibPage.getSiteNav().selectDataListPage().render();

        // --- Step 5 ---
        // --- Step action ---
        // Click "Visitors Feedback" link;
        // --- Expected results ---
        // Visitors feddback data list is opened;
        dataListPage.selectDataList("Visitor Feedback (Quick Start Editorial)");

        // --- Step 6 ---
        // --- Step action ---
        // Verify the prensense of recently created comment;
        // --- Expected results ---
        // Recently created comment for blog is present in data list;
        VisitorFeedbackList feedbackList = new VisitorFeedbackList(drone);
        feedbackList.render();
        VisitorFeedbackRow newFeedback = feedbackList.getRowForSpecificValues(visitorEmail, visitorComment, visitorName, visitorWebsite);
        Assert.assertEquals(newFeedback.getVisitorComment(), visitorComment, "Recently created comment for blog is not present in data list");
        Assert.assertEquals(newFeedback.getRelevantAsset(), blogName, "Blog file name is not present in data list");

        ShareUtil.logout(drone);
    }

    /**
     * AONE-5594:Verify correct work of report post function
     */
    @Test(groups = {"WQSShare", "EnterpriseOnly"})
    public void AONE_5594() throws Exception
    {
        String visitorName = "name " + getTestName();
        String visitorEmail = getTestName() + "@" + DOMAIN_FREE;
        String visitorWebsite = "website " + getTestName();
        String visitorComment = "Comment by " + visitorName;

        // --- Prec Step 4 ---
        // --- Step action ---
        // Any blog post is opened;
        drone.navigateTo(wcmqsURL);
        WcmqsHomePage wcmqsHomePage = new WcmqsHomePage(drone);
        wcmqsHomePage.render();
        wcmqsHomePage.selectMenu("blog");

        WcmqsBlogPage blogsPage = new WcmqsBlogPage(drone);
        blogsPage.render();
        String blogName = "blog2.html";
        blogsPage.clickBlogNameFromShare(blogName);

        WcmqsLoginPage wcmqsLoginPage = new WcmqsLoginPage(drone);
        wcmqsLoginPage.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        // --- Step 1 ---
        // --- Step action ---
        // Create any comment for blog post;
        // --- Expected results ---
        // Comment is successfully created;
        WcmqsBlogPostPage blogPostPage = new WcmqsBlogPostPage(drone);
        blogPostPage.render();
        blogPostPage.setVisitorName(visitorName);
        blogPostPage.setVisitorEmail(visitorEmail);
        blogPostPage.setVisitorWebsite(visitorWebsite);
        blogPostPage.setVisitorComment(visitorComment);
        blogPostPage.clickPostButton();
        Assert.assertTrue(blogPostPage.isAddCommentMessageDisplay(), "Comment was not posted.");

        // --- Step 2 ---
        // --- Step action ---
        // Login in Alfresco Share as admin;
        // --- Expected results ---
        // Admin is logged in Alfresco Share;
        ShareUtil.loginAs(drone, shareUrl, ADMIN_USERNAME, ADMIN_PASSWORD);

        // --- Step 3 ---
        // --- Step action ---
        // Navigate to WCMQS site's Data list;
        // --- Expected results ---
        // Data list page is opened;

        DocumentLibraryPage docLibPage = SiteUtil.openSiteFromSearch(drone, siteName).getSiteNav().selectSiteDocumentLibrary().render();
        DataListPage dataListPage = docLibPage.getSiteNav().selectDataListPage().render();

        // --- Step 4 ---
        // --- Step action ---
        // Click Visitors feedback link;
        // --- Expected results ---
        // Visitors feedback data list is opened;
        dataListPage.selectDataList("Visitor Feedback (Quick Start Editorial)");

        // --- Step 5 ---
        // --- Step action ---
        // Verify "Comment has been flagged" field for recently created comment;
        // --- Expected results ---
        // "Comment has been flagged" contains "false" value;
        VisitorFeedbackList feedbackList = new VisitorFeedbackList(drone);
        feedbackList.render();
        VisitorFeedbackRow newFeedback = feedbackList.getRowForSpecificValues(visitorEmail, visitorComment, visitorName, visitorWebsite);
        Assert.assertEquals(newFeedback.getCommnetFlag(), "false", "Comment has been flagged field is " + newFeedback.getCommnetFlag());

        // --- Step 6 ---
        // --- Step action ---
        // Open Blog1 again;
        // --- Expected results ---
        // Blog is opened;
        drone.navigateTo(wcmqsURL);
        wcmqsHomePage = new WcmqsHomePage(drone);
        wcmqsHomePage.render();
        wcmqsHomePage.selectMenu("blog");

        WcmqsBlogPage blogsPage2 = new WcmqsBlogPage(drone);
        blogsPage2.render();
        blogsPage2.clickBlogNameFromShare(blogName);

        WcmqsBlogPostPage blogPostPage2 = new WcmqsBlogPostPage(drone);
        blogPostPage2.render();

        // --- Step 7 ---
        // --- Step action ---
        // Click "Report this post" link;
        // --- Expected results ---
        // "This comment has been removed." is displayed under the comment;
        WcmqsComment comment = blogPostPage2.getCommentSection(visitorName, visitorComment);
        WcmqsBlogPostPage blogPostPage3 = comment.clickReportComment().render();
        String removedComment = "*** This comment has been removed. ***";
        Assert.assertEquals(blogPostPage3.getCommentSection(visitorName, removedComment).getCommentFromContent(), removedComment,
                "Comment has not been removed");

        // --- Step 8 ---
        // --- Step action ---
        // Navigate to Visitors feedback data list again;
        // --- Expected results ---
        // Visitors feedback data list is opened;
        ShareUtil.loginAs(drone, shareUrl, ADMIN_USERNAME, ADMIN_PASSWORD);

//        DocumentLibraryPage docLibPage2 = ShareUser.openSiteDocumentLibraryFromSearch(drone, siteName).render();

        DocumentLibraryPage docLibPage2 = SiteUtil.openSiteFromSearch(drone, siteName).getSiteNav().selectSiteDocumentLibrary().render();
        DataListPage dataListPage2 = docLibPage2.getSiteNav().selectDataListPage().render();
        dataListPage2.selectDataList("Visitor Feedback (Quick Start Editorial)");

        VisitorFeedbackList feedbackList2 = new VisitorFeedbackList(drone);
        feedbackList2.render();

        // --- Step 9 ---
        // --- Step action ---
        // Verify "Comment has been flagged" field for recently created comment;
        // --- Expected results ---
        // "Comment has been flagged" contains "true" value;
        VisitorFeedbackRow newFeedback2 = feedbackList.getRowForSpecificValues(visitorEmail, visitorComment, visitorName, visitorWebsite);
        Assert.assertEquals(newFeedback2.getCommnetFlag(), "true", "Comment has been flagged field is " + newFeedback2.getCommnetFlag());

        ShareUtil.logout(drone);
    }
}
