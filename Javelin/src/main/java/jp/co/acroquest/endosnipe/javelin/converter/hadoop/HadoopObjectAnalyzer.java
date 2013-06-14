/*******************************************************************************
 * ENdoSnipe 5.0 - (https://github.com/endosnipe)
 * 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2012 Acroquest Technology Co.,Ltd.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package jp.co.acroquest.endosnipe.javelin.converter.hadoop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import jp.co.acroquest.endosnipe.javelin.converter.hadoop.HadoopAction.ActionType;
import jp.co.acroquest.endosnipe.javelin.converter.hadoop.HadoopTaskStatus.State;

/**
 * Hadoopのオブジェクトを解析するクラス。
 *
 * @author y_asazuma
 *
 */
public class HadoopObjectAnalyzer
{
    /** JobStatus.getRunState()の戻り値 */
    public enum HadoopJobStatus
    {
        /** 内部エラー */
        ERROR,          // -1
        /** 実行中 */
        RUNNING,        // 1
        /** 成功 */
        SUCCEEDED,      // 2
        /** 失敗 */
        FAILED,         // 3
        /** 準備中 */
        PREP,           // 4
        /** 停止された */
        KILLED;         // 5

        /**
         * JobStatusをHadoop内部で使われる数値に変換します。
         * 
         * @param status ステータスの列挙体
         * @return １～５の数値(-1はエラー)
         */
        public static int getNumber(HadoopJobStatus status)
        {
            int ret = -1;
            switch (status)
            {
            case RUNNING:
                ret = 1;
                break;
            case SUCCEEDED:
                ret = 2;
                break;
            case FAILED:
                ret = 3;
                break;
            case PREP:
                ret = 4;
                break;
            case KILLED:
                ret = 5;
                break;
            }
            return ret;
        }

        /**
         * Hadoop内部で使われる数値をJobStatusに変換します。
         * 
         * @param status １～５の数値
         * @return ステータス列挙体
         */
        public static HadoopJobStatus getStatus(int status)
        {
            HadoopJobStatus ret = HadoopJobStatus.ERROR;
            switch (status)
            {
            case 1:
                ret = HadoopJobStatus.RUNNING;
                break;
            case 2:
                ret = HadoopJobStatus.SUCCEEDED;
                break;
            case 3:
                ret = HadoopJobStatus.FAILED;
                break;
            case 4:
                ret = HadoopJobStatus.PREP;
                break;
            case 5:
                ret = HadoopJobStatus.KILLED;
                break;
            }
            return ret;
        }
    }

    /** JobIDを分解するためのセパレータ */
    private static String SEPARATOR = "_";

    /**
     * HadoopのTaskTrackerStatusオブジェクトからホスト名を取得する。
     *
     * @param taskTrackerStatus HadoopのTaskTrackerStatus
     *
     * @throws NoSuchMethodException NoSuchMethodException
     * @throws InvocationTargetException InvocationTargetException
     * @throws IllegalAccessException IllegalAccessException
     * @throws SecurityException SecurityException
     * @throws IllegalArgumentException IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public static String hostNamefromTaskTrackerStatus(Object taskTrackerStatus)
        throws IllegalArgumentException, SecurityException, IllegalAccessException,
               InvocationTargetException, NoSuchMethodException
    {
        Class taskTrackerClass = taskTrackerStatus.getClass();
        // TaskTrackerStatus以外のクラスの場合は処理を飛ばす
        if (!taskTrackerClass.getName().equals("org.apache.hadoop.mapred.TaskTrackerStatus"))
        {
            return null;
        }

        // Host名をリターン
        return taskTrackerClass.getMethod("getHost", new Class[]{}).invoke(taskTrackerStatus, new Object[]{}).toString();
    }

    /**
     * HadoopのTaskTrackerStatusからHadoopTaskStatusオブジェクトのリストを作成する。
     *
     * @param taskTrackerStatus HadoopのTaskTrackerStatusオブジェクト
     *
     * @return 変換したHadoopTaskStatus
     *
     * @throws NoSuchMethodException NoSuchMethodException
     * @throws InvocationTargetException InvocationTargetException
     * @throws IllegalAccessException IllegalAccessException
     * @throws SecurityException SecurityException
     * @throws IllegalArgumentException IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<HadoopTaskStatus> transTaskStatus(Object taskTrackerStatus)
        throws IllegalArgumentException, SecurityException, IllegalAccessException,
               InvocationTargetException, NoSuchMethodException
    {
        Class taskTrackerClass = taskTrackerStatus.getClass();
        // TaskTrackerStatus以外のクラスの場合は処理を飛ばす
        if (!taskTrackerClass.getName().equals("org.apache.hadoop.mapred.TaskTrackerStatus"))
        {
            return null;
        }

        // TaskStatusのリストを取り出す
        Object taskObjectList = taskTrackerClass.getMethod("getTaskReports", new Class[]{}).invoke(taskTrackerStatus, new Object[]{});

        ArrayList<HadoopTaskStatus> hadoopTaskStatusList = new ArrayList<HadoopTaskStatus>();

        java.util.ArrayList<Object> taskList = (java.util.ArrayList)taskObjectList;

        // HadoopTaskStatusの設定
        for (Object taskStatus : taskList)
        {
            HadoopTaskStatus hadoopTaskStatus = new HadoopTaskStatus();

            // state
            String state = taskStatus.getClass().getMethod("getRunState", new Class[]{}).invoke(taskStatus, new Object[]{}).toString();

            // RUNNINGは無視
            if (state.endsWith(State.RUNNING.toString()))
                continue;

            hadoopTaskStatus.setState(state);

            // taskID
            Object taskIdObject = taskStatus.getClass().getMethod("getTaskID", new Class[]{}).invoke(taskStatus, new Object[]{});
            hadoopTaskStatus.setTaskID(taskIdObject.toString());
            // jobID
            hadoopTaskStatus.setJobID(taskIdObject.getClass().getMethod("getJobID", new Class[]{}).invoke(taskIdObject, new Object[]{}).toString());
            //Phase
            hadoopTaskStatus.setPhase(taskStatus.getClass().getMethod("getPhase", new Class[]{}).invoke(taskStatus, new Object[]{}).toString());

            hadoopTaskStatusList.add(hadoopTaskStatus);
        }

        return hadoopTaskStatusList;
    }

    /**
     * HadoopのHeartbeatResponseオブジェクトからHadoopActionオブジェクトのリストを作成する。
     *
     * @param heartbeatResponse TaskTrackerAction[]
     *
     * @return HadoopActionのリスト
     *
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws SecurityException SecurityException
     * @throws IllegalAccessException IllegalAccessException
     * @throws InvocationTargetException InvocationTargetException
     * @throws NoSuchMethodException NoSuchMethodException
     * @throws NoSuchFieldException NoSuchFieldException
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<HadoopAction> taskTrackerActionListFromHearbeatResponse(Object heartbeatResponse)
        throws IllegalArgumentException, SecurityException,
               IllegalAccessException, InvocationTargetException,
               NoSuchMethodException, NoSuchFieldException
    {
        Class heartbeatResponseClass = heartbeatResponse.getClass();
        // HeartbeatResponse以外のクラスの場合は処理を飛ばす
        if (!heartbeatResponseClass.getName().equals("org.apache.hadoop.mapred.HeartbeatResponse"))
        {
            return null;
        }
        // TaskTrackerAction[]を解析
        Method method = heartbeatResponseClass.getMethod("getActions", new Class[]{});
        method.setAccessible(true);
        Object[] actionList = (Object[])(method.invoke(heartbeatResponse, new Object[]{}));
        ArrayList<HadoopAction> ret = new ArrayList<HadoopAction>();

        // HadoopActionを追加
        for (Object action : actionList)
        {
            HadoopAction hadoopAction = new HadoopAction();

            // アクション種別の判定
            // リフレクションは基底クラスのメソッドを呼び出せないので、基底クラスからメソッドを取得する。
            Method getActionIdMethod = action.getClass().getSuperclass().getDeclaredMethod("getActionId", new Class[]{});
            getActionIdMethod.setAccessible(true);
            String actionType = getActionIdMethod.invoke(action, new Object[]{}).toString();
            hadoopAction.setActionType(actionType);

            // KillJob、TaskTrackerの再構築は解析を省略
            if ( (actionType.toString().equals(ActionType.REINIT_TRACKER.toString())) ||
                 (actionType.toString().equals(ActionType.KILL_JOB.toString())) )
                continue;

            // 新規タスク実行の場合
            if (actionType.toString().equals(ActionType.LAUNCH_TASK.toString()))
            {
                // Taskオブジェクト取得
                Method getTaskMethod = action.getClass().getDeclaredMethod("getTask", new Class[]{});
                getTaskMethod.setAccessible(true);
                Object task = getTaskMethod.invoke(action, new Object[]{});

                // Mapタスクか？
                Method isMapTaskMethod = task.getClass().getDeclaredMethod("isMapTask", new Class[]{});
                isMapTaskMethod.setAccessible(true);
                hadoopAction.setMapTask(((Boolean)(isMapTaskMethod.invoke(task, new Object[]{}))).booleanValue());

                // ジョブID
                Method getJobIDMethod = task.getClass().getSuperclass().getDeclaredMethod("getJobID", new Class[]{});
                getJobIDMethod.setAccessible(true);
                hadoopAction.setJobID(getJobIDMethod.invoke(task, new Object[]{}).toString());

                // タスク試行ID
                Method getTaskIDMethod = task.getClass().getSuperclass().getDeclaredMethod("getTaskID", new Class[]{});
                getTaskIDMethod.setAccessible(true);
                hadoopAction.setTaskID(getTaskIDMethod.invoke(task, new Object[]{}).toString());

                // JobConfオブジェクト取得
                Method getConfMethod = task.getClass().getSuperclass().getDeclaredMethod("getConf", new Class[]{});
                getConfMethod.setAccessible(true);
                Object jobConf = getConfMethod.invoke(task, new Object[]{});

                // ジョブ名
                hadoopAction.setJobName(jobConf.getClass().getDeclaredMethod("getJobName", new Class[]{}).invoke(jobConf, new Object[]{}).toString());

                // 処理対象データ
                if (task.getClass().toString().equals("class org.apache.hadoop.mapred.MapTask"))
                {
                    Field splitMetaInfoField = task.getClass().getDeclaredField("splitMetaInfo");
                    splitMetaInfoField.setAccessible(true);
                    Object splitMetaInfo = splitMetaInfoField.get(task);
                    String splitLocation = splitMetaInfo.getClass().getMethod("getSplitLocation", new Class[]{}).invoke(splitMetaInfo, new Object[]{}).toString();
                    String startOffset = splitMetaInfo.getClass().getMethod("getStartOffset", new Class[]{}).invoke(splitMetaInfo, new Object[]{}).toString();

                    hadoopAction.setInputData(splitLocation + "(" + startOffset + ")");
                }
            }
            // その他
            else
            {
                // タスク試行ID
                Method getTaskIDMethod = action.getClass().getDeclaredMethod("getTaskID", new Class[]{});
                getTaskIDMethod.setAccessible(true);
                Object taskAttemptID = getTaskIDMethod.invoke(action, new Object[]{});
                hadoopAction.setTaskID(taskAttemptID.toString());

                // ジョブID
                Method getJobIDMethod = taskAttemptID.getClass().getDeclaredMethod("getJobID", new Class[]{});
                getJobIDMethod.setAccessible(true);
                Object jobID = getJobIDMethod.invoke(taskAttemptID, new Object[]{});
                hadoopAction.setJobID(jobID.toString());
            }

            ret.add(hadoopAction);
        }

        return ret;
    }

    /**
     * JobStatusオブジェクトからジョブIDを取得する。
     *
     * @param jobStatus JobStatusオブジェクト
     *
     * @return ジョブID
     *
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws SecurityException SecurityException
     * @throws IllegalAccessException IllegalAccessException
     * @throws InvocationTargetException InvocationTargetException
     * @throws NoSuchMethodException NoSuchMethodException
     */
    @SuppressWarnings("unchecked")
    public static String getJobIDfromJobStatus(Object jobStatus)
        throws IllegalArgumentException, SecurityException, IllegalAccessException,
               InvocationTargetException, NoSuchMethodException
    {
        Class jobStatusClass = jobStatus.getClass();
        // JobStatus以外のクラスの場合は処理を飛ばす
        if (!jobStatusClass.getName().equals("org.apache.hadoop.mapred.JobStatus"))
        {
            return null;
        }

        // JobIDを取得
        return jobStatusClass.getMethod("getJobId", new Class[]{}).invoke(jobStatus, new Object[]{}).toString();
    }

    /**
     * JobStatusオブジェクトを解析して、ジョブが完了しているか判定する。
     *
     * @param jobStatus JobStatusオブジェクト
     *
     * @return ジョブのステータス
     *
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws SecurityException SecurityException
     * @throws IllegalAccessException IllegalAccessException
     * @throws InvocationTargetException InvocationTargetException
     * @throws NoSuchMethodException NoSuchMethodException
     */
    @SuppressWarnings("unchecked")
    public static HadoopJobStatus getJobState(Object jobStatus)
        throws IllegalArgumentException, SecurityException, IllegalAccessException,
               InvocationTargetException, NoSuchMethodException
    {
        HadoopJobStatus ret = HadoopJobStatus.ERROR;

        if (null == jobStatus)
            return ret;

        Class jobStatusClass = jobStatus.getClass();
        // JobStatus以外のクラスの場合は処理を飛ばす
        if (!jobStatusClass.getName().equals("org.apache.hadoop.mapred.JobStatus"))
        {
            return ret;
        }

        // getRunState()を取得、実行
        int state = ((Integer)(jobStatusClass.getMethod("getRunState", new Class[]{}).invoke(jobStatus, new Object[]{}))).intValue();

        ret = HadoopJobStatus.getStatus(state);

        return ret;
    }

    /**
     * JobIDを基にジョブのステータスを取得します。
     * 
     * @param jobID ジョブID
     * @param jobTracker JobTrackerインスタンス
     * 
     * @return ジョブのステータス
     * 
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws SecurityException SecurityException
     * @throws IllegalAccessException IllegalAccessException
     * @throws InvocationTargetException InvocationTargetException
     * @throws NoSuchMethodException NoSuchMethodException
     * @throws InstantiationException InstantiationException
     */
    @SuppressWarnings("unchecked")
    public static HadoopJobStatus checkJobStatus (String jobID, Object jobTracker)
        throws IllegalArgumentException, SecurityException, IllegalAccessException,
               InvocationTargetException, NoSuchMethodException, InstantiationException,
               ClassNotFoundException
    {
        HadoopJobStatus status = HadoopJobStatus.ERROR;

        if (jobID == null || jobID.equals(""))
            return status;

        // String型のJobIDを分解し、JobIDオブジェクトを作成する
        String[] elem = jobID.split(SEPARATOR);
        // JobIDは"job_xxxxxxxxxxxx_yyyの形のはず
        if (3 != elem.length)
            return status;

        // JobIDクラスを生成するためのオブジェクト
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> jobIDClass = loader.loadClass("org.apache.hadoop.mapred.JobID");
        // JobIDのコンストラクタ引数は(String, int)
        Class<?>[] classes = new Class[]{String.class, int.class};
        Constructor<?> jobIDConstructor = jobIDClass.getConstructor(classes);
        // 引数付きコンストラクタでJobIDオブジェクト生成
        Object[] args = {elem[1], Integer.valueOf(elem[2])};
        Object[] jobIDObject = {jobIDConstructor.newInstance(args)};

        // getJobStatusメソッドの引数の型
        Class<?>[] getJobStatusTypes = new Class[]{jobIDClass};

        // getJobStatusメソッドを実行してJobStatusオブジェクトを取得
        Method getJobStatusMethod = jobTracker.getClass().getMethod("getJobStatus", getJobStatusTypes);
        Object jobStatusObject = getJobStatusMethod.invoke(jobTracker, jobIDObject);

        status = getJobState(jobStatusObject);

        return status;
    }
}
