package org.gradle.plugin.environment

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
/**
 *
 *
 * @author ferenczil
 */
class EnvironmentMultiProjectTest {
    Project project;
    Project child;

    @Before
    public void setUp() {
        project = ProjectBuilder.builder().build();
        project.apply plugin: 'environment'
        project.environment.configFile='src/test/resources/config.gradle'
        project.environment.localConfigFile='src/test/resources/local.gradle'

        child = ProjectBuilder.builder().withParent(project).withName("child").build()
        child.apply plugin: 'environment'
        child.environment.configFile='src/test/resources/config-child.gradle'
        child.environment.localConfigFile='src/test/resources/local-child.gradle'
    }

    public void directAddressTest() {
        assertTrue(project.extensions.environment instanceof EnvironmentFactory)
        assertTrue(project.environment.test.subone.config.toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subone_schema, dbUser:subone_user, dbPass:subone_pass]'))
        assertTrue(project.environment.test.subtwo.config.toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true, dbSchema:subtwo_schema, dbUser:subtwo_user, dbPass:subtwo_pass]'))
        assertTrue(project.environment.test.config.toString().equals('[dbUrl:jdbc:mysql://test.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true]'))
        assertTrue(project.environment.prod.config.toString().equals('[dbUrl:jdbc:mysql://prod.env.mysql.url:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:false]'))
        assertTrue(project.environment.dev.config.toString().equals('[dbUrl:jdbc:mysql://localhost:3306, dbDriver:com.mysql.jdbc.Driver, dbShowSql:true]'))
    }

    @Test
    public void directAddressChildTest() {
        assertTrue(child.extensions.environment instanceof EnvironmentFactory)

        def prod = child.environment.prod.config
        // config overridden in child, in environment
        assertEquals("jdbc:mysql://child-prod.env.mysql.url:3306", prod.dbUrl)
        // config was only overridden in parent's environment
        assertEquals(false, prod.dbShowSql)
        // config from parent's defaults
        assertEquals("com.mysql.jdbc.Driver", prod.dbDriver)

        def subone = child.environment.test.subone.config
        // from a sub-environment, defined in parent
        assertEquals('subone_user', subone.dbUser)
        // from a sub-environment, defined in child (override)
        assertEquals('subone_schema_child', subone.dbSchema)

        def deepsub = child.environment.test.subone.deepsub.config
        // parent's deep environment, no override in child
        assertEquals("deepdriver", deepsub.dbDriver)
        // deep environment override in child
        assertEquals("deepsub_child", deepsub.dbSchema)
        // overridden in child's defaults
        assertEquals(false, prod.dbShowSql)
    }

    @Test
    public void testEnvInheritance() {
        // uat is not defined in config-child.gradle
        // must exist since it's defined in parent
        def uat = child.environment.uat.config

        assertNotNull(uat)

        // defined in parent's env
        assertEquals("jdbc:mysql://uat.env.mysql", uat.dbUrl)
        // defined in parent's env, (this time it's a string not a boolean)
        assertEquals("false", uat.dbShowSql)
        // config from parent's defaults
        assertEquals("com.mysql.jdbc.Driver", uat.dbDriver)
    }

    @Test
    public void localTest() {
        def local = child.environment.getLocal()
        // overridden in child's local.gradle
        assertEquals("local.dbdriver.child", local.dbDriver)
        assertEquals("newprop-child", local.newprop)

        // overridden in parent's local.gradle
        assertEquals(false, local.dbShowSql)

        // no local override, but expect value from child's config.gradle
        assertEquals("jdbc:mysql://child-localhost:3306", local.dbUrl)
    }

}
