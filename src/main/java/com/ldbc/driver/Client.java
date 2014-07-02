package com.ldbc.driver;

import com.google.common.collect.Lists;
import com.ldbc.driver.control.ConcurrentControlService;
import com.ldbc.driver.control.ConsoleAndFileDriverConfiguration;
import com.ldbc.driver.control.DriverConfigurationException;
import com.ldbc.driver.control.LocalControlService;
import com.ldbc.driver.generator.GeneratorFactory;
import com.ldbc.driver.runtime.ConcurrentErrorReporter;
import com.ldbc.driver.runtime.WorkloadRunner;
import com.ldbc.driver.runtime.coordination.CompletionTimeException;
import com.ldbc.driver.runtime.coordination.ConcurrentCompletionTimeService;
import com.ldbc.driver.runtime.coordination.NaiveSynchronizedConcurrentCompletionTimeService;
import com.ldbc.driver.runtime.metrics.*;
import com.ldbc.driver.temporal.Duration;
import com.ldbc.driver.temporal.SystemTimeSource;
import com.ldbc.driver.temporal.Time;
import com.ldbc.driver.temporal.TimeSource;
import com.ldbc.driver.util.ClassLoaderHelper;
import com.ldbc.driver.util.CsvFileReader;
import com.ldbc.driver.util.CsvFileWriter;
import com.ldbc.driver.util.RandomDataGeneratorFactory;
import com.ldbc.driver.validation.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

// TODO replace log4j with some interface like StatusReportingService

public class Client {
    private static Logger logger = Logger.getLogger(Client.class);

    private static final long RANDOM_SEED = 42;

    public static void main(String[] args) throws ClientException {
        try {
            TimeSource systemTimeSource = new SystemTimeSource();
            ConsoleAndFileDriverConfiguration configuration = ConsoleAndFileDriverConfiguration.fromArgs(args);
            // TODO this method will not work with multiple processes - should come from controlService
            Time workloadStartTime = systemTimeSource.now().plus(Duration.fromSeconds(10));
            ConcurrentControlService controlService = new LocalControlService(workloadStartTime, configuration);
            Client client = new Client(controlService, systemTimeSource);
            client.start();
        } catch (DriverConfigurationException e) {
            String errMsg = String.format("Error parsing parameters: %s", e.getMessage());
            logger.error(errMsg);
        } catch (Exception e) {
            logger.error("Client terminated unexpectedly\n" + ConcurrentErrorReporter.stackTraceToString(e));
        }
    }

    // check results
    private DbValidator.DbValidationResult databaseValidationResult = null;
    private WorkloadValidator.WorkloadValidationResult workloadValidationResult = null;
    private WorkloadStatistics workloadStatistics = null;

    private final ClientMode clientMode;

    public Client(ConcurrentControlService controlService, TimeSource timeSource) throws ClientException {
        if (controlService.configuration().shouldPrintHelpString())
            clientMode = new PrintHelpMode(controlService);
            // Print Help
        else if (null != controlService.configuration().validationParamsCreationOptions())
            // Create Validation Parameters
            clientMode = new CreateValidationParamsMode(controlService);
        else if (null != controlService.configuration().databaseValidationFilePath())
            // Validate Database
            clientMode = new ValidateDatabaseMode(controlService);
        else if (controlService.configuration().validateWorkload())
            // Validate Workload
            clientMode = new ValidateWorkloadMode(controlService);
        else if (controlService.configuration().calculateWorkloadStatistics())
            // Calculate Statistics
            clientMode = new CalculateWorkloadStatisticsMode(controlService);
        else
            // Execute Workload
            clientMode = new ExecuteWorkloadMode(controlService, timeSource);

        clientMode.init();
    }

    public void start() throws ClientException {
        clientMode.execute();
    }

    public DbValidator.DbValidationResult databaseValidationResult() {
        return databaseValidationResult;
    }

    public WorkloadValidator.WorkloadValidationResult workloadValidationResult() {
        return workloadValidationResult;
    }

    public WorkloadStatistics workloadStatistics() {
        return workloadStatistics;
    }

    private interface ClientMode {
        void init() throws ClientException;

        void execute() throws ClientException;
    }

    private class ExecuteWorkloadMode implements ClientMode {
        private final ConcurrentControlService controlService;
        private final TimeSource timeSource;

        private Workload workload = null;
        private Db db = null;
        private ConcurrentMetricsService metricsService = null;
        private ConcurrentCompletionTimeService completionTimeService = null;
        private WorkloadRunner workloadRunner = null;

        ExecuteWorkloadMode(ConcurrentControlService controlService, TimeSource timeSource) throws ClientException {
            this.controlService = controlService;
            this.timeSource = timeSource;
        }

        @Override
        public void init() throws ClientException {
            try {
                workload = ClassLoaderHelper.loadWorkload(controlService.configuration().workloadClassName());
                workload.init(controlService.configuration());
            } catch (Exception e) {
                throw new ClientException(String.format("Error loading Workload class: %s", controlService.configuration().workloadClassName()), e);
            }
            logger.info(String.format("Loaded Workload: %s", workload.getClass().getName()));

            try {
                db = ClassLoaderHelper.loadDb(controlService.configuration().dbClassName());
                db.init(controlService.configuration().asMap());
            } catch (DbException e) {
                throw new ClientException(String.format("Error loading DB class: %s", controlService.configuration().dbClassName()), e);
            }
            logger.info(String.format("Loaded DB: %s", db.getClass().getName()));

            ConcurrentErrorReporter errorReporter = new ConcurrentErrorReporter();

            try {
                // TODO threaded may scale better with >8 cores, but consumes more resources & performs worse with <8 cores
                // completionTimeService = new ThreadedQueuedConcurrentCompletionTimeService(controlService.configuration().peerIds(), errorReporter);
                completionTimeService = new NaiveSynchronizedConcurrentCompletionTimeService(controlService.configuration().peerIds());
                completionTimeService.submitInitiatedTime(controlService.workloadStartTime());
                completionTimeService.submitCompletedTime(controlService.workloadStartTime());
                for (String peerId : controlService.configuration().peerIds()) {
                    completionTimeService.submitExternalCompletionTime(peerId, controlService.workloadStartTime());
                }
                // Wait for workloadStartTime to be applied
                Future<Time> globalCompletionTimeFuture = completionTimeService.globalCompletionTimeFuture();
                while (false == globalCompletionTimeFuture.isDone()) {
                    if (errorReporter.errorEncountered())
                        throw new WorkloadException(String.format("Encountered error while waiting for GCT to initialize. Driver terminating.\n%s", errorReporter.toString()));
                }
                if (false == globalCompletionTimeFuture.get().equals(controlService.workloadStartTime())) {
                    throw new WorkloadException("Completion Time future failed to return expected value");
                }
            } catch (Exception e) {
                throw new ClientException(
                        String.format("Error while instantiating Completion Time Service with peer IDs %s", controlService.configuration().peerIds().toString()), e);
            }

            metricsService = new ThreadedQueuedConcurrentMetricsService(timeSource, errorReporter, controlService.configuration().timeUnit());
            GeneratorFactory generators = new GeneratorFactory(new RandomDataGeneratorFactory(RANDOM_SEED));

            logger.info(String.format("Retrieving operation stream for workload: %s", workload.getClass().getSimpleName()));
            Iterator<Operation<?>> timeMappedOperations;
            try {
                Iterator<Operation<?>> operations = workload.operations(generators);
                timeMappedOperations = generators.timeOffsetAndCompress(
                        operations,
                        controlService.workloadStartTime(),
                        controlService.configuration().timeCompressionRatio());
            } catch (WorkloadException e) {
                throw new ClientException("Error while retrieving operation stream for workload", e);
            }

            // TODO add method of selecting appropriate window size
            // TODO if GCT Delta is very large it makes sense to have a smaller window
            // TODO otherwise operations are spread very far apart

            logger.info(String.format("Instantiating %s", WorkloadRunner.class.getSimpleName()));
            try {
                workloadRunner = new WorkloadRunner(
                        timeSource,
                        db,
                        timeMappedOperations,
                        workload.operationClassifications(),
                        metricsService,
                        errorReporter,
                        completionTimeService,
                        controlService.configuration().threadCount(),
                        controlService.configuration().showStatus(),
                        controlService.workloadStartTime(),
                        controlService.configuration().toleratedExecutionDelay(),
                        controlService.configuration().spinnerSleepDuration(),
                        controlService.configuration().compressedGctDeltaDuration());
            } catch (WorkloadException e) {
                throw new ClientException(String.format("Error instantiating %s", WorkloadRunner.class.getSimpleName()), e);
            }
            logger.info(String.format("Instantiated %s - Starting Benchmark (%s operations)", WorkloadRunner.class.getSimpleName(), controlService.configuration().operationCount()));

            logger.info("Driver Configuration");
            logger.info(controlService.toString());
        }

        @Override
        public void execute() throws ClientException {
            // TODO revise if this necessary here, and if not where??
            controlService.waitForCommandToExecuteWorkload();

            try {
                workloadRunner.executeWorkload();
            } catch (WorkloadException e) {
                throw new ClientException("Error running Workload", e);
            }

            // TODO revise if this necessary here, and if not where??
            controlService.waitForAllToCompleteExecutingWorkload();

            logger.info("Cleaning up Workload...");
            try {
                workload.cleanup();
            } catch (WorkloadException e) {
                String errMsg = "Error during Workload cleanup";
                throw new ClientException(errMsg, e);
            }

            logger.info("Cleaning up DB...");
            try {
                db.cleanup();
            } catch (DbException e) {
                throw new ClientException("Error during DB cleanup", e);
            }

            logger.info("Shutting down completion time service...");
            try {
                completionTimeService.shutdown();
            } catch (CompletionTimeException e) {
                throw new ClientException("Error during shutdown of completion time service", e);
            }

            logger.info("Shutting down metrics collection service...");
            WorkloadResultsSnapshot workloadResults;
            try {
                workloadResults = metricsService.results();
                metricsService.shutdown();
            } catch (MetricsCollectionException e) {
                throw new ClientException("Error during shutdown of metrics collection service", e);
            }

            logger.info(String.format("Runtime: %s", workloadResults.totalRunDuration()));

            logger.info("Exporting workload metrics...");
            try {
                MetricsManager.export(workloadResults, new SimpleOperationMetricsFormatter(), System.out, MetricsManager.DEFAULT_CHARSET);
                if (null != controlService.configuration().resultFilePath()) {
                    File resultFile = new File(controlService.configuration().resultFilePath());
                    MetricsManager.export(workloadResults, new JsonOperationMetricsFormatter(), new FileOutputStream(resultFile), MetricsManager.DEFAULT_CHARSET);
                }
                controlService.shutdown();
            } catch (MetricsCollectionException e) {
                throw new ClientException("Could not export workload metrics", e);
            } catch (FileNotFoundException e) {
                throw new ClientException(
                        String.format("Error encountered while trying to write result file: %s", controlService.configuration().resultFilePath()), e);
            }
        }
    }

    private class CalculateWorkloadStatisticsMode implements ClientMode {
        private final ConcurrentControlService controlService;

        private Workload workload = null;
        private Iterator<Operation<?>> timeMappedOperations = null;

        CalculateWorkloadStatisticsMode(ConcurrentControlService controlService) throws ClientException {
            this.controlService = controlService;
        }

        @Override
        public void init() throws ClientException {
            try {
                workload = ClassLoaderHelper.loadWorkload(controlService.configuration().workloadClassName());
                workload.init(controlService.configuration());
            } catch (Exception e) {
                throw new ClientException(String.format("Error loading Workload class: %s", controlService.configuration().workloadClassName()), e);
            }
            logger.info(String.format("Loaded Workload: %s", workload.getClass().getName()));

            GeneratorFactory generators = new GeneratorFactory(new RandomDataGeneratorFactory(RANDOM_SEED));

            logger.info(String.format("Retrieving operation stream for workload: %s", workload.getClass().getSimpleName()));
            try {
                Iterator<Operation<?>> operations = workload.operations(generators);
                timeMappedOperations = generators.timeOffsetAndCompress(
                        operations,
                        controlService.workloadStartTime(),
                        controlService.configuration().timeCompressionRatio());
            } catch (WorkloadException e) {
                throw new ClientException("Error while retrieving operation stream for workload", e);
            }

            logger.info("Driver Configuration");
            logger.info(controlService.toString());
        }

        @Override
        public void execute() throws ClientException {
            logger.info(String.format("Calculating workload statistics for: %s", workload.getClass().getSimpleName()));
            try {
                WorkloadStatisticsCalculator workloadStatisticsCalculator = new WorkloadStatisticsCalculator();
                workloadStatistics = workloadStatisticsCalculator.calculate(
                        timeMappedOperations,
                        workload.operationClassifications(),
                        WorkloadValidator.DEFAULT_MAX_EXPECTED_INTERLEAVE);
                logger.info("Calculation complete\n" + workloadStatistics);
            } catch (MetricsCollectionException e) {
                throw new ClientException("Error while calculating workload statistics", e);
            }

            logger.info("Cleaning up Workload...");
            try {
                workload.cleanup();
            } catch (WorkloadException e) {
                String errMsg = "Error during Workload cleanup";
                throw new ClientException(errMsg, e);
            }
        }
    }

    private class CreateValidationParamsMode implements ClientMode {
        private final ConcurrentControlService controlService;

        private Workload workload = null;
        private Db db = null;
        private Iterator<Operation<?>> timeMappedOperations = null;

        CreateValidationParamsMode(ConcurrentControlService controlService) throws ClientException {
            this.controlService = controlService;
        }

        @Override
        public void init() throws ClientException {
            try {
                workload = ClassLoaderHelper.loadWorkload(controlService.configuration().workloadClassName());
                workload.init(controlService.configuration());
            } catch (Exception e) {
                throw new ClientException(String.format("Error loading Workload class: %s", controlService.configuration().workloadClassName()), e);
            }
            logger.info(String.format("Loaded Workload: %s", workload.getClass().getName()));

            try {
                db = ClassLoaderHelper.loadDb(controlService.configuration().dbClassName());
                db.init(controlService.configuration().asMap());
            } catch (DbException e) {
                throw new ClientException(String.format("Error loading DB class: %s", controlService.configuration().dbClassName()), e);
            }
            logger.info(String.format("Loaded DB: %s", db.getClass().getName()));

            GeneratorFactory generators = new GeneratorFactory(new RandomDataGeneratorFactory(RANDOM_SEED));

            logger.info(String.format("Retrieving operation stream for workload: %s", workload.getClass().getSimpleName()));
            try {
                Iterator<Operation<?>> operations = workload.operations(generators);
                timeMappedOperations = generators.timeOffsetAndCompress(
                        operations,
                        controlService.workloadStartTime(),
                        controlService.configuration().timeCompressionRatio());
            } catch (WorkloadException e) {
                throw new ClientException("Error while retrieving operation stream for workload", e);
            }

            logger.info("Driver Configuration");
            logger.info(controlService.toString());
        }

        @Override
        public void execute() throws ClientException {
            File validationFileToGenerate = new File(controlService.configuration().validationParamsCreationOptions().filePath());
            int validationSetSize = controlService.configuration().validationParamsCreationOptions().validationSetSize();
            // TODO get from config parameter
            boolean performSerializationMarshallingChecks = true;

            logger.info(String.format("Generating database validation file: %s", validationFileToGenerate.getAbsolutePath()));

            List<Operation<?>> timeMappedOperationsList = Lists.newArrayList(timeMappedOperations);

            Iterator<ValidationParam> validationParamsGenerator =
                    new ValidationParamsGenerator(db, workload, timeMappedOperationsList.iterator(), validationSetSize);

            Iterator<String[]> csvRows =
                    new ValidationParamsToCsvRows(validationParamsGenerator, workload, performSerializationMarshallingChecks);

            CsvFileWriter csvFileWriter;
            try {
                csvFileWriter = new CsvFileWriter(validationFileToGenerate, CsvFileWriter.DEFAULT_COLUMN_SEPARATOR_STRING);
            } catch (IOException e) {
                throw new ClientException("Error encountered trying to open CSV file writer", e);
            }

            try {
                csvFileWriter.writeRows(csvRows);
            } catch (IOException e) {
                throw new ClientException("Error encountered trying to write validation parameters to CSV file writer", e);
            }

            try {
                csvFileWriter.close();
            } catch (IOException e) {
                throw new ClientException("Error encountered trying to close CSV file writer", e);
            }

            int validationParametersGenerated = ((ValidationParamsGenerator) validationParamsGenerator).entriesWrittenSoFar();

            timeMappedOperations = timeMappedOperationsList.iterator();
            logger.info(String.format("Successfully generated %s database validation parameters", validationParametersGenerated));

            logger.info("Cleaning up Workload...");
            try {
                workload.cleanup();
            } catch (WorkloadException e) {
                String errMsg = "Error during Workload cleanup";
                throw new ClientException(errMsg, e);
            }

            logger.info("Cleaning up DB...");
            try {
                db.cleanup();
            } catch (DbException e) {
                throw new ClientException("Error during DB cleanup", e);
            }
        }
    }

    private class ValidateDatabaseMode implements ClientMode {
        private final ConcurrentControlService controlService;

        private Workload workload = null;
        private Db db = null;

        ValidateDatabaseMode(ConcurrentControlService controlService) throws ClientException {
            this.controlService = controlService;
        }

        @Override
        public void init() throws ClientException {
            try {
                workload = ClassLoaderHelper.loadWorkload(controlService.configuration().workloadClassName());
                workload.init(controlService.configuration());
            } catch (Exception e) {
                throw new ClientException(String.format("Error loading Workload class: %s", controlService.configuration().workloadClassName()), e);
            }
            logger.info(String.format("Loaded Workload: %s", workload.getClass().getName()));

            try {
                db = ClassLoaderHelper.loadDb(controlService.configuration().dbClassName());
                db.init(controlService.configuration().asMap());
            } catch (DbException e) {
                throw new ClientException(String.format("Error loading DB class: %s", controlService.configuration().dbClassName()), e);
            }
            logger.info(String.format("Loaded DB: %s", db.getClass().getName()));

            logger.info("Driver Configuration");
            logger.info(controlService.toString());
        }

        @Override
        public void execute() throws ClientException {
            File validationParamsFile = new File(controlService.configuration().databaseValidationFilePath());

            logger.info(String.format("Validating database against expected results\n * Db: %s\n * Validation Params File: %s",
                    db.getClass().getName(), validationParamsFile.getAbsolutePath()));

            CsvFileReader csvFileReader;
            try {
                csvFileReader = new CsvFileReader(validationParamsFile, CsvFileWriter.DEFAULT_COLUMN_SEPARATOR_REGEX_STRING);
            } catch (IOException e) {
                throw new ClientException("Error encountered trying to create CSV file reader", e);
            }

            try {
                Iterator<ValidationParam> validationParams = new ValidationParamsFromCsvRows(csvFileReader, workload);
                DbValidator dbValidator = new DbValidator();
                databaseValidationResult = dbValidator.validate(validationParams, db);
                if (false == databaseValidationResult.isSuccessful()) {
                    throw new ClientException(String.format("Database validation failed\n%s", databaseValidationResult.errorMessage()));
                }
            } catch (WorkloadException e) {
                throw new ClientException(String.format("Error reading validation parameters file\nFile: %s", validationParamsFile.getAbsolutePath()), e);
            }

            csvFileReader.closeReader();

            logger.info("Database Validation Successful");

            logger.info("Cleaning up Workload...");
            try {
                workload.cleanup();
            } catch (WorkloadException e) {
                String errMsg = "Error during Workload cleanup";
                throw new ClientException(errMsg, e);
            }

            logger.info("Cleaning up DB...");
            try {
                db.cleanup();
            } catch (DbException e) {
                throw new ClientException("Error during DB cleanup", e);
            }
        }
    }

    private class ValidateWorkloadMode implements ClientMode {
        private final ConcurrentControlService controlService;

        private Iterator<Operation<?>> timeMappedOperations = null;
        private Workload workload = null;

        ValidateWorkloadMode(ConcurrentControlService controlService) throws ClientException {
            this.controlService = controlService;
        }

        @Override
        public void init() throws ClientException {
            try {
                workload = ClassLoaderHelper.loadWorkload(controlService.configuration().workloadClassName());
                workload.init(controlService.configuration());
            } catch (Exception e) {
                throw new ClientException(String.format("Error loading Workload class: %s", controlService.configuration().workloadClassName()), e);
            }
            logger.info(String.format("Loaded Workload: %s", workload.getClass().getName()));

            GeneratorFactory generators = new GeneratorFactory(new RandomDataGeneratorFactory(RANDOM_SEED));

            logger.info(String.format("Retrieving operation stream for workload: %s", workload.getClass().getSimpleName()));
            try {
                Iterator<Operation<?>> operations = workload.operations(generators);
                timeMappedOperations = generators.timeOffsetAndCompress(
                        operations,
                        controlService.workloadStartTime(),
                        controlService.configuration().timeCompressionRatio());
            } catch (WorkloadException e) {
                throw new ClientException("Error while retrieving operation stream for workload", e);
            }

            logger.info("Driver Configuration");
            logger.info(controlService.toString());
        }

        @Override
        public void execute() throws ClientException {
            logger.info(String.format("Validating workload: %s", workload.getClass().getSimpleName()));
            WorkloadValidator workloadValidator = new WorkloadValidator();
            workloadValidationResult = workloadValidator.validate(
                    timeMappedOperations,
                    workload.operationClassifications(),
                    WorkloadValidator.DEFAULT_MAX_EXPECTED_INTERLEAVE);
            if (false == workloadValidationResult.isSuccessful()) {
                throw new ClientException(String.format("Workload validation failed\n%s", workloadValidationResult.errorMessage()));
            }
            logger.info("Workload Validation Successful");

            logger.info("Cleaning up Workload...");
            try {
                workload.cleanup();
            } catch (WorkloadException e) {
                String errMsg = "Error during Workload cleanup";
                throw new ClientException(errMsg, e);
            }
        }
    }

    private class PrintHelpMode implements ClientMode {
        private final ConcurrentControlService controlService;

        PrintHelpMode(ConcurrentControlService controlService) {
            this.controlService = controlService;
        }

        @Override
        public void init() throws ClientException {
        }

        @Override
        public void execute() throws ClientException {
            logger.info(controlService.configuration().helpString());
        }
    }
}