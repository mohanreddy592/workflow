package com.netsmartz.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

public class ActivitiTest1
{

  @Test
  public void exclusiveGateway()
  {
    ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
        .setJdbcDriver("com.mysql.jdbc.Driver").setJdbcUrl("jdbc:mysql://localhost/activiti").setJdbcPassword("root")
        .setJdbcUsername("root").setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE).buildProcessEngine();

    RepositoryService repositoryService = processEngine.getRepositoryService();
    RuntimeService runtimeService = processEngine.getRuntimeService();
    IdentityService identityService = processEngine.getIdentityService();
    TaskService taskService = processEngine.getTaskService();
    repositoryService.createDeployment().addClasspathResource("diagrams/Test_1.bpmn20.xml").deploy();

    // remove tasks already present
    // List<Task> availableTaskList = taskService.createTaskQuery().taskName("Work on order").list();
    List<Task> availableTaskList = taskService.createTaskQuery().list();
    for (Task task : availableTaskList)
    {
      taskService.complete(task.getId());
    }

    Map<String, Object> variableMap = new HashMap<String, Object>();
    variableMap.put("result","1");

    identityService.setAuthenticatedUserId("nikhil");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Test_1",variableMap);
    assertNotNull(processInstance.getId());

    List<User> userList = new ArrayList<>();
    userList.add(identityService.createUserQuery().userId("nikhil").singleResult());
    userList.add(identityService.createUserQuery().userId("kermit").singleResult());
    userList.add(identityService.createUserQuery().userId("fozzie").singleResult());

    List<Task> taskList = new ArrayList<>();
    taskList.add(taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult());
    taskList.add(taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult());
    taskList.add(taskService.createTaskQuery().taskDefinitionKey("userTask3").singleResult());

    assertEquals(3,taskList.size());

    for (Task task : taskList)
    {
      System.out.println("found task " + task.getName());
    }

  }
}
