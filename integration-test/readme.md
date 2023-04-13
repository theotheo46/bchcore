To get ready:
 - Start test network (see **network/README.md**)
 - Initialize NPM packages (this folder): 
   - **npm install**

To run Gherkin test in JS:
```shell
core/integration-test $ npm run filters
```

To update wallet lib run: **sbt clean makeNPM**

NOTE: You need **node version >= 15.x.x**


The feature file is located at **src/test/resources/feature**

The JS steps implementations are in **src/test/js**

## Cucumber Scala

### IntelliJ IDEA plugin
Install the plugin, after that the Gherkin tests might be run from the IDEA.

### SBT plugin
```shell
core $ sbt "integration_test/cucumber --tags @filters"
```
