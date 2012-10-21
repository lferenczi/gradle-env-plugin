package org.gradle.plugin.environment

import org.gradle.api.*;

import groovy.util.ConfigSlurper

/**
 * Plugin to handle multi-dimensional environment mappings
 * 
 * @author lferenczi
 */
class EnvironmentPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.extensions.create("environment", EnvironmentFactory)
	}
}

/**
 * Factory for generating the initial structure
 */
class EnvironmentFactory {
	def configFile = 'config.gradle'
	def localConfigFile = 'local.gradle'
	def defaultEnv = 'dev'
	def defaultEnvKey = 'env'
	def base = null
	def local = null
	
	protected getBase() {
		if (base == null) {
			try {
				def raw = new ConfigSlurper().parse(new File(configFile).toURL())
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
			def File l = new File(localConfigFile)
			def local = (l.exists()) ? new ConfigSlurper().parse(l.toURL()) : new ConfigObject();
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
