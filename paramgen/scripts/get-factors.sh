#!/bin/bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

rm -rf scratch/factors
mkdir -p scratch/factors
cp -r ${LDBC_SNB_DATA_ROOT_DIRECTORY}/factors/parquet/raw/composite-merged-fk/* scratch/factors/
