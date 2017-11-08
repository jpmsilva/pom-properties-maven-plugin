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

package io.github.jpmsilva.ppmp;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.Validate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;

/**
 * Reads properties stored in dependencies and imports them into the scope of the project running this mojo.
 *
 * @author Joao Silva
 */
@Mojo(name = "read-dependency-pom", defaultPhase = INITIALIZE, threadSafe = true, requiresDependencyResolution = COMPILE_PLUS_RUNTIME, requiresDependencyCollection = COMPILE_PLUS_RUNTIME)
public class ReadDependencyPomMojo extends AbstractMojo {

  /**
   * The repository system, used to resolve dependencies.
   */
  @Component
  private RepositorySystem repositorySystem;

  /**
   * The project builder, used to resolve the model of the dependencies.
   */
  @Component
  private ProjectBuilder projectBuilder;

  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * The list of dependencies from which to load properties.
   */
  @Parameter(property = "dependencies")
  private List<Dependency> dependencies = Collections.emptyList();

  /**
   * The Maven session.
   */
  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession mavenSession;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Validate.notNull(repositorySystem);
    Validate.notNull(projectBuilder);
    Validate.notNull(project);
    Validate.notNull(dependencies);
    Validate.notNull(mavenSession);
    for (Dependency dependency : dependencies) {
      Properties projectProperties = getMavenProject(dependency).getProperties();
      getLog().info("Adding properties from dependency " + dependency + ": " + projectProperties);
      project.getProperties().putAll(projectProperties);
    }
  }

  private MavenProject getMavenProject(Dependency dependency) throws MojoExecutionException {
    try {
      Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
      ProjectBuildingResult result = projectBuilder.build(artifact, mavenSession.getProjectBuildingRequest());
      return result.getProject();
    } catch (ProjectBuildingException e) {
      throw new MojoExecutionException("Could not obtain artifact for dependency " + dependency, e);
    }
  }

}
