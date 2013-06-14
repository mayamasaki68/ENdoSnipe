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

import java.util.ArrayList;

/**
 * HadoopのTaskTrackerStatus情報を保持するクラス
 *
 * @author asazuma
 *
 */
public class HadoopInfo
{
    /** ホスト名 */
    private String host_ = null;

    /** ジョブ登録時のジョブID */
    private String submitJobID_ = null;

    /** 完了したジョブID */
    private String completeJobID_ = null;

    /** 停止されたジョブID */
    private String killedJobID_ = null;

    /** タスク状態のリスト */
    private ArrayList<HadoopTaskStatus> taskStatuses_ = null;

    /** アクション情報 のリスト*/
    private ArrayList<HadoopAction> taskTrackerActions_ = null;

    /**
     * コンストラクタ
     */
    public HadoopInfo()
    {
        // 何もしない
    }

    /**
     * ホスト名を取得する。
     *
     * @return ホスト名
     */
    public String getHost()
    {
        return this.host_;
    }

    /**
     * ホスト名を設定する。
     *
     * @param host ホスト名
     */
    public void setHost(String host)
    {
        this.host_ = host;
    }

    /**
     * タスク状態のリストを取得する。
     *
     * @return タスク状態のリスト
     */
    public ArrayList<HadoopTaskStatus> getTaskStatuses()
    {
        return this.taskStatuses_;
    }

    /**
     * タスク状態のリストを設定する。
     *
     * @param taskStatusList タスク状態のリスト
     */
    public void setTaskStatuses(ArrayList<HadoopTaskStatus> taskStatusList)
    {
        this.taskStatuses_ = taskStatusList;
    }

    /**
     * タスク状態をリストに追加する。
     *
     * @param taskStatus タスク状態
     */
    public void addTaskStatus(HadoopTaskStatus taskStatus)
    {
        if (taskStatus == null) return;

        if (this.taskStatuses_ == null)
        {
            this.taskStatuses_ = new ArrayList<HadoopTaskStatus>(1);
        }

        this.taskStatuses_.add(taskStatus);
    }

    /**
     * アクション情報のリストを取得する。
     *
     * @return アクション情報のリスト
     */
    public ArrayList<HadoopAction> getTaskTrackerActions()
    {
        return this.taskTrackerActions_;
    }

    /**
     * アクション情報のリストを設定する。
     *
     * @param hadoopTaskTrackerActions アクション情報のリスト
     */
    public void setTaskTrackerActions(ArrayList<HadoopAction> hadoopTaskTrackerActions)
    {
        this.taskTrackerActions_ = hadoopTaskTrackerActions;
    }

    /**
     * アクション情報を追加する。
     *
     * @param hadoopTaskTrackerAction HadoopのTaskTrackerのアクション情報
     */
    public void addTaskTrackerAction(HadoopAction hadoopTaskTrackerAction)
    {
        if (this.taskTrackerActions_ == null)
        {
            this.taskTrackerActions_ = new ArrayList<HadoopAction>(1);
        }

        this.taskTrackerActions_.add(hadoopTaskTrackerAction);
    }

    /**
     * ジョブ登録情報を持っているか
     *
     * @return {@code true}：持っている／{@code false}：持っていない
     */
    public boolean hasSubmitInfo()
    {
        return this.submitJobID_ != null;
    }

    /**
     * ジョブ登録時のジョブIDを取得
     *
     * @return ジョブID
     */
    public String getSubmitJobID()
    {
        return this.submitJobID_;
    }

    /**
     * ジョブ登録時のジョブIDを設定
     *
     * @param submitJobID ジョブID
     */
    public void setSubmitJobID(String submitJobID)
    {
        this.submitJobID_ = submitJobID;
    }

    /**
     * 完了ジョブ情報を持っているか
     *
     * @return {@code true}：持っている／{@code false}：持っていない
     */
    public boolean hasCompleteInfo()
    {
        return this.completeJobID_ != null;
    }

    /**
     * 完了したジョブIDを取得
     *
     * @return ジョブID
     */
    public String getCompleteJobID()
    {
        return this.completeJobID_;
    }

    /**
     * 完了したジョブIDを設定
     *
     * @param completeJobID ジョブID
     */
    public void setCompleteJobID(String completeJobID)
    {
        this.completeJobID_ = completeJobID;
    }

    /**
     * ステータス情報を持っているかを取得する。
     *
     * @return {@code true}：持っている／{@code false}：持っていない
     */
    public boolean hasStatuses()
    {
        if (this.taskStatuses_ == null)
            return false;
        else
            return !this.taskStatuses_.isEmpty();
    }

    /**
     * アクション情報を持っているかを取得する。
     *
     * @return {@code true}：持っている／{@code false}：持っていない
     */
    public boolean hasActions()
    {
        if (this.taskTrackerActions_ == null)
            return false;
        else
            return !this.taskTrackerActions_.isEmpty();
    }

    /**
     * 停止されたJobIDを取得する。
     * 
     * @return 停止されたジョブのID
     */
    public String getKilledJobID()
    {
        return this.killedJobID_;
    }

    /**
     * 停止されたJobIDを設定する。
     * 
     * @param killedJobID 停止されたジョブのID
     */
    public void setKilledJobID(String killedJobID)
    {
        this.killedJobID_ = killedJobID;
    }

    /**
     * 停止されたジョブ情報を持っているかを取得する。
     *
     * @return {@code true}：持っている／{@code false}：持っていない
     */
    public boolean hasKilledInfo()
    {
        return this.killedJobID_ != null;
    }
}
