package org.ldbcouncil.snb.driver.workloads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.ldbcouncil.snb.driver.Client;
import org.ldbcouncil.snb.driver.Operation;
import org.ldbcouncil.snb.driver.Workload;
import org.ldbcouncil.snb.driver.WorkloadStreams;
import org.ldbcouncil.snb.driver.client.ClientMode;
import org.ldbcouncil.snb.driver.client.ResultsDirectory;
import org.ldbcouncil.snb.driver.client.ValidateDatabaseMode;
import org.ldbcouncil.snb.driver.control.ConsoleAndFileDriverConfiguration;
import org.ldbcouncil.snb.driver.control.ControlService;
import org.ldbcouncil.snb.driver.control.DriverConfiguration;
import org.ldbcouncil.snb.driver.control.DriverConfigurationException;
import org.ldbcouncil.snb.driver.control.LocalControlService;
import org.ldbcouncil.snb.driver.control.Log4jLoggingServiceFactory;
import org.ldbcouncil.snb.driver.generator.GeneratorFactory;
import org.ldbcouncil.snb.driver.generator.RandomDataGeneratorFactory;
import org.ldbcouncil.snb.driver.temporal.SystemTimeSource;
import org.ldbcouncil.snb.driver.temporal.TimeSource;
import org.ldbcouncil.snb.driver.testutils.TestUtils;
import org.ldbcouncil.snb.driver.util.Tuple2;
import org.ldbcouncil.snb.driver.validation.DbValidationResult;
import org.ldbcouncil.snb.driver.validation.WorkloadValidationResult;
import org.ldbcouncil.snb.driver.validation.WorkloadValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hamcrest.MatcherAssert.assertThat;

public abstract class WorkloadTest
{
    TimeSource timeSource = new SystemTimeSource();

    private DriverConfiguration withTempResultDirs( DriverConfiguration configuration, File temporaryFolder )
            throws IOException, DriverConfigurationException
    {
        return configuration.applyArg(
                ConsoleAndFileDriverConfiguration.RESULT_DIR_PATH_ARG,
                new File(temporaryFolder, UUID.randomUUID().toString()).getAbsolutePath()
        );
    }

    private DriverConfiguration withWarmup( DriverConfiguration configuration )
            throws IOException, DriverConfigurationException
    {
        return (0 == configuration.warmupCount())
        ? configuration.applyArg( ConsoleAndFileDriverConfiguration.WARMUP_COUNT_ARG, Long.toString( 10 ) )
        : configuration;
        
    }

    private DriverConfiguration withSkip( DriverConfiguration configuration )
            throws IOException, DriverConfigurationException
    {
                   return (0 == configuration.skipCount())
                    ? configuration.applyArg( ConsoleAndFileDriverConfiguration.SKIP_COUNT_ARG, Long.toString( 10 ) )
                    : configuration;
    }

    private DriverConfiguration withMode( DriverConfiguration configuration, String mode )
            throws IOException, DriverConfigurationException
    {
        return configuration.applyArg( ConsoleAndFileDriverConfiguration.MODE_ARG, mode );
    }

    public void shouldHaveOneToOneMappingBetweenOperationClassesAndOperationTypes(Workload workload) throws Exception
    {
        Map<Integer,Class<? extends Operation>> typeToClassMapping = workload.operationTypeToClassMapping();
        assertThat(
                typeToClassMapping.keySet().size(),
                equalTo( Sets.newHashSet( typeToClassMapping.values() ).size() )
        );
    }

    public void shouldHaveNonNegativeTypesForAllOperations(Workload workload, List<Tuple2<Operation,Object>> operationsAndResults) throws Exception
    {
        for ( Map.Entry<Integer,Class<? extends Operation>> entry :
                workload.operationTypeToClassMapping().entrySet() )
        {
            assertTrue(
                entry.getKey() >= 0,
                    format( "%s has negative type: %s", entry.getValue().getSimpleName(), entry.getKey() )
                    
            );
        }
        for ( Tuple2<Operation,Object> operation : operationsAndResults )
        {
            assertTrue(
                operation._1().type() >= 0,
                    format( "%s has negative type: %s", operation.getClass().getSimpleName(), operation._1().type() )
                    
            );
        }
    }

    public void shouldBeAbleToSerializeAndMarshalAllOperations(Workload workload, List<Tuple2<Operation,Object>> operationsAndResults) throws Exception
    {
        // When
        ObjectMapper mapper = new ObjectMapper();

        // Then
        for ( int i = 0; i < operationsAndResults.size(); i++ )
        {
            String serializedOperation = mapper.writeValueAsString(operationsAndResults.get( i )._1());
            Object deserializedOperation = mapper.readValue(serializedOperation, workload.getOperationClass());
            
        assertThat(
                format( "original != marshal(serialize(original))\n" +
                        "Original: %s\n"                             +
                        "Serialized: %s\n"                           +
                        "Marshaled: %s",
                        operationsAndResults.get( i )._1(),
                        serializedOperation,
                        deserializedOperation
                ),
                deserializedOperation,
                equalTo( operationsAndResults.get( i )._1() ) );
        }
    }

    public void shouldBeAbleToSerializeAndMarshalAllOperationResults(List<Tuple2<Operation,Object>> operationsAndResults) throws Exception
    {
        // When
        ObjectMapper mapper = new ObjectMapper();
        // Then
        for ( int i = 0; i < operationsAndResults.size(); i++ )
        {
                String serializedResult = mapper.writeValueAsString(operationsAndResults.get( i )._2());
                Object marshaledOperationResult = operationsAndResults.get( i )._1().deserializeResult(serializedResult);
                
            assertThat(
                    format( "original != marshal(serialize(original))\n" +
                            "Original: %s\n"                             +
                            "Serialized: %s\n"                           +
                            "Marshaled: %s",
                            operationsAndResults.get( i )._2(),
                            serializedResult,
                            marshaledOperationResult
                    ),
                    marshaledOperationResult,
                    equalTo( operationsAndResults.get( i )._2() ) );
        }
    }

    public boolean shouldGenerateManyOperationsInReasonableTime(DriverConfiguration configuration, File temporaryFolder) throws Exception
    {
        configuration = withTempResultDirs( configuration, temporaryFolder );
        long operationCount = 1_000_000;
        long timeoutAsMilli = TimeUnit.SECONDS.toMillis( 5 );

        Workload workload = new ClassNameWorkloadFactory( configuration.workloadClassName() )
            .createWorkload();
        workload.init( configuration );
        GeneratorFactory gf = new GeneratorFactory( new RandomDataGeneratorFactory( 42L ) );
        Iterator<Operation> operations = gf.limit(
                WorkloadStreams.mergeSortedByStartTimeExcludingChildOperationGenerators(
                        gf,
                        workload.streams( gf, true )
                ),
                operationCount
        );
        long timeout = timeSource.nowAsMilli() + timeoutAsMilli;
        return TestUtils.generateBeforeTimeout( operations, timeout, timeSource, operationCount );
    }

    public void shouldBeRepeatableWhenTwoIdenticalWorkloadsAreUsedWithIdenticalGeneratorFactories(DriverConfiguration configuration, File temporaryFolder) throws Exception
    {
        configuration = withSkip( withWarmup( withTempResultDirs( configuration, temporaryFolder ) ) );
        WorkloadFactory workloadFactory = new ClassNameWorkloadFactory( configuration.workloadClassName() );
        GeneratorFactory gf1 = new GeneratorFactory( new RandomDataGeneratorFactory( 42L ) );
        GeneratorFactory gf2 = new GeneratorFactory( new RandomDataGeneratorFactory( 42L ) );
        Workload workloadA = workloadFactory.createWorkload();
        Workload workloadB = workloadFactory.createWorkload();
        workloadA.init( configuration );
        workloadB.init( configuration );

        List<Class> operationsA = ImmutableList.copyOf(
                Iterators.transform(
                        gf1.limit(
                                WorkloadStreams.mergeSortedByStartTimeExcludingChildOperationGenerators(
                                        gf1,
                                        workloadA.streams( gf1, true )
                                ),
                                configuration.operationCount()
                        ),
                        new Function<Operation,Class>()
                        {
                            @Override
                            public Class apply( Operation operation )
                            {
                                return operation.getClass();
                            }
                        }
                )
        );

        List<Class> operationsB = ImmutableList.copyOf(
                Iterators.transform(
                        gf1.limit(
                                WorkloadStreams.mergeSortedByStartTimeExcludingChildOperationGenerators(
                                        gf2,
                                        workloadB.streams( gf2, true )
                                ),
                                configuration.operationCount()
                        ),
                        new Function<Operation,Class>()
                        {
                            @Override
                            public Class apply( Operation operation )
                            {
                                return operation.getClass();
                            }
                        }
                )
        );

        assertThat( operationsA.size(), is( operationsB.size() ) );

        Iterator<Class> operationsAIt = operationsA.iterator();
        Iterator<Class> operationsBIt = operationsB.iterator();

        while ( operationsAIt.hasNext() )
        {
            Class a = operationsAIt.next();
            Class b = operationsBIt.next();
            assertThat( a, equalTo( b ) );
        }
    }

    public void shouldLoadFromConfigFile(DriverConfiguration configuration, File temporaryFolder) throws Exception
    {
        configuration = withSkip( withWarmup( withTempResultDirs( configuration, temporaryFolder ) ) );
        File configurationFile = new File(temporaryFolder, "config.properties");
        Files.write( configurationFile.toPath(), configuration.toPropertiesString().getBytes() );

        assertTrue( configurationFile.exists() );

        configuration = ConsoleAndFileDriverConfiguration.fromArgs( new String[]{
                "-P", configurationFile.getAbsolutePath()
        } );

        ResultsDirectory resultsDirectory = new ResultsDirectory( configuration );

        for ( File file : resultsDirectory.expectedFiles() )
        {
            assertFalse( file.exists(), format( "Did not expect file to exist %s", file.getAbsolutePath() ));
        }

        // When
        Client client = new Client();
        ControlService controlService = new LocalControlService(
                timeSource.nowAsMilli(),
                configuration,
                new Log4jLoggingServiceFactory( false ),
                timeSource
        );
        ClientMode clientMode = client.getClientModeFor( controlService );
        clientMode.init();
        clientMode.startExecutionAndAwaitCompletion();

        // Then
        for ( File file : resultsDirectory.expectedFiles() )
        {
            assertTrue( file.exists() );
        }
        assertThat( resultsDirectory.expectedFiles(), equalTo( resultsDirectory.files() ) );

        if ( configuration.warmupCount() > 0 )
        {
            long resultsLogSize = resultsDirectory.getResultsLogFileLength( true );
            assertThat(
                    format( "Expected %s <= entries in results log <= %s\nFound %s\nResults Log: %s",
                            operationCountLower( configuration.warmupCount() ),
                            operationCountUpper( configuration.warmupCount() ),
                            resultsLogSize,
                            resultsDirectory.getResultsLogFile( true ).getAbsolutePath()
                    ),
                    resultsLogSize,
                    allOf(
                            greaterThanOrEqualTo( operationCountLower( configuration.warmupCount() ) ),
                            lessThanOrEqualTo( operationCountUpper( configuration.warmupCount() ) )
                    )
            );
        }
        long resultsLogSize = resultsDirectory.getResultsLogFileLength( false );
        assertThat(
                format( "Expected %s <= entries in results log <= %s\nFound %s\nResults Log: %s",
                        operationCountLower( configuration.operationCount() ),
                        operationCountUpper( configuration.operationCount() ),
                        resultsLogSize,
                        resultsDirectory.getResultsLogFile( false ).getAbsolutePath()
                ),
                resultsLogSize,
                allOf(
                        greaterThanOrEqualTo( operationCountLower( configuration.operationCount() ) ),
                        lessThanOrEqualTo( operationCountUpper( configuration.operationCount() ) )
                )
        );
    }

    public void shouldAssignMonotonicallyIncreasingScheduledStartTimesToOperations(DriverConfiguration configuration, File temporaryFolder) throws Exception
    {
        GeneratorFactory gf = new GeneratorFactory( new RandomDataGeneratorFactory( 42L ) );
        configuration = withSkip( withWarmup( withTempResultDirs( configuration, temporaryFolder ) ) );

        Workload workload =
                    new ClassNameWorkloadFactory( configuration.workloadClassName() ).createWorkload();
        workload.init( configuration );

        List<Operation> operations = Lists.newArrayList(
                gf.limit(
                        WorkloadStreams.mergeSortedByStartTimeExcludingChildOperationGenerators(
                                gf,
                                workload.streams( gf, true )
                        ),
                        configuration.operationCount()
                )
        );

        Operation prevOperation = operations.get( 0 );
        long prevOperationScheduledStartTime = prevOperation.scheduledStartTimeAsMilli() - 1;
        for ( Operation operation : operations )
        {
            assertTrue(
                operation.scheduledStartTimeAsMilli() >= prevOperationScheduledStartTime ,
                format("Operation %s has lower start time than %s", operation, prevOperation));
            prevOperationScheduledStartTime = operation.scheduledStartTimeAsMilli();
            prevOperation = operation;
        }
    }

    public void shouldRunWorkload(DriverConfiguration configuration, File temporaryFolder) throws Exception
    {
        configuration = withSkip( withWarmup( withTempResultDirs( configuration, temporaryFolder ) ) );
        ResultsDirectory resultsDirectory = new ResultsDirectory( configuration );

        for ( File file : resultsDirectory.expectedFiles() )
        {
            assertFalse( 
                file.exists(),
                format( "Did not expect file to exist %s", file.getAbsolutePath() ) );
        }

        Client client = new Client();
        ControlService controlService = new LocalControlService(
                timeSource.nowAsMilli(),
                configuration,
                new Log4jLoggingServiceFactory( false ),
                timeSource
        );
        ClientMode clientMode = client.getClientModeFor( controlService );
        clientMode.init();
        clientMode.startExecutionAndAwaitCompletion();

        for ( File file : resultsDirectory.expectedFiles() )
        {
            assertTrue( file.exists() );
        }
        assertThat( resultsDirectory.expectedFiles(), equalTo( resultsDirectory.files() ) );

        if ( configuration.warmupCount() > 0 )
        {
            long resultsLogSize = resultsDirectory.getResultsLogFileLength( true );
            assertThat(
                    format( "Expected %s <= entries in results log <= %s\nFound %s\nResults Log: %s",
                            operationCountLower( configuration.warmupCount() ),
                            operationCountUpper( configuration.warmupCount() ),
                            resultsLogSize,
                            resultsDirectory.getResultsLogFile( true ).getAbsolutePath()
                    ),
                    resultsLogSize,
                    allOf(
                            greaterThanOrEqualTo( operationCountLower( configuration.warmupCount() ) ),
                            lessThanOrEqualTo( operationCountUpper( configuration.warmupCount() ) )
                    )
            );
        }
        long resultsLogSize = resultsDirectory.getResultsLogFileLength( false );
        assertThat(
                format( "Expected %s <= entries in results log <= %s\nFound %s\nResults Log: %s",
                        operationCountLower( configuration.operationCount() ),
                        operationCountUpper( configuration.operationCount() ),
                        resultsLogSize,
                        resultsDirectory.getResultsLogFile( false ).getAbsolutePath()
                ),
                resultsLogSize,
                allOf(
                        greaterThanOrEqualTo( operationCountLower( configuration.operationCount() ) ),
                        lessThanOrEqualTo( operationCountUpper( configuration.operationCount() ) )
                )
        );
    }

    public void shouldCreateValidationParametersThenUseThemToPerformDatabaseValidationThenPass(DriverConfiguration configuration, File temporaryFolder) throws Exception
    {
        configuration = withSkip( withWarmup( withTempResultDirs( withMode(configuration, "create_validation" ), temporaryFolder ) ) );
        // **************************************************
        // where validation parameters should be written (ensure file does not yet exist)
        // **************************************************
        File validationParamsFile = new File(temporaryFolder, "validation.csv");
        assertThat( validationParamsFile.length(), is( 0l ) );

        configuration = configuration.applyArg(ConsoleAndFileDriverConfiguration.DB_VALIDATION_FILE_PATH_ARG, validationParamsFile.getAbsolutePath());
        configuration = configuration.applyArg(ConsoleAndFileDriverConfiguration.VALIDATION_PARAMS_SIZE_ARG, Integer.toString(500));

        ResultsDirectory resultsDirectory = new ResultsDirectory( configuration );

        for ( File file : resultsDirectory.expectedFiles() )
        {
            assertFalse( file.exists(), format( "Did not expect file to exist %s", file.getAbsolutePath() ) );
        }

        // **************************************************
        // create validation parameters file
        // **************************************************
        Client clientForValidationFileCreation = new Client();
        ControlService controlService = new LocalControlService(
                timeSource.nowAsMilli(),
                configuration,
                new Log4jLoggingServiceFactory( false ),
                timeSource
        );
        ClientMode clientModeForValidationFileCreation =
                clientForValidationFileCreation.getClientModeFor( controlService );
        clientModeForValidationFileCreation.init();
        clientModeForValidationFileCreation.startExecutionAndAwaitCompletion();

        // **************************************************
        // check that validation file creation worked
        // **************************************************
        assertTrue( validationParamsFile.length() > 0 );

        // **************************************************
        // configuration for using validation parameters file to validate the database
        // **************************************************
        configuration = configuration.applyArg(ConsoleAndFileDriverConfiguration.MODE_ARG, "validate_database");
        configuration = configuration
                .applyArg(
                        ConsoleAndFileDriverConfiguration.DB_VALIDATION_FILE_PATH_ARG,
                        validationParamsFile.getAbsolutePath()
                );

        // **************************************************
        // validate the database
        // **************************************************
        Client clientForDatabaseValidation = new Client();
        controlService = new LocalControlService(
                timeSource.nowAsMilli(),
                configuration,
                new Log4jLoggingServiceFactory( false ),
                timeSource
        );
        ValidateDatabaseMode clientModeForDatabaseValidation =
                (ValidateDatabaseMode) clientForDatabaseValidation.getClientModeFor( controlService );
        clientModeForDatabaseValidation.init();
        DbValidationResult dbValidationResult =
                clientModeForDatabaseValidation.startExecutionAndAwaitCompletion();

        // **************************************************
        // check that validation was successful
        // **************************************************
        assertTrue( validationParamsFile.length() > 0 );
        assertThat( dbValidationResult, is( notNullValue() ) );
        assertTrue( 
                dbValidationResult.isSuccessful(),
                format( "Validation with following error\n%s", dbValidationResult.resultMessage() ) );
    }

    public void shouldPassWorkloadValidation(DriverConfiguration configuration, File temporaryFolder) throws Exception
    {
        configuration = withSkip( withWarmup( withTempResultDirs( configuration, temporaryFolder ) ) );
        WorkloadValidator workloadValidator = new WorkloadValidator();
        WorkloadValidationResult workloadValidationResult = workloadValidator.validate(
                new ClassNameWorkloadFactory( configuration.workloadClassName() ),
                configuration,
                new Log4jLoggingServiceFactory( true )
        );
        assertTrue( workloadValidationResult.isSuccessful(),workloadValidationResult.errorMessage() );
    }

    // TODO add tests related to the results log tolerances that are provided by the workload

    public static final double LOWER_PERCENT = 0.9;
    public static final double UPPER_PERCENT = 1.1;
    public static final long DIFFERENCE_ABSOLUTE = 50;

    public static long operationCountLower( long operationCount )
    {
        return Math.min(
                percent( operationCount, LOWER_PERCENT ),
                operationCount - DIFFERENCE_ABSOLUTE
        );
    }

    public static long operationCountUpper( long operationCount )
    {
        return Math.max(
                percent( operationCount, UPPER_PERCENT ),
                operationCount + DIFFERENCE_ABSOLUTE
        );
    }

    public static long percent( long value, double percent )
    {
        return Math.round( value * percent );
    }
}
