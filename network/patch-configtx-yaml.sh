#!/bin/bash

FILE_TO_MODIFY="hlf/config/configtx.yaml"

### Проверка на существование файла configtx.yaml
echo -e "\n###Checking for file existence configtx.yaml###"
if [ -f $FILE_TO_MODIFY ]; then
    echo -e "file $FILE_TO_MODIFY exist\n"
else
    echo -e "file $FILE_TO_MODIFY does not exist\n"
    exit -1
fi

### Модификация файла configtx.yaml
echo -e "\n###Modification configtx.yaml###"
TO_MODIFY=$(sed -n '/BatchTimeout/p' $FILE_TO_MODIFY)
MODIFICATION="    BatchTimeout: 1s"
if [ "$(uname)" == "Darwin" ]; then
    sed -i "" "s|$TO_MODIFY|$MODIFICATION|g" $FILE_TO_MODIFY        
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    sed -i "s|$TO_MODIFY|$MODIFICATION|g" $FILE_TO_MODIFY
fi

echo -e "Work done\n"
