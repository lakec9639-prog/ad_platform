package com.ad.mapper;

import com.ad.entity.RuleExecutionLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RuleExecutionLogMapper extends BaseMapper<RuleExecutionLog> {

    List<RuleExecutionLog> selectByRuleId(
            @Param("ruleId") Long ruleId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);
}
