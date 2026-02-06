# Testing Notes

## Mockito / ByteBuddy Agent Warning

The test setup preloads the ByteBuddy agent via Surefire to avoid Mockito self-attaching at runtime. This is configured in `pom.xml` with:

- `net.bytebuddy:byte-buddy-agent` as a test dependency
- Surefire `argLine` pointing to the agent JAR in the local Maven repository

You may still see a JVM warning like:

- `Sharing is only supported for boot loader classes because bootstrap classpath has been appended`

This is a benign class-data-sharing warning caused by the Java agent. Tests should still pass normally.
