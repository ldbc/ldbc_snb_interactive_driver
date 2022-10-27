package org.ldbcouncil.snb.driver.workloads.interactive.db;

import com.google.common.collect.Lists;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.*;

import java.util.Date;

public class DummyLdbcSnbInteractiveOperationInstances {

    /*
    LONG READS
     */

    public static LdbcQuery1 read1() {
        return new LdbcQuery1(1, "3", 4);
    }

    public static LdbcQuery2 read2() {
        return new LdbcQuery2(1, new Date(3), 4);
    }

    public static LdbcQuery3a read3a() {
        return new LdbcQuery3a(1, "3", "4", new Date(5), 6, 7);
    }

    public static LdbcQuery3b read3b() {
        return new LdbcQuery3b(1, "3", "4", new Date(5), 6, 7);
    }

    public static LdbcQuery4 read4() {
        return new LdbcQuery4(1, new Date(3), 4, 5);
    }

    public static LdbcQuery5 read5() {
        return new LdbcQuery5(1, new Date(3), 4);
    }

    public static LdbcQuery6 read6() {
        return new LdbcQuery6(1, "3", 4);
    }

    public static LdbcQuery7 read7() {
        return new LdbcQuery7(1, 3);
    }

    public static LdbcQuery8 read8() {
        return new LdbcQuery8(1, 3);
    }

    public static LdbcQuery9 read9() {
        return new LdbcQuery9(1, new Date(3), 4);
    }

    public static LdbcQuery10 read10() {
        return new LdbcQuery10(1, 3, 4);
    }

    public static LdbcQuery11 read11() {
        return new LdbcQuery11(1, "3", 4, 5);
    }

    public static LdbcQuery12 read12() {
        return new LdbcQuery12(1, "3", 4);
    }

    public static LdbcQuery13a read13a() {
        return new LdbcQuery13a(1, 3);
    }

    public static LdbcQuery13b read13b() {
        return new LdbcQuery13b(1, 3);
    }

    public static LdbcQuery14a read14a() {
        return new LdbcQuery14a(1, 3);
    }

    public static LdbcQuery14b read14b() {
        return new LdbcQuery14b(1, 3);
    }

    /*
    SHORT READS
     */

    public static LdbcShortQuery1PersonProfile short1() {
        return new LdbcShortQuery1PersonProfile(1);
    }

    public static LdbcShortQuery2PersonPosts short2() {
        return new LdbcShortQuery2PersonPosts(2, 3);
    }

    public static LdbcShortQuery3PersonFriends short3() {
        return new LdbcShortQuery3PersonFriends(3);
    }

    public static LdbcShortQuery4MessageContent short4() {
        return new LdbcShortQuery4MessageContent(4);
    }

    public static LdbcShortQuery5MessageCreator short5() {
        return new LdbcShortQuery5MessageCreator(5);
    }

    public static LdbcShortQuery6MessageForum short6() {
        return new LdbcShortQuery6MessageForum(6);
    }

    public static LdbcShortQuery7MessageReplies short7() {
        return new LdbcShortQuery7MessageReplies(7);
    }

    /*
    UPDATES
     */

    public static LdbcInsert1AddPerson write1() {
        return new LdbcInsert1AddPerson(
                1, "2", "3", "4", new Date(5), new Date(6), "7", "8", 9, Lists.newArrayList("10", "11"), Lists.<String>newArrayList(), Lists.newArrayList(13l),
                Lists.newArrayList(new LdbcInsert1AddPerson.Organization(14, 15), new LdbcInsert1AddPerson.Organization(16, 17)),
                Lists.<LdbcInsert1AddPerson.Organization>newArrayList());
    }

    public static LdbcInsert2AddPostLike write2() {
        return new LdbcInsert2AddPostLike(1, 2, new Date(3));
    }

    public static LdbcInsert3AddCommentLike write3() {
        return new LdbcInsert3AddCommentLike(1, 2, new Date(3));
    }

    public static LdbcInsert4AddForum write4() {
        return new LdbcInsert4AddForum(1, "2", new Date(3), 4, Lists.newArrayList(5l, 6l));
    }

    public static LdbcInsert5AddForumMembership write5() {
        return new LdbcInsert5AddForumMembership(1, 2, new Date(3));
    }

    public static LdbcInsert6AddPost write6() {
        return new LdbcInsert6AddPost(1, "2", new Date(3), "4", "5", "6", "7", 8, 9, 10, 11, Lists.newArrayList(12l));
    }

    public static LdbcInsert7AddComment write7() {
        return new LdbcInsert7AddComment(1, new Date(2), "3", "4", "5", 6, 7, 8, 9, 10, Lists.newArrayList(11l, 12l));
    }

    public static LdbcInsert8AddFriendship write8() {
        return new LdbcInsert8AddFriendship(1, 2, new Date(3));
    }

    /* DELETES
     */ 
    public static LdbcDelete1RemovePerson delete1() {
        return new LdbcDelete1RemovePerson(1);
    }

    public static LdbcDelete2RemovePostLike delete2() {
        return new LdbcDelete2RemovePostLike(1, 2);
    }

    public static LdbcDelete3RemoveCommentLike delete3() {
        return new LdbcDelete3RemoveCommentLike(1, 2);
    }

    public static LdbcDelete4RemoveForum delete4() {
        return new LdbcDelete4RemoveForum(1);
    }

    public static LdbcDelete5RemoveForumMembership delete5() {
        return new LdbcDelete5RemoveForumMembership(1, 2);
    }

    public static LdbcDelete6RemovePostThread delete6() {
        return new LdbcDelete6RemovePostThread(1);
    }

    public static LdbcDelete7RemoveCommentSubthread delete7() {
        return new LdbcDelete7RemoveCommentSubthread(1);
    }

    public static LdbcDelete8RemoveFriendship delete8() {
        return new LdbcDelete8RemoveFriendship(1, 2);
    }
}
