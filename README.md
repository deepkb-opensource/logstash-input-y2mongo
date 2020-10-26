# How to write a Java input plugin

> This article was inspired from official Elastic [documentation](https://www.elastic.co/guide/en/logstash/current/java-input-plugin.html)

To develop a new Java input plugin for Logstash, you need write a new Java class that conforms to the Logstash Java Inputs API, package it, and install it with the logstash-plugin utility. We�ll go through each of those steps in this example.

## Set up your environment
1. Copy the example repo

Start by copying the example input plugin [here](https://github.com/synapticielfactory/logstash-input-example). The plugin API is currently part of the Logstash codebase so you must have a local copy of that available. You can obtain a copy of the Logstash codebase with the following git command :

```
git clone --branch <branch_name> --single-branch https://github.com/elastic/logstash.git <target_folder>
```

The branch_name should correspond to the version of Logstash containing the preferred revision of the Java plugin API.
> The first iteration of native support for input, filter, and output Java plugins was introduced in the 6.6.0 release of Logstash.
check this [blog](https://www.elastic.co/blog/previewing-native-support-for-java-plugins-in-logstash) for more informations.

> The GA version of the Java plugin API is available in the 7.1 and later branches of the Logstash codebase.

Specify the `target_folder` for your local copy of the Logstash codebase. If you do not specify `target_folder`, it defaults to a new folder called `logstash` under your current folder.

For this example i used the master branch of logstash. at the time of writing this article, the lastest realeased logstash version was 7.9.1 and the master branch was containing the codebase for major version 8.0.0


```
git clone https://github.com/elastic/logstash.git

Microsoft Windows [version 6.1.7601]
Copyright (c) 2009 Microsoft Corporation. Tous droits r�serv�s.

R�pertoire de C:\es\labs\logstash_dev\logstash

08/09/2020  16:24    <REP>          .
08/09/2020  16:24    <REP>          ..
08/09/2020  16:21    <REP>          .ci
08/09/2020  16:21                24 .dockerignore
08/09/2020  16:21             1�452 .fossa.yml
08/09/2020  16:21    <REP>          .github
08/09/2020  16:21               920 .gitignore
08/09/2020  16:24    <REP>          .gradle
08/09/2020  16:21                16 .ruby-version
08/09/2020  16:21    <REP>          bin
08/09/2020  16:24    <REP>          build
08/09/2020  16:21            19�305 build.gradle
08/09/2020  16:21    <REP>          ci
08/09/2020  16:21    <REP>          config
08/09/2020  16:21            11�900 CONTRIBUTING.md
08/09/2020  16:21             2�358 CONTRIBUTORS
08/09/2020  16:21             1�872 COPYING.csv
08/09/2020  16:21    <REP>          data
08/09/2020  16:21    <REP>          docker
08/09/2020  16:21             2�470 Dockerfile
08/09/2020  16:21               715 Dockerfile.base
08/09/2020  16:21    <REP>          docs
08/09/2020  16:21             1�206 Gemfile.template
08/09/2020  16:21    <REP>          gradle
08/09/2020  16:21                74 gradle.properties
08/09/2020  16:21             5�955 gradlew
08/09/2020  16:21             3�058 gradlew.bat
08/09/2020  16:21    <REP>          lib
08/09/2020  16:21               881 LICENSE.txt
08/09/2020  16:21    <REP>          licenses
08/09/2020  16:24    <REP>          logstash-core
08/09/2020  16:21    <REP>          logstash-core-plugin-api
08/09/2020  16:21    <REP>          modules
08/09/2020  16:21           610�493 NOTICE.TXT
08/09/2020  16:21    <REP>          pkg
08/09/2020  16:21    <REP>          qa
08/09/2020  16:21             1�472 Rakefile
08/09/2020  16:21    <REP>          rakelib
08/09/2020  16:21            11�038 README.md
08/09/2020  16:21             2�187 ROADMAP.md
08/09/2020  16:21            17�630 rubyUtils.gradle
08/09/2020  16:21               821 settings.gradle
08/09/2020  16:21    <REP>          spec
08/09/2020  16:21             2�820 STYLE.md
08/09/2020  16:21    <REP>          tools
08/09/2020  16:21             1�029 versions.yml
08/09/2020  16:25    <REP>          x-pack
              23 fichier(s)          699�696 octets
              24 R�p(s)  32�811�548�672 octets libres

```

2. Generate the .jar file

After you have obtained a copy of the appropriate revision of the Logstash codebase, you need to compile it to generate the .jar file containing the Java plugin API. From the root directory of your Logstash codebase (`$LS_HOME`), you can compile it with `./gradlew assemble` (or `gradlew.bat assemble` if you�re running on Windows). This should produce the `$LS_HOME/logstash-core/build/libs/logstash-core-x.y.z.jar` where x, y, and z refer to the version of Logstash.

```
Microsoft Windows [version 6.1.7601]
Copyright (c) 2009 Microsoft Corporation. Tous droits r�serv�s.

R�pertoire de C:\es\labs\logstash_dev\logstash\build\libs

08/09/2020  16:24    <REP>          .
08/09/2020  16:24    <REP>          ..
08/09/2020  16:24               261 logstash-8.0.0.jar
               1 fichier(s)              261 octets
               2 R�p(s)  32�577�519�616 octets libres

```
After you have successfully compiled Logstash, you need to tell your Java plugin where to find the `logstash-core-x.y.z.jar` file. Create a new file named `gradle.properties` in the root folder of your plugin project. That file should have a single line:

```
LOGSTASH_CORE_PATH=<target_folder>/logstash-core
```

where `target_folder` is the root folder of your local copy of the Logstash codebase.
Here is my `gradle.properties` example

```
LOGSTASH_CORE_PATH=c:/es/labs/logstash_dev/logstash/logstash-core
```

3. Code the plugin

Start by importing the example input plugin [here](https://github.com/synapticielfactory/logstash-input-example) in your prefered Java IDE, for me i used eclipse.

<img src="./screens/gradle_project_eclipse.png" align="middle">

The example input plugin will accept a list of local file system paths and will watch them and generate a list of events before terminating.
Let�s look at the main class in the example input.

```java
package ma.synapticiel.logstash;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Input;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import MyEvent;

@LogstashPlugin(name = "fsmonitor")
public class FsMonitor implements Input {

	private static final List<Object> DEFAULT_PATHS = Arrays.asList(".");
	public static final PluginConfigSpec<List<Object>> INPUT_FOLDER_CONFIG = PluginConfigSpec.arraySetting("paths",
			DEFAULT_PATHS, false, true);

	private String id;
	private List<Object> paths;
	private final CountDownLatch done = new CountDownLatch(1);
	private volatile boolean stopped;

	public FsMonitor(String id, Configuration config, Context context) {
		this.id = id;
		paths = config.get(INPUT_FOLDER_CONFIG);
	}

	@Override
	public void start(Consumer<Map<String, Object>> consumer) {

		try {

			while (!stopped) {
				try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
					for (int i = 0; i < paths.size(); i++) {
						Path p = Paths.get(paths.get(i).toString());
						p.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
								StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE,
								StandardWatchEventKinds.OVERFLOW);
					}

					while (true) {
						WatchKey key = watchService.take();
						final Watchable watchable = key.watchable();
						final Path directory = (Path) watchable;

						for (WatchEvent<?> watchEvent : key.pollEvents()) {
							final Kind<?> kind = watchEvent.kind();
							final Path context = (Path) watchEvent.context();

							MyEvent event = new MyEvent();
							event.setFileId(
									UUID.nameUUIDFromBytes((directory.toString() + context.toString()).getBytes())
											.toString());
							event.setFileName(context.toString());
							event.setFilePath(directory.toString());
							event.setEventType(kind.toString());

							consumer.accept(Collections.singletonMap("message", event.toString()));
						}

						boolean valid = key.reset();
						if (!valid) {
							break;
						}
					}
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		} finally {
			stopped = true;
			done.countDown();
		}
	}

	@Override
	public void stop() {
		stopped = true; // set flag to request cooperative stop of input
	}

	@Override
	public void awaitStop() throws InterruptedException {
		done.await(); // blocks until input has stopped
	}

	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(INPUT_FOLDER_CONFIG);
	}

	@Override
	public String getId() {
		return this.id;
	}
}
```

Let�s step through and examine each part of that class.

## Class declaration

```java
@LogstashPlugin(name = "fsmonitor")
public class FsMonitor implements Input {
```

Notes about the class declaration:

1. All Java plugins must be annotated with the `@LogstashPlugin` annotation. Additionally :

... The `name` property of the annotation must be supplied and defines the name of the plugin as it will be used in the Logstash pipeline definition. For example, this input would be referenced in the input section of the Logstash pipeline defintion as `input { fsmonitor => { .... } }`

... The value of the name property must match the name of the class excluding casing and underscores.

2. The class must implement the co.elastic.logstash.api.Input interface.
3. Java plugins may not be created in the org.logstash or co.elastic.logstash packages to prevent potential clashes with classes in Logstash itself.

## Plugin settings

The snippet below contains both the setting definition and the method referencing it.

```java
	private static final List<Object> DEFAULT_PATHS = Arrays.asList(".");
	public static final PluginConfigSpec<List<Object>> INPUT_FOLDER_CONFIG = PluginConfigSpec.arraySetting("paths",
			DEFAULT_PATHS, false, true);
@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(INPUT_FOLDER_CONFIG);
	}
```

The `PluginConfigSpec` class allows developers to specify the settings that a plugin supports complete with setting name, data type, deprecation status, required status, and default value. In this example, the `paths` setting defines the array of file system paths to be monitored. This setting is required and if it is not explicitly set an exception will raised.

```
Unable to configure plugins: java.lang.IllegalStateException: Setting value
for 'paths' of type 'class java.lang.String' incompatible with defined type of
'interface java.util.List'"
```

The `configSchema` method must return a list of all settings that the plugin supports. In a future phase of the Java plugin project, the Logstash execution engine will validate that all required settings are present and that no unsupported settings are present.


## Constructor and initialization

```java
	private String id;
	private List<Object> paths;
	private final CountDownLatch done = new CountDownLatch(1);
	private volatile boolean stopped;

	public FsMonitor(String id, Configuration config, Context context) {
		this.id = id;
		paths = config.get(INPUT_FOLDER_CONFIG);
	}
```

All Java input plugins must have a constructor taking a String `id` and `Configuration` and `Context` argument. This is the constructor that will be used to instantiate them at runtime. The retrieval and validation of all plugin settings should occur in this constructor. In this example, the value of the plugin setting is retrieved and stored in local variable for later use in the `start` method.

Any additional initialization may occur in the constructor as well. If there are any unrecoverable errors encountered in the configuration or initialization of the input plugin, a descriptive exception should be thrown. The exception will be logged and will prevent Logstash from starting.

## Start Method

```java
@Override
	public void start(Consumer<Map<String, Object>> consumer) {

		try {

			while (!stopped) {
				try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
					for (int i = 0; i < paths.size(); i++) {
						Path p = Paths.get(paths.get(i).toString());
						p.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
								StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE,
								StandardWatchEventKinds.OVERFLOW);
					}

					while (true) {
						WatchKey key = watchService.take();
						final Watchable watchable = key.watchable();
						final Path directory = (Path) watchable;

						for (WatchEvent<?> watchEvent : key.pollEvents()) {
							final Kind<?> kind = watchEvent.kind();
							final Path context = (Path) watchEvent.context();

							MyEvent event = new MyEvent();
							event.setFileId(
									UUID.nameUUIDFromBytes((directory.toString() + context.toString()).getBytes())
											.toString());
							event.setFileName(context.toString());
							event.setFilePath(directory.toString());
							event.setEventType(kind.toString());

							consumer.accept(Collections.singletonMap("message", event.toString()));
						}

						boolean valid = key.reset();
						if (!valid) {
							break;
						}
					}
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		} finally {
			stopped = true;
			done.countDown();
		}
	}
```

The `start` method begins the event-producing loop in an input.
Inputs are flexible and may produce events through many different mechanisms including:

- a pull mechanism such as periodic queries of external database
- a push mechanism such as events sent from clients to a local network port
- a timed computation such as a heartbeat
- any other mechanism that produces a useful stream of events. Event streams may be either finite or infinite. If the input produces an infinite stream of events, this method should loop until a stop request is made through the `stop` method. If the input produces a finite stream of events, this method should terminate when the last event in the stream is produced or a stop request is made, whichever comes first.

Events should be constructed as instances of `Map<String, Object>` and pushed into the event pipeline via the `Consumer<Map<String, Object>>.accept()` method. To reduce allocations and GC pressure, inputs may reuse the same map instance by modifying its fields between calls to `Consumer<Map<String, Object>>.accept()` because the event pipeline will create events based on a copy of the map�s data.

## Stop and awaitStop methods

```java
private final CountDownLatch done = new CountDownLatch(1);
private volatile boolean stopped;

@Override
public void stop() {
    stopped = true; // set flag to request cooperative stop of input
}

@Override
public void awaitStop() throws InterruptedException {
    done.await(); // blocks until input has stopped
}
```

The `stop` method notifies the input to stop producing events. The stop mechanism may be implemented in any way that honors the API contract though a `volatile boolean` flag works well for many use cases.

Inputs stop both asynchronously and cooperatively. Use the `awaitStop` method to block until the input has completed the stop process. Note that this method should not signal the input to stop as the `stop` method does. The awaitStop mechanism may be implemented in any way that honors the API contract though a `CountDownLatch` works well for many use cases.

## getId method

```java
@Override
public String getId() {
    return id;
}
```

For input plugins, the getId method should always return the id that was provided to the plugin through its constructor at instantiation time.

## Unit tests

Lastly, but certainly not least importantly, unit tests are strongly encouraged. The example input plugin includes an example unit test that you can use as a template for your own.

## Package and deploy

Java plugins are packaged as `Ruby gems` for dependency management and interoperability with Ruby plugins. Once they are packaged as gems, they may be installed with the `logstash-plugin` utility just as Ruby plugins are. Because no knowledge of Ruby or its toolchain should be required for Java plugin development, the procedure for packaging Java plugins as Ruby gems has been automated through a custom task in the Gradle build file provided with the example Java plugins. The following sections describe how to configure and execute that packaging task as well as how to install the packaged Java plugin in Logstash.

## Configuring the Gradle packaging task

The following section appears near the top of the `build.gradle` file supplied with the example Java Input plugin :

```
import java.nio.file.Files
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

apply plugin: 'java'
apply from: LOGSTASH_CORE_PATH + "/../rubyUtils.gradle"


// ===========================================================================
// plugin info
// ===========================================================================
group                      'ma.synapticiel.logstash' // must match the package of the main plugin class
version                    "${file("VERSION").text.trim()}" // read from required VERSION file
description                = "This is an example of Java input implementation"
pluginInfo.licenses        = ['Apache-2.0'] // list of SPDX license IDs
pluginInfo.longDescription = "This gem is a Logstash plugin required to be installed on top of the Logstash core pipeline using \$LS_HOME/bin/logstash-plugin install gemname. This gem is not a stand-alone program"
pluginInfo.authors         = ['Synapticiel LLC']
pluginInfo.email           = ['contact@synapticiel.co']
pluginInfo.homepage        = "http://www.synapticiel.co"
pluginInfo.pluginType      = "input"
pluginInfo.pluginClass     = "FsMonitor"
pluginInfo.pluginName      = "fsmonitor" // must match the @LogstashPlugin annotation in the main plugin class
// ===========================================================================

sourceCompatibility = 1.8
targetCompatibility = 1.8
```
You should configure the values above for your plugin.

- The `version` value will be automatically read from the `VERSION` file in the root of your plugin�s codebase.
- `pluginInfo.pluginType` should be set to one of input, filter, codec, or output.
- `pluginInfo.pluginName` must match the name specified on the `@LogstashPlugin` annotation on the main plugin class. The Gradle packaging task will validate that and return an error if they do not match.

## Running the Gradle packaging task

Several Ruby source files along with a `gemspec` file and a `Gemfile` are required to package the plugin as a Ruby gem. These Ruby files are used only for defining the Ruby gem structure or at Logstash startup time to register the Java plugin. They are not used during runtime event processing. The Gradle packaging task automatically generates all of these files based on the values configured in the section above.

You run the Gradle packaging task with the following command:

```
./gradlew gem
```

For Windows platforms: Substitute `gradlew.bat` for `./gradlew` as appropriate in the command.

That task will produce a gem file in the root directory of your plugin�s codebase with the name `logstash-{plugintype}-<pluginName>-<version>.gem`

```
Microsoft Windows [version 6.1.7601]
Copyright (c) 2009 Microsoft Corporation. Tous droits r�serv�s.
R�pertoire de C:\scub-foundation\workdir\logstash_plugins\logstash-input-plugin

11/09/2020  17:41    <REP>          .
11/09/2020  17:41    <REP>          ..
11/09/2020  15:09             1�382 .classpath
11/09/2020  15:08               160 .gitattributes
11/09/2020  15:08               108 .gitignore
11/09/2020  15:08    <REP>          .gradle
11/09/2020  15:09               650 .project
11/09/2020  15:09    <REP>          .settings
11/09/2020  15:09    <REP>          bin
11/09/2020  17:38    <REP>          build
11/09/2020  17:39             4�095 build.gradle
11/09/2020  18:51               454 Gemfile
11/09/2020  15:08    <REP>          gradle
11/09/2020  17:37                65 gradle.properties
11/09/2020  15:08             5�764 gradlew
11/09/2020  15:08             2�942 gradlew.bat
11/09/2020  17:38    <REP>          lib
09/09/2020  10:25            11�558 LICENSE
11/09/2020  18:51         4�291�584 logstash-input-fsmonitor-1.0.gem
11/09/2020  18:51             1�276 logstash-input-fsmonitor.gemspec
12/09/2020  14:49            20�727 README.md
11/09/2020  15:51    <REP>          screens
11/09/2020  15:08               378 settings.gradle
11/09/2020  15:08    <REP>          src
11/09/2020  17:38    <REP>          vendor
12/09/2020  13:20                 5 VERSION
              16 fichier(s)        4�342�130 octets
              11 R�p(s)  31�721�205�760 octets libres

```

## Installing the Java plugin in Logstash

After you have packaged your Java plugin as a Ruby gem, you can install it in Logstash with this command:

```
bin/logstash-plugin install --no-verify --local /path/to/logstash-input-fsmonitor-1.0.gem
```

For Windows platforms: Substitute backslashes for forward slashes as appropriate in the command.

## Running Logstash with the Java input plugin

The following is a minimal Logstash configuration that can be used to test that the Java input plugin is correctly installed and functioning.

```
input {
  fsmonitor {
	paths => ["/tmp/f1", "/tmp/f2"]
  }
}

 filter {
      json {
        source => "message"
      }
	  
  mutate { remove_field => [ "message" ] }

}
	
output {
  stdout { codec => rubydebug }
}
```


Copy the above Logstash configuration to a file such as `fsmonitor.conf`. Start Logstash with:

```
bin/logstash -f /path/to/fsmonitor.conf
```

> The Java execution engine, `the default execution engine since Logstash 7.0`, is required as Java plugins are not supported in the Ruby execution engine.

The expected Logstash output (excluding initialization) with the configuration above is:

```
{
      "@version" => "1",
     "eventType" => "ENTRY_MODIFY",
        "fileId" => "cc925d5f-8292-3faf-9db5-6cb3d50b987b",
    "@timestamp" => 2020-09-12T13:55:25.215Z,
      "filePath" => "\\tmp\\f1",
      "fileName" => "my_file.txt"
}
{
      "@version" => "1",
     "eventType" => "ENTRY_CREATE",
        "fileId" => "c04af31a-935c-3d21-b3a6-64ac617dd7eb",
    "@timestamp" => 2020-09-12T13:55:33.795Z,
      "filePath" => "\\tmp\\f2",
      "fileName" => "INV36969427.pdf"
}
{
      "@version" => "1",
     "eventType" => "ENTRY_MODIFY",
        "fileId" => "c04af31a-935c-3d21-b3a6-64ac617dd7eb",
    "@timestamp" => 2020-09-12T13:55:33.800Z,
      "filePath" => "\\tmp\\f2",
      "fileName" => "INV36969427.pdf"
}
{
      "@version" => "1",
     "eventType" => "ENTRY_MODIFY",
        "fileId" => "c04af31a-935c-3d21-b3a6-64ac617dd7eb",
    "@timestamp" => 2020-09-12T13:55:33.795Z,
      "filePath" => "\\tmp\\f2",
      "fileName" => "INV36969427.pdf"
}
```