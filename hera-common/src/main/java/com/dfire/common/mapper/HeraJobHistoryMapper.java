package com.dfire.common.mapper;

import com.dfire.common.entity.HeraJobHistory;
import com.dfire.common.entity.vo.JobLogHistoryVo;
import com.dfire.common.entity.vo.PageHelperTimeRange;
import com.dfire.common.mybatis.HeraInsertLangDriver;
import com.dfire.common.mybatis.HeraSelectLangDriver;
import com.dfire.common.mybatis.HeraUpdateLangDriver;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε5:08 2018/4/23
 * @desc
 */
public interface HeraJobHistoryMapper {

    String SELECT_BY_PAGE_GROUP_WHERE = " where ( b.group_id = #{jobId} )"
            + " and (a.start_time>=CAST(#{beginDt,jdbcType=VARCHAR} AS date) and  a.start_time< ADDDATE( CAST(#{endDt,jdbcType=VARCHAR} AS date) ,1)  ) ";
    String SELECT_BY_PAGE_GROUP_ORDER_LIMIT = " order by a.id asc limit #{offset,jdbcType=INTEGER},#{pageSize,jdbcType=INTEGER} ";
    String SELECT_BY_PAGE_JOB_WHERE = " where ( a.job_id = #{jobId}  )"
            + " and (a.start_time>=CAST(#{beginDt,jdbcType=VARCHAR} AS date) and  a.start_time< ADDDATE( CAST(#{endDt,jdbcType=VARCHAR} AS date) ,1)  ) ";
    String SELECT_BY_PAGE_JOB_ORDER_LIMIT = " order by a.id desc limit #{offset,jdbcType=INTEGER},#{pageSize,jdbcType=INTEGER} ";

    String COLUMNS_SELECT240PX = " , CASE WHEN a.start_time IS NULL and a.end_time is null THEN 0 ELSE CEIL(TIMESTAMPDIFF(SECOND, a.start_time, case when a.end_time is not null then a.end_time else mm.end_time_max end)/mm.rang_unit) END AS dur240px"
            + " ,CASE WHEN a.start_time IS NULL THEN 240 ELSE CEIL(TIMESTAMPDIFF(SECOND, mm.start_time_min, a.start_time)/mm.rang_unit) END AS begintime240px ";

    String INSIDE_JOIN_SELECT_FROM240PX = "select MIN(inside.start_time) start_time_min ,CASE WHEN MAX(inside.start_time)>=MAX(inside.end_time) then now() ELSE MAX(inside.end_time) END as end_time_max"
            + " ,TIMESTAMPDIFF(SECOND,MIN(inside.start_time),CASE WHEN MAX(inside.start_time)>=MAX(inside.end_time) then now() ELSE MAX(inside.end_time) END )/240.0 AS rang_unit"
            + " from ";

    @Insert("insert into hera_action_history (#{heraJobHistory})")
    @Lang(HeraInsertLangDriver.class)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(HeraJobHistory heraJobHistory);

    @Delete("delete from hera_action_history where id = #{id}")
    int delete(@Param("id") Long id);

    @Update("update hera_action_history (#{heraJobHistory}) where id = #{id}")
    @Lang(HeraUpdateLangDriver.class)
    int update(HeraJobHistory heraJobHistory);

    @Select("select * from hera_action_history")
    @Lang(HeraSelectLangDriver.class)
    List<HeraJobHistory> getAll();

    @Select("select * from hera_action_history where id = #{id}")
    HeraJobHistory findById(@Param("id") Long id);

    @Select("select * from hera_action_history where action_id = #{actionId}")
    List<HeraJobHistory> findByActionId(@Param("actionId") Long actionId);

    /**
     * ζ΄ζ°ζ₯εΏ
     *
     * @param heraJobHistory
     * @return
     */
    @Update("update hera_action_history set log = #{log} where id = #{id}")
    int updateHeraJobHistoryLog(HeraJobHistory heraJobHistory);

    /**
     * ζ΄ζ°ηΆζ
     *
     * @param heraJobHistory
     * @return
     */
    @Update("update hera_action_history set status = #{status} where id = #{id}")
    int updateHeraJobHistoryStatus(HeraJobHistory heraJobHistory);

    /**
     * ζ΄ζ°ζ₯εΏεηΆζ
     *
     * @param heraJobHistory
     * @return
     */
    @Update("update hera_action_history set log = #{log},status = #{status},end_time = #{endTime},illustrate=#{illustrate} where id = #{id}")
    Integer updateHeraJobHistoryLogAndStatus(HeraJobHistory heraJobHistory);

    /**
     * ζ΄ζ°properties
     *
     * @param heraJobHistory
     * @return
     */
    @Update("update hera_action_history set properties = #{properties} where id = #{id}")
    Integer updateHeraJobHistoryProperties(HeraJobHistory heraJobHistory);

    /**
     * ζ Ήζ?jobIdζ₯θ―’θΏθ‘εε²
     *
     * @param jobId
     * @return
     */
    @Select("select * from hera_action_history where job_id = #{job_id} order by id desc")
    List<HeraJobHistory> findByJobId(@Param("job_id") Long jobId);

    /**
     * ζ Ήζ?IDζ₯θ―’ζ₯εΏιθ’­
     *
     * @param id
     * @return
     */
    @Select("select log,status from hera_action_history where id = #{id}")
    HeraJobHistory selectLogById(Integer id);


    @Select("select count(1) from hera_action_history where job_id = #{id}")
    Integer selectCountById(Integer id);


    /**
     * θ·εjobidηζΆι΄θε΄ηζ§θ‘δΈͺζ°
     *
     * @param pageHelperTimeRange
     * @return
     */
    @Select("select count(1) as cnt "
            + "from hera_action_history a "
            + "where ( a.job_id = #{jobId}  )"
            + "and (a.start_time>=CAST(#{beginDt,jdbcType=VARCHAR} AS date) and  a.start_time< ADDDATE( CAST(#{endDt,jdbcType=VARCHAR} AS date) ,1)  ) "
    )
    Integer selectCountByPageJob(PageHelperTimeRange pageHelperTimeRange);

    /**
     * θ·εjobidηζΆι΄θε΄ηζ§θ‘εε²ζη»
     *
     * @param pageHelperTimeRange
     * @return
     */
    @Select("select a.id,a.action_id,a.job_id,a.start_time,a.end_time,a.execute_host,a.operator,a.status,a.trigger_type,a.illustrate,a.host_group_id,a.batch_id,a.biz_label"
            + ",b.name as job_name,b.description ,b.group_id,c.name as group_name "
            + COLUMNS_SELECT240PX
            + " from hera_action_history a "
            + " inner join hera_job b on a.job_id=b.id  "
            + " inner join hera_group c on b.group_id=c.id  "
            + " inner join ( " + INSIDE_JOIN_SELECT_FROM240PX + "(select a.start_time ,a.end_time from hera_action_history a " + SELECT_BY_PAGE_JOB_WHERE + SELECT_BY_PAGE_JOB_ORDER_LIMIT + ") inside ) mm on 1=1 "
            + SELECT_BY_PAGE_JOB_WHERE
            + SELECT_BY_PAGE_JOB_ORDER_LIMIT)
    List<JobLogHistoryVo> selectByPageJob(PageHelperTimeRange pageHelperTimeRange);


    /**
     * θ·εgroupIdδΈηζΆι΄θε΄ηζ§θ‘εε²δΈͺζ°
     *
     * @param pageHelperTimeRange
     * @return
     */
    @Select("select count(1) as cnt "
            + "from hera_action_history a "
            + "inner join hera_job b on a.job_id=b.id  "
            + SELECT_BY_PAGE_GROUP_WHERE
    )
    Integer selectCountByPageGroup(PageHelperTimeRange pageHelperTimeRange);

    /**
     * θ·εgroupIdδΈηζΆι΄θε΄ηζ§θ‘εε²ζη»
     * mmε­ζ₯θ―’(εͺζ1θ‘,θε΄εηζε°εΌε§ζΆι΄δΈζε€§η»ζζΆι΄),240pxζε¨η½ι‘΅ε¨ιΏδΈΊ240pxιΏεΊ¦,δΈΊdur240px-ζ§θ‘ζΆιΏ,begintime240px-εΌε§ζΆι΄ηδ½η½?
     *
     * @param pageHelperTimeRange
     * @return
     */
    @Select("select a.id,a.action_id,a.job_id,a.start_time,a.end_time,a.execute_host,a.operator,a.status,a.trigger_type,a.illustrate,a.host_group_id,a.batch_id,a.biz_label"
            + ",b.name as job_name,b.description ,b.group_id,c.name as group_name "
            + COLUMNS_SELECT240PX
            + " from hera_action_history a "
            + " inner join hera_job b on a.job_id=b.id  "
            + " inner join hera_group c on b.group_id=c.id  "
            + " inner join ( " + INSIDE_JOIN_SELECT_FROM240PX + " (select a.start_time,a.end_time from hera_action_history a inner join hera_job b on a.job_id=b.id "
            + SELECT_BY_PAGE_GROUP_WHERE + SELECT_BY_PAGE_GROUP_ORDER_LIMIT + " ) inside  ) mm on 1=1 "
            + SELECT_BY_PAGE_GROUP_WHERE
            + SELECT_BY_PAGE_GROUP_ORDER_LIMIT)
    List<JobLogHistoryVo> selectByPageGroup(PageHelperTimeRange pageHelperTimeRange);


    @Select("select job_id,start_time,end_time,status from hera_action_history where action_id >= CURRENT_DATE () * 10000000000")
    List<HeraJobHistory> findTodayJobHistory();

    @Update("update hera_action_history set illustrate=#{illustrate},status=#{status},end_time=#{endTime} where id=#{id} ")
    int updateStatusAndIllustrate(@Param("id") Long id,
                                  @Param("status") String status,
                                  @Param("illustrate") String illustrate,
                                  @Param("endTime") Date endTime);

    @Delete("delete from hera_action_history where action_id < DATE_SUB(CURRENT_DATE(),INTERVAL #{beforeDay} DAY) * 10000000000;")
    Integer deleteHistoryRecord(Integer beforeDay);

    @Select("select * from hera_action_history where job_id = #{jobId} order by id desc limit 1")
    HeraJobHistory findNewest(Integer jobId);


    @Select("select id,job_id,properties from hera_action_history where id = #{id}")
    HeraJobHistory findPropertiesBy(Long id);


    @Select("select action_id,start_time,end_time from hera_action_history " +
            "where action_id >#{action_id} and job_id = #{jobId}  and status='failed' and json_extract(properties,'$.rerun_id')=#{rerunId} limit #{startPos},#{limit} ")
    List<HeraJobHistory> findRerunFailed(@Param("jobId") Integer jobId,
                                         @Param("rerunId") String rerunId,
                                         @Param("action_id") long actionId,
                                         @Param("startPos") Integer startPos,
                                         @Param("limit") Integer limit);


    @Select("select action_id,host_group_id,operator from hera_action_history " +
            "where action_id>#{lastId} and job_id = #{jobId} and status='failed' and json_extract(properties,'$.rerun_id')=#{rerunId} order by id limit #{limit} ")
    List<HeraJobHistory> findRerunFailedIdsByLimit(@Param("lastId") Long lastId,
                                                   @Param("jobId") Integer jobId,
                                                   @Param("rerunId") String rerunId,
                                                   @Param("limit") Integer limit);

    @Select("select job_id,start_time,end_time,status from hera_action_history where action_id >= #{date} * 10000000000 && action_id < (#{date}+1) * 10000000000")
    List<HeraJobHistory> findJobHistoryLimitDate(@Param("date") Integer date);

    @Select("select count(1) from hera_action_history " +
            "where action_id >#{action_id} and job_id = #{jobId} and status='failed' and json_extract(properties,'$.rerun_id')=#{rerunId} ")
    Integer findRerunFailedCount(@Param("jobId") Integer jobId,
                                 @Param("rerunId") String rerunId,
                                 @Param("action_id") long actionId);
}
