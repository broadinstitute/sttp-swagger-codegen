set -x
root_dir=~/git/sttp-swagger-codegen
swagger_codegen_jar=~/software/swagger/swagger-codegen-cli.jar
module_jar=$root_dir/target/sttp-swagger-codegen-1.0.0.jar
testrun_dir=testrun
codegen_dir=codegen
swagger_file=terra-api.yaml
cd $root_dir || exit
echo "Building and packaging sstp module"
mvn package
echo "Done with sttp module"
cd $testrun_dir || exit
if [ -f "$codegen_dir" ]; then
  if [ -f "$codegen_dir.old" ]; then
    rm -r $codegen_dir.old
  fi
  mv $codegen_dir $codegen_dir.old
fi
mkdir $codegen_dir
echo "Now generating code"
java -cp $swagger_codegen_jar:$module_jar io.swagger.codegen.Codegen -l sttp -i $swagger_file -o $codegen_dir
echo "Done generating code, now trying to build from generated code"
cd $codegen_dir || exit
sbt compile
