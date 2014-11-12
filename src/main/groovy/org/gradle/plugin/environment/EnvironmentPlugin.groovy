package org.gradle.plugin.environment

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Plugin to handle multi-dimensional environment mappings
 * 
 * @author lferenczi
 */
class EnvironmentPlugin implements Plugin<Project> {

    public static final PLUGIN_NAME = "environment";

	@Override
	public void apply(Project project) {
		project.extensions.create(PLUGIN_NAME, EnvironmentFactory, project)
	}
}

/**
 * Factory for generating the initial structure
 */
class EnvironmentFactory {
    Logger log = LoggerFactory.getLogger('environment')

	def configFile = project.projectDir.getPath() + '/config.gradle'
	def localConfigFile = project.projectDir.getPath() + '/local.gradle'
	def defaultEnv = 'dev'
	def defaultEnvKey = 'env'
	def base = null
	def local = null

    private Project project = null

    public EnvironmentFactory(Project project) {
        this.project = project
        log.info("Environment: initializing project: {}", project.name)
    }

    /**
     * Loads config.gradle and returns the raw ConfigObject
     *
     * Parsing of config is recursive, returned config is merged with parent project's configuration
     *
     * @return Raw ConfigObject
     */
    protected getRawConfig() {
		File configFile = new File(configFile)
		ConfigObject selfConfig = new ConfigObject()
		if (configFile.exists()) {
			selfConfig = new ConfigSlurper().parse(configFile.toURI().toURL())
			log.info("Environment: project [{}] config file read at: {}", project.name, configFile.toURI().toURL())
		} else {
			log.info("Environment: no config.gradle specified for project: {}, looked at path: {}", project.name, configFile.toURI().toURL())
		}

		ConfigObject parentConfig = new ConfigObject()
		if (project != project.getRootProject()) {
			EnvironmentFactory parentEF = (EnvironmentFactory) project.getParent().extensions.findByName(EnvironmentPlugin.PLUGIN_NAME)
			if (parentEF != null) {
				log.info("Environment: found parent configuration, merging")
				parentConfig.putAll(parentEF.getRawConfig())
			}
		}
		parentConfig.merge(selfConfig)
    }

    /**
     * Loads local.gradle and returns the raw ConfigObject
     *
     * Parsing of the local config is recursive, returned local config is merged with parent project's configuration
     *
     * @return Raw local ConfigObject
     */
    protected getLocalRawConfig() {
		File localConfigFile = new File(localConfigFile)
		ConfigObject selfConfig = new ConfigObject()
		if (localConfigFile.exists()) {
			selfConfig = new ConfigSlurper().parse(localConfigFile.toURI().toURL())
			log.info("Environment: project [{}] local config file read at: {}", project.name, localConfigFile.toURI().toURL())
		} else {
			log.info("Environment: no local.gradle specified for project: {}", project.name)
		}

		ConfigObject parentConfig = new ConfigObject()
		if (project != project.getRootProject()) {
			EnvironmentFactory parentEF = (EnvironmentFactory) project.getParent().extensions.findByName(EnvironmentPlugin.PLUGIN_NAME)
			if (parentEF != null) {
				log.info("Environment: found parent configuration, merging")
				parentConfig.putAll(parentEF.getLocalRawConfig())
			}
		}
		parentConfig.merge(selfConfig)
    }


	protected getBase() {
		if (base == null) {
			try {
				def raw = getRawConfig()
				this.base = init(new ConfigObject(), raw)
			}
			catch (Exception e) {
				throw new InvalidUserDataException("Problem loading configuration file: ${configFile}", e)
			}
		}
		return base;
	}
	
	/**
	 * Recursively initialize environments
	 */
	protected init(baseline, raw) {
		def env = new Environment()
		if (raw.containsKey(defaultEnvKey)) {
			def nest = raw.remove(defaultEnvKey)
			nest.each { key, value ->
				env.nested.put(key, init(baseline.clone().merge(raw), value))
			}
		}
		env.config.merge(baseline).merge(raw)
		env
	}

	/**
	 * Iterate the flattened environment structure. The closure
	 * will be called with the name of the environment as a key
	 * and the environment's configuration as the value. 
	 */
	public forEach(closure) {
		flatten().each { k,v ->
			v.config.put('env',k)
			closure(k,v.config)
		}
	}
	
	/**
	 * Returns the configuration for a single environment.
	 */
	public get(name=defaultEnv) {
		def all = flatten(defaultEnv, null, true)
		if (all.containsKey(name)) {
			all[name].config.put('env',name)
			return all[name].config
		}
		else {
			throw new InvalidUserDataException("Unknown environment: ${name}")
		}
	}

	public getLocal(name=defaultEnv) {
		try {
            def local = getLocalRawConfig()
			def c = new ConfigObject()
			c.merge(get(name))
			if (name==defaultEnv) {
				c.merge(local)
			}
			return c;
		}
		catch (Exception e) {
			throw new InvalidUserDataException("Problem loading local configuration file: ${localConfigFile}", e)
		}
	}
	
	/**
	 * Flatten the environment structure. Returns a map where the key is 
	 * the name of the environment (the full path of the environment is
	 * namespaced with colons) and the value is the environment
	 */
	protected flatten(name=defaultEnv, env=null, getAll=false) {
		if (env==null) env = getBase()
		def all = [:]
		if (getAll || name==defaultEnv) {
			all.put(name, env)
		}
		if (env.nested == [:]) {
			all.put(name, env)
		}
		else {
			env.nested.each { k,v ->
				def n = (name==defaultEnv) ? k : name + ":" + k
				all.putAll(flatten(n, v, getAll))
			}
		}
		all
	}
	
	/**
	 * Convenient access to the embedded environments
	 */
	def propertyMissing(String name) {
		getBase()
		if (name==defaultEnv) {
			return base
		}
		if (!base.nested.containsKey(name)) {
			throw new InvalidUserDataException("Unknown environment: ${name}")
		}
		base.nested[name]
	}
}

class Environment {
	def config = new ConfigObject()
	def nested = [:]
	
	def propertyMissing(String name) {
		if (!nested.containsKey(name)) {
			throw new InvalidUserDataException("Unknown environment: ${name}")
		}
		nested[name]
	}
}
