#!/bin/bash

FILE_TO_MODIFY="hlf/config/core.yaml"
DIR_TO_RELATIVE_PATH_BUILDER="buildpack"

### Проверка на существование файла core.yaml
echo -e "\n###Checking for file existence core.yaml###"
if [ -f $FILE_TO_MODIFY ]; then 
    echo -e "file $FILE_TO_MODIFY exist\n"
else
    echo -e "file $FILE_TO_MODIFY does not exist\n"
    exit -1
fi

### Проверка на существование билдера
echo -e "\n###Checking for file existence directory of builder###"
if [ -e $DIR_TO_RELATIVE_PATH_BUILDER ]; then 
    echo -e "directory $DIR_TO_RELATIVE_PATH_BUILDER exist\n"
else
    echo -e "directory $DIR_TO_RELATIVE_PATH_BUILDER does not exist\n"
    exit -1
fi

### Изменение файла
echo "###Configure core.yaml###"
DIR_TO_ABSOLUTE_PATH_BUILDER=$(realpath -e $DIR_TO_RELATIVE_PATH_BUILDER)
NEW_FILE=$FILE_TO_MODIFY".tmp"
touch $NEW_FILE
ID="    externalBuilders: []"
BUILDER="    externalBuilders:"
NAME="         - name: cnft-external"
PATH="           path: $DIR_TO_ABSOLUTE_PATH_BUILDER"
ENVIRONMENT="           propagateEnvironment:\n              - ENVVAR_NAME_TO_PROPAGATE_FROM_PEER\n              - LD_LIBRARY_PATH\n              - LIBPATH\n              - PATH\n              - TMPDIR"
IFS=''

STR2DELETE_1="        # - path: /path/to/directory"
STR2DELETE_2="        #   name: descriptive-builder-name"
STR2DELETE_3="        #   propagateEnvironment:"
STR2DELETE_4="        #      - ENVVAR_NAME_TO_PROPAGATE_FROM_PEER"
STR2DELETE_5="        #      - GOPROXY"
while read line
do 
    if [ "$line" = "$ID" ];
    then
        echo $BUILDER >> $NEW_FILE
        echo $NAME >> $NEW_FILE
        echo $PATH >> $NEW_FILE
        echo -e $ENVIRONMENT >> $NEW_FILE
    elif [ "$line" != "$STR2DELETE_1" ] && [ "$line" != "$STR2DELETE_2" ] && [ "$line" != "$STR2DELETE_3" ] && [ "$line" != "$STR2DELETE_4" ] && [ "$line" != "$STR2DELETE_5" ];
    then
        echo $line >> $NEW_FILE
    fi
done < $FILE_TO_MODIFY
/bin/mv $NEW_FILE $FILE_TO_MODIFY 

echo -e "Work done\n"
