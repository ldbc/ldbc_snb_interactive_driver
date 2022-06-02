package org.ldbcouncil.snb.driver.workloads.interactive;


import org.ldbcouncil.snb.driver.Operation;
import org.ldbcouncil.snb.driver.WorkloadException;
import org.ldbcouncil.snb.driver.generator.QueryEventStreamReader;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery6;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import static java.lang.String.format;

public class Query6EventStreamReader implements Iterator<Operation>
{
    private final Iterator<Object[]> csvRows;

    public Query6EventStreamReader( Iterator<Object[]> csvRows )
    {
        this.csvRows = csvRows;
    }

    @Override
    public boolean hasNext()
    {
        return csvRows.hasNext();
    }

    @Override
    public Operation next()
    {
        Object[] rowAsObjects = csvRows.next();
        Operation operation = new LdbcQuery6(
                (long) rowAsObjects[0],
                (String) rowAsObjects[1],
                LdbcQuery6.DEFAULT_LIMIT
        );
        operation.setDependencyTimeStamp( 0 );
        return operation;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException( format( "%s does not support remove()", getClass().getSimpleName() ) );
    }

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
