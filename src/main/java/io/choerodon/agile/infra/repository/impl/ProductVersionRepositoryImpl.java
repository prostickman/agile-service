package io.choerodon.agile.infra.repository.impl;

import io.choerodon.agile.infra.common.annotation.DataLog;
import io.choerodon.agile.infra.common.utils.RedisUtil;
import io.choerodon.agile.infra.dataobject.VersionIssueDO;
import io.choerodon.core.exception.CommonException;
import io.choerodon.agile.domain.agile.converter.ProductVersionConverter;
import io.choerodon.agile.domain.agile.entity.ProductVersionE;
import io.choerodon.agile.domain.agile.repository.ProductVersionRepository;
import io.choerodon.agile.infra.dataobject.ProductVersionDO;
import io.choerodon.agile.infra.mapper.ProductVersionMapper;
import io.choerodon.mybatis.helper.OptionalHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Created by jian_zhang02@163.com on 2018/5/14.
 */
@Component
public class ProductVersionRepositoryImpl implements ProductVersionRepository {

    @Autowired
    private ProductVersionMapper versionMapper;
    @Autowired
    private ProductVersionConverter versionConverter;
    @Autowired
    private RedisUtil redisUtil;

    private static final String INSERT_ERROR = "error.version.insert";
    private static final String DELETE_ERROR = "error.version.delete";
    private static final String UPDATE_ERROR = "error.version.update";
    private static final String AGILE = "Agile:";
    private static final String PIECHART = AGILE + "PieChart";
    private static final String FIX_VERSION = "fixVersion";

    @Override
    public ProductVersionE createVersion(ProductVersionE versionE) {
        ProductVersionDO version = versionConverter.entityToDo(versionE);
        if (versionMapper.insertSelective(version) != 1) {
            throw new CommonException(INSERT_ERROR);
        }
        redisUtil.deleteRedisCache(new String[]{PIECHART + versionE.getProjectId() + ':' + FIX_VERSION + "*"});
        return versionConverter.doToEntity(versionMapper.selectByPrimaryKey(version.getVersionId()));
    }

    @Override
    @DataLog(type = "batchDeleteVersionByVersion", single = false)
    public Boolean deleteVersion(ProductVersionE versionE) {
        ProductVersionDO version = versionConverter.entityToDo(versionE);
        if (versionMapper.delete(version) != 1) {
            throw new CommonException(DELETE_ERROR);
        }
        return true;
    }

    @Override
    public ProductVersionE updateVersion(ProductVersionE versionE, List<String> fieldList) {
        ProductVersionDO version = versionConverter.entityToDo(versionE);
        OptionalHelper.optional(fieldList);
        if (versionMapper.updateOptional(version) != 1) {
            throw new CommonException(UPDATE_ERROR);
        }
        redisUtil.deleteRedisCache(new String[]{PIECHART + versionE.getProjectId() + ':' + FIX_VERSION + "*"});
        return versionConverter.doToEntity(versionMapper.selectByPrimaryKey(version.getVersionId()));
    }

    @Override
    @DataLog(type = "batchMoveVersion", single = false)
    public Boolean batchIssueToDestination(Long projectId, Long targetVersionId, List<VersionIssueDO> versionIssues, Date date, Long userId) {
        versionMapper.issueToDestination(projectId, targetVersionId, versionIssues, date, userId);
        return true;
    }

    @Override
    public Boolean releaseVersion(Long projectId, Long versionId, Date releaseDate) {
        versionMapper.releaseVersion(projectId, versionId, releaseDate);
        return true;
    }

    @Override
    public ProductVersionE updateVersion(ProductVersionE versionE) {
        ProductVersionDO version = versionConverter.entityToDo(versionE);
        if (versionMapper.updateByPrimaryKeySelective(version) != 1) {
            throw new CommonException(UPDATE_ERROR);
        }
        redisUtil.deleteRedisCache(new String[]{PIECHART + versionE.getProjectId() + ':' + FIX_VERSION + "*"});
        return versionConverter.doToEntity(versionMapper.selectByPrimaryKey(version.getVersionId()));
    }

    @Override
    public int deleteByVersionIds(Long projectId, List<Long> versionIds) {
        redisUtil.deleteRedisCache(new String[]{PIECHART + projectId + ':' + FIX_VERSION + "*"});
        return versionMapper.deleteByVersionIds(projectId, versionIds);
    }

    @Override
    public int batchUpdateSequence(Integer sequence, Long projectId, Integer add, Long versionId) {
        return versionMapper.batchUpdateSequence(sequence, projectId, add, versionId);
    }

}
