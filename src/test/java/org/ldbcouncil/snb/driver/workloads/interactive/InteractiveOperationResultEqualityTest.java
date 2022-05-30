package org.ldbcouncil.snb.driver.workloads.interactive;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery10Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery11Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery12Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery13Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery14Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery1Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery2Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery3Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery4Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery5Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery6Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery7Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery8Result;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery9Result;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class InteractiveOperationResultEqualityTest
{
    @Test
    public void ldbcQuery1ResultShouldDoEqualsCorrectly() {
        long friendId1 = 1;
        String friendLastName1 = "last1";
        int friendDistance1 = 1;
        long friendBirthday1 = 1;
        long friendCreationDate1 = 1;
        String friendGender1 = "\u16a0";
        String friendBrowserUsed1 = "\u16a0";
        String friendLocationIp1 = "\u16a0";
        Iterable<String> friendEmails1 = Lists.newArrayList("1a", "1b");
        Iterable<String> friendLanguages1 = Lists.newArrayList("1a", "1b");
        String friendCityName1 = "\u16a0";
        Iterable<LdbcQuery1Result.Organization> friendUniversities1a = Arrays.asList(new LdbcQuery1Result.Organization("someUniversityA", 13, "1c"), new LdbcQuery1Result.Organization("someUniversityA2", 14, "1f"));
        Iterable<LdbcQuery1Result.Organization> friendCompanies1a = Arrays.asList(new LdbcQuery1Result.Organization("someCompanyA", 16, "1c"), new LdbcQuery1Result.Organization("someCompanyA2", 14, "1f"));
        Iterable<LdbcQuery1Result.Organization> friendUniversities1b = Arrays.asList(new LdbcQuery1Result.Organization("someUniversityA", 13, "1c"), new LdbcQuery1Result.Organization("someUniversityA2", 14, "1f"));
        Iterable<LdbcQuery1Result.Organization> friendCompanies1b = Arrays.asList(new LdbcQuery1Result.Organization("someCompanyA", 16, "1c"), new LdbcQuery1Result.Organization("someCompanyA2", 14, "1f"));

        long friendId2 = 2;
        String friendLastName2 = "last2";
        int friendDistance2 = 2;
        long friendBirthday2 = 2;
        long friendCreationDate2 = 2;
        String friendGender2 = "\u3055";
        String friendBrowserUsed2 = "\u3055";
        String friendLocationIp2 = "\u3055";
        Iterable<String> friendEmails2 = Lists.newArrayList("2a", "2b");
        Iterable<String> friendLanguages2 = Lists.newArrayList("2a", "2b");
        String friendCityName2 = "\u3055";
        Iterable<LdbcQuery1Result.Organization> friendUniversities2 = Arrays.asList(new LdbcQuery1Result.Organization("someUniversityC", 13, "2c"), new LdbcQuery1Result.Organization("someUniversityD", 14, "2f"));
        Iterable<LdbcQuery1Result.Organization> friendCompanies2 = Arrays.asList(new LdbcQuery1Result.Organization("someCompanyC", 16, "2c"), new LdbcQuery1Result.Organization("someCompanyC", 14, "2f"));

        LdbcQuery1Result result1a = new LdbcQuery1Result(
                friendId1,
                friendLastName1,
                friendDistance1,
                friendBirthday1,
                friendCreationDate1,
                friendGender1,
                friendBrowserUsed1,
                friendLocationIp1,
                friendEmails1,
                friendLanguages1,
                friendCityName1,
                friendUniversities1a,
                friendCompanies1a
        );

        LdbcQuery1Result result1b = new LdbcQuery1Result(
                friendId1,
                friendLastName1,
                friendDistance1,
                friendBirthday1,
                friendCreationDate1,
                friendGender1,
                friendBrowserUsed1,
                friendLocationIp1,
                Lists.newArrayList("1b", "1a"),
                Lists.newArrayList("1b", "1a"),
                friendCityName1,
                friendUniversities1b,
                friendCompanies1b
        );

        LdbcQuery1Result result1c = new LdbcQuery1Result(
                friendId1,
                friendLastName1,
                friendDistance1,
                friendBirthday1,
                friendCreationDate1,
                friendGender1,
                friendBrowserUsed1,
                friendLocationIp1,
                Lists.newArrayList("1b", "1a"),
                Lists.newArrayList("1b", "1a"),
                friendCityName1,
                friendUniversities1a,
                friendCompanies1b
        );

        LdbcQuery1Result result2a = new LdbcQuery1Result(
                friendId2,
                friendLastName2,
                friendDistance2,
                friendBirthday2,
                friendCreationDate2,
                friendGender2,
                friendBrowserUsed2,
                friendLocationIp2,
                friendEmails2,
                friendLanguages2,
                friendCityName2,
                friendUniversities2,
                friendCompanies2
        );

        LdbcQuery1Result result3a = new LdbcQuery1Result(
                friendId1,
                friendLastName1,
                friendDistance1,
                friendBirthday1,
                friendCreationDate1,
                friendGender1,
                friendBrowserUsed1,
                friendLocationIp1,
                friendEmails1,
                friendLanguages1,
                friendCityName1,
                friendUniversities1a,
                friendCompanies2
        );

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, equalTo(result1c));
        assertThat(result1b, equalTo(result1c));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery2ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u16a0";
        String personLastName1 = "\u16a0";
        long postOrCommentId1 = 1;
        String postOrCommentContent1 = "\u16a0";
        long postOrCommentCreationDate1 = 1;

        long personId2 = 2;
        String personFirstName2 = "\u3055";
        String personLastName2 = "\u3055";
        long postOrCommentId2 = 2;
        String postOrCommentContent2 = "\u3055";
        long postOrCommentCreationDate2 = 2;

        LdbcQuery2Result result1a = new LdbcQuery2Result(personId1, personFirstName1, personLastName1, postOrCommentId1, postOrCommentContent1, postOrCommentCreationDate1);
        LdbcQuery2Result result1b = new LdbcQuery2Result(personId1, personFirstName1, personLastName1, postOrCommentId1, postOrCommentContent1, postOrCommentCreationDate1);
        LdbcQuery2Result result2a = new LdbcQuery2Result(personId2, personFirstName2, personLastName2, postOrCommentId2, postOrCommentContent2, postOrCommentCreationDate2);
        LdbcQuery2Result result3a = new LdbcQuery2Result(personId1, personFirstName1, personLastName1, postOrCommentId1, postOrCommentContent1, postOrCommentCreationDate2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery3ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u3055";
        String personLastName1 = "\u4e35";
        int xCount1 = 4;
        int yCount1 = 5;
        int count1 = 6;

        long personId2 = 7;
        String personFirstName2 = "8";
        String personLastName2 = "9";
        int xCount2 = 10;
        int yCount2 = 11;
        int count2 = 12;

        LdbcQuery3Result result1a = new LdbcQuery3Result(personId1, personFirstName1, personLastName1, xCount1, yCount1, count1);
        LdbcQuery3Result result1b = new LdbcQuery3Result(personId1, personFirstName1, personLastName1, xCount1, yCount1, count1);
        LdbcQuery3Result result2a = new LdbcQuery3Result(personId2, personFirstName2, personLastName2, xCount2, yCount2, count2);
        LdbcQuery3Result result3a = new LdbcQuery3Result(personId1, personFirstName1, personLastName1, xCount1, yCount1, count2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery4ResultShouldDoEqualsCorrectly() {
        String tagName1 = "\u16a0";
        int tagCount1 = 2;

        String tagName2 = "\u4e35";
        int tagCount2 = 4;

        LdbcQuery4Result result1a = new LdbcQuery4Result(tagName1, tagCount1);
        LdbcQuery4Result result1b = new LdbcQuery4Result(tagName1, tagCount1);
        LdbcQuery4Result result2a = new LdbcQuery4Result(tagName2, tagCount2);
        LdbcQuery4Result result3a = new LdbcQuery4Result(tagName1, tagCount2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery5ResultShouldDoEqualsCorrectly() {
        String forumTitle1 = "\u16a0";
        int postCount1 = 2;

        String forumTitle2 = "\u4e35";
        int postCount2 = 4;

        LdbcQuery5Result result1a = new LdbcQuery5Result(forumTitle1, postCount1);
        LdbcQuery5Result result1b = new LdbcQuery5Result(forumTitle1, postCount1);
        LdbcQuery5Result result2a = new LdbcQuery5Result(forumTitle2, postCount2);
        LdbcQuery5Result result3a = new LdbcQuery5Result(forumTitle1, postCount2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery6ResultShouldDoEqualsCorrectly() {
        String tagName1 = "\u16a0";
        int tagCount1 = 2;

        String tagName2 = "\u4e35";
        int tagCount2 = 4;

        LdbcQuery6Result result1a = new LdbcQuery6Result(tagName1, tagCount1);
        LdbcQuery6Result result1b = new LdbcQuery6Result(tagName1, tagCount1);
        LdbcQuery6Result result2a = new LdbcQuery6Result(tagName2, tagCount2);
        LdbcQuery6Result result3a = new LdbcQuery6Result(tagName1, tagCount2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery7ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u3055";
        String personLastName1 = "\u4e35";
        long likeCreationDate1 = 4;
        long commentOrPostId1 = 5;
        String commentOrPostContent1 = "6";
        int minutesLatency1 = 7;
        boolean isNew1 = true;

        long personId2 = 8;
        String personFirstName2 = "9";
        String personLastName2 = "10";
        long likeCreationDate2 = 11;
        long commentOrPostId2 = 12;
        String commentOrPostContent2 = "13";
        int minutesLatency2 = 14;
        boolean isNew2 = false;

        LdbcQuery7Result result1a = new LdbcQuery7Result(personId1, personFirstName1, personLastName1, likeCreationDate1, commentOrPostId1, commentOrPostContent1, minutesLatency1, isNew1);
        LdbcQuery7Result result1b = new LdbcQuery7Result(personId1, personFirstName1, personLastName1, likeCreationDate1, commentOrPostId1, commentOrPostContent1, minutesLatency1, isNew1);
        LdbcQuery7Result result2a = new LdbcQuery7Result(personId2, personFirstName2, personLastName2, likeCreationDate2, commentOrPostId2, commentOrPostContent2, minutesLatency2, isNew2);
        LdbcQuery7Result result3a = new LdbcQuery7Result(personId1, personFirstName1, personLastName1, likeCreationDate1, commentOrPostId1, commentOrPostContent1, minutesLatency1, isNew2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery8ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u3055";
        String personLastName1 = "\u4e35";
        long commentCreationDate1 = 4;
        long commentId1 = 5;
        String commentContent1 = "6";

        long personId2 = 7;
        String personFirstName2 = "8";
        String personLastName2 = "9";
        long commentCreationDate2 = 10;
        long commentId2 = 11;
        String commentContent2 = "12";

        LdbcQuery8Result result1a = new LdbcQuery8Result(personId1, personFirstName1, personLastName1, commentCreationDate1, commentId1, commentContent1);
        LdbcQuery8Result result1b = new LdbcQuery8Result(personId1, personFirstName1, personLastName1, commentCreationDate1, commentId1, commentContent1);
        LdbcQuery8Result result2a = new LdbcQuery8Result(personId2, personFirstName2, personLastName2, commentCreationDate2, commentId2, commentContent2);
        LdbcQuery8Result result3a = new LdbcQuery8Result(personId1, personFirstName1, personLastName1, commentCreationDate1, commentId1, commentContent2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery9ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u3055";
        String personLastName1 = "\u4e35";
        long commentOrPostId1 = 4;
        String commentOrPostContent1 = "\u0634";
        long commentOrPostCreationDate1 = 6;

        long personId2 = 7;
        String personFirstName2 = "8";
        String personLastName2 = "9";
        long commentOrPostId2 = 10;
        String commentOrPostContent2 = "11";
        long commentOrPostCreationDate2 = 12;

        LdbcQuery9Result result1a = new LdbcQuery9Result(personId1, personFirstName1, personLastName1, commentOrPostId1, commentOrPostContent1, commentOrPostCreationDate1);
        LdbcQuery9Result result1b = new LdbcQuery9Result(personId1, personFirstName1, personLastName1, commentOrPostId1, commentOrPostContent1, commentOrPostCreationDate1);
        LdbcQuery9Result result2a = new LdbcQuery9Result(personId2, personFirstName2, personLastName2, commentOrPostId2, commentOrPostContent2, commentOrPostCreationDate2);
        LdbcQuery9Result result3a = new LdbcQuery9Result(personId1, personFirstName1, personLastName1, commentOrPostId1, commentOrPostContent1, commentOrPostCreationDate2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery10ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u3055";
        String personLastName1 = "\u4e35";
        int commonInterestScore1 = 4;
        String personGender1 = "\u0634";
        String personCityName1 = "6";

        long personId2 = 7;
        String personFirstName2 = "8";
        String personLastName2 = "9";
        int commonInterestScore2 = 10;
        String personGender2 = "11";
        String personCityName2 = "12";

        LdbcQuery10Result result1a = new LdbcQuery10Result(personId1, personFirstName1, personLastName1, commonInterestScore1, personGender1, personCityName1);
        LdbcQuery10Result result1b = new LdbcQuery10Result(personId1, personFirstName1, personLastName1, commonInterestScore1, personGender1, personCityName1);
        LdbcQuery10Result result2a = new LdbcQuery10Result(personId2, personFirstName2, personLastName2, commonInterestScore2, personGender2, personCityName2);
        LdbcQuery10Result result3a = new LdbcQuery10Result(personId1, personFirstName1, personLastName1, commonInterestScore1, personGender1, personCityName2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery11ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u3055";
        String personLastName1 = "\u4e35";
        String organizationName1 = "\u05e4";
        int organizationWorkFromYear1 = 5;

        long personId2 = 6;
        String personFirstName2 = "7";
        String personLastName2 = "8";
        String organizationName2 = "9";
        int organizationWorkFromYear2 = 10;

        LdbcQuery11Result result1a = new LdbcQuery11Result(personId1, personFirstName1, personLastName1, organizationName1, organizationWorkFromYear1);
        LdbcQuery11Result result1b = new LdbcQuery11Result(personId1, personFirstName1, personLastName1, organizationName1, organizationWorkFromYear1);
        LdbcQuery11Result result2a = new LdbcQuery11Result(personId2, personFirstName2, personLastName2, organizationName2, organizationWorkFromYear2);
        LdbcQuery11Result result3a = new LdbcQuery11Result(personId1, personFirstName1, personLastName1, organizationName1, organizationWorkFromYear2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
    }

    @Test
    public void ldbcQuery12ResultShouldDoEqualsCorrectly() {
        long personId1 = 1;
        String personFirstName1 = "\u3055";
        String personLastName1 = "\u4e35";
        Iterable<String> tagNames1 = Lists.newArrayList("\u05e4", "\u0634");
        int replyCount1 = 1;

        long personId2 = 6;
        String personFirstName2 = "7";
        String personLastName2 = "9";
        Iterable<String> tagNames2 = Lists.newArrayList("9", "10");
        int replyCount2 = 11;

        LdbcQuery12Result result1a = new LdbcQuery12Result(personId1, personFirstName1, personLastName1, tagNames1, replyCount1);
        LdbcQuery12Result result1b = new LdbcQuery12Result(personId1, personFirstName1, personLastName1, Lists.newArrayList("\u0634", "\u05e4"), replyCount1);
        LdbcQuery12Result result2a = new LdbcQuery12Result(personId2, personFirstName2, personLastName2, tagNames2, replyCount2);
        LdbcQuery12Result result3a = new LdbcQuery12Result(personId1, personFirstName1, personLastName1, tagNames1, replyCount2);
        LdbcQuery12Result result4a = new LdbcQuery12Result(personId1, personFirstName1, personLastName1, null, replyCount1);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result3a)));
        assertThat(result1a, not(equalTo(result4a)));
    }

    @Test
    public void ldbcQuery13ResultShouldDoEqualsCorrectly() {
        int shortestPathLength1 = 1;

        int shortestPathLength2 = 2;

        LdbcQuery13Result result1a = new LdbcQuery13Result(shortestPathLength1);
        LdbcQuery13Result result1b = new LdbcQuery13Result(shortestPathLength1);
        LdbcQuery13Result result2a = new LdbcQuery13Result(shortestPathLength2);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1b, not(equalTo(result2a)));
    }

    @Test
    public void ldbcQuery14ResultShouldDoEqualsCorrectly() {
        LdbcQuery14Result result1a = new LdbcQuery14Result(Lists.newArrayList(1l, 2l), 1d);
        LdbcQuery14Result result1b = new LdbcQuery14Result(Lists.newArrayList(1l, 2l), 1d);
        LdbcQuery14Result result2a = new LdbcQuery14Result(Lists.newArrayList(1l, 3l), 2d);
        LdbcQuery14Result result3a = new LdbcQuery14Result(Lists.newArrayList(2l, 1l), 1d);
        LdbcQuery14Result result4a = new LdbcQuery14Result(Lists.newArrayList(1l, 2l), 2d);

        assertThat(result1a, equalTo(result1b));
        assertThat(result1a, not(equalTo(result2a)));
        assertThat(result1a, not(equalTo(result3a)));
        assertThat(result1a, not(equalTo(result4a)));
        assertThat(result2a, not(equalTo(result3a)));
        assertThat(result2a, not(equalTo(result4a)));
        assertThat(result3a, not(equalTo(result4a)));
    }
}
