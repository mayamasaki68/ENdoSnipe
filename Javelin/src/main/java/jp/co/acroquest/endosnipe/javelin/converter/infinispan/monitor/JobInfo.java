package jp.co.acroquest.endosnipe.javelin.converter.infinispan.monitor;

import java.util.HashMap;
import java.util.Map;

import net.arnx.jsonic.JSON;

/**
 * ジョブの情報
 * 
 * @author hiramatsu
 *
 */
public class JobInfo
{
    /**　ジョブ終了状態　*/
    private String              status_;

    /**　ジョブサブミット時刻　*/
    private long                submitTime_;

    /**　ジョブ開始時刻　*/
    private long                startTime_;

    /**　ジョブ終了時刻　*/
    private long                finishTime_;

    /**　ジョブ名　*/
    private String              jobName_;

    /**　ジョブID　*/
    private String              jobId_;

    /** jobID の JSONキー  */
    private static final String JOB_ID      = "JobID";

    /** jobName の JSONキー  */
    private static final String JOB_NAME    = "JobName";

    /** submitTime の JSONキー  */
    private static final String SUBMIT_TIME = "SubmitTime";

    /** startTime の JSONキー  */
    private static final String START_TIME  = "StartTime";

    /** finishTime の JSONキー  */
    private static final String FINISH_TIME = "FinishTime";

    /** status の JSONキー  */
    private static final String STATUS      = "Status";

    /**
     * ジョブ実行結果状態を取得する。
     * @return ジョブ実行結果状態
     */
    public String getStatus()
    {
        return status_;
    }

    /**
     * ジョブ実行結果状態を変更する。
     * @param status ジョブ実行結果状態
     */
    public void setStatus(String status)
    {
        this.status_ = status;
    }

    /**
     * ジョブサブミット時刻を取得する。
     * @return ジョブサブミット時刻
     */
    public long getSubmitTime()
    {
        return submitTime_;
    }

    /**
     * ジョブサブミット時刻を変更する。
     * @param submitTime ジョブサブミット時刻
     */
    public void setSubmitTime(long submitTime)
    {
        this.submitTime_ = submitTime;
    }

    /**
     * ジョブ開始時刻を取得する。
     * @return ジョブ開始時刻
     */
    public long getStartTime()
    {
        return startTime_;
    }

    /**
     * ジョブ開始時刻を変更する。
     * @param startTime ジョブ開始時刻
     */
    public void setStartTime(long startTime)
    {
        this.startTime_ = startTime;
    }

    /**
     * ジョブ終了時刻を取得する。
     * @return ジョブ終了時刻
     */
    public long getFinishTime()
    {
        return finishTime_;
    }

    /**
     * ジョブ終了時刻を変更する。
     * @param finishTime ジョブ終了時刻
     */
    public void setFinishTime(long finishTime)
    {
        this.finishTime_ = finishTime;
    }

    /**
     * ジョブ名を取得する。
     * @return ジョブ名
     */
    public String getJobName()
    {
        return jobName_;
    }

    /**
     * ジョブ名を変更する。
     * @param jobName ジョブ名
     */
    public void setJobName(String jobName)
    {
        this.jobName_ = jobName;
    }

    /**
     * ジョブIDを取得する。
     * @return ジョブID
     */
    public String getJobId()
    {
        return jobId_;
    }

    /**
     * ジョブIDを変更する。
     * @param jobId ジョブID
     */
    public void setJobId(String jobId)
    {
        this.jobId_ = jobId;
    }

    @Override
    public String toString()
    {
        return "JobInfo [status_=" + status_ + ", submitTime_=" + submitTime_
                + ", startTime_=" + startTime_ + ", finishTime_=" + finishTime_
                + ", jobName_=" + jobName_ + ", jobId_=" + jobId_ + "]";
    }

    /**
     * @return json形式のジョブ情報
     */
    public String getJson()
    {
        Map<String, Object> jobStatusMap = new HashMap<String, Object>();
        jobStatusMap.put(JOB_ID, this.jobId_);
        jobStatusMap.put(JOB_NAME, this.jobName_);
        jobStatusMap.put(SUBMIT_TIME, this.submitTime_);
        jobStatusMap.put(START_TIME, this.startTime_);
        jobStatusMap.put(FINISH_TIME, this.finishTime_);

        jobStatusMap.put(STATUS, this.status_);

        String jsonString = JSON.encode(jobStatusMap);

        return jsonString;
    }

}
