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
package jp.co.acroquest.endosnipe.report.controller;

import java.util.ArrayList;
import java.util.List;

import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.dispatcher.ReportPublishDispatcher;
import jp.co.acroquest.endosnipe.report.controller.processor.SummaryReportProcessor;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;
import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportPublishTask;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;

/**
 * レポート出力処理を実行するタスクオブジェクト 進捗率の通知を伴う。
 * 
 * @author M.Yoshida
 */
public class ReportPublishTask  {
	/** 検索条件 */
	private ReportSearchCondition searchCondition_;

	/** レポート種別 */
	private ReportType[] publishTypes_;

	/** 終了時にコールバックする */
	private Runnable callback_;

	/** ロガー */
	private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
			ReportPublishTask.class);

	/**
	 * コンストラクタ
	 * 
	 * @param cond
	 *            検索条件
	 * @param publishType
	 *            レポート種別
	 * @param callback
	 *            コールバック
	 */
	public ReportPublishTask(ReportSearchCondition cond,
			ReportType[] publishType, Runnable callback) {
		List<ReportType> additionalTypes = new ArrayList<ReportType>();

		for (ReportType type : publishType) {
			additionalTypes.add(type);
		}

		searchCondition_ = cond;
		publishTypes_ = (ReportType[]) additionalTypes
				.toArray(new ReportType[0]);
		callback_ = callback;
	}

	/**
	 * 第二引数の全ての子要素に対してレポート出力を実行する。
	 * 
	 * @param monitor IProgressMonitorクラスのオブジェクト
	 * @param targetItemName レポート出力対象の親の項目名
	 * @return 実行完了時の状態
	 */
	public void createReport(String targetItemName) {
		searchCondition_
				.setTargetItemName(targetItemName);

		ReportPublishDispatcher dispatcher = ReportPublishDispatcher
				.getInstance();

		SummaryReportProcessor.setOutputFileTypeList(publishTypes_);

		for (ReportType type : publishTypes_) {
			ReportProcessReturnContainer retCont;
			retCont = dispatcher.dispatch(type, searchCondition_);

			if (retCont.getHappendedError() != null) {
				if (retCont.getHappendedError() instanceof InterruptedException) {
					return;
				}

				LOGGER.log(LogIdConstants.REPORT_PUBLISH_STOPPED_WARN,
						retCont.getHappendedError(),
						ReporterConfigAccessor.getReportName(type));
				continue;
			}
		}

		if (this.callback_ != null) {
			this.callback_.run();
		}

        return;
	}
}
