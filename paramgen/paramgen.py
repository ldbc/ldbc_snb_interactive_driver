from path_selection import PathCuration
import duckdb
from datetime import timedelta, datetime
import glob
import os
from pathlib import Path
import argparse

remove_lower_times_dict = {
    "Q_1"   : "DELETE FROM Q_1   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_1   t2 WHERE t2.personId  = t1.personId  AND t2.firstName = t1.firstName);",
    "Q_2"   : "DELETE FROM Q_2   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_2   t2 WHERE t2.personId  = t1.personId  AND t2.maxDate = t1.maxDate);",
    "Q_3a"  : "DELETE FROM Q_3a  t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_3a  t2 WHERE t2.personId  = t1.personId  AND t2.countryXName = t1.countryXName AND t2.countryYName = t1.countryYName AND t2.startDate = t1.startDate AND t2.durationDays = t1.durationDays);",
    "Q_3b"  : "DELETE FROM Q_3b  t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_3b  t2 WHERE t2.personId  = t1.personId  AND t2.countryXName = t1.countryXName AND t2.countryYName = t1.countryYName AND t2.startDate = t1.startDate AND t2.durationDays = t1.durationDays);",
    "Q_4"   : "DELETE FROM Q_4   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_4   t2 WHERE t2.personId  = t1.personId  AND t2.startDate = t1.startDate AND t2.durationDays = t1.durationDays);",
    "Q_5"   : "DELETE FROM Q_5   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_5   t2 WHERE t2.personId  = t1.personId  AND t2.minDate = t1.minDate);",
    "Q_6"   : "DELETE FROM Q_6   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_6   t2 WHERE t2.personId  = t1.personId  AND t2.tagName = t1.tagName);",
    "Q_7"   : "DELETE FROM Q_7   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_7   t2 WHERE t2.personId  = t1.personId);",
    "Q_8"   : "DELETE FROM Q_8   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_8   t2 WHERE t2.personId  = t1.personId);",
    "Q_9"   : "DELETE FROM Q_9   t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_9   t2 WHERE t2.personId  = t1.personId  AND t2.maxDate = t1.maxDate);",
    "Q_10"  : "DELETE FROM Q_10  t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_10  t2 WHERE t2.personId  = t1.personId  AND t2.month = t1.month);",
    "Q_11"  : "DELETE FROM Q_11  t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_11  t2 WHERE t2.personId  = t1.personId  AND t2.countryName = t1.countryName AND t2.workFromYear = t1.workFromYear);",
    "Q_12"  : "DELETE FROM Q_12  t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_12  t2 WHERE t2.personId  = t1.personId  AND t2.tagClassName = t1.tagClassName);",
    "Q_13a" : "DELETE FROM Q_13a t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_13a t2 WHERE t2.person1Id = t1.person1Id AND t2.person2Id = t1.person2Id);",
    "Q_14a" : "DELETE FROM Q_14a t1 WHERE t1.useUntil < (SELECT max(t2.useUntil) FROM Q_14a t2 WHERE t2.person1Id = t1.person1Id AND t2.person2Id = t1.person2Id);"
}

remove_duplicates = {
    "Q_1"   : "CREATE TABLE Q_1_filtered AS SELECT personId, firstName, useFrom, useUntil FROM Q_1 GROUP BY personId, firstName, useFrom, useUntil;",
    "Q_2"   : "CREATE TABLE Q_2_filtered AS SELECT personId, maxDate, useFrom, useUntil FROM Q_2 GROUP BY personId, maxDate, useFrom, useUntil;",
    "Q_3a"   : "CREATE TABLE Q_3a_filtered AS SELECT personId, countryXName, countryYName, startDate, durationDays, useFrom, useUntil FROM Q_3a GROUP BY personId, countryXName, countryYName, startDate, durationDays, useFrom, useUntil;",
    "Q_3b"   : "CREATE TABLE Q_3b_filtered AS SELECT personId, countryXName, countryYName, startDate, durationDays, useFrom, useUntil FROM Q_3b GROUP BY personId, countryXName, countryYName, startDate, durationDays, useFrom, useUntil;",
    "Q_4"   : "CREATE TABLE Q_4_filtered AS SELECT personId, startDate, durationDays, useFrom, useUntil FROM Q_4 GROUP BY personId, startDate, durationDays, useFrom, useUntil;",
    "Q_5"   : "CREATE TABLE Q_5_filtered AS SELECT personId, minDate, useFrom, useUntil FROM Q_5 GROUP BY personId, minDate, useFrom, useUntil;",
    "Q_6"   : "CREATE TABLE Q_6_filtered AS SELECT personId, tagName, useFrom, useUntil FROM Q_6 GROUP BY personId, tagName, useFrom, useUntil;",
    "Q_7"   : "CREATE TABLE Q_7_filtered AS SELECT personId, useFrom, useUntil FROM Q_7 GROUP BY personId, useFrom, useUntil;",
    "Q_8"   : "CREATE TABLE Q_8_filtered AS SELECT personId, useFrom, useUntil FROM Q_8 GROUP BY personId, useFrom, useUntil;",
    "Q_9"   : "CREATE TABLE Q_9_filtered AS SELECT personId, maxDate, useFrom, useUntil FROM Q_9 GROUP BY personId, maxDate, useFrom, useUntil;",
    "Q_10"   : "CREATE TABLE Q_10_filtered AS SELECT personId, month, useFrom, useUntil FROM Q_10 GROUP BY personId, month, useFrom, useUntil;",
    "Q_11"   : "CREATE TABLE Q_11_filtered AS SELECT personId, countryName, workFromYear, useFrom, useUntil FROM Q_11 GROUP BY personId, countryName, workFromYear, useFrom, useUntil;",
    "Q_12"   : "CREATE TABLE Q_12_filtered AS SELECT personId, tagClassName, useFrom, useUntil FROM Q_12 GROUP BY personId, tagClassName, useFrom, useUntil;",
    "Q_13a"   : "CREATE TABLE Q_13a_filtered AS SELECT person1Id, person2Id, useFrom, useUntil FROM Q_13a GROUP BY person1Id, person2Id, useFrom, useUntil;",
    "Q_14a"   : "CREATE TABLE Q_14a_filtered AS SELECT person1Id, person2Id, useFrom, useUntil FROM Q_14a GROUP BY person1Id, person2Id, useFrom, useUntil;"
}

def generate_parameter_for_query_type(cursor, date_limit, date_start, create_tables, query_variant):
    """
    Creates parameter for given query variant.
    Args:
        - cursor (DuckDBPyConnection): cursor to the DuckDB instance
        - date_limit (datetime): The day to filter on. This date will be used to compare creation and deletion dates
        - date_start (datetime): The first day of the inserts. This is used for parameters that do not contain creation and deletion dates
        - create_tables (boolean): Whether to create tables at first run
        - query_variant (str): number of the query to generate the parameters
    """
    date_limit_string = date_limit.strftime('%Y-%m-%d')
    date_limit_long = date_limit.timestamp() * 1000
    date_start_long = date_start.timestamp() * 1000
    with open(f"paramgen-queries/pg-{query_variant}.sql", "r") as parameter_query_file:
        parameter_query = parameter_query_file.read().replace(':date_limit_filter', f'\'{date_limit_string}\'')
        parameter_query = parameter_query.replace(':date_limit_long', str(date_limit_long))
        parameter_query = parameter_query.replace(':date_start_long', str(date_start_long))
        if create_tables:
            cursor.execute(f"CREATE TABLE 'Q_{query_variant}' AS SELECT * FROM ({parameter_query});")
        cursor.execute(f"INSERT INTO 'Q_{query_variant}' SELECT * FROM ({parameter_query});")


def create_views_of_factor_tables(cursor, factor_tables_path):
    """
    Args:
        - cursor (DuckDBPyConnection): cursor to the DuckDB instance
        - factor_tables_path    (str): path to the factor tables. Only Unix paths are supported
        - preview_tables    (boolean): Whether the first five rows of the factor table should be shown.
    """

    if factor_tables_path[-1] != '*':
        if factor_tables_path[-1] != '/':
            factor_tables_path = factor_tables_path + '/*'
        else:
            factor_tables_path = factor_tables_path + '*'
    print("============ Loading the factor tables ============")
    directories = glob.glob(f'{factor_tables_path}')
    if (len(directories) == 0):
        raise ValueError(f"{factor_tables_path} is empty")
    # Create views of raw parquet files
    for directory in directories:
        path_dir = Path(directory)
        if path_dir.is_dir():
            print(f"Loading {path_dir.name}")
            cursor.execute(f"DROP VIEW IF EXISTS {path_dir.name}")
            cursor.execute(
                f"""
                CREATE VIEW {path_dir.name} AS 
                SELECT * FROM read_parquet('{str(Path(directory).absolute()) + "/*.parquet"}');
                """
            )


def generate_parameters(cursor, date_limit, date_start, end_date, window_time):
    """
    Generates paramters for all query types until end_date is reached.
    Args:
        - cursor      (DuckDBPyConnection): cursor to the DuckDB instance
        - date_limit  (datetime): The day to filter on. This date will be used to compare creation and deletion dates
        - date_start  (datetime): The first day of the inserts. This is used for parameters that do not contain creation and deletion dates
        - end_date    (datetime): The last day of the inserts and when the loop stops.
        - window_time (timedelta): 
    """
    print("Start time of initial_snapshot: " + str(date_limit))
    print("End time of initial_snapshot: " + str(end_date))
    print("Time bucket size: " + str(window_time))

    create_tables = True
    while (date_limit < end_date):
        print("============ Generating parameters ============")
        for query_variant in ["1", "2", "3a", "3b", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13a", "14a"]:
            print(f"- Q{query_variant}, date {date_limit.strftime('%Y-%m-%d')}")
            generate_parameter_for_query_type(cursor, date_limit, date_start, create_tables, query_variant)
        create_tables = False
        date_limit = date_limit + window_time


def export_parameters(cursor):
    """
    Export parameters to interactive-Q{query_variant}.parquet files
    Args:
        - cursor      (DuckDBPyConnection): cursor to the DuckDB instance
    """
    print("============ Output parameters ============")
    for query_variant in ["1", "2", "3a", "3b", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13a", "14a"]:
        print(f"- Q{query_variant} TO ../parameters/interactive-{query_variant}.parquet")
        query = remove_lower_times_dict[f"Q_{query_variant}"]#remove_duplicates
        cursor.execute(query)
        query = remove_duplicates[f"Q_{query_variant}"]#remove_duplicates
        cursor.execute(query)
        cursor.execute(f"COPY 'Q_{query_variant}_filtered' TO '../parameters/interactive-{query_variant}.parquet' WITH (FORMAT PARQUET);")

def generate_short_parameters(cursor, date_start):
    """
    Generates personIds and messageIds for manual testing of short queries
    Args:
        - cursor      (DuckDBPyConnection): cursor to the DuckDB instance
        - date_start  (datetime): The first day of the inserts. This is used for parameters that do not contain creation and deletion dates
    """
    print("============ Generate Short Query Parameters ============")
    for query_variant in ["personId", "messageId"]:
        generate_parameter_for_query_type(cursor, date_start, date_start, True, query_variant)
        print(f"- Q{query_variant} TO ../parameters/interactive-{query_variant}.parquet")
        cursor.execute(f"COPY 'Q_{query_variant}' TO '../parameters/interactive-{query_variant}.parquet' WITH (FORMAT PARQUET);")


def main(factor_tables_dir, raw_parquet_dir, start_date, end_date, time_bucket_size_in_days, generate_short_query_parameters):
    # Remove previous database if exists
    Path('scratch/paramgen.duckdb').unlink(missing_ok=True)
    cursor = duckdb.connect(database="scratch/paramgen.duckdb")

    date_start = datetime.strptime(start_date, "%Y-%m-%d")
    date_limit = date_start
    window_time = timedelta(days=time_bucket_size_in_days)
    end_date = datetime.strptime(end_date, "%Y-%m-%d")
    relative_factor_path = factor_tables_dir[:-2]
    Path(f"{relative_factor_path}/people4Hops").mkdir(parents=True, exist_ok=True)
    parquet_output_dir = f"{relative_factor_path}/people4Hops/curated_paths.parquet"

    print("============ Generate People 4 Hops ============")
    path_curation = PathCuration(raw_parquet_dir + 'graphs/parquet/raw/composite-merged-fk/', factor_tables_dir[:-2])
    path_curation.get_people_4_hops_paths('2012-11-28', '2013-01-01', 1, parquet_output_dir)

    files = glob.glob('scratch/factors/people4Hops/*')
    for f in files:
        print(f)
        if f != 'scratch/factors/people4Hops/curated_paths.parquet':
            os.remove(f)
    create_views_of_factor_tables(cursor, factor_tables_dir)

    generate_parameter_for_query_type(cursor, date_start, date_start, True, "13b")
    generate_parameter_for_query_type(cursor, date_start, date_start, True, "14b")

    generate_parameters(cursor, date_limit, date_start, end_date, window_time)
    export_parameters(cursor)

    if (generate_short_query_parameters):
        generate_short_parameters(cursor, date_start)

    # Remove temporary database
    Path('paramgen.snb.db').unlink(missing_ok=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--raw_parquet_dir',
        help="raw_parquet_dir: directory containing the raw parquet file for Person and Person_knows_Person e.g. '/data/out-sf1/graphs/parquet/raw/'",
        type=str,
        required=True
    )
    parser.add_argument(
        '--factor_tables_dir',
        help="factor_tables_dir: directory containing the factor tables e.g. '/data/out-sf1'",
        type=str,
        default='factors/',
        required=False
    )
    parser.add_argument(
        '--start_date',
        help="start_date: Start date of the update streams, e.g. '2012-11-28'",
        type=str,
        default='2012-11-28',
        required=False
    )
    parser.add_argument(
        '--end_date',
        help="end_date: End date of the update streams, e.g. '2013-01-01'",
        type=str,
        default='2013-01-01',
        required=False
    )
    parser.add_argument(
        '--time_bucket_size_in_days',
        help="time_bucket_size_in_days: How many days the parameters should include, e.g. 1",
        type=int,
        default=1,
        required=False
    )
    parser.add_argument(
        '--generate_short_query_parameters',
        help="generate_short_query_parameters: Generate parameters to use manually for the short queries (these are not loaded by the driver)",
        type=bool,
        default=False,
        required=False
    )
    args = parser.parse_args()

    main(args.factor_tables_dir, args.raw_parquet_dir, args.start_date, args.end_date, args.time_bucket_size_in_days, args.generate_short_query_parameters)
