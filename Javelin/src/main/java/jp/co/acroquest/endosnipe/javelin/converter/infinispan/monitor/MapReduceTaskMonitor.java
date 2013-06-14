package jp.co.acroquest.endosnipe.javelin.converter.infinispan.monitor;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.acroquest.endosnipe.common.logger.SystemLogger;

/**
 * mapreduceジョブ、タスクの情報を取得する。
 * 
 * @author hiramatsu
 *
 */
public class MapReduceTaskMonitor
{
    /**　ジョブ情報を保持するマップ　*/
    private static Map<String, JobInfo>  jobInfoMap__  = new ConcurrentHashMap<String, JobInfo>();

    /**　タスク情報を保持するマップ　*/
    private static Map<String, TaskInfo> taskInfoMap__ = new ConcurrentHashMap<String, TaskInfo>();

    /**　最後のジョブが実行された時刻　*/
    private static String                previousDate__;

    /**　現在の時刻（yyyyMMddHHmm）中に実行されたジョブの個数　*/
    private static int                   num__;

    private MapReduceTaskMonitor()
    {}

    /**
     * ジョブ開始前の処理を行う。
     * 
     * @param accessor 対象のジョブ
     */
    public static void preProcess(MapReduceTaskAccessor accessor)
    {
        long submitTime = System.currentTimeMillis();
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        String dateString = sdf.format(date);
        if (previousDate__ == null || !previousDate__.equals(sdf.format(date)))
        {
            previousDate__ = dateString;
            num__ = 1;
        }
        else
        {
            num__++;
        }
        DecimalFormat df = new DecimalFormat("00000000");
        String jobId = "job_" + dateString + "_" + df.format(num__);
        JobInfo jobInfo = new JobInfo();
        jobInfo.setSubmitTime(submitTime);
        jobInfo.setStartTime(submitTime);
        String tmpName = accessor.getMapperName();
        String[] tmpNames = tmpName.split("\\.");
        tmpName = tmpNames[tmpNames.length - 1];
        String mapper = "Mapper";
        jobInfo.setJobName(tmpName.endsWith(mapper) ? tmpName.substring(0,
                tmpName.length() - mapper.length()) : tmpName);
        jobInfo.setJobId(jobId);
        accessor.setJobId(jobId);
        saveJobInfo(jobInfo);
    }

    /**
     * ジョブ終了時の処理を行う。
     * 
     * @param accessor 対象のジョブ
     */
    public static void postProcess(MapReduceTaskAccessor accessor)
    {
        long finishTime = System.currentTimeMillis();
        String jobId = accessor.getJobId();
        JobInfo jobInfo = jobInfoMap__.get(jobId);
        if (jobInfo == null)
        {
            return;
        }
        jobInfo.setFinishTime(finishTime);
        jobInfo.setStatus("SUCCEEDED");
        SystemLogger.getInstance().warn("postProcess:" + jobInfo);
    }

    /**
     * ジョブ失敗時の処理を行う。
     * 
     * @param accessor 対象のジョブ
     * @param throwable タスク失敗原因の例外
     */
    public static void postProcessNG(MapReduceTaskAccessor accessor,
            Throwable throwable)
    {
        long finishTime = System.currentTimeMillis();
        String jobId = accessor.getJobId();
        JobInfo jobInfo = jobInfoMap__.get(jobId);
        if (jobInfo == null)
        {
            return;
        }
        jobInfo.setFinishTime(finishTime);
        jobInfo.setStatus("FAILED");
        SystemLogger.getInstance().warn("postProcessNG:" + jobInfo);
    }

    /**
     * タスク開始前の処理を行う。
     * 
     * @param accessor 対象のタスク
     * @param address タスクの実行されるアドレス
     */
    public static void preProcessTask(MapReduceTaskAccessor accessor,
            String address)
    {
        long startTime = System.currentTimeMillis();
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setStartTime(startTime);
        taskInfo.setJobID(accessor.getJobId());
        DecimalFormat df = new DecimalFormat("00000000");
        String taskId = accessor.getJobId() + "_"
                + (df.format(accessor.getSizeOfTaskIdMap() + 1));
        taskInfo.setTaskAttemptID(taskId);
        accessor.putTaskId(address, taskId);
        taskInfo.setHostName(address);
        saveTaskInfo(taskInfo);
        SystemLogger.getInstance().warn("preProcessTask:" + taskInfo);
    }

    /**
     * org.infinispan.remoting.rpc.RpcManager.invokeRemotely開始前の処理を行う。
     * 
     * @param accessor 対象のタスク
     * @param address タスクの実行されるアドレス
     */
    public static void preProcessTaskForInvokeRemotely(
            MapReduceTaskAccessor accessor, String address)
    {
        long startTime = System.currentTimeMillis();
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setStartTime(startTime);
        taskInfo.setJobID(accessor.getJobId());
        taskInfo.setTaskAttemptID(accessor.getJobId());
        accessor.putTaskId(address, accessor.getJobId());
        taskInfo.setHostName(address);
        saveTaskInfo(taskInfo);
        SystemLogger.getInstance().warn(
                "preProcessTaskForInvokeRemotely:" + taskInfo);
    }

    /**
     * org.infinispan.remoting.rpc.RpcManager.invokeRemotelyInFuture開始前の処理を行う。
     * 
     * @param accessor 対象のタスク
     * @param address タスクの実行されるアドレス
     */
    public static void preProcessTaskForInvokeRemotelyInFuture(
            MapReduceTaskAccessor accessor, String address)
    {
        long startTime = System.currentTimeMillis();
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setStartTime(startTime);
        taskInfo.setJobID(accessor.getJobId());
        DecimalFormat df = new DecimalFormat("00000000");
        String taskId = accessor.getJobId() + "_"
                + (df.format(accessor.getSizeOfTaskIdMap() + 1));
        taskInfo.setTaskAttemptID(taskId);
        accessor.putTaskId(address, taskId);
        taskInfo.setHostName(address);
        saveTaskInfo(taskInfo);
        SystemLogger.getInstance().warn(
                "preProcessTaskForInvokeRemotelyInFuture:" + taskInfo);
    }

    /**
     * タスク終了時の処理を行う。
     * 
     * @param accessor 対象のタスク
     */
    public static void postProcessTask(MapReduceTaskAccessor accessor)
    {
        long finishTime = System.currentTimeMillis();
        TaskInfo taskInfo = taskInfoMap__.get(accessor.getJobId());
        if (taskInfo != null)
        {
            taskInfo.setFinishTime(finishTime);
            taskInfo.setStatus("SUCCEEDED");
        }
        SystemLogger.getInstance().warn("postProcessTask:" + taskInfo);
    }

    /**
     * org.infinispan.remoting.rpc.RpcManager.invokeRemotely終了後の処理を行う。
     * 
     * @param accessor 対象のタスク
     * @param map タスクの実行されたアドレスと終了状態のマップ
     */
    public static void postProcessTaskForInvokeRemotely(
            MapReduceTaskAccessor accessor, Map<String, Boolean> map)
    {
        long finishTime = System.currentTimeMillis();
        TaskInfo taskInfoOrg = taskInfoMap__.remove(accessor.getJobId());
        DecimalFormat df = new DecimalFormat("00000000");
        int index = 0;
        Set<Entry<String, Boolean>> entrySet = map.entrySet();
        for (Entry<?, ?> entry : entrySet)
        {
            String address = (String)entry.getKey();
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setStartTime(taskInfoOrg.getStartTime());
            taskInfo.setFinishTime(finishTime);
            taskInfo.setJobID(taskInfoOrg.getJobID());
            taskInfo.setTaskAttemptID(taskInfoOrg.getTaskAttemptID() + "_"
                    + df.format(index + 1));
            taskInfo.setHostName(address);
            taskInfo.setStatus(map.get(address).booleanValue() ? "SUCCEEDED" : "FAILED");
            saveTaskInfo(taskInfo);
            if (index > 0)
            {
                accessor.putTaskId(taskInfo.getHostName(),
                        taskInfo.getTaskAttemptID());
            }
            index++;
            SystemLogger.getInstance().warn(
                    "postProcessTaskForInvokeRemotely:" + taskInfo);
        }
    }

    /**
     * タスク失敗時の処理を行う。
     * 
     * @param accessor 対象のタスク
     */
    public static void postProcessNGTask(MapReduceTaskAccessor accessor)
    {
        long finishTime = System.currentTimeMillis();
        TaskInfo taskInfo = taskInfoMap__.get(accessor.getJobId());
        if (taskInfo != null)
        {
            taskInfo.setFinishTime(finishTime);
            taskInfo.setStatus("FAILED");
        }
        SystemLogger.getInstance().warn("postProcessNGTask:" + taskInfo);
    }

    /**
     * ジョブ情報を保存する。
     * 
     * @param jobInfo 登録するジョブの情報
     */
    public static void saveJobInfo(JobInfo jobInfo)
    {
        jobInfoMap__.put(jobInfo.getJobId(), jobInfo);
        SystemLogger.getInstance().warn("saveJobInfo:" + jobInfo);
    }

    /**
     * タスク情報を保存する。
     * 
     * @param taskInfo 登録するタスクの情報
     */
    public static void saveTaskInfo(TaskInfo taskInfo)
    {
        taskInfoMap__.put(taskInfo.getTaskAttemptID(), taskInfo);
        SystemLogger.getInstance().warn("saveTaskInfo:" + taskInfo);
    }

    /**
     * 終了したジョブをマップから取り除き、コレクションとして返す。
     * 
     * @return 終了したジョブのコレクション
     */
    public static Collection<JobInfo> cloneJobInfoMapAsCollection()
    {
        Collection<JobInfo> removedCollection = new ArrayList<JobInfo>();
        Map<String, JobInfo> newJobInfoMap = new ConcurrentHashMap<String, JobInfo>();

        for (JobInfo jobInfo : jobInfoMap__.values())
        {
            if (jobInfo.getStatus() == null)
            {
                newJobInfoMap.put(jobInfo.getJobId(), jobInfo);
            }
            else
            {
                removedCollection.add(jobInfo);
            }
        }
        jobInfoMap__ = newJobInfoMap;
        return removedCollection;
    }

    /**
     * 終了したタスクをマップから取り除き、コレクションとして返す。
     * 
     * @return 終了したタスクのコレクション
     */
    public static Collection<TaskInfo> cloneTaskInfoMapAsCollection()
    {
        Collection<TaskInfo> removedCollection = new ArrayList<TaskInfo>();
        Map<String, TaskInfo> newTaskInfoMap = new ConcurrentHashMap<String, TaskInfo>();

        for (TaskInfo taskInfo : taskInfoMap__.values())
        {
            if (taskInfo.getStatus() == null)
            {
                newTaskInfoMap.put(taskInfo.getJobID(), taskInfo);
            }
            else
            {
                removedCollection.add(taskInfo);
            }
        }
        taskInfoMap__ = newTaskInfoMap;
        return removedCollection;
    }
}
