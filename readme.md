# File Storage For Scala

A common interface for doing basic file storage operations on multiple platforms

## Install
| Snapshot | Release |
| --- | --- |
| [![Sonatype Nexus (Releases)][Badge-SonatypeReleases]][Link-SonatypeReleases] | [![Sonatype Nexus (Snapshots)][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots] |

```sbt
// Choose any you want to use
libraryDependencies += "com.deliganli" %% "fstore4s-s3"    % "0.1.0"
libraryDependencies += "com.deliganli" %% "fstore4s-gcs"   % "0.1.0"
libraryDependencies += "com.deliganli" %% "fstore4s-local" % "0.1.0"

// You can add the core library if you want to implement your own file store
// Other modules above use core inherently
libraryDependencies += "com.deliganli" %% "fstore4s-core" % "0.1.0"
```
## Details

Current supported file storage platforms are;
- Amazon Web Services (AWS) S3
- Google Cloud Storage
- Local file storage

The methods for manipulating data are same across all platforms.

```scala
store.uri(address) // Generates signed uri valid over configured time
store.list(prefix) // Returns a stream with files under the prefix
.
.
```

Platform can be changed easily by using another instance of `FileStorage[F,T]`

```scala
S3Store.create[IO, SimpleDomainFile](config, credentialProvider)
GCSStore.create[IO, SimpleDomainFile](config)
LocalFileStore.create[IO, SimpleDomainFile](config)
```

Those `create` methods are just helpers, one can create the `FileStore` directly
by providing necessary underlying store instance

Unifies storage providers' `InputStream`, `OutputStream`, `Iterator` and other java 
streaming based functionality under `fs2.Stream`

## What is Address?

It is basically a case class that has an implementation for `FileCodec`

`FileCodec[T]` is used for naming the file from structured fields and vice versa

`SimpleDomainFile` is a basic implementation that used across tests, simply it names 
the stored files with 3 properties; version, category, name. For example;

```scala
Address("test-dir", SimpleDomainFile("V1", "attachment", "some-file.txt"))
```

This structured data can be used to encode the file to the filestore with the filename
of `test-dir/V1_attachment_some-file.txt`. The `Address` class can be constructed 
from the filename as well so when we read filenames from the store directly we can 
have our structured data back

With this way we can provide `case class -> filename` and `filename -> case class` 
transformations that guarantees each unique case class corresponds to a unique file

## This library is NOT

- Sophisticated scala wrapper for particular file storage to use all possible functionality
- Battle tested, production ready

## This library might be useful at

- Using one of the cloud file stores for production and using local store for tests
- Quickly switch between file stores with minimal code change
- Streaming applications that makes use of fs2

## Bottom-Line
This library is very crude at the moment, not even sure there would be a demand since
this is an abstraction there will be the trade-offs between customizability and 
unification. If developed further, one probably will always find themselves deciding between
providing optional configuration that may not work with other vendors and not allowing
any vendor specific usage

The drawbacks of 'melting everything in one pot' approach may discourage users with higher 
volume and performance demands due to the lack of vendor-specific configuration. Though I 
tried to make everything streaming to make life easier for them as well.

[Badge-SonatypeReleases]: https://img.shields.io/nexus/s/com.deliganli/fstore4s-core_2.13?server=https%3A%2F%2Foss.sonatype.org "Sonatype Releases"
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/snapshots/com/deliganli/fstore4s-core_2.13/ "Sonatype Releases"

[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/r/https/oss.sonatype.org/com.deliganli/fstore4s-core_2.13.svg "Sonatype Releases"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/com/deliganli/fstore4s-core_2.13/ "Sonatype Releases"
