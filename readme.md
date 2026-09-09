Project structure

### Benchmarks
- OopAndCacheMissBench - demonstrated benefits of using SoA

### Simple Tests
- LongAdderTest - demonstrates benefit of using LongAdder vs AtomicLong
- MultipleCountersQuestTest - special quest for those who most attentive
- FalseSharingDemoTest - one more demo of false sharing


Use this command to run JMH test:

``
./gradlew jmh
``

Use this command to run JUnit test:

``
./gradlew test
``