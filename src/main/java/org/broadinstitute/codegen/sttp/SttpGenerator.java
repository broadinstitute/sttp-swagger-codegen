package org.broadinstitute.codegen.sttp;

import io.swagger.codegen.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.swagger.codegen.languages.AbstractScalaCodegen;
import org.apache.commons.lang3.StringUtils;

public class SttpGenerator extends AbstractScalaCodegen implements CodegenConfig {
  protected String authScheme = "";
  protected boolean authPreemptive;
  protected boolean asyncHttpClient = !authScheme.isEmpty();
  protected String groupId = "io.swagger";
  protected String artifactId = "swagger-sttp-client";
  protected String artifactVersion = "1.0.0";
  protected String clientName = "SttpClient";

  public SttpGenerator() {
    super();
    outputFolder = "generated-code/scala";
    modelTemplateFiles.put("model.mustache", ".scala");
    apiTemplateFiles.put("api.mustache", ".scala");
    embeddedTemplateDir = templateDir = "sttp";
    apiPackage = "io.swagger.client.api";
    modelPackage = "io.swagger.client.model";

    setReservedWordsLowerCase(
            Arrays.asList(
                    // local variable names used in API methods (endpoints)
                    "path", "contentTypes", "contentType", "queryParams", "headerParams",
                    "formParams", "postBody", "mp", "basePath", "apiInvoker",

                    // scala reserved words
                    "abstract", "case", "catch", "class", "def", "do", "else", "extends",
                    "false", "final", "finally", "for", "forSome", "if", "implicit",
                    "import", "lazy", "match", "new", "null", "object", "override", "package",
                    "private", "protected", "return", "sealed", "super", "this", "throw",
                    "trait", "try", "true", "type", "val", "var", "while", "with", "yield")
    );

    additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
    additionalProperties.put(CodegenConstants.GROUP_ID, groupId);
    additionalProperties.put(CodegenConstants.ARTIFACT_ID, artifactId);
    additionalProperties.put(CodegenConstants.ARTIFACT_VERSION, artifactVersion);
    additionalProperties.put("asyncHttpClient", asyncHttpClient);
    additionalProperties.put("authScheme", authScheme);
    additionalProperties.put("authPreemptive", authPreemptive);
    additionalProperties.put("clientName", clientName);
    additionalProperties.put(CodegenConstants.STRIP_PACKAGE_NAME, stripPackageName);

    supportingFiles.add(new SupportingFile("Decoders.scala.mustache",
            (sourceFolder + File.separator + invokerPackage).replace(".", java.io.File.separator), "Decoders.scala"));
    supportingFiles.add(new SupportingFile("Encoders.scala.mustache",
            (sourceFolder + File.separator + invokerPackage).replace(".", java.io.File.separator), "Encoders.scala"));
    supportingFiles.add(new SupportingFile("Backends.scala.mustache",
            (sourceFolder + File.separator + invokerPackage).replace(".", java.io.File.separator), "Backends.scala"));
    supportingFiles.add(new SupportingFile("client.mustache",
            (sourceFolder + File.separator + invokerPackage).replace(".", java.io.File.separator), clientName + ".scala"));
    supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
    supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
    supportingFiles.add(new SupportingFile("build.sbt.mustache", "", "build.sbt"));

    importMapping.remove("List");
    importMapping.remove("Set");
    importMapping.remove("Map");

    importMapping.put("Date", "java.util.Date");
    importMapping.put("ListBuffer", "scala.collection.mutable.ListBuffer");

    typeMapping = new HashMap<String, String>();
    typeMapping.put("enum", "NSString");
    typeMapping.put("array", "List");
    typeMapping.put("set", "Set");
    typeMapping.put("boolean", "Boolean");
    typeMapping.put("string", "String");
    typeMapping.put("int", "Int");
    typeMapping.put("long", "Long");
    typeMapping.put("float", "Float");
    typeMapping.put("byte", "Byte");
    typeMapping.put("short", "Short");
    typeMapping.put("char", "Char");
    typeMapping.put("double", "Double");
    typeMapping.put("object", "Any");
    typeMapping.put("file", "File");
    typeMapping.put("binary", "Array[Byte]");
    typeMapping.put("ByteArray", "Array[Byte]");
    typeMapping.put("ArrayByte", "Array[Byte]");
    typeMapping.put("date-time", "Date");
    typeMapping.put("DateTime", "Date");

    instantiationTypes.put("array", "ListBuffer");
    instantiationTypes.put("map", "HashMap");

    cliOptions.add(new CliOption(CodegenConstants.MODEL_PROPERTY_NAMING, CodegenConstants.MODEL_PROPERTY_NAMING_DESC).defaultValue("camelCase"));
  }

  @Override
  public void processOpts() {
    super.processOpts();
    if (additionalProperties.containsKey(CodegenConstants.MODEL_PROPERTY_NAMING)) {
      setModelPropertyNaming((String) additionalProperties.get(CodegenConstants.MODEL_PROPERTY_NAMING));
    }
  }

  public void setModelPropertyNaming(String naming) {
    if ("original".equals(naming) || "camelCase".equals(naming) ||
            "PascalCase".equals(naming) || "snake_case".equals(naming)) {
      this.modelPropertyNaming = naming;
    } else {
      throw new IllegalArgumentException("Invalid model property naming '" +
              naming + "'. Must be 'original', 'camelCase', " +
              "'PascalCase' or 'snake_case'");
    }
  }

  public String getModelPropertyNaming() {
    return this.modelPropertyNaming;
  }
  @Override
  public String toVarName(String name) {
    // sanitize name
    name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

    if("_".equals(name)) {
      name = "_u";
    }

    // if it's all uppper case, do nothing
    if (name.matches("^[A-Z_]*$")) {
      return name;
    }

    name = getNameUsingModelPropertyNaming(name);

    // for reserved word or word starting with number, append _
    if (isReservedWord(name) || name.matches("^\\d.*")) {
      name = escapeReservedWord(name);
    }

    return name;
  }

  @Override
  public String toParamName(String name) {
    // should be the same as variable name
    return toVarName(name);
  }

  public String getNameUsingModelPropertyNaming(String name) {
    switch (CodegenConstants.MODEL_PROPERTY_NAMING_TYPE.valueOf(getModelPropertyNaming())) {
      case original:    return name;
      case camelCase:   return camelize(name, true);
      case PascalCase:  return camelize(name);
      case snake_case:  return underscore(name);
      default:          throw new IllegalArgumentException("Invalid model property naming '" +
              name + "'. Must be 'original', 'camelCase', " +
              "'PascalCase' or 'snake_case'");
    }

  }

  @Override
  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

  @Override
  public String getName() {
    return "sttp";
  }

  @Override
  public String getHelp() {
    return "Generates a Scala client library (beta).";
  }

  @Override
  public String toOperationId(String operationId) {
    // throw exception if method name is empty
    if (StringUtils.isEmpty(operationId)) {
      throw new RuntimeException("Empty method name (operationId) not allowed");
    }

    // method name cannot use reserved keyword, e.g. return
    if (isReservedWord(operationId)) {
      throw new RuntimeException(operationId + " (reserved word) cannot be used as method name");
    }

    return camelize(operationId, true);
  }

  @Override
  public String toModelName(final String name) {
    final String sanitizedName = sanitizeName(modelNamePrefix + this.stripPackageName(name) + modelNameSuffix);

    // camelize the model name
    // phone_number => PhoneNumber
    final String camelizedName = camelize(sanitizedName);

    // model name cannot use reserved keyword, e.g. return
    if (isReservedWord(camelizedName)) {
      final String modelName = "Model" + camelizedName;
      LOGGER.warn(camelizedName + " (reserved word) cannot be used as model name. Renamed to " + modelName);
      return modelName;
    }

    // model name starts with number
    if (name.matches("^\\d.*")) {
      final String modelName = "Model" + camelizedName; // e.g. 200Response => Model200Response (after camelize)
      LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
      return modelName;
    }

    return camelizedName;
  }

  @Override
  public String toEnumName(CodegenProperty property) {
    return formatIdentifier(stripPackageName(property.baseName), true);
  }

  static class ExtendedCodegenOperation extends CodegenOperation {

    public ExtendedCodegenOperation(CodegenOperation o) {
      super();

      // Copy all fields of CodegenOperation
      this.responseHeaders.addAll(o.responseHeaders);
      this.hasAuthMethods = o.hasAuthMethods;
      this.hasConsumes = o.hasConsumes;
      this.hasProduces = o.hasProduces;
      this.hasParams = o.hasParams;
      this.hasOptionalParams = o.hasOptionalParams;
      this.returnTypeIsPrimitive = o.returnTypeIsPrimitive;
      this.returnSimpleType = o.returnSimpleType;
      this.subresourceOperation = o.subresourceOperation;
      this.isMapContainer = o.isMapContainer;
      this.isListContainer = o.isListContainer;
      this.isMultipart = o.isMultipart;
      this.hasMore = o.hasMore;
      this.isResponseBinary = o.isResponseBinary;
      this.hasReference = o.hasReference;
      this.isRestfulIndex = o.isRestfulIndex;
      this.isRestfulShow = o.isRestfulShow;
      this.isRestfulCreate = o.isRestfulCreate;
      this.isRestfulUpdate = o.isRestfulUpdate;
      this.isRestfulDestroy = o.isRestfulDestroy;
      this.isRestful = o.isRestful;
      this.path = o.path;
      this.operationId = o.operationId;
      this.returnType = o.returnType;
      this.httpMethod = o.httpMethod;
      this.returnBaseType = o.returnBaseType;
      this.returnContainer = o.returnContainer;
      this.summary = o.summary;
      this.unescapedNotes = o.unescapedNotes;
      this.notes = o.notes;
      this.baseName = o.baseName;
      this.defaultResponse = o.defaultResponse;
      this.discriminator = o.discriminator;
      this.consumes = o.consumes;
      this.produces = o.produces;
      this.bodyParam = o.bodyParam;
      this.allParams = o.allParams;
      this.bodyParams = o.bodyParams;
      this.pathParams = o.pathParams;
      this.queryParams = o.queryParams;
      this.headerParams = o.headerParams;
      this.formParams = o.formParams;
      this.authMethods = o.authMethods;
      this.tags = o.tags;
      this.responses = o.responses;
      this.imports = o.imports;
      this.examples = o.examples;
      this.externalDocs = o.externalDocs;
      this.vendorExtensions = o.vendorExtensions;
      this.nickname = o.nickname;
      this.operationIdLowerCase = o.operationIdLowerCase;
      this.operationIdCamelCase = o.operationIdCamelCase;
    }

    public String getHttpMethodLowerCase() {
      return httpMethod.toLowerCase();
    }

  }

  @Override
  public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
    Map<String, Object> operations = (Map<String, Object>) super.postProcessOperations(objs).get("operations");
    List<CodegenOperation> os = (List<CodegenOperation>) operations.get("operation");
    List<ExtendedCodegenOperation> newOs = new ArrayList<>();
    for (CodegenOperation o : os) {
      ExtendedCodegenOperation eco = new ExtendedCodegenOperation(o);

      // detect multipart form types
      if (eco.hasConsumes == Boolean.TRUE) {
        Map<String, String> firstType = eco.consumes.get(0);
        if (firstType != null) {
          if ("multipart/form-data".equals(firstType.get("mediaType"))) {
            eco.isMultipart = Boolean.TRUE;
          }
        }
      }

      newOs.add(eco);
    }
    operations.put("operation", newOs);
    return objs;
  }

}
