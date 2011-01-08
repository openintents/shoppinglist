set PATH=.;c:\cygwin\bin;%PATH%
bash androidxml2po.bash -e
mkdir translations_shoppinglist
copy shoppinglist* translations_shoppinglist
tar -cvvzf translations_shoppinglist.tgz translations_shoppinglist