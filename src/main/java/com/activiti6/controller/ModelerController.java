package com.activiti6.controller;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.identity.Group;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 流程控制器
 */
@Controller
public class ModelerController {

    private static final Logger logger = LoggerFactory.getLogger(ModelerController.class);

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;


    @RequestMapping("index")
    public ModelAndView index(ModelAndView modelAndView) {
        modelAndView.setViewName("index");
        List<Model> modelList = repositoryService.createModelQuery().list();

        modelAndView.addObject("modelList", repositoryService.createModelQuery().list());
        return modelAndView;
    }

    /**
     * 跳转编辑器页面
     *
     * @return
     */
    @GetMapping("editor")
    public String editor() {
        return "modeler";
    }


    /**
     * 创建模型
     *
     * @param response
     * @param name     模型名称
     * @param key      模型key
     */
    @RequestMapping("/create")
    public void create(HttpServletResponse response, String name, String key) throws IOException {
        logger.info("创建模型入参name：{},key:{}", name, key);
        Model model = repositoryService.newModel();
        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
        modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, "");
        modelNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
        model.setName(name);
        model.setKey(key);
        model.setMetaInfo(modelNode.toString());
        repositoryService.saveModel(model);
        createObjectNode(model.getId());
        response.sendRedirect("/editor?modelId=" + model.getId());
        logger.info("创建模型结束，返回模型ID：{}", model.getId());
    }

    /**
     * 创建模型时完善ModelEditorSource
     *
     * @param modelId
     */
    @SuppressWarnings("deprecation")
    private void createObjectNode(String modelId) {
        logger.info("创建模型完善ModelEditorSource入参模型ID：{}", modelId);
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        try {
            repositoryService.addModelEditorSource(modelId, editorNode.toString().getBytes("utf-8"));
        } catch (Exception e) {
            logger.info("创建模型时完善ModelEditorSource服务异常：{}", e);
        }
        logger.info("创建模型完善ModelEditorSource结束");
    }

    /**
     * 发布流程
     *
     * @param modelId 模型ID
     * @return
     */
    @ResponseBody
    @RequestMapping("/publish")
    public Object publish(String modelId) {
        logger.info("流程部署入参modelId：{}", modelId);
        Map<String, String> map = new HashMap<String, String>();
        try {
            Model modelData = repositoryService.getModel(modelId);
            byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
            if (bytes == null) {
                logger.info("部署ID:{}的模型数据为空，请先设计流程并成功保存，再进行发布", modelId);
                map.put("code", "FAILURE");
                return map;
            }
            JsonNode modelNode = new ObjectMapper().readTree(bytes);
            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            Deployment deployment = repositoryService.createDeployment()
                    .name(modelData.getName())
                    .addBpmnModel(modelData.getKey() + ".bpmn20.xml", model)
                    .deploy();
            modelData.setDeploymentId(deployment.getId());
            repositoryService.saveModel(modelData);
            map.put("code", "SUCCESS");
        } catch (Exception e) {
            logger.info("部署modelId:{}模型服务异常：{}", modelId, e);
            map.put("code", "FAILURE");
        }
        logger.info("流程部署出参map：{}", map);
        return map;
    }

    /**
     * 撤销流程定义
     *
     * @param modelId 模型ID
     * @param result
     * @return
     */
    @ResponseBody
    @RequestMapping("/revokePublish")
    public Object revokePublish(String modelId) {
        logger.info("撤销发布流程入参modelId：{}", modelId);
        Map<String, String> map = new HashMap<String, String>();
        Model modelData = repositoryService.getModel(modelId);
        if (null != modelData) {
            try {
                /**
                 * 参数不加true:为普通删除，如果当前规则下有正在执行的流程，则抛异常
                 * 参数加true:为级联删除,会删除和当前规则相关的所有信息，包括历史
                 */
                repositoryService.deleteDeployment(modelData.getDeploymentId(), true);
                map.put("code", "SUCCESS");
            } catch (Exception e) {
                logger.error("撤销已部署流程服务异常：{}", e);
                map.put("code", "FAILURE");
            }
        }
        logger.info("撤销发布流程出参map：{}", map);
        return map;
    }

    /**
     * 删除流程实例
     *
     * @param modelId 模型ID
     * @param result
     * @return
     */
    @ResponseBody
    @RequestMapping("/delete")
    public Object deleteProcessInstance(String modelId) {
        logger.info("删除流程实例入参modelId：{}", modelId);
        Map<String, String> map = new HashMap<String, String>();
        Model modelData = repositoryService.getModel(modelId);
        if (null != modelData) {
            try {
                ProcessInstance pi = runtimeService.createProcessInstanceQuery().processDefinitionKey(modelData.getKey()).singleResult();
                if (null != pi) {
                    runtimeService.deleteProcessInstance(pi.getId(), "");
                    historyService.deleteHistoricProcessInstance(pi.getId());
                }
                map.put("code", "SUCCESS");
            } catch (Exception e) {
                logger.error("删除流程实例服务异常：{}", e);
                map.put("code", "FAILURE");
            }
        }
        logger.info("删除流程实例出参map：{}", map);
        return map;
    }

    /**
     * 删除流程实例
     *
     * @param modelId 模型ID
     * @param result
     * @return
     */
    @ResponseBody
    @RequestMapping("/deleteModel")
    public Object deleteModel(String modelId) {
        logger.info("删除模型入参 modelId：{}", modelId);
        Map<String, String> map = new HashMap<>();

        try {
            // 1. 直接删除模型（核心修改点）
            repositoryService.deleteModel(modelId);

            // 2. 可选：如果模型已部署，可同时删除部署（根据业务需求）
            Model model = repositoryService.getModel(modelId);
            if (model != null && model.getDeploymentId() != null) {
                repositoryService.deleteDeployment(model.getDeploymentId(), true); // true 表示级联删除流程实例
            }

            map.put("code", "SUCCESS");
        } catch (Exception e) {
            logger.error("删除模型失败：", e);
            map.put("code", "FAILURE");
            map.put("error", e.getMessage());
        }

        logger.info("删除模型出参 map：{}", map);
        return map;
    }


    /**
     * 查询所有运行中的流程实例
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("/queryProcInstance")
    public Object queryProcInstance() {
        logger.info("查询所有运行中的流程实例");
        Map<String, Object> resultMap = new HashMap<>();
        try {
            List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
            resultMap.put("code", "SUCCESS");
            resultMap.put("data", processInstances);
        } catch (Exception e) {
            logger.error("查询运行中的流程实例服务异常：{}", e);
            resultMap.put("code", "FAILURE");
            resultMap.put("error", e.getMessage());
        }
        logger.info("查询运行中的流程实例出参resultMap：{}", resultMap);
        return resultMap;
    }

    /**
     * 根据模型id，开启一个流程
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("/startProcess")
    public Object startProcess(String processDefKey) {
        logger.info("根据模型ID开启流程入参Id：{}", processDefKey);
        Map<String, String> map = new HashMap<>();
        try {
            runtimeService.startProcessInstanceByKey(processDefKey);
            map.put("code", "SUCCESS");
        } catch (Exception e) {
            logger.error("根据模型ID开启流程服务异常：{}", e);
            map.put("code", "FAILURE");
            map.put("error", e.getMessage());
        }
        logger.info("根据模型ID开启流程出参map：{}", map);
        return map;
    }

    /**
     * 委派任务
     *
     * @param taskId 任务ID
     * @param userId 委派给的用户的ID
     * @return
     */
    @ResponseBody
    @RequestMapping("/delegateTask")
    public Object delegateTask(String taskId, String userId) {
        logger.info("委派任务入参taskId：{}, userId：{}", taskId, userId);
        Map<String, String> map = new HashMap<>();
        try {
            taskService.delegateTask(taskId, userId);
            map.put("code", "SUCCESS");
        } catch (Exception e) {
            logger.error("委派任务服务异常：{}", e);
            map.put("code", "FAILURE");
            map.put("error", e.getMessage());
        }
        logger.info("委派任务出参map：{}", map);
        return map;
    }

    /**
     * 收回任务委派，可查询当前登录用户的owner任务，任务id暴露给用户，可实现收回委派；
     * 若其他用户通过url直接取消委派，则拒绝；
     *
     * @param taskId 任务id
     * @return
     */
    @ResponseBody
    @RequestMapping("/resolveTask")
    public Object resolveTask(String userID, String taskId) {
        logger.info("收回委派任务入参taskId：{}", taskId);
        Map<String, String> map = new HashMap<>();
        try {
            // 获取当前任务的原处理人（owner）
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                map.put("code", "FAILURE");
                map.put("error", "任务不存在");
                return map;
            }

            if (!task.getOwner().equals(userID)) {
                map.put("code", "FAILURE");
                map.put("error", "当前用户不是该任务的所有者");
                return map;
            }

            // 调用 resolveTask 方法，将任务返回给原处理人
            taskService.resolveTask(taskId);
            map.put("code", "SUCCESS");
        } catch (Exception e) {
            logger.error("收回委派任务异常：{}", e.getMessage());
            map.put("code", "FAILURE");
            map.put("error", e.getMessage());
        }
        logger.info("收回委派任务出参map：{}", map);
        return map;
    }

    /**
     * 查询某个用户名下的所有任务，包括 owner 和 assignee
     *
     * @param username 用户名
     * @return
     */
    @ResponseBody
    @RequestMapping("/queryUserTasks")
    public Object queryUserTasks(String username) {
        logger.info("查询用户名为 {} 的所有任务", username);
        Map<String, Object> resultMap = new HashMap<>();
        try {
            Set<String> taskIdSet = new HashSet<>();
            List<Task> tasksAsOwner = taskService.createTaskQuery().taskOwner(username).list();
            List<Task> tasksAsAssignee = taskService.createTaskQuery().taskAssignee(username).list();
            List<Map<String, String>> allTasks = new ArrayList<>();
            for (Task task : tasksAsOwner) {
                if (taskIdSet.add(task.getId())) {
                    Map<String, String> taskInfo = new HashMap<>();
                    taskInfo.put("id", task.getId());
                    taskInfo.put("name", task.getName());
                    allTasks.add(taskInfo);
                }
            }
            for (Task task : tasksAsAssignee) {
                if (taskIdSet.add(task.getId())) {
                    Map<String, String> taskInfo = new HashMap<>();
                    taskInfo.put("id", task.getId());
                    taskInfo.put("name", task.getName());
                    allTasks.add(taskInfo);
                }
            }
            resultMap.put("code", "SUCCESS");
            resultMap.put("data", allTasks);
        } catch (Exception e) {
            logger.error("查询用户名为 {} 的任务服务异常：{}", username, e.getMessage());
            resultMap.put("code", "FAILURE");
            resultMap.put("error", e.getMessage());
        }
        logger.info("查询用户名为 {} 的所有任务出参resultMap：{}", username, resultMap);
        return resultMap;
    }


    @Autowired
    private IdentityService identityService;

    /**
     * 根据用户ID查询用户所在的组
     *
     * @param userId 用户ID
     * @return 用户所在的组列表
     */
    @ResponseBody
    @RequestMapping("/getUserGroups")
    public Object getUserGroups(String userId) {
        logger.info("根据用户ID查询用户所在的组，入参userId：{}", userId);
        Map<String, Object> resultMap = new HashMap<>();
        try {
            List<Group> groups = identityService.createGroupQuery().groupMember(userId).list();
            List<Map<String, String>> groupList = new ArrayList<>();
            for (Group group : groups) {
                Map<String, String> groupInfo = new HashMap<>();
                groupInfo.put("id", group.getId());
                groupInfo.put("name", group.getName());
                groupList.add(groupInfo);
            }
            resultMap.put("code", "SUCCESS");
            resultMap.put("data", groupList);
        } catch (Exception e) {
            logger.error("根据用户ID查询用户所在组服务异常：{}", e);
            resultMap.put("code", "FAILURE");
            resultMap.put("error", e.getMessage());
        }
        logger.info("根据用户ID查询用户所在组出参resultMap：{}", resultMap);
        return resultMap;
    }
}
