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
package jp.co.acroquest.endosnipe.collector.notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jp.co.acroquest.endosnipe.collector.processor.AlarmType;

/**
 * シグナルのアラームを通知に使用するEntityクラス。
 * 
 * @author fujii
 */
public class AlarmEntry
{
    /** アラーム名 */
    private String alarmID_;

    /** 閾値 */
    private Double risingThreshold_;

    /** 状態値 */
    private Double alarmValue_;

    /** 計測間隔(単位:ミリ秒) */
    private double alarmInterval_;

    /** アラームの状態値 */
    private int alarmState_;

    /** アラームを出すかどうか */
    private boolean sendAlarm_;

    /** アラームの種類 */
    private AlarmType alarmType_;

    /** JavelinのIPアドレス */
    private String ipAddress_;

    /** Javelinのポート番号 */
    private int port_;

    /** ログデータの保存されるDBの名前 */
    private String databaseName_;

    /** パラメータ（追加情報） */
    private final Map<String, Object> parameters_;

    /** シグナルのレベル(3 or 5) */
    private int signalLevel_;

    /**
     * コンストラクタ。
     */
    public AlarmEntry()
    {
        this.parameters_ = new HashMap<String, Object>();
    }

    /**
     * アラーム名を取得する。
     * @return アラーム名
     */
    public String getAlarmID()
    {
        return this.alarmID_;
    }

    /**
     * アラーム名を設定する。
     * @param alarmID アラーム名
     */
    public void setAlarmID(final String alarmID)
    {
        this.alarmID_ = alarmID;
    }

    /**
     * 発生閾値を取得する。
     * @return 発生閾値
     */
    public Double getRisingThreshold()
    {
        return this.risingThreshold_;
    }

    /**
     * 発生閾値を設定する。
     * @param risingThreshold 発生閾値
     */
    public void setRisingThreshold(final Double risingThreshold)
    {
        this.risingThreshold_ = risingThreshold;
    }

    /**
     * 状態値を取得する。
     * @return 状態値
     */
    public Double getAlarmValue()
    {
        return this.alarmValue_;
    }

    /**
     * 状態値を設定する。
     * @param alarmValue 状態値
     */
    public void setAlarmValue(final Double alarmValue)
    {
        this.alarmValue_ = alarmValue;
    }

    /**
     * 計測間隔(単位:ミリ秒を取得する。
     * @return 計測間隔(単位:ミリ秒
     */
    public double getAlarmInterval()
    {
        return this.alarmInterval_;
    }

    /**
     * 計測間隔(単位:ミリ秒を設定する。
     * @param alarmInterval 計測間隔(単位:ミリ秒
     */
    public void setAlarmInterval(final double alarmInterval)
    {
        this.alarmInterval_ = alarmInterval;
    }

    /**
     * アラームの状態値を取得する。
     * @return アラームの状態値
     */
    public int getAlarmState()
    {
        return this.alarmState_;
    }

    /**
     * アラームの状態値を設定する。
     * @param alarmState アラームの状態値
     */
    public void setAlarmState(final int alarmState)
    {
        this.alarmState_ = alarmState;
    }

    /**
     * アラームを出すかどうかを設定する。
     * @param sendAlarm セットする sendAlarm
     */
    public void setSendAlarm(final boolean sendAlarm)
    {
        this.sendAlarm_ = sendAlarm;
    }

    /**
     * アラームを出すかどうかを取得する。
     * @return 対象の系列名
     */
    public boolean isSendAlarm()
    {
        return this.sendAlarm_;
    }

    /**
     * @param alarmType セットする alarmType
     */
    public void setAlarmType(final AlarmType alarmType)
    {
        this.alarmType_ = alarmType;
    }

    /**
     * @return alarmType　アラーム種別
     */
    public AlarmType getAlarmType()
    {
        return this.alarmType_;
    }

    /**
     * このアラームを検知したJavelinのIPアドレスを返します。<br />
     * 
     * @return IPアドレス
     */
    public String getIpAddress()
    {
        return ipAddress_;
    }

    /**
     * このアラームを検知したJavelinのIPアドレスを設定します。<br />
     * 
     * @param ipAddress IPアドレス
     */
    public void setIpAddress(final String ipAddress)
    {
        ipAddress_ = ipAddress;
    }

    /**
     * このアラームを検知したJavelinのポート番号を返します。<br />
     * 
     * @return port
     */
    public int getPort()
    {
        return port_;
    }

    /**
     * このアラームを検知したJavelinのポート番号を設定します。<br />
     * 
     * @param port ポート番号
     */
    public void setPort(final int port)
    {
        port_ = port;
    }

    /**
     * このアラームを検知したJavelinからのデータを保存するDBの名前を返します。<br />
     * 
     * @return databaseName DB名
     */
    public String getDatabaseName()
    {
        return databaseName_;
    }

    /**
     * このアラームを検知したJavelinからのデータを保存するDBの名前を設定します。<br />
     * 
     * @param databaseName DB名
     */
    public void setDatabaseName(final String databaseName)
    {
        databaseName_ = databaseName;
    }

    /**
     * パラメータを追加します。
     *
     * @param key キー
     * @param value 値
     */
    public void addParamter(final String key, final Object value)
    {
        this.parameters_.put(key, value);
    }

    /**
     * パラメータを取得します。
     *
     * @return パラメータ
     */
    public Map<String, Object> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters_);
    }

    /**
     * シグナルのレベルを取得する。
     * @return シグナルのレベル
     */
    public int getSignalLevel()
    {
        return signalLevel_;
    }

    /**
     * シグナルのレベルを設定する。
     * @param signalLevel シグナルのレベル
     */
    public void setSignalLevel(final int signalLevel)
    {
        signalLevel_ = signalLevel;
    }

}
