package com.activiti6.config;

import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.data.UserDataManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 引用本系统的用户权限体系来实现Activiti查询逻辑即可
 */
@Component
public class CustomerUserManager implements UserDataManager {

    // @Autowired
    // private UserDataSource userDataSource;

    @Override
    public List<User> findUserByQueryCriteria(UserQueryImpl query, Page page) {
        return null;
    }

    @Override
    public long findUserCountByQueryCriteria(UserQueryImpl query) {
        return 0;
    }

    @Override
    public List<Group> findGroupsByUser(String userId) {
        return null;
    }

    @Override
    public List<User> findUsersByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
        return null;
    }

    @Override
    public long findUserCountByNativeQuery(Map<String, Object> parameterMap) {
        return 0;
    }

    @Override
    public UserEntity create() {
        return null;
    }

    @Override
    public UserEntity findById(String entityId) {
        return null;
    }

    @Override
    public void insert(UserEntity entity) {

    }

    @Override
    public UserEntity update(UserEntity entity) {
        return null;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public void delete(UserEntity entity) {

    }
}
