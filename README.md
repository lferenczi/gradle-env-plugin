## gradle-env-plugin

In the groovy world `ConfigSlurper` provides an excellent way of configuring a system for many different environments. Besides it's strong points it falls short on two specific areas:
- Iterating over all defined environments is not possible
- It doesn't allow nesting of environments

This two missing feature lead to the implementation of this plugin.

## Usage

Create a file called `config.gradle` in your project root:

	dbUrl = 'localhost'
	dbUser = 'myuser'
	dbPass = 'mypass'
	dbSchema = 'myschema'
	dbType = 'mysql'
	env {
		prod {
			dbUrl = 'prod.mysystem.com'
			dbUser = 'produser'
			dbPass = 'prodpass'
			
			env {
				prod1 {
					dbSchema = 'prod1schema'
				}
				prod2 {
					dbSchema = 'prod2schema'
				}
			}
		}
		test {
			dbUrl = 'test.mysystem.com'
		}
	}

Apply the plugin with `apply plugin: 'environment'` and use the `environment` property to access the configuration:

	def envConfig = environment.get('prod:prod1')
	def envConfig = environment.get()		// Defaults

Or you can iterate over all the environment:

	environment.forEach { name, config ->
		println "Configuration for ${name} is: ${config.toProperties()}"
	}
	
One example of the possible uses is to create dynamic tasks for each environment:

	environment.forEach { name, config ->
		def envName = name.replace(":", "-")
		
		task "jar-${envName}"(type: Jar) {
			from: mydir
			expand config.toProperties()
		}
	}

In some cases we need to convert the config to a standard Properties format. The ConfigObject defines a method to do this:

	def config = environment.get()
	def p = config.toProperties()

The generated configuration will always contain a key called `env` which contains the name of the environment of that particular instance. This can be useful if you'd like to substitute it filtered/expanded files. 

	assert "dev" == environment.get().env

## Override logic

To calculate the configuration for each environment the plugin uses a simple recursive override mechanism. 

- Defaults are everything what is defined outside of the top level env {} block
- Environments will will override the defaults if the same key is specified within an environment
- Sub environments will override the parent environment's config

Please note that the configuration is calculated for each environment separately, they won't affect each other !

Taking the example above the calculated configuration for `prod:prod2` will look like this:

	dbUrl = 'prod.mysystem.com'		// defined on parent env level
	dbUser = 'produser'				// defined on parent env level
	dbPass = 'prodpass'				// defined on parent env level
	dbSchema = 'prod2schema'		// override from the actual environment
	dbType = 'mysql'				// default, no overrides

Configuration for the `test` environment is the following:

	dbUrl = 'test.mysystem.com'		// override by the environment
	dbUser = 'myuser'				// default
	dbPass = 'mypass'				// default
	dbSchema = 'myschema'			// default
	dbType = 'mysql'				// default

## Local override

To allow developer specific settings to the configuration without adding them to the main configuration file the developer can create a `local.gradle` file. Certain limitations are applied when using this file:

- Only the `getLocal()` method will read this file
- No environment specification is allowed
- The file specifies overrides over the default configuration (i.e.: only change what you need, everything else will be default)
- The `getLocal()` method will only override for the default environment

The recommended usage is to create a `local.gradle.sample` file within the project and tell developers to copy and rename this file to `local.gradle` to specify their own settings. The resulting `local.gradle` files shouldn't be checked in to the version control system.

## API

To identify a specific environment in the tree the plugin use colon as separator character when specifying nested environments. For example the `prod:prod1` string identifies the `prod1` within `prod` environment.

- `environment.get()`

Returns the default configuration as a `ConfigObject`

- `environment.get(name)`

Returns the configuration as a `ConfigObject` for a given environment name

- `environment.getLocal(name)`

Returns the configuration as a `ConfigObject` for a given environment name. 
If no environment specified (using the default) and a `local.gradle` configuration file is present it will be merged with the defaults and returned. This allows developers to use their own settings on top of the defaults.

- `environment.forEach(closure)`

Invokes the given closure with the (name, config) parameters where name is the current environment's name and config is the `ConfigObject` associated with it. Please note that this method will only iterates the leaf environments, because most of the time that's what is really needed. Taking the example above the iterator will call the closure with the following parameters: `[dev, prod:prod1, prod:prod2, test]` Note the missing `prod` environment. 

## Properties

Overwrite the following properties to change defaults in the plugin:

	environment.configFile = 'config.gradle'	// Default configuration file
	environment.defaultEnv = 'dev'				// Default environment name

## How is it working ?

Initially the plugin parses the config file using `ConfigSlurper`. A recursive method will visit all environments and create a merged `ConfigObject` for each of them, where merging will always be based on the parent's configuration. Given the recursive nature of the method this will ensure proper merging regardless of how many environments are nested into each other. 

The config parsing is lazy, will only happen if the build refers to the environment. (i.e.: for a `gradle tasks` it won't be triggered normally)

## Limitations

- The `environment` key can't be used. (ConfigSlurper is working behind the scenes, the usage of this key would conflict with it's inner mechanism)
- Keys with dots in their names are not tested. At this stage please only use simple strings as keys. 
