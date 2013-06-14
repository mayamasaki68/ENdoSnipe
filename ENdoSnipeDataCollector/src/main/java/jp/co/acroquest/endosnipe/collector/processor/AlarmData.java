/*******************************************************************************
 * ENdoSnipe 5.0 - (https://github.com/endosnipe)
 * 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Acroquest Technology Co.,Ltd.
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
package jp.co.acroquest.endosnipe.collector.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import jp.co.acroquest.endosnipe.collector.JavelinDataLogger;
import jp.co.acroquest.endosnipe.collector.notification.AlarmEntry;
import jp.co.acroquest.endosnipe.data.dto.SignalDefinitionDto;

/**
 * 閾値判定結果を保持するクラス
 * @author fujii
 *
 */
public class AlarmData
{
    /** 現在の閾値判定レベル */
    private int alarmLevel_;

    /** 現在のアラームレベルでアラームを発報した計測時刻 */
    private Long alarmedTime_;

    /** レベルごとに閾値を超えた計測時刻 */
    private Map<Integer, Long> startExceedanceTimeMap_ = new TreeMap<Integer, Long>();

    /** レベルごとに閾値を下回った計測時刻 */
    private final Map<Integer, Long> recoverTimeMap_ = new TreeMap<Integer, Long>();

    /** 現在のアラーム状態　*/
    private int alarmStatus_;

    /**
     * コンストラクタ
     */
    public AlarmData()
    {
        this.alarmLevel_ = JavelinDataLogger.NORMAL_ALARM_LEVEL;
    }

    /**
     * @return alarmLevel
     */
    public int getAlarmLevel()
    {
        return this.alarmLevel_;
    }

    /**
     * @param alarmLevel セットする alarmLevel
     */
    public void setAlarmLevel(final int alarmLevel)
    {
        this.alarmLevel_ = alarmLevel;
    }

    /**
     * @return alarmedTime
     */
    public Long getAlarmedTime()
    {
        return this.alarmedTime_;
    }

    /**
     * @param alarmedTime セットする alarmedTime
     */
    public void setAlarmedTime(final Long alarmedTime)
    {
        this.alarmedTime_ = alarmedTime;
    }

    /**
     * @return alarmStatus
     */
    public int getAlarmStatus()
    {
        return this.alarmStatus_;
    }

    /**
     * @param alarmStatus セットする alarmStatus
     */
    public void setAlarmStatus(final int alarmStatus)
    {
        this.alarmStatus_ = alarmStatus;
    }

    /**
     * レベルごとに閾値を超えた計測時刻を取得する。
     * @return レベルごとに閾値を超えた計測時刻
     */
    public Map<Integer, Long> getStartExceedanceTimeMap()
    {
        return this.startExceedanceTimeMap_;
    }

    /**
     * 引数で指定した閾値レベルの閾値超過開始時刻を取得する。
     * @param level 閾値レベル
     * @return 引数で指定した閾値レベルの閾値超過開始時刻
     */
    public Long getStartExceedanceTime(final int level)
    {
        return this.startExceedanceTimeMap_.get(level);
    }

    /**
     * 引数で指定した閾値レベルより小さい閾値レベルの最大閾値超過開始時刻を取得する。
     * @param level 閾値レベル
     * @return 引数で指定した閾値レベルより小さい閾値レベルの最大閾値超過開始時刻
     */
    public Long getMaxStartExceedanceTime(final int level)
    {
        Long maxStartExceedanceTime = null;
        for (Entry<Integer, Long> startExceedanceEntry : this.startExceedanceTimeMap_.entrySet())
        {
            Integer targetLevel = startExceedanceEntry.getKey();
            Long value = startExceedanceEntry.getValue();
            if (targetLevel.intValue() < level)
            {
                maxStartExceedanceTime = value;
            }
            else
            {
                // TreeMapにより閾値レベル順にソートされるため、引数で指定した閾値レベルより大きいレベルが見つかった時点で、
                // 処理を終了する。
                break;
            }
        }
        return maxStartExceedanceTime;
    }

    /**
     * レベルごとに閾値を超えた計測時刻を設定する。
     * @param startExceedanceTimeMap レベルごとに閾値を超えた計測時刻
     */
    public void setStartExceedanceTimeMap(final Map<Integer, Long> startExceedanceTimeMap)
    {
        this.startExceedanceTimeMap_ = startExceedanceTimeMap;
    }

    /**
     * 閾値超過開始時刻を設定する。
     * @param level 閾値レベル
     * @param exceedanceTime 閾値超過時刻
     */
    public void addStartExceedanceTime(final int level, final Long exceedanceTime)
    {
        this.startExceedanceTimeMap_.put(level, exceedanceTime);
    }

    /**
     * 閾値超過アラームの中でもっとも閾値レベルが高い閾値のアラームを取得する。
     * @param level 閾値レベル
     * @param signalDefinition 閾値判定条件
     * @return 閾値超過アラームの中でもっとも閾値レベルが高い閾値のアラーム
     */
    public AlarmEntry getExceedanceAlarmEntry(final int level,
            final SignalDefinitionDto signalDefinition)
    {
        double escalationPeriod = signalDefinition.getEscalationPeriod();
        long currentTime = System.currentTimeMillis();
        AlarmEntry alarmEntry = null;
        List<Integer> removeList = new ArrayList<Integer>();

        for (Entry<Integer, Long> exceedanceTimeEntry : this.startExceedanceTimeMap_.entrySet())
        {
            Integer targetLevel = exceedanceTimeEntry.getKey();
            Long value = exceedanceTimeEntry.getValue();

            // 指定した閾値レベルよりも高い閾値レベルの中で、障害発生時刻が設定されているものが対象。
            if (targetLevel.intValue() > level && (value + escalationPeriod) < currentTime)
            {
                alarmEntry = new AlarmEntry();
                alarmEntry.setAlarmType(AlarmType.FAILURE);
                alarmEntry.setAlarmInterval(escalationPeriod);
                alarmEntry.setAlarmState(targetLevel);
                alarmEntry.setSendAlarm(true);
                alarmEntry.setSignalLevel(signalDefinition.getLevel());
                setAlarmLevel(targetLevel.intValue());
                removeList.add(targetLevel);
            }
        }
        // 判定に使用した障害レベルをキーから削除する。
        for (Integer removeLevel : removeList)
        {
            this.startExceedanceTimeMap_.remove(removeLevel);
        }

        return alarmEntry;
    }

    /**
     * 閾値超過開始時刻をクリアする。
     */
    public void clearStartExceedance()
    {
        this.startExceedanceTimeMap_.clear();
    }

    /**
     * 引数で指定した閾値レベルよりも低い閾値レベルで発生した閾値超過アラームを削除する。
     * @param level 削除判定に利用する閾値レベル
     */
    public void clearStartExceedance(final int level)
    {
        Set<Entry<Integer, Long>> startExceedanceEntrySet = this.startExceedanceTimeMap_.entrySet();
        Iterator<Entry<Integer, Long>> it = startExceedanceEntrySet.iterator();
        while (it.hasNext())
        {
            Entry<Integer, Long> startExceedanceEntry = it.next();
            Integer targetLevel = startExceedanceEntry.getKey();
            if (targetLevel.intValue() < level)
            {
                it.remove();
            }
        }
    }

    /**
     * 指定した閾値レベルと一致する復旧アラームの時刻を取得する。
     * @param level 閾値レベル
     * @return 指定した閾値レベルと一致する復旧アラームの時刻
     */
    public Long getRecoverTime(final int level)
    {
        return this.recoverTimeMap_.get(level);
    }

    /**
     * 復旧アラームの中でもっとも閾値レベルが低い時刻を取得する。
     * @param level 閾値レベル
     * @return 復旧アラームの中でもっとも閾値レベルが低い時刻
     */
    public Long getMinRecoverTime(final int level)
    {
        Long minRecoverTime = null;
        for (Entry<Integer, Long> recoverEntry : this.recoverTimeMap_.entrySet())
        {
            Integer targetLevel = recoverEntry.getKey();
            Long value = recoverEntry.getValue();
            if (targetLevel.intValue() > level)
            {
                minRecoverTime = value;
                break;
            }
        }
        return minRecoverTime;
    }

    /**
     * 復旧アラームの中でもっとも閾値レベルが低い時刻を取得する。
     * @param alarmData 現在の閾値情報
     * @param level 閾値レベル
     * @param signalDefinition 閾値判定条件
     * @return 復旧アラームの中でもっとも閾値レベルが低い時刻
     */
    public AlarmEntry getRecoverAlarmEntry(final AlarmData alarmData, final int level,
            final SignalDefinitionDto signalDefinition)
    {
        double escalationPeriod = signalDefinition.getEscalationPeriod();
        long currentTime = System.currentTimeMillis();
        AlarmEntry alarmEntry = null;
        // 
        for (Entry<Integer, Long> recoverTimeEntry : this.recoverTimeMap_.entrySet())
        {
            Integer targetLevel = recoverTimeEntry.getKey();
            Long value = recoverTimeEntry.getValue();

            // 指定した閾値レベルよりも小さいレベルの中で、復旧時刻が設定されているものが対象。
            if (targetLevel.intValue() < level && (value + escalationPeriod) < currentTime)
            {
                alarmEntry = new AlarmEntry();
                alarmEntry.setAlarmType(AlarmType.RECOVER);
                alarmEntry.setAlarmInterval(escalationPeriod);
                alarmEntry.setAlarmState(targetLevel);
                alarmEntry.setSendAlarm(true);
                alarmEntry.setSignalLevel(signalDefinition.getLevel());
                setAlarmLevel(targetLevel.intValue());
                break;
            }
        }
        return alarmEntry;
    }

    /**
     * 復旧開始時刻をクリアする。
     */
    public void clearRecoverExceedance()
    {
        this.recoverTimeMap_.clear();
    }

    /**
     * 引数で指定した閾値レベルよりも高い閾値レベルで発生した復旧アラームを削除する。
     * @param level 削除判定に利用する閾値レベル
     */
    public void clearRecoverTimeMap(final int level)
    {
        Set<Entry<Integer, Long>> recoverEntrySet = this.recoverTimeMap_.entrySet();
        Iterator<Entry<Integer, Long>> it = recoverEntrySet.iterator();
        while (it.hasNext())
        {
            Entry<Integer, Long> recoverEntry = it.next();
            Integer targetLevel = recoverEntry.getKey();
            if (targetLevel.intValue() > level)
            {
                it.remove();
            }
        }
    }

    /**
     * 復旧開始時刻を設定する。
     * @param level 閾値レベル
     * @param recoverTime 閾値超過時刻
     */
    public void addRecoverTime(final int level, final Long recoverTime)
    {
        this.recoverTimeMap_.put(level, recoverTime);
    }

    /**
     * 閾値判定結果をリセットする。
     */
    public void reset()
    {
        this.alarmLevel_ = -1;
        this.alarmStatus_ = -1;

        this.startExceedanceTimeMap_.clear();
        this.recoverTimeMap_.clear();

    }

}
