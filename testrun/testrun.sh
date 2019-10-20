root_dir=~/git/sttp-swagger-codegen
swagger_codegen_jar=~/software/swagger/swagger-codegen-cli.jar
module_jar=$root_dir/target/sttp-swagger-codegen-1.0.0.jar
testrun_dir=testrun
codegen_dir=~/tmp/sttp_testrun
swagger_file=terra-api.yaml
cd $root_dir || exit
echo "Building and packaging sstp module"
mvn clean package
echo "Done with sttp module"
cd $testrun_dir || exit
if [ -d "$codegen_dir" ]; then
  if [ -d "$codegen_dir.old" ]; then
    echo "Removing $codegen_dir.old"
    rm -r $codegen_dir.old
  fi
  echo "Moving $codegen_dir to $codegen_dir.old"
  mv $codegen_dir $codegen_dir.old
fi
echo "Generated JAR file $module_jar."
ls -l $module_jar
mkdir $codegen_dir
echo "Now generating code in folder $codegen_dir"
java -cp $swagger_codegen_jar:$module_jar io.swagger.codegen.Codegen -l sttp -i $swagger_file -o $codegen_dir
echo "Done generating code, now trying to build from generated code"
cd $codegen_dir || exit
pwd
sbt compile
