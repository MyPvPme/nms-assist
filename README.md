# nms-assist ![Gitlab Pipeline Status](https://img.shields.io/gitlab/pipeline-status/mypvpme%2Fnms-assist?branch=master&link=https%3A%2F%2Fgitlab.com%2Fmypvpme%2Fnms-assist%2F-%2Fpipelines)


Just a little helper library to work with NMS.  
So far, only tools for easily working with packets are included, which may or may not be expanded in the future.

If you see this on GitHub, this is a mirror of the original repo located at:  
https://gitlab.com/mypvpme/nms-assist

## Use
If you would like to use this, you are welcome to do so, you can get a current version from our Maven repository

#### Gradle
```groovy
repositories {
    maven("https://repo.mypvp.me/repository/maven")
}
```
```groovy
implementation("me.mypvp:nms-assist:0.1.0")
```

#### Maven
```xml
<repositories>
  <repository>
    <id>MyPvP</id>
    <url>https://repo.mypvp.me/repository/maven/</url>
  </repository>
</repositories>
```
```xml
<dependency>
  <groupId>me.mypvp</groupId>
  <artifactId>nms-assist</artifactId>
  <version>0.1.0</version>
</dependency>
```

