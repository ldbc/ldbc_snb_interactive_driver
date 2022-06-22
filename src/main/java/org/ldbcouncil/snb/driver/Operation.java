package org.ldbcouncil.snb.driver;
/**
 * Operation.java
 * 
 * Describes Operation performed by the driver, e.g. queries.
 * Each Operation is expected to have a RESULT_TYPE, which is the expected
 * result of the opertion. The operation must be able to deserialize the
 * result object.
 */
import org.ldbcouncil.snb.driver.temporal.TemporalUtil;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.*;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.ldbcouncil.snb.driver.Operation;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

@JsonTypeInfo(use=DEDUCTION)
@JsonSubTypes( {
    @JsonSubTypes.Type(LdbcQuery1.class),
    @JsonSubTypes.Type(LdbcQuery2.class),
    @JsonSubTypes.Type(LdbcQuery3.class),
    @JsonSubTypes.Type(LdbcQuery4.class),
    @JsonSubTypes.Type(LdbcQuery5.class),
    @JsonSubTypes.Type(LdbcQuery6.class),
    @JsonSubTypes.Type(LdbcQuery7.class),
    @JsonSubTypes.Type(LdbcQuery8.class),
    @JsonSubTypes.Type(LdbcQuery9.class),
    @JsonSubTypes.Type(LdbcQuery10.class),
    @JsonSubTypes.Type(LdbcQuery11.class),
    @JsonSubTypes.Type(LdbcQuery12.class),
    @JsonSubTypes.Type(LdbcQuery13.class),
    @JsonSubTypes.Type(LdbcQuery14.class),
    @JsonSubTypes.Type(LdbcShortQuery1PersonProfile.class),
    @JsonSubTypes.Type(LdbcShortQuery2PersonPosts.class),
    @JsonSubTypes.Type(LdbcShortQuery3PersonFriends.class),
    @JsonSubTypes.Type(LdbcShortQuery4MessageContent.class),
    @JsonSubTypes.Type(LdbcShortQuery5MessageCreator.class),
    @JsonSubTypes.Type(LdbcShortQuery6MessageForum.class),
    @JsonSubTypes.Type(LdbcShortQuery7MessageReplies.class),
    @JsonSubTypes.Type(LdbcUpdate1AddPerson.class),
    @JsonSubTypes.Type(LdbcUpdate2AddPostLike.class),
    @JsonSubTypes.Type(LdbcUpdate3AddCommentLike.class),
    @JsonSubTypes.Type(LdbcUpdate4AddForum.class),
    @JsonSubTypes.Type(LdbcUpdate5AddForumMembership.class),
    @JsonSubTypes.Type(LdbcUpdate6AddPost.class),
    @JsonSubTypes.Type(LdbcUpdate7AddComment.class),
    @JsonSubTypes.Type(LdbcUpdate8AddFriendship.class),
})
public abstract class Operation<RESULT_TYPE>
{
    private static final TemporalUtil temporalutil = new TemporalUtil();
    private long scheduledStartTimeAsMilli = -1;
    private long timeStamp = -1;
    private long dependencyTimeStamp = -1;

    public final void setScheduledStartTimeAsMilli( long scheduledStartTimeAsMilli )
    {
        this.scheduledStartTimeAsMilli = scheduledStartTimeAsMilli;
    }

    public final void setDependencyTimeStamp( long dependencyTimeStamp )
    {
        this.dependencyTimeStamp = dependencyTimeStamp;
    }

    public final long scheduledStartTimeAsMilli()
    {
        return scheduledStartTimeAsMilli;
    }

    public final long dependencyTimeStamp()
    {
        return dependencyTimeStamp;
    }

    public long timeStamp()
    {
        return timeStamp;
    }

    public void setTimeStamp( long timeStamp )
    {
        this.timeStamp = timeStamp;
    }

    /**
     * Get type of operation
     * @see org.ldbcouncil.snb.driver.workloads.interactive.LdbcSnbInteractiveWorkloadConfiguration#operationTypeToClassMapping()
     * for mapping of LDBC queries
     * @return Type as integer
     */
    public abstract int type();

    @Override
    public String toString()
    {
        return "Operation{" +
               "scheduledStartTime=" + temporalutil.milliTimeToDateTimeString( scheduledStartTimeAsMilli ) +
               ", timeStamp=" + temporalutil.milliTimeToDateTimeString( timeStamp ) +
               ", dependencyTimeStamp=" + temporalutil.milliTimeToDateTimeString( dependencyTimeStamp ) +
               '}';
    }

    /**
     * Maps the operation variable of an operation to the getter function
     * @return Map with operation variable as key and name of getter function
     */
    public abstract Map<String, Object> parameterMap();

    public Operation newInstance(){
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes a list of result objects
     * @param serializedOperationResult The serialized result object in a list
     * @return Deserialized result object
     * @throws IOException in case the given string cannot be deserialized to the result type
     */
    public abstract RESULT_TYPE deserializeResult( String serializedOperationResult) throws IOException;

}
