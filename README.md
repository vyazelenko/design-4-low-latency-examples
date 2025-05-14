# design-4-low-latency-examples
To run the benchmarks do:
* Run the build:
```bash
$ mvn clean package
```
* Run the benchmarks:
```bash
$ java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED target/benchmarks.jar
```