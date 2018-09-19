sh_file_dir=$(cd `dirname $0`; pwd)
cd $sh_file_dir
java -cp .:gnome-adx-0.0.1-SNAPSHOT-jar-with-dependencies.jar AdxSystem ./adx.conf

