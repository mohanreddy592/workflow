package com.netsmartz.workflow;

import static org.junit.Assert.assertNotNull;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;

public class SimpleProcessTest
{

  @Test
  public void startBookOrder()
  {
    ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();

    RuntimeService runtimeService = processEngine.getRuntimeService();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    repositoryService.createDeployment().addClasspathResource("diagrams/bookorder.simple.bpmn20.xml").deploy();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simplebookorder2");
    assertNotNull(processInstance.getId());
    System.out.println("id " + processInstance.getId() + " " + processInstance.getProcessDefinitionId());
  }

}
