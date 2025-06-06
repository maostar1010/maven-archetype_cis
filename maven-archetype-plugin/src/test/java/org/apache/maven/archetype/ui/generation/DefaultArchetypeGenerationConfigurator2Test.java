/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.archetype.ui.generation;

import java.io.File;
import java.util.Properties;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isNull;

/**
 * Tests the ability to use variables in default fields in batch mode.
 */
public class DefaultArchetypeGenerationConfigurator2Test extends PlexusTestCase {
    private DefaultArchetypeGenerationConfigurator configurator;
    private ArchetypeGenerationQueryer queryer;
    private ArchetypeDescriptor descriptor;

    @Override
    protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
        configuration.setClassPathScanning("index");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        configurator = (DefaultArchetypeGenerationConfigurator) lookup(ArchetypeGenerationConfigurator.ROLE);

        descriptor = new ArchetypeDescriptor();
        RequiredProperty groupId = new RequiredProperty();
        groupId.setKey("groupId");
        groupId.setDefaultValue("com.example.${groupName}");
        RequiredProperty artifactId = new RequiredProperty();
        artifactId.setKey("artifactId");
        artifactId.setDefaultValue("${serviceName}");
        RequiredProperty thePackage = new RequiredProperty();
        thePackage.setKey("package");
        thePackage.setDefaultValue("com.example.${groupName}");
        RequiredProperty groupName = new RequiredProperty();
        groupName.setKey("groupName");
        groupName.setDefaultValue(null);
        RequiredProperty serviceName = new RequiredProperty();
        serviceName.setKey("serviceName");
        serviceName.setDefaultValue(null);
        descriptor.addRequiredProperty(groupId);
        descriptor.addRequiredProperty(artifactId);
        descriptor.addRequiredProperty(thePackage);
        descriptor.addRequiredProperty(groupName);
        descriptor.addRequiredProperty(serviceName);

        ArchetypeArtifactManager manager = EasyMock.createMock(ArchetypeArtifactManager.class);

        File archetype = new File("archetype.jar");

        EasyMock.expect(manager.exists(
                        eq("archetypeGroupId"),
                        eq("archetypeArtifactId"),
                        eq("archetypeVersion"),
                        anyObject(),
                        anyObject()))
                .andReturn(true);
        EasyMock.expect(manager.getArchetypeFile(
                        eq("archetypeGroupId"),
                        eq("archetypeArtifactId"),
                        eq("archetypeVersion"),
                        anyObject(),
                        anyObject()))
                .andReturn(archetype);
        EasyMock.expect(manager.isFileSetArchetype(archetype)).andReturn(true);
        EasyMock.expect(manager.isOldArchetype(archetype)).andReturn(false);
        EasyMock.expect(manager.getFileSetArchetypeDescriptor(archetype)).andReturn(descriptor);

        EasyMock.replay(manager);
        configurator.setArchetypeArtifactManager(manager);

        queryer = EasyMock.mock(ArchetypeGenerationQueryer.class);
        configurator.setArchetypeGenerationQueryer(queryer);
    }

    public void testJIRA509FileSetArchetypeDefaultsWithVariables() throws Exception {
        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest();
        request.setArchetypeGroupId("archetypeGroupId");
        request.setArchetypeArtifactId("archetypeArtifactId");
        request.setArchetypeVersion("archetypeVersion");
        Properties properties = new Properties();
        properties.setProperty("groupName", "myGroupName");
        properties.setProperty("serviceName", "myServiceName");

        configurator.configureArchetype(request, Boolean.FALSE, properties);

        assertEquals("com.example.myGroupName", request.getGroupId());
        assertEquals("myServiceName", request.getArtifactId());
        assertEquals("1.0-SNAPSHOT", request.getVersion());
        assertEquals("com.example.myGroupName", request.getPackage());
    }

    public void testInteractive() throws Exception {
        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest();
        request.setArchetypeGroupId("archetypeGroupId");
        request.setArchetypeArtifactId("archetypeArtifactId");
        request.setArchetypeVersion("archetypeVersion");
        Properties properties = new Properties();

        EasyMock.expect(queryer.getPropertyValue(eq("groupName"), anyString(), isNull()))
                .andReturn("myGroupName");

        EasyMock.expect(queryer.getPropertyValue(eq("serviceName"), anyString(), isNull()))
                .andReturn("myServiceName");

        EasyMock.expect(queryer.getPropertyValue(anyString(), anyString(), anyObject()))
                .andAnswer(new IAnswer<String>() {

                    @Override
                    public String answer() throws Throwable {
                        return (String) EasyMock.getCurrentArguments()[1];
                    }
                })
                .anyTimes();

        EasyMock.expect(queryer.confirmConfiguration(anyObject())).andReturn(Boolean.TRUE);

        EasyMock.replay(queryer);
        configurator.configureArchetype(request, Boolean.TRUE, properties);

        assertEquals("com.example.myGroupName", request.getGroupId());
        assertEquals("myServiceName", request.getArtifactId());
        assertEquals("1.0-SNAPSHOT", request.getVersion());
        assertEquals("com.example.myGroupName", request.getPackage());
    }

    public void testArchetype406ComplexCustomPropertyValue() throws Exception {
        RequiredProperty custom = new RequiredProperty();
        custom.setKey("serviceUpper");
        custom.setDefaultValue("${serviceName.toUpperCase()}");
        descriptor.addRequiredProperty(custom);

        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest();
        request.setArchetypeGroupId("archetypeGroupId");
        request.setArchetypeArtifactId("archetypeArtifactId");
        request.setArchetypeVersion("archetypeVersion");
        Properties properties = new Properties();

        EasyMock.expect(queryer.getPropertyValue(eq("groupName"), anyString(), isNull()))
                .andReturn("myGroupName");

        EasyMock.expect(queryer.getPropertyValue(eq("serviceName"), anyString(), isNull()))
                .andReturn("myServiceName");

        EasyMock.expect(queryer.getPropertyValue(anyString(), anyString(), anyObject()))
                .andAnswer(new IAnswer<String>() {

                    @Override
                    public String answer() throws Throwable {
                        return (String) EasyMock.getCurrentArguments()[1];
                    }
                })
                .anyTimes();

        EasyMock.expect(queryer.confirmConfiguration(anyObject())).andReturn(Boolean.TRUE);

        EasyMock.replay(queryer);
        configurator.configureArchetype(request, Boolean.TRUE, properties);

        assertEquals("MYSERVICENAME", request.getProperties().get("serviceUpper"));
    }

    public void testArchetype618() throws Exception {
        RequiredProperty custom = getRequiredProperty("serviceName");
        custom.setKey("camelArtifact");
        custom.setDefaultValue(
                "${artifactId.class.forName('org.codehaus.plexus.util.StringUtils').capitaliseAllWords($artifactId.replaceAll('[^A-Za-z_\\$0-9]', ' ').replaceFirst('^(\\d)', '_$1').replaceAll('\\d', '$0 ').replaceAll('[A-Z](?=[^A-Z])', ' $0').toLowerCase()).replaceAll('\\s', '')}");
        descriptor.addRequiredProperty(custom);

        getRequiredProperty("artifactId").setDefaultValue(null);

        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest();
        request.setArchetypeGroupId("archetypeGroupId");
        request.setArchetypeArtifactId("archetypeArtifactId");
        request.setArchetypeVersion("archetypeVersion");
        Properties properties = new Properties();

        EasyMock.expect(queryer.getPropertyValue(eq("groupName"), anyString(), isNull()))
                .andReturn("myGroupName");

        EasyMock.expect(queryer.getPropertyValue(eq("artifactId"), anyString(), isNull()))
                .andReturn("my-service-name");

        EasyMock.expect(queryer.getPropertyValue(anyString(), anyString(), anyObject()))
                .andAnswer(new IAnswer<String>() {

                    @Override
                    public String answer() throws Throwable {
                        return (String) EasyMock.getCurrentArguments()[1];
                    }
                })
                .anyTimes();

        EasyMock.expect(queryer.confirmConfiguration(anyObject())).andReturn(Boolean.TRUE);

        EasyMock.replay(queryer);
        configurator.configureArchetype(request, Boolean.TRUE, properties);

        assertEquals("MyServiceName", request.getProperties().get("camelArtifact"));
    }

    private RequiredProperty getRequiredProperty(String propertyName) {
        if (propertyName != null) {
            for (RequiredProperty candidate : descriptor.getRequiredProperties()) {
                if (propertyName.equals(candidate.getKey())) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
