package org.ldbcouncil.snb.driver.validation;

import com.google.common.collect.Lists;
import org.ldbcouncil.snb.driver.csv.simple.SimpleCsvFileWriter;
import org.ldbcouncil.snb.driver.runtime.metrics.OperationMetricsSnapshot;
import org.ldbcouncil.snb.driver.runtime.metrics.WorkloadResultsSnapshot;
import org.ldbcouncil.snb.driver.util.Tuple;
import org.ldbcouncil.snb.driver.util.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultsLogValidationTest
{
    private static final List<Tuple2<String,Long>> DELAYS = Lists.newArrayList(
            Tuple.tuple2( "A", 1l ),
            Tuple.tuple2( "B", 1l ),
            Tuple.tuple2( "B", 1l ),
            Tuple.tuple2( "C", 4l ),
            Tuple.tuple2( "B", 5l ),
            Tuple.tuple2( "C", 6l ),
            Tuple.tuple2( "C", 10l ),
            Tuple.tuple2( "B", 11l ),
            Tuple.tuple2( "D", 1000l ),
            Tuple.tuple2( "E", 10000l )
    );
    
    private ResultsLogValidationResult runValidation(boolean recordDelayedOperations, int toleratedExcessiveDelayCount)
    {
        long excessiveDelayThresholdAsMilli = 10;
        long excessiveDelayCount = 10;
        double toleratedExcessiveDelayCountPercentage = 0.05d;
        Map<String,Long> excessiveDelayCountPerType = new HashMap<>();
        excessiveDelayCountPerType.put( "A", 1l );
        excessiveDelayCountPerType.put( "B", 2l );
        excessiveDelayCountPerType.put( "C", 3l );
        long minDelayAsMilli = 0;
        long maxDelayAsMilli = 0;
        long meanDelayAsMilli = 0;
        Map<String,Long> minDelayAsMilliPerType = new HashMap<>();
        Map<String,Long> maxDelayAsMilliPerType = new HashMap<>();
        Map<String,Long> meanDelayAsMilliPerType = new HashMap<>();
        ResultsLogValidationSummary summary = new ResultsLogValidationSummary(
                excessiveDelayThresholdAsMilli,
                excessiveDelayCount,
                excessiveDelayCountPerType,
                minDelayAsMilli,
                maxDelayAsMilli,
                meanDelayAsMilli,
                minDelayAsMilliPerType,
                maxDelayAsMilliPerType,
                meanDelayAsMilliPerType
        );

        OperationMetricsSnapshot operationA = new OperationMetricsSnapshot("A",TimeUnit.MILLISECONDS, 1, null);
        OperationMetricsSnapshot operationB = new OperationMetricsSnapshot("B",TimeUnit.MILLISECONDS, 1, null);
        OperationMetricsSnapshot operationC = new OperationMetricsSnapshot("C",TimeUnit.MILLISECONDS, 1, null);
        List<OperationMetricsSnapshot> metrics = new ArrayList<>(
            Arrays.asList(
            operationA, operationB, operationC
        ));

        WorkloadResultsSnapshot workloadResults = new WorkloadResultsSnapshot(
            metrics,
            1,
            10,
            3,
            TimeUnit.MILLISECONDS
        );

        Map<String,Long> toleratedExcessiveDelayCountPerType = new HashMap<>();
        toleratedExcessiveDelayCountPerType.put( "A", excessiveDelayCountPerType.get( "A" ) );
        toleratedExcessiveDelayCountPerType.put( "B", excessiveDelayCountPerType.get( "B" ) );
        toleratedExcessiveDelayCountPerType.put( "C", excessiveDelayCountPerType.get( "C" ) );
        ResultsLogValidator validator = new ResultsLogValidator();
        ResultsLogValidationTolerances tolerances = new ResultsLogValidationTolerances(
                excessiveDelayThresholdAsMilli,
                toleratedExcessiveDelayCount,
                toleratedExcessiveDelayCountPercentage
        );
        ResultsLogValidationResult result = validator.validate(
                summary,
                tolerances,
                recordDelayedOperations,
                workloadResults
        );
        return result;
    }
    
    @Test
    public void shouldPassValidationWhenResultsAreGood()
    {
        boolean recordDelayedOperations = false;
        int toleratedExcessiveDelayCount = 10;
        ResultsLogValidationResult result = runValidation(recordDelayedOperations, toleratedExcessiveDelayCount);
        assertTrue( result.isSuccessful(), result.toString() );
    }

    @Test
    public void shouldFailValidationWhenExcessiveDelayCountIsExceeded()
    {
        boolean recordDelayedOperations = true;
        int toleratedExcessiveDelayCount = 9;
        ResultsLogValidationResult result = runValidation(recordDelayedOperations, toleratedExcessiveDelayCount);
        
        assertFalse( result.isSuccessful(), result.toString() );
        assertThat(
                result.toString(),
                result.errors().get( 0 ).errorType(),
                equalTo( ResultsLogValidationResult.ValidationErrorType.TOO_MANY_LATE_OPERATIONS )
        );
    }

    @Test
    public void shouldReturnExpectedSummaryWhenComputedThenSerializedAndMarshaled() throws IOException
    {
        // Given
        long excessiveDelayThreshold = 5;
        ResultsLogValidationSummaryCalculator calculator = new ResultsLogValidationSummaryCalculator(
                10000,
                excessiveDelayThreshold
        );

        // When
        for ( Tuple2<String,Long> delay : DELAYS )
        {
            calculator.recordDelay( delay._1(), delay._2() );
        }

        ResultsLogValidationSummary summary = calculator.snapshot();
        String serializedSummary = summary.toJson();
        System.out.println( serializedSummary );
        ResultsLogValidationSummary summaryAfterMarshal = ResultsLogValidationSummary.fromJson(
                serializedSummary
        );

        // Then
        doSummaryAsserts( summary );
        doSummaryAsserts( summaryAfterMarshal );
    }

    @Test
    public void shouldReturnExpectedSummaryWhenValidatedFromFile(@TempDir File temporaryFolder) throws IOException, ValidationException
    {
        // Given
        long excessiveDelayThreshold = 5;
        File file = new File(temporaryFolder, "output.csv");

        try ( SimpleCsvFileWriter writer =
                      new SimpleCsvFileWriter( file, SimpleCsvFileWriter.DEFAULT_COLUMN_SEPARATOR, false ) )
        {
            writer.writeRow(
                    "operation_type",
                    "scheduled_start_time",
                    "actual_start_time",
                    "duration",
                    "result_code"
            );
            for ( Tuple2<String,Long> delay : DELAYS )
            {
                writer.writeRow(
                        // operation type
                        delay._1(),
                        // scheduled start time
                        Long.toString( 0 ),
                        // actual start time
                        Long.toString( delay._2() ),
                        // duration
                        Long.toString( 0 ),
                        // result code
                        Long.toString( 0 )
                );
            }
        }

        // When
        ResultsLogValidator validator = new ResultsLogValidator();
        ResultsLogValidationSummary summary = validator.compute( file, excessiveDelayThreshold );
        String serializedSummary = summary.toJson();
        System.out.println( serializedSummary );
        ResultsLogValidationSummary summaryAfterMarshal = ResultsLogValidationSummary.fromJson(
                serializedSummary
        );

        // Then
        doSummaryAsserts( summary );
        doSummaryAsserts( summaryAfterMarshal );
    }

    private void doSummaryAsserts( ResultsLogValidationSummary summary )
    {
        assertThat( summary.excessiveDelayThresholdAsMilli(), equalTo( 5l ) );
        assertThat( summary.excessiveDelayCount(), equalTo( 5l ) );
        assertThat(
                format( "Found: %s", summary.excessiveDelayCountPerType().keySet().toString() ),
                summary.excessiveDelayCountPerType().size(), equalTo( 5 )
        );
        assertThat( summary.excessiveDelayCountPerType().get( "A" ), equalTo( 0l ) );
        assertThat( summary.excessiveDelayCountPerType().get( "B" ), equalTo( 1l ) );
        assertThat( summary.excessiveDelayCountPerType().get( "C" ), equalTo( 2l ) );
        assertThat( summary.excessiveDelayCountPerType().get( "D" ), equalTo( 1l ) );
        assertThat( summary.excessiveDelayCountPerType().get( "E" ), equalTo( 1l ) );
        assertThat( summary.minDelayAsMilli(), equalTo( 1l ) );
        assertThat( summary.maxDelayAsMilli(), equalTo( 10000l ) );
        assertThat( summary.meanDelayAsMilli(), equalTo( 1104l ) );
        assertThat( summary.minDelayAsMilliPerType().size(), equalTo( 5 ) );
        assertThat( summary.minDelayAsMilliPerType().get( "A" ), equalTo( 1l ) );
        assertThat( summary.minDelayAsMilliPerType().get( "B" ), equalTo( 1l ) );
        assertThat( summary.minDelayAsMilliPerType().get( "C" ), equalTo( 4l ) );
        assertThat( summary.minDelayAsMilliPerType().get( "D" ), equalTo( 1000l ) );
        assertThat( summary.minDelayAsMilliPerType().get( "E" ), equalTo( 10000l ) );
        assertThat( summary.maxDelayAsMilliPerType().size(), equalTo( 5 ) );
        assertThat( summary.maxDelayAsMilliPerType().get( "A" ), equalTo( 1l ) );
        assertThat( summary.maxDelayAsMilliPerType().get( "B" ), equalTo( 11l ) );
        assertThat( summary.maxDelayAsMilliPerType().get( "C" ), equalTo( 10l ) );
        assertThat( summary.maxDelayAsMilliPerType().get( "D" ), equalTo( 1000l ) );
        assertThat( summary.maxDelayAsMilliPerType().get( "E" ), equalTo( 10000l ) );
        assertThat( summary.meanDelayAsMilliPerType().size(), equalTo( 5 ) );
        assertThat( summary.meanDelayAsMilliPerType().get( "A" ), equalTo( 1l ) );
        assertThat( summary.meanDelayAsMilliPerType().get( "B" ), equalTo( 5l ) );
        assertThat( summary.meanDelayAsMilliPerType().get( "C" ), equalTo( 7l ) );
        assertThat( summary.meanDelayAsMilliPerType().get( "D" ), equalTo( 1000l ) );
        assertThat( summary.meanDelayAsMilliPerType().get( "E" ), equalTo( 10000l ) );
    }
}
