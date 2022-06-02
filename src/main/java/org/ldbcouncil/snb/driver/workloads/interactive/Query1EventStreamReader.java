package org.ldbcouncil.snb.driver.workloads.interactive;

import org.ldbcouncil.snb.driver.Operation;
import org.ldbcouncil.snb.driver.WorkloadException;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery1;
import org.ldbcouncil.snb.driver.generator.QueryEventStreamReader;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Iterator;

import static java.lang.String.format;

public class Query1EventStreamReader implements Iterator<Operation>
{
    private final Iterator<Object[]> objectArray;

    public Query1EventStreamReader( Iterator<Object[]> objectArray )
    {
        this.objectArray = objectArray;
    }

    @Override
    public boolean hasNext()
    {
        return objectArray.hasNext();
    }

    @Override
    public Operation next()
    {
        Object[] rowAsObjects = objectArray.next();
        Operation operation = new LdbcQuery1(
                (long) rowAsObjects[0],
                (String) rowAsObjects[1],
                LdbcQuery1.DEFAULT_LIMIT
        );
        operation.setDependencyTimeStamp( 0 );
        return operation;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException( format( "%s does not support remove()", getClass().getSimpleName() ) );
    }

    /**
     * Inner class used for decoding Resultset data for query 1 parameters.
     */
    public static class QueryDecoder implements QueryEventStreamReader.EventDecoder<Object[]>
    {
        // personId|firstName
        // 2199032251700|Andrea

        /**
         * @param rs: Resultset object containing the row to decode
        * @return Object array
         * @throws SQLException when an error occurs reading the resultset
         */
        @Override
        public Object[] decodeEvent( ResultSet rs ) throws WorkloadException
        {
            try
            {
                long personId = rs.getLong(1);
                String personName = rs.getString(2);
                return new Object[]{personId, personName};
            }
            catch (SQLException e){
                throw new WorkloadException(format("Error while decoding ResultSet for Query1Event: %s", e));
            }
        }
    }
}
