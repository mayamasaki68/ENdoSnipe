package jp.co.acroquest.endosnipe.javelin.converter.infinispan.monitor;

import java.util.HashMap;
import java.util.Map;

import net.arnx.jsonic.JSON;

/**
 * タスク情報
 * @author hiramatsu
 */
public class TaskInfo
{
    /**　タスク終了状態　*/
    private String              status_;

    /**　タスク開始時刻　*/
    private long                startTime_;

    /**　タスク終了時刻　*/
    private long                finishTime_;

    /**　ジョブID　*/
    private String              jobID_;

    /**　タスクID　*/
    private String              taskAttemptID_;

    /**　ホストサーバ名　*/
    private String              hostName_;

    /** jobID の JSONキー  */
    private static final String JOB_ID          = "JobID";

    /** startTime の JSONキー  */
    private static final String START_TIME      = "StartTime";

    /** finishTime の JSONキー  */
    private static final String FINISH_TIME     = "FinishTime";

    /** status の JSONキー  */
    private static final String STATUS          = "Status";

    /** taskAttemptId の JSONキー  */
    private static final String TASK_ATTEMPT_ID = "TaskAttemptID";

    /** status の JSONキー  */
    private static final String HOST_NAME       = "Hostname";

    /**
     * タスク終了時状態を取得する。
     * 
     * @return 終了時状態
     */
    public String getStatus()
    {
        return status_;
    }

    /**
     * タスク終了時状態を設定する。
     * 
     * @param status 設定する終了状態
     */
    public void setStatus(String status)
    {
        this.status_ = status;
    }

    /**
     * タスク開始時刻を取得する。
     * 
     * @return タスク開始時刻
     */
    public long getStartTime()
    {
        return startTime_;
    }

    /**
     * タスク開始時刻を設定する。
     * 
     * @param startTime 設定するタスク開始時刻
     */
    public void setStartTime(long startTime)
    {
        this.startTime_ = startTime;
    }

    /**
     * タスク終了時刻を取得する。
     * 
     * @return タスク終了時刻
     */
    public long getFinishTime()
    {
        return finishTime_;
    }

    /**
     * タスク終了時刻を設定する。
     * 
     * @param finishTime 設定するタスク終了時刻
     */
    public void setFinishTime(long finishTime)
    {
        this.finishTime_ = finishTime;
    }

    /**
     * 対応するジョブのIDを取得する。
     * 
     * @return 対応するジョブのID
     */
    public String getJobID()
    {
        return jobID_;
    }

    /**
     * 対応するジョブのIDを設定する。
     * 
     * @param jobID 設定する対応するジョブのID
     */
    public void setJobID(String jobID)
    {
        this.jobID_ = jobID;
    }

    /**
     * タスクIDを取得する。
     * 
     * @return タスクID
     */
    public String getTaskAttemptID()
    {
        return taskAttemptID_;
    }

    /**
     * タスクIDを設定する。
     * 
     * @param taskAttemptID 設定するタスクID
     */
    public void setTaskAttemptID(String taskAttemptID)
    {
        this.taskAttemptID_ = taskAttemptID;
    }

    /**
     * ホストサーバ名を取得する。
     * 
     * @return ホストサーバ名
     */
    public String getHostName()
    {
        return hostName_;
    }

    /**
     * ホストサーバ名を設定する。
     * 
     * @param hostName 設定するホストサーバ名
     */
    public void setHostName(String hostName)
    {
        this.hostName_ = hostName;
    }

    @Override
    public String toString()
    {
        return "TaskInfo [status_=" + status_ + ", startTime_=" + startTime_
                + ", finishTime_=" + finishTime_ + ", jobID_=" + jobID_
                + ", taskAttemptID_=" + taskAttemptID_ + ", hostName_="
                + hostName_ + "]";
    }

    /**
     * json形式のタスク情報を返す。
     * 
     * @return json形式のタスク情報
     */
    public String getJson()
    {
        Map<String, Object> taskStatusMap = new HashMap<String, Object>();
        taskStatusMap.put(JOB_ID, this.jobID_);
        taskStatusMap.put(START_TIME, this.startTime_);
        taskStatusMap.put(FINISH_TIME, this.finishTime_);
        taskStatusMap.put(TASK_ATTEMPT_ID, this.taskAttemptID_);
        taskStatusMap.put(HOST_NAME, this.hostName_);
        taskStatusMap.put(STATUS, this.status_);

        String jsonString = JSON.encode(taskStatusMap);

        return jsonString;
    }

}
