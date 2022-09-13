#!/bin/bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

python3 paramgen/paramgen.py --factor_tables_dir 'factors/*' --start_date '2012-11-28' --end_date '2013-01-01' --time_bucket_size_in_days 1
