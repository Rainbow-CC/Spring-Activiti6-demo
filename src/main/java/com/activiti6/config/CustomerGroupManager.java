package com.activiti6.config;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.GroupQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupEntityImpl;
import org.activiti.engine.impl.persistence.entity.data.GroupDataManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 委托模式，实现逻辑的是 XXXDataManager 实现类，XXXEntityManager 是上层代理
 */
@Slf4j
@Component
public class CustomerGroupManager implements GroupDataManager {


    /**
     * IdentityService #createGroupQuery() 会调用此方法，来查询用户组信息
     * @param query
     * @param page
     * @return
     */
    @Override
    public List<Group> findGroupByQueryCriteria(GroupQueryImpl query, Page page) {
        // 测试查询调用
        log.info("User findGroupByQueryCriteria method");
        List<Group> list = new ArrayList<>();
        GroupEntity groupEntity = new GroupEntityImpl();
        groupEntity.setName("group1");
        groupEntity.setId("1");
        list.add(groupEntity);
        return list;
    }

    @Override
    public long findGroupCountByQueryCriteria(GroupQueryImpl query) {
        return 0;
    }

    /**
     * 通过系统的角色体系，查找用户拥有的角色，返回角色列表
     * @param userId
     * @return
     */
    @Override
    public List<Group> findGroupsByUser(String userId) {
        List<Group> list = new ArrayList<>();
        GroupEntity groupEntity = new GroupEntityImpl();
        groupEntity.setName("group1");
        groupEntity.setId("1");

        list.add(groupEntity);
        return list;
    }

    @Override
    public List<Group> findGroupsByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
        return null;
    }

    @Override
    public long findGroupCountByNativeQuery(Map<String, Object> parameterMap) {
        return 0;
    }




    @Override
    public GroupEntity create() {
        return null;
    }

    @Override
    public GroupEntity findById(String entityId) {
        return null;
    }

    @Override
    public void insert(GroupEntity entity) {

    }



    @Override
    public GroupEntity update(GroupEntity entity) {
        return null;
    }



    @Override
    public void delete(String id) {

    }

    @Override
    public void delete(GroupEntity entity) {

    }

}
