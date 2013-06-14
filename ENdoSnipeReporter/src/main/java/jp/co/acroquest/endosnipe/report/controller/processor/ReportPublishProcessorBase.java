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
package jp.co.acroquest.endosnipe.report.controller.processor;

import java.io.File;

import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.controller.dispatcher.ReportPublishProcessor;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * レポート出力プロセッサのベースクラス。
 * 各レポートタイプに対応するレポート出力プロセッサは、本クラスを継承して作成すること。
 * 
 * 
 * @author M.Yoshida
 *
 */
public abstract class ReportPublishProcessorBase implements ReportPublishProcessor
{
    /** レポートプロセッサの工程の数 */
    public static final int     PROCESS_PHASE_NUM      = 3;

    /**  */
    private static final String GET_DATA_PHASE_KEY     = "reporter.report.progress.detail.getData";

    /**  */
    private static final String CONVERT_DATA_PHASE_KEY = "reporter.report.progress.detail.convData";

    /**  */
    private static final String OUTPUT_DATA_PHASE_KEY  = "reporter.report.progress.detail.output";

    /** レポート種別 */
    private ReportType          rType_;

    /** 出力先ディレクトリ名 */
    private String              outputDir_;

    /**
     * コンストラクタ。
     * 
     * @param type レポート種別
     */
    public ReportPublishProcessorBase(ReportType type)
    {
        rType_ = type;
    }

    /**
     * 自分が処理するレポート種別を取得する。
     * 
     * @return レポート種別
     */
    protected ReportType getReportType()
    {
        return rType_;
    }

    /**
     * 出力先ファイルのファイル名を取得する（拡張子無し）
     * 
     * @return 出力先ファイルのファイル名
     */
    protected String getOutputFileName()
    {
        File outputFile = new File(outputDir_, ReporterConfigAccessor.getOutputFileName(rType_));
        return outputFile.getAbsolutePath();
    }

    /**
     * 出力先フォルダのパスを取得する
     * 
     * @return 出力先フォルダのパス
     */
    protected String getOutputFolderName()
    {
        File outputFolder = new File(outputDir_);
        return outputFolder.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    public ReportProcessReturnContainer publish(ReportSearchCondition cond)
        throws InterruptedException
    {
        outputDir_ = cond.getOutputFilePath();

        ReportProcessReturnContainer retContainer = new ReportProcessReturnContainer();

        Object rawData = getReportPlotData(cond, retContainer);


        if (rawData != null)
        {
            Object convertedPlotData = convertPlotData(rawData, cond, retContainer);

            outputReport(convertedPlotData, cond, retContainer);
        }

        return retContainer;
    }

    /**
     * DBなどのエンティティから、レポートに出力するデータを取得する。
     * 
     * @param cond            データ取得条件
     * @param reportContainer 返り値などを渡すためのコンテナ
     * @return 取得したレポートのデータ
     */
    protected abstract Object getReportPlotData(ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer);

    /**
     * エンティティから取得したデータを、実際の表示の型に変換する。
     * 本メソッドは、変換を行わない場合は、オーバーライド不要。
     * 
     * @param rawData         エンティティから取得した元データ
     * @param cond            データ取得／変換条件
     * @param reportContainer 返り値などを渡すためのコンテナ
     * @return 変換後のデータ
     */
    protected Object convertPlotData(Object rawData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        return rawData;
    }

    /**
     * データをレポートに出力する。
     * 
     * @param plotData        レポートに出力するデータ
     * @param cond            データ出力条件
     * @param reportContainer 返り値などを渡すためのコンテナ
     */
    protected abstract void outputReport(Object plotData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer);
}
