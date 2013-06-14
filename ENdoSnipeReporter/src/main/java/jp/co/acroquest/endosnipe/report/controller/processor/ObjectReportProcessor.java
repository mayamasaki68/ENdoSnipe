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
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.controller.TemplateFileManager;
import jp.co.acroquest.endosnipe.report.converter.compressor.CompressOperator;
import jp.co.acroquest.endosnipe.report.dao.util.GraphItemAccessUtil;
import jp.co.acroquest.endosnipe.report.entity.ItemData;
import jp.co.acroquest.endosnipe.report.entity.ObjectRecord;
import jp.co.acroquest.endosnipe.report.output.RecordReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * オブジェクト数のレポートを生成するレポートプロセッサ。
 * 
 * @author akiba
 */
public class ObjectReportProcessor extends ReportPublishProcessorBase {
	/** ロガー */
	private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
			ObjectReportProcessor.class);

	/**
	 * ReportProcessorを生成する。
	 * 
	 * @param type
	 *            レポート種別。
	 */
	public ObjectReportProcessor(ReportType type) {
		super(type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object getReportPlotData(ReportSearchCondition cond,
			ReportProcessReturnContainer reportContainer) {
		// 検索条件の取得
		String database = cond.getDatabases().get(0);
		Timestamp startTime = cond.getStartDate();
		Timestamp endTime = cond.getEndDate();
		String parentItemName = cond.getTargetItemName();
		Map<String, List<ItemData>> itemMap = new HashMap<String, List<ItemData>>();

		try {
			List<String> measurementItemNameList = GraphItemAccessUtil
					.findChildMeasurementItemName(database, parentItemName);

			int itemNameListLength = measurementItemNameList.size();

			for (int index = 0; index < itemNameListLength; index++) {
				String measurementItemName = measurementItemNameList.get(index);

				List<ItemData> itemDataList = GraphItemAccessUtil.findItemData(
						database, measurementItemName,
						CompressOperator.SIMPLE_AVERAGE, startTime, endTime);

				itemMap.put(measurementItemName, itemDataList);
			}
		} catch (SQLException ex) {
			LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
					ReporterConfigAccessor.getReportName(getReportType()));
			return null;
		}

		return itemMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object convertPlotData(Object rawData,
			ReportSearchCondition cond,
			ReportProcessReturnContainer reportContainer) {
		// データ変換は特に行いません。
		return rawData;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void outputReport(Object convertedData,
			ReportSearchCondition cond,
			ReportProcessReturnContainer reportContainer) {
		// map のデータを6グラフの個別のデータに分ける
		Map<String, List<? extends Object>> data = (Map<String, List<? extends Object>>) convertedData;

		// レポート出力の引数情報を取得する
		String outputFolderPath = getOutputFolderName()
				+ File.separator
				+ ReporterConfigAccessor.getProperty(super.getReportType()
						.getId() + ".outputFile");
		Timestamp startTime = cond.getStartDate();
		Timestamp endTime = cond.getEndDate();

		// Mapから全てのキーと値のエントリをSet型のコレクションとして取得する
		Set<Map.Entry<String, List<? extends Object>>> entrySet = data
				.entrySet();

		// キーと値のコレクションの反復子を取得する
		Iterator<Entry<String, List<? extends Object>>> it = entrySet
				.iterator();

		// 次の要素がまだ存在する場合はtrueが返される
		while (it.hasNext()) {
			// キーと値をセットを持つ、Map.Entry型のオブジェクトを取得する
			Entry<String, List<? extends Object>> entry = it.next();

			// Map.Entry型のオブジェクトからキーを取得する
			String key = entry.getKey();
			// Map.Entry型のオブジェクトから値を取得する
			List<ItemData> value = (List<ItemData>) entry.getValue();

			// 出力するレポートの種類にあわせてテンプレートのファイルパスを取得する
			String templateFilePath;
			try {
				templateFilePath = TemplateFileManager.getInstance()
						.getTemplateFile(ReportType.OBJECT_ITEM);
			} catch (IOException exception) {
				reportContainer.setHappendedError(exception);
				return;
			}

			// レポート出力を実行する
			RecordReporter<ObjectRecord> reporter = new RecordReporter<ObjectRecord>(
					getReportType());
			reporter.outputReports(templateFilePath, outputFolderPath
					+ File.separator + key, value, startTime, endTime);
		}

		// List<ItemData> listDataList = (List<ItemData>) data
		// .get(Constants.ITEMNAME_JAVAPROCESS_COLLECTION_LIST_COUNT);
		//
		// List<ItemData> queueDataList = (List<ItemData>) data
		// .get(Constants.ITEMNAME_JAVAPROCESS_COLLECTION_QUEUE_COUNT);
		//
		// List<ItemData> setDataList = (List<ItemData>) data
		// .get(Constants.ITEMNAME_JAVAPROCESS_COLLECTION_SET_COUNT);
		//
		// List<ItemData> mapDataList = (List<ItemData>) data
		// .get(Constants.ITEMNAME_JAVAPROCESS_COLLECTION_MAP_COUNT);
		//
		// List<ItemData> sizeDataList = (List<ItemData>) data
		// .get(Constants.ITEMNAME_JAVAPROCESS_COLLECTION_HISTOGRAM_SIZE);
		//
		// List<ItemData> countDataList = (List<ItemData>) data
		// .get(Constants.ITEMNAME_JAVAPROCESS_COLLECTION_HISTOGRAM_COUNT);

		// reporter.outputReports(
		// templateFilePath, outputFolderPath + File.separator +
		// Constants.ITEMNAME_JAVAPROCESS_COLLECTION_QUEUE_COUNT,
		// queueDataList, startTime, endTime);
		// reporter.outputReports(
		// templateFilePath, outputFolderPath + File.separator +
		// Constants.ITEMNAME_JAVAPROCESS_COLLECTION_SET_COUNT,
		// setDataList, startTime, endTime);
		// reporter.outputReports(
		// templateFilePath, outputFolderPath + File.separator +
		// Constants.ITEMNAME_JAVAPROCESS_COLLECTION_MAP_COUNT,
		// mapDataList, startTime, endTime);
		// reporter.outputReports(
		// templateFilePath,
		// outputFolderPath + File.separator +
		// Constants.ITEMNAME_JAVAPROCESS_COLLECTION_HISTOGRAM_SIZE,
		// sizeDataList, startTime, endTime);
		// reporter.outputReports(
		// templateFilePath,
		// outputFolderPath + File.separator +
		// Constants.ITEMNAME_JAVAPROCESS_COLLECTION_HISTOGRAM_COUNT,
		// countDataList, startTime, endTime);
	}
}
