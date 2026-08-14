package com.ad.mapper;

import com.ad.entity.Campaign;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CampaignMapper extends BaseMapper<Campaign> {

    int updateBatchStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);
}
