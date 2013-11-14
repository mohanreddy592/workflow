package com.netsmartz.workflow;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.ParallelGateway;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.ScriptTask;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class DynamicActivitiProcessTest implements TaskListener
{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  @Rule
  public ActivitiRule activitiRule = new ActivitiRule();

  @Test
  public void testDynamicDeploy() throws Exception
  {
    // For Postgres - Make sure that you have database name activiti in your postgres sql server
    /*
     * ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
     * .setJdbcDriver("org.postgresql.Driver").setJdbcUrl("jdbc:postgresql://localhost:5432/activiti").setJdbcPassword("postgres")
     * .setJdbcUsername("postgres").setDatabaseSchemaUpdate("false").buildProcessEngine();
     */
    /*
     * For Mysql - Make sure that you have database name activiti in your mysql server
     */
    ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
        .setJdbcDriver("com.mysql.jdbc.Driver").setJdbcUrl("jdbc:mysql://localhost:3306/activiti").setJdbcPassword("root")
        .setJdbcUsername("root").setDatabaseSchemaUpdate("false").buildProcessEngine();

    // 1. Build up the model from scratch
    BpmnModel model = new BpmnModel();
    Process process = new Process();
    model.addProcess(process);
    process.setId("my-process");
    process.setName("Qmplus test");
    process.addFlowElement(createStartEvent());
    process.addFlowElement(createUserTask("task1","First task","fred"));
    process.addFlowElement(createUserTask("task2","Second task","john"));
    List<SequenceFlow> outgoing = new ArrayList<SequenceFlow>();
    outgoing.add(createSequenceFlow("task3","task4"));
    outgoing.add(createSequenceFlow("task3","task2"));
    process.addFlowElement(createParallelTask("task3","option task",outgoing));
    process.addFlowElement(createEndEvent());
    process.addFlowElement(createScriptTask("task4","script task","12343"));

    process.addFlowElement(createSequenceFlow("start","task1"));
    process.addFlowElement(createSequenceFlow("task1","task3"));
    process.addFlowElement(createSequenceFlow("task3","task2"));
    process.addFlowElement(createSequenceFlow("task3","task4"));
    process.addFlowElement(createSequenceFlow("task2","end"));
    process.addFlowElement(createSequenceFlow("task4","end"));
    // 2. Generate graphical information
    new BpmnAutoLayout(model).execute();
    // 3. Deploy the process to the engine
    Deployment deployment = processEngine.getRepositoryService().createDeployment().addBpmnModel("dynamic-model.bpmn",model)
        .name("Dynamic process deployment").deploy();

    // 4. Start a process instance
    ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("my-process");

    // 5. Check if task is available
    List<Task> tasks = processEngine.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).list();

    org.junit.Assert.assertEquals(1,tasks.size());
    Assert.assertEquals("First task",tasks.get(0).getName());
    Assert.assertEquals("fred",tasks.get(0).getAssignee());

    // 6. Save process diagram to a file
    InputStream processDiagram = processEngine.getRepositoryService().getProcessDiagram(processInstance.getProcessDefinitionId());
    FileUtils.copyInputStreamToFile(processDiagram,new File("diagrams/process.png"));

    // 7. Save resulting BPMN xml to a file
    InputStream processBpmn = processEngine.getRepositoryService().getResourceAsStream(deployment.getId(),"dynamic-model.bpmn");
    FileUtils.copyInputStreamToFile(processBpmn,new File("diagrams/process.bpmn20.xml"));

    // 8. create user
    CreateUser();
    // 9. create group
    createGroup();
    // 10. assign process to user

  }

  protected UserTask createUserTask(String id, String name, String assignee)
  {
    UserTask userTask = new UserTask();
    userTask.setName(name);
    userTask.setId(id);
    userTask.setAssignee(assignee);
    return userTask;
  }

  protected SequenceFlow createSequenceFlow(String from, String to)
  {
    SequenceFlow flow = new SequenceFlow();
    flow.setSourceRef(from);
    flow.setTargetRef(to);
    return flow;
  }

  protected ParallelGateway createParallelTask(String id, String name, List<SequenceFlow> outgoingFlows)
  {
    ParallelGateway p = new ParallelGateway();
    p.setId(id);
    p.setName(name);
    p.setOutgoingFlows(outgoingFlows);
    return p;

  }

  protected ScriptTask createScriptTask(String id, String name, String script)
  {
    ScriptTask st = new ScriptTask();
    st.setId(id);
    st.setScript("out.println(" + script + ")");
    st.setScriptFormat("groovy");
    st.setName(name);
    return st;
  }

  protected StartEvent createStartEvent()
  {
    StartEvent startEvent = new StartEvent();
    startEvent.setId("start");
    return startEvent;
  }

  protected EndEvent createEndEvent()
  {
    EndEvent endEvent = new EndEvent();
    endEvent.setId("end");
    return endEvent;
  }

  protected void CreateUser()
  {
    IdentityService identityService = activitiRule.getIdentityService();
    User newUser = identityService.newUser("kamal");
    newUser.setEmail("kamalarora033@gmail.com");
    newUser.setFirstName("kamal");
    newUser.setId("kamal");
    newUser.setLastName("arora");
    newUser.setPassword("arora");
    identityService.saveUser(newUser);
  }

  protected void createGroup()
  {
    IdentityService identityService = activitiRule.getIdentityService();
    Group newGroup = identityService.newGroup("development");
    newGroup.setType("test group");
    newGroup.setName("Java");
    identityService.saveGroup(newGroup);
    identityService.createMembership("kamal","development");
    identityService.setAuthenticatedUserId("kamal");
  }

  public void notify(DelegateTask delegateTask)
  {
    /*
     * IdentityService idS = activitiRule.getIdentityService();
     * UserQuery uq = idS.createUserQuery();
     * User user = uq.userEmailLike("kamalarora033@gmail.com").singleResult();
     */
    // TODO send email when a user assigned a task or process
  }
}