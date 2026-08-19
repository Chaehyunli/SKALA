# STEP 02 - Structured Output

```bash
./gradlew :step02-structured-output:bootRun
curl "http://localhost:8102/api/course?subject=Spring%20AI"
```

핵심:

```java
.call()
.entity(CoursePlan.class, spec -> spec.validateSchema());
```
