#!/bin/bash

source bin/datomic_version.sh
cd vendor/datomic
bin/transactor config/samples/free-transactor-template.properties
