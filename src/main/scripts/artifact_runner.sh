#!/bin/sh
if [ -n "$1" ]
then
  command="java -Xmx256m -Dlogging.config=/maven/config/logback.xml"

  for param in `printenv | awk '/^__PROP_/{print $0}'`
  do
    eq_symbol_index=`expr index ${param} =`

    prop_key=`expr ${param:0:$((eq_symbol_index-1))} | sed -e 's/__PROP_//; s/_/./g'`
    prop_value=`expr ${param:${eq_symbol_index}}`
    command="${command} -D${prop_key}=${prop_value}"
  done

  command="${command} -jar $1"
  echo ${command}
  eval ${command}
else
  echo "No artifact to run."
  exit 1
fi
