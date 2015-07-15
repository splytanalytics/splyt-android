#!/bin/sh

# This script modifies the gson jar file and replaces the package name for all of the classes from com.google.* to com.rsb.*
# This allows us to package this external code with our core jar and avoid both requiring an external dependency and the
# the possibility of a conflict if the user is already referencing this library in their application
# See http://stackoverflow.com/questions/13746737/hand-edit-a-jar-to-change-package-names

# Get the path to this script.
SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use jar jar links to change the package name
java -jar $SCRIPT_PATH/../jarjar/jarjar-1.4.jar process $SCRIPT_PATH/rules.txt gson-2.2.3.jar gson-2.2.3-custom.jar