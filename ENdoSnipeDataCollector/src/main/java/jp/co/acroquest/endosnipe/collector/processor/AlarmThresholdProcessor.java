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

import java.util.Map;
import java.util.Map.Entry;

import jp.co.acroquest.endosnipe.collector.JavelinDataLogger;
import jp.co.acroquest.endosnipe.collector.notification.AlarmEntry;
import jp.co.acroquest.endosnipe.collector.util.AlarmThresholdUtil;
import jp.co.acroquest.endosnipe.common.entity.ResourceData;
import jp.co.acroquest.endosnipe.data.dto.SignalDefinitionDto;

/**
 * 指定された閾値を超えたときにアラームを発生させる。
 * @author fujii
 *
 */
public class AlarmThresholdProcessor implements AlarmProcessor
{
    /**
     * 指定された閾値に対するアラーム処理
     * @param currentResourceData 現在のリソースデータ
     * @param prevResourceData 一つ前のリソースデータ
     * @param signalDefinition アラーム通知の設定（閾値など）
     * @param alarmData アラーム通知の有無を計算するためのデータ。このメソッドの中で更新する
     * @return アラーム通知の有無と、通知するアラームに表示させる情報を入れたAlarmEntry
     */
    public AlarmEntry calculateAlarmLevel(final ResourceData currentResourceData,
            final ResourceData prevResourceData, final SignalDefinitionDto signalDefinition,
            final AlarmData alarmData)
    {
        // 初期状態
        if (currentResourceData == null)
        {
            AlarmEntry alarmEntry = new AlarmEntry();
            alarmEntry.setSendAlarm(false);
            alarmEntry.setAlarmState(JavelinDataLogger.NORMAL_ALARM_LEVEL);
            alarmEntry.setSignalLevel(signalDefinition.getLevel());
            return alarmEntry;
        }
        String matchingPattern = signalDefinition.getMatchingPattern();

        Number itemValueNumber =
                                 AlarmThresholdUtil.getNumberFromResourceData(currentResourceData,
                                                                              matchingPattern);

        if (itemValueNumber == null)
        {
            return null;
        }

        double itemValue = itemValueNumber.doubleValue();
        AlarmEntry alarmEntry =
                                createAlarmEntry(currentResourceData.measurementTime, itemValue,
                                                 alarmData, signalDefinition);
        return alarmEntry;
    }

    /**
     * 
     * @param measurementTime
     * @param value
     * @param alarmData
     * @param signalDefinition
     * @return
     */
    private AlarmEntry createAlarmEntry(final long measurementTime, final double value,
            final AlarmData alarmData, final SignalDefinitionDto signalDefinition)
    {
        // 返り値となる AlarmEntry の初期化
        AlarmEntry entry = null;
        Map<Integer, Double> thresholdMaping = signalDefinition.getThresholdMaping();

        int currentLevel = alarmData.getAlarmLevel();
        int tmpLevel = JavelinDataLogger.NORMAL_ALARM_LEVEL;
        for (Entry<Integer, Double> thresholdEntry : thresholdMaping.entrySet())
        {
            Integer judgeLevel = thresholdEntry.getKey();
            Double threshold = thresholdEntry.getValue();
            if (value >= threshold)
            {
                tmpLevel = judgeLevel;
            }
        }

        if (tmpLevel < currentLevel)
        {
            // 不要になるアラームを削除する。
            // 現在の閾値レベルよりも下回った場合は超過アラームを全て削除する。
            // 現在の閾値レベルよりも高い閾値レベルを持つ復旧アラームを全て削除する。
            alarmData.clearStartExceedance();
            alarmData.clearRecoverTimeMap(tmpLevel);
            Long stopExceedanceTime = alarmData.getRecoverTime(tmpLevel);
            if (stopExceedanceTime == null)
            {
                addStopExceedanceTime(alarmData, tmpLevel);
            }
            entry = alarmData.getRecoverAlarmEntry(alarmData, currentLevel, signalDefinition);
        }
        else if (tmpLevel > currentLevel)
        {
            // 不要になるアラームを削除する。
            // 現在の閾値レベルよりも下回った場合は復旧アラームを全てする。
            // 現在の閾値レベルよりも高い閾値レベルを持つ超過アラームを全て削除する。
            alarmData.clearRecoverExceedance();
            alarmData.clearStartExceedance(tmpLevel);
            Long startExceedanceTime = alarmData.getStartExceedanceTime(tmpLevel);
            if (startExceedanceTime == null)
            {
                addStartExceedanceTime(alarmData, tmpLevel);
            }
            entry = alarmData.getExceedanceAlarmEntry(currentLevel, signalDefinition);
        }
        if (entry != null)
        {
            entry.setAlarmValue(value);
            entry.setAlarmID(signalDefinition.getSignalName());
        }
        return entry;
    }

    /**
     * 現在の閾値に対応する超過開始時刻を設定する。
     * @param alarmData 閾値判定データ
     * @param level 閾値レベル
     */
    private void addStartExceedanceTime(final AlarmData alarmData, final int level)
    {
        Long targetExceedanceTime = alarmData.getMinRecoverTime(level);
        if (targetExceedanceTime == null)
        {
            targetExceedanceTime = Long.valueOf(System.currentTimeMillis());
        }
        alarmData.addStartExceedanceTime(level, targetExceedanceTime);
    }

    /**
     * 現在の閾値に対応する復旧開始時刻を設定する。
     * @param alarmData 閾値判定データ
     * @param level 閾値レベル
     */
    private void addStopExceedanceTime(final AlarmData alarmData, final int level)
    {
        Long targetExceedanceTime = alarmData.getMinRecoverTime(level);
        if (targetExceedanceTime == null)
        {
            targetExceedanceTime = Long.valueOf(System.currentTimeMillis());
        }
        alarmData.addRecoverTime(level, targetExceedanceTime);
    };

}
