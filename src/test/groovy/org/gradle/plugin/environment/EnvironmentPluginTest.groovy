package org.gradle.plugin.environment;

import static org.junit.Assert.*

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project
import org.gradle.plugin.environment.EnvironmentFactory;
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

class EnvironmentPluginTest {

	Project project;

	@Before
	public void setUp() {
		project = ProjectBuilder.builder().build();
		project.apply plugin: 'environment'
		project.environment.configFile='src/test/resources/config.gradle'
		project.environment.localConfigFile='src/test/resources/local.gradle'
	}

	@Test
	public void directAddressTest() {
		assertTrue(project.extensions.environment instanceof EnvironmentFactory)
		assertTrue(project.environment.test.subone.config.toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subone_schema, dbUser:subone_user, dbPass:subone_pass]'))
		assertTrue(project.environment.test.subtwo.config.toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subtwo_schema, dbUser:subtwo_user, dbPass:subtwo_pass]'))
		assertTrue(project.environment.test.config.toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true]'))
		assertTrue(project.environment.prod.config.toString().equals('[dbUrl:jdbc:mysql://prod.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:false]'))
		assertTrue(project.environment.dev.config.toString().equals('[dbUrl:jdbc:mysql://localhost:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true]'))
	}
	
	@Test
	public void negativeTest() {
		try {
			project.environment.test2.config
			assertTrue(false) // Should never reach
		}
		catch (Exception e) {
			assertTrue(e instanceof InvalidUserDataException)
		}
		try {
			project.environment.test.qwerty.config
			assertTrue(false) // Should never reach
		}
		catch (Exception e) {
			assertTrue(e instanceof InvalidUserDataException)
		}
	}
	
	@Test
	public void singeGetTest() {
		assertTrue(project.environment.get().toString().equals('[dbUrl:jdbc:mysql://localhost:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, env:dev]'))
		assertTrue(project.environment.get("test:subtwo").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subtwo_schema, dbUser:subtwo_user, dbPass:subtwo_pass, env:test:subtwo]'))
		assertTrue(project.environment.get("test:subone").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subone_schema, dbUser:subone_user, dbPass:subone_pass, env:test:subone]'))
		assertTrue(project.environment.get("test:subone:deepsub").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:deepdriver, dbShowSql:true, dbSchema:deepsub, dbUser:subone_user, dbPass:subone_pass, env:test:subone:deepsub]'))
		assertTrue(project.environment.get("test").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, env:test]'))
	}
	
	@Test
	public void eachTest() {
		def all = ['dev', 'test:subone:deepsub', 'test:subtwo', 'uat', 'prod']
		project.environment.forEach { name, config ->
			all.remove(name)
		}
		assertTrue(all.empty)
	}
	
	@Test
	public void localTest() {
		assertTrue(project.environment.getLocal().toString().equals('[dbUrl:jdbc:mysql://localhost:3306, dbDriver:local.dbdriver, dbShowSql:false, env:dev, newprop:newprop]'))
		assertTrue(project.environment.getLocal("test:subtwo").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subtwo_schema, dbUser:subtwo_user, dbPass:subtwo_pass, env:test:subtwo]'))
		assertTrue(project.environment.getLocal("test:subone").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subone_schema, dbUser:subone_user, dbPass:subone_pass, env:test:subone]'))
		assertTrue(project.environment.getLocal("test:subone:deepsub").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:deepdriver, dbShowSql:true, dbSchema:deepsub, dbUser:subone_user, dbPass:subone_pass, env:test:subone:deepsub]'))
		assertTrue(project.environment.getLocal("test").toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, env:test]'))
	}
}
