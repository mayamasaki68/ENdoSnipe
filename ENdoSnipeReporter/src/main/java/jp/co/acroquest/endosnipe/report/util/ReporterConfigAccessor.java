/*
 * Copyright (c) 2004-2009 SMG Co., Ltd. All Rights Reserved.
 * Please read the associated COPYRIGHTS file for more details.
 *
 * THE  SOFTWARE IS  PROVIDED BY  SMG Co., Ltd., WITHOUT  WARRANTY  OF
 * ANY KIND,  EXPRESS  OR IMPLIED,  INCLUDING BUT  NOT LIMITED  TO THE
 * WARRANTIES OF  MERCHANTABILITY,  FITNESS FOR A  PARTICULAR  PURPOSE
 * AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDER BE LIABLE FOR ANY
 * CLAIM, DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package jp.co.acroquest.endosnipe.report.util;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;

import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportType;

/**
 * Reporterコンポーネントのコンフィグファイル（reporter.properties）の
 * 情報を取得するためのアクセサクラス。
 * 
 * @author M.Yoshida
 */
public class ReporterConfigAccessor
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER                  =
                                                                   ENdoSnipeLogger.getLogger(
                                                                                             ReporterConfigAccessor.class);
    
    /** コンフィグファイルパス */
    private static final String          PROPERTY_RESOURCE_PATH  = "/reporter.properties";
    
    /** テンプレートファイル */
    private static final String          REPORT_TEMPLATE_SUFFIX  = ".template";
    
    /** プロセッサ */
    private static final String          REPORT_PROCESSOR_SUFFIX = ".processor";
    
    /** レポート名称 */
    private static final String          REPORT_NAME_SUFFIX      = ".reportName";
    
    /** 出力ファイル */
    private static final String          REPORT_OUTPUT_SUFFIX    = ".outputFile";
    
    /** 説明 */
    private static final String          REPORT_EXPLANATION_SUFFIX    = ".explanation";
    
    /** 設定保持フィールド */
    private static Properties            configProperties__        = null;

    /**
     * インスタンス化防止のためのコンストラクタ
     */
    private ReporterConfigAccessor()
    {
    }

    private static Properties getReporterConfigProperties()
    {
        if (configProperties__ != null)
        {
            return configProperties__;
        }

        URL templateSetting = ReporterConfigAccessor.class.getResource(PROPERTY_RESOURCE_PATH);
        configProperties__ = new Properties();

        try
        {
            configProperties__.load(templateSetting.openStream());
        }
        catch (IOException e)
        {
            LOGGER.log(LogIdConstants.READ_FAULT_CONFIG, e, new Object[0]);
        }

        return configProperties__;
    }

    /**
     * 指定されたキーに関連するプロパティ値を取得する。
     * @param key キー
     * @return キーに関連する値
     */
    public static String getProperty(String key)
    {
        Properties configProp = getReporterConfigProperties();

        return configProp.getProperty(key);
    }

    /**
     * 指定されたキーに関連するプロパティに、置換パラメータを適用して取得する。
     * 
     * @param key   キー
     * @param param キーに関連する値に適用する置換パラメータ
     * @return キーに関連する値（置換済み）
     */
    public static String getPropertyWithParam(String key, Object... param)
    {
        String messagePattern = getProperty(key);
        return MessageFormat.format(messagePattern, param);
    }

    /**
     * 指定したレポートタイプのテンプレートへのパスをプロパティから取得する。
     * 
     * @param type レポートタイプ
     * @return テンプレートへのパス
     */
    public static String getReportTemplateResourcePath(ReportType type)
    {
        return getProperty(type.getId() + REPORT_TEMPLATE_SUFFIX);
    }

    /**
     * 指定したレポートタイプのレポートプロセッサの完全限定名をプロパティから取得する。
     * 
     * @param type レポートタイプ
     * @return レポートプロセッサの完全限定名
     */
    public static String getReportProcessorName(ReportType type)
    {
        return getProperty(type.getId() + REPORT_PROCESSOR_SUFFIX);
    }

    /**
     * 指定したレポートタイプのレポート名をプロパティから取得する。
     * 
     * @param type レポートタイプ
     * @return レポート名
     */
    public static String getReportName(ReportType type)
    {
        return getProperty(type.getId() + REPORT_NAME_SUFFIX);
    }

    /**
     * 指定したレポートタイプのレポート出力ファイル名を取得する。
     * 
     * @param type レポートタイプ
     * @return レポート出力ファイル名
     */
    public static String getOutputFileName(ReportType type)
    {
        return getProperty(type.getId() + REPORT_OUTPUT_SUFFIX);
    }
    
    /**
     * 指定したレポートタイプの説明を取得する。
     * 
     * @param type レポートタイプ
     * @return レポートの説明
     */
    public static String getExplanation(ReportType type)
    {
        return getProperty(type.getId() + REPORT_EXPLANATION_SUFFIX);
    }
}
