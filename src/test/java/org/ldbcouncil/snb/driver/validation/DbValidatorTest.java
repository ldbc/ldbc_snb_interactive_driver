package org.ldbcouncil.snb.driver.validation;

import com.google.common.collect.Lists;
import org.ldbcouncil.snb.driver.Db;
import org.ldbcouncil.snb.driver.DbException;
import org.ldbcouncil.snb.driver.Workload;
import org.ldbcouncil.snb.driver.WorkloadException;
import org.ldbcouncil.snb.driver.control.ConsoleAndFileDriverConfiguration;
import org.ldbcouncil.snb.driver.control.DriverConfigurationException;
import org.ldbcouncil.snb.driver.control.Log4jLoggingServiceFactory;
import org.ldbcouncil.snb.driver.control.LoggingService;
import org.ldbcouncil.snb.driver.generator.GeneratorFactory;
import org.ldbcouncil.snb.driver.generator.RandomDataGeneratorFactory;
import org.ldbcouncil.snb.driver.testutils.TestUtils;
import org.ldbcouncil.snb.driver.util.MapUtils;
import org.ldbcouncil.snb.driver.workloads.interactive.LdbcSnbInteractiveWorkload;
import org.ldbcouncil.snb.driver.workloads.interactive.LdbcSnbInteractiveWorkloadConfiguration;
import org.ldbcouncil.snb.driver.workloads.interactive.db.DummyLdbcSnbInteractiveDb;
import org.ldbcouncil.snb.driver.workloads.interactive.db.DummyLdbcSnbInteractiveOperationInstances;
import org.ldbcouncil.snb.driver.workloads.interactive.db.DummyLdbcSnbInteractiveOperationResultInstances;
import org.ldbcouncil.snb.driver.workloads.interactive.db.DummyLdbcSnbInteractiveOperationResultSets;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcNoResult;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery14a;
import org.ldbcouncil.snb.driver.workloads.interactive.queries.LdbcQuery14Result;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DbValidatorTest
{
    @Test
    public void shouldFailValidationWhenDbImplementationIsIncorrect()
            throws DbException, WorkloadException, IOException, DriverConfigurationException
    {
        // Given
        LoggingService loggingService = new Log4jLoggingServiceFactory( false ).loggingServiceFor( "Test" );
        long operationCount = 1;
        String dbClassName = DummyLdbcSnbInteractiveDb.class.getName();
        String workloadClassName = LdbcSnbInteractiveWorkload.class.getName();
        ConsoleAndFileDriverConfiguration configuration =
                ConsoleAndFileDriverConfiguration.fromDefaults( dbClassName, workloadClassName, operationCount );

        Map<String,String> paramsMap = LdbcSnbInteractiveWorkloadConfiguration.defaultConfigSF1();
        paramsMap.put( LdbcSnbInteractiveWorkloadConfiguration.PARAMETERS_DIRECTORY,
                TestUtils.getResource( "/snb/interactive/" ).getAbsolutePath() );
        paramsMap.put( LdbcSnbInteractiveWorkloadConfiguration.UPDATES_DIRECTORY,
                TestUtils.getResource( "/snb/interactive/" ).getAbsolutePath() );
        configuration = (ConsoleAndFileDriverConfiguration) configuration.applyArgs( paramsMap );

        Workload workload = new LdbcSnbInteractiveWorkload();
        workload.init( configuration );

        GeneratorFactory gf = new GeneratorFactory( new RandomDataGeneratorFactory( 42l ) );
        List<ValidationParam> correctValidationParamsList =
                Lists.newArrayList( gf.limit( gf.repeating( buildParams().iterator() ), 10000 ) );

        LdbcQuery14a operation14 = DummyLdbcSnbInteractiveOperationInstances.read14a();
        List<LdbcQuery14Result> unexpectedResult14 = DummyLdbcSnbInteractiveOperationResultSets.read14Results();
        unexpectedResult14.add( DummyLdbcSnbInteractiveOperationResultInstances.read14Result() );

        ValidationParam unexpectedValidationParam14 = ValidationParam.createTyped( operation14, unexpectedResult14 );
        correctValidationParamsList.add( unexpectedValidationParam14 );

        Iterator<ValidationParam> validationParams = correctValidationParamsList.iterator();
        Db db = new DummyLdbcSnbInteractiveDb();
        db.init(
                new HashMap<String,String>(),
                loggingService,
                workload.operationTypeToClassMapping()
        );
        DbValidator dbValidator = new DbValidator();

        // When
        DbValidationResult validationResult = dbValidator.validate(
                validationParams,
                db,
                correctValidationParamsList.size(),
                workload
        );

        // Then
        System.out.println( validationResult.resultMessage() );
        assertThat( validationResult.isSuccessful(), is( false ) );
    }

    @Test
    public void shouldPassValidationWhenDbImplementationIsCorrect()
            throws WorkloadException, DbException, IOException, DriverConfigurationException
    {
        // Given
        LoggingService loggingService = new Log4jLoggingServiceFactory( false ).loggingServiceFor( "Test" );
        long operationCount = 1;
        ConsoleAndFileDriverConfiguration configuration = ConsoleAndFileDriverConfiguration.fromDefaults(
                DummyLdbcSnbInteractiveDb.class.getName(),
                LdbcSnbInteractiveWorkload.class.getName(),
                operationCount
        );

        Map<String,String> paramsMap = LdbcSnbInteractiveWorkloadConfiguration.defaultConfigSF1();
        paramsMap.put( LdbcSnbInteractiveWorkloadConfiguration.PARAMETERS_DIRECTORY,
                TestUtils.getResource( "/snb/interactive/" ).getAbsolutePath() );
        paramsMap.put( LdbcSnbInteractiveWorkloadConfiguration.UPDATES_DIRECTORY,
                TestUtils.getResource( "/snb/interactive/" ).getAbsolutePath() );
        configuration = (ConsoleAndFileDriverConfiguration) configuration.applyArgs( paramsMap );

        Workload workload = new LdbcSnbInteractiveWorkload();
        workload.init( configuration );

        GeneratorFactory gf = new GeneratorFactory( new RandomDataGeneratorFactory( 42l ) );
        Iterator<ValidationParam> validationParams = gf.limit(
                gf.repeating( buildParams().iterator() ),
                10000
        );

        Db db = new DummyLdbcSnbInteractiveDb();
        db.init(
                new HashMap<String,String>(),
                loggingService,
                workload.operationTypeToClassMapping()
        );

        DbValidator dbValidator = new DbValidator();

        // When
        DbValidationResult validationResult = dbValidator.validate(
                validationParams,
                db,
                10000,
                workload
        );

        // Then
        System.out.println( validationResult.resultMessage() );
        assertThat( format( "Validation Result\n%s", validationResult.resultMessage() ),
                validationResult.isSuccessful(), is( true ) );
    }

    List<ValidationParam> buildParams()
    {
        ValidationParam validationParamLong1 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read1(),
                DummyLdbcSnbInteractiveOperationResultSets.read1Results()
        );

        ValidationParam validationParamLong2 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read2(),
                DummyLdbcSnbInteractiveOperationResultSets.read2Results()
        );

        ValidationParam validationParamLong3a = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read3a(),
                DummyLdbcSnbInteractiveOperationResultSets.read3Results()
        );
        ValidationParam validationParamLong3b = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read3b(),
                DummyLdbcSnbInteractiveOperationResultSets.read3Results()
        );
        ValidationParam validationParamLong4 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read4(),
                DummyLdbcSnbInteractiveOperationResultSets.read4Results()
        );

        ValidationParam validationParamLong5 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read5(),
                DummyLdbcSnbInteractiveOperationResultSets.read5Results()
        );

        ValidationParam validationParamLong6 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read6(),
                DummyLdbcSnbInteractiveOperationResultSets.read6Results()
        );

        ValidationParam validationParamLong7 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read7(),
                DummyLdbcSnbInteractiveOperationResultSets.read7Results()
        );

        ValidationParam validationParamLong8 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read8(),
                DummyLdbcSnbInteractiveOperationResultSets.read8Results()
        );

        ValidationParam validationParamLong9 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read9(),
                DummyLdbcSnbInteractiveOperationResultSets.read9Results()
        );

        ValidationParam validationParamLong10 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read10(),
                DummyLdbcSnbInteractiveOperationResultSets.read10Results()
        );

        ValidationParam validationParamLong11 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read11(),
                DummyLdbcSnbInteractiveOperationResultSets.read11Results()
        );

        ValidationParam validationParamLong12 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read12(),
                DummyLdbcSnbInteractiveOperationResultSets.read12Results()
        );

        ValidationParam validationParamLong13a = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read13a(),
                DummyLdbcSnbInteractiveOperationResultInstances.read13Result()
        );
        ValidationParam validationParamLong13b = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read13b(),
                DummyLdbcSnbInteractiveOperationResultInstances.read13Result()
        );
        ValidationParam validationParamLong14a = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read14a(),
                DummyLdbcSnbInteractiveOperationResultSets.read14Results()
        );
        ValidationParam validationParamLong14b = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.read14b(),
                DummyLdbcSnbInteractiveOperationResultSets.read14Results()
        );
        ValidationParam validationParamShort1 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.short1(),
                DummyLdbcSnbInteractiveOperationResultSets.short1Results()
        );

        ValidationParam validationParamShort2 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.short2(),
                DummyLdbcSnbInteractiveOperationResultSets.short2Results()
        );

        ValidationParam validationParamShort3 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.short3(),
                DummyLdbcSnbInteractiveOperationResultSets.short3Results()
        );

        ValidationParam validationParamShort4 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.short4(),
                DummyLdbcSnbInteractiveOperationResultSets.short4Results()
        );

        ValidationParam validationParamShort5 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.short5(),
                DummyLdbcSnbInteractiveOperationResultSets.short5Results()
        );

        ValidationParam validationParamShort6 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.short6(),
                DummyLdbcSnbInteractiveOperationResultSets.short6Results()
        );

        ValidationParam validationParamShort7 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.short7(),
                DummyLdbcSnbInteractiveOperationResultSets.short7Results()
        );

        ValidationParam validationParamWrite1 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write1(),
                LdbcNoResult.INSTANCE
        );

        ValidationParam validationParamWrite2 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write2(),
                LdbcNoResult.INSTANCE
        );

        ValidationParam validationParamWrite3 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write3(),
                LdbcNoResult.INSTANCE
        );

        ValidationParam validationParamWrite4 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write4(),
                LdbcNoResult.INSTANCE
        );

        ValidationParam validationParamWrite5 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write5(),
                LdbcNoResult.INSTANCE
        );

        ValidationParam validationParamWrite6 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write6(),
                LdbcNoResult.INSTANCE
        );

        ValidationParam validationParamWrite7 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write7(),
                LdbcNoResult.INSTANCE
        );

        ValidationParam validationParamWrite8 = ValidationParam.createTyped(
                DummyLdbcSnbInteractiveOperationInstances.write8(),
                LdbcNoResult.INSTANCE
        );

        return Lists.newArrayList(
                validationParamLong1,
                validationParamLong2,
                validationParamLong3a,
                validationParamLong3b,
                validationParamLong4,
                validationParamLong5,
                validationParamLong6,
                validationParamLong7,
                validationParamLong8,
                validationParamLong9,
                validationParamLong10,
                validationParamLong11,
                validationParamLong12,
                validationParamLong13a,
                validationParamLong13b,
                validationParamLong14a,
                validationParamLong14b,
                validationParamShort1,
                validationParamShort2,
                validationParamShort3,
                validationParamShort4,
                validationParamShort5,
                validationParamShort6,
                validationParamShort7,
                validationParamWrite1,
                validationParamWrite2,
                validationParamWrite3,
                validationParamWrite4,
                validationParamWrite5,
                validationParamWrite6,
                validationParamWrite7,
                validationParamWrite8
        );
    }
}
