# ProkyoNet
[![Open Source Love](https://badges.frapsoft.com/os/v2/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/)
[![codecov](https://codecov.io/gh/Prokyo/ProkyoNet/branch/develop/graph/badge.svg)](https://codecov.io/gh/Prokyo/ProkyoNet)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8bc600f901f84de1aa4202ddd7a876f0)](https://app.codacy.com/app/Microsamp/ProkyoNet?utm_source=github.com&utm_medium=referral&utm_content=Prokyo/ProkyoNet&utm_campaign=Badge_Grade_Settings)
[![CircleCI](https://circleci.com/gh/Prokyo/ProkyoNet/tree/develop.svg?style=svg)](https://circleci.com/gh/Prokyo/ProkyoNet/tree/develop)
[![CircleCI](https://circleci.com/gh/Prokyo/ProkyoNet/tree/master.svg?style=svg)](https://circleci.com/gh/Prokyo/ProkyoNet/tree/master)

ProkyoNet is a wrapper for Netty providing a simple and high performance API.

## Getting started

ProkyoNet is well documented in our [Github wiki](https://github.com/Prokyo/ProkyoNet/wiki).

## Features overview
- [x]  TCP packets
- [x]  Compression
- [ ]  UDP packets
- [ ]  HTTP/1.1
- [ ]  HTTP/2
- [ ]  QUIC
- [ ]  Encryption (PGP)
- [ ]  Statistics

## Maven

Build `ProkyoNet` once so you have this dependency in your local maven repository and can then use it in your maven projects.

**ProkyoServer**
```xml
<dependency>
	<groupId>de.prokyo.network</groupId>
	<artifactId>prokyo-net-server</artifactId>
	<version>0.1</version>
</dependency>
```

**ProkyoClient**
```xml
<dependency>
	<groupId>de.prokyo.network</groupId>
	<artifactId>prokyo-net-client</artifactId>
	<version>0.1</version>
</dependency>
```

## License
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

MIT License
