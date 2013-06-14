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
package jp.co.acroquest.endosnipe.web.dashboard.controller;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import jp.co.acroquest.endosnipe.data.dao.JavelinLogDao;
import jp.co.acroquest.endosnipe.data.entity.JavelinLog;
import jp.co.acroquest.endosnipe.perfdoctor.WarningUnit;
import jp.co.acroquest.endosnipe.web.dashboard.dto.PerfDoctorTableDto;
import jp.co.acroquest.endosnipe.web.dashboard.dto.comparator.PerfDoctorTableDtoComparator;
import jp.co.acroquest.endosnipe.web.dashboard.form.PerfDoctorTermDataForm;
import jp.co.acroquest.endosnipe.web.dashboard.manager.DatabaseManager;
import jp.co.acroquest.endosnipe.web.dashboard.service.JvnFileEntryJudge;
import jp.co.acroquest.endosnipe.web.dashboard.service.MeasurementValueService;
import jp.co.acroquest.endosnipe.web.dashboard.service.PerformanceDoctorService;
import jp.co.acroquest.endosnipe.web.dashboard.service.SignalService;
import jp.co.acroquest.endosnipe.web.dashboard.service.TreeMenuService;
import net.arnx.jsonic.JSON;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wgp.manager.WgpDataManager;

/**
 * PerformanceDoctorControllerクラス
 * 
 * @author acroquest
 * 
 */
@Controller
@RequestMapping("/performanceDoctor")
public class PerformanceDoctorController
{
	/** PerformanceDoctorページのID。 */
	private static final String PERFDOCTOR_POSTFIX_ID = "/performanceDoctor";

	/** シグナルのツリーメニューのサフィックスID */
	private static final String TREE_MENU_SIGNAL_SUFEIX_ID = "-signalNode";

	/** ツリーメニュー用サービス */
	@Autowired
	protected TreeMenuService treeMenuService;

	/** 計測値用サービス */
	@Autowired
	protected MeasurementValueService measurementValueService;

	/** シグナル用サービス */
	@Autowired
	protected SignalService signalService;

	/** WGPデータ用サービス */
	@Autowired
	private WgpDataManager wgpDataManager;

	/** Performance Doctor用サービス */
	@Autowired
	protected PerformanceDoctorService perfDoctorService;

	/**
	 * コンストラクタ
	 */
	public PerformanceDoctorController()
	{

	}

	/**
	 * PerformanceDoctorで表示するための期間データを返す。
	 * 
	 * @param data
	 *            取得する期間データの定義
	 * @return PerformanceDoctorにおける期間データ
	 */
	@RequestMapping(value = "/getPerfDoctor", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, List<PerfDoctorTableDto>> getTermPerfDoctorData(@RequestParam(value = "data") final String data)
	{
		Map<String, List<PerfDoctorTableDto>> responceDataList = new HashMap<String, List<PerfDoctorTableDto>>();
		DatabaseManager dbManager = DatabaseManager.getInstance();
		String dbName = dbManager.getDataBaseName(1);

		PerfDoctorTermDataForm termDataForm = JSON.decode(data, PerfDoctorTermDataForm.class);
		List<String> dataGroupIdList = termDataForm.getDataGroupIdList();

		for (String dataGroupId : dataGroupIdList)
		{
			List<JavelinLog> list = new ArrayList<JavelinLog>();
			Timestamp start = new Timestamp(Long.valueOf(termDataForm.getStartTime()));
			Timestamp end = new Timestamp(Long.valueOf(termDataForm.getEndTime()));

			try
			{
				list = JavelinLogDao.selectByTermAndName(dbName, start, end, dataGroupId, true);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			JvnFileEntryJudge judge = new JvnFileEntryJudge();
			List<WarningUnit> warnings = judge.judge(list, true, true);
			List<PerfDoctorTableDto> dtoList = new ArrayList<PerfDoctorTableDto>();
			for (WarningUnit warning : warnings)
			{
				PerfDoctorTableDto dto = new PerfDoctorTableDto();
				dto.setDate(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(warning.getStartTime())));
				dto.setDescription(warning.getDescription());
				dto.setLevel(warning.getLevel());
				dto.setClassName(warning.getClassName());
				dto.setMethodName(warning.getMethodName());
				dto.setDetail(warning.getLogFileName());
				dto.setLogFileName(warning.getLogFileName());
				dtoList.add(dto);
			}

			list.clear();
			Collections.sort(dtoList, new PerfDoctorTableDtoComparator());
			responceDataList.put(dataGroupId + PERFDOCTOR_POSTFIX_ID, dtoList);
		}
		return responceDataList;
	}

	/**
	 * ログファイルをダウンロードする。
	 * 
	 * @param res
	 *            {@link HttpServletResponse}オブジェクト
	 * @param fileName
	 *            ログファイル名
	 */
	@RequestMapping(value = "/download", method = RequestMethod.POST)
	@ResponseBody
	public void downloadJvnLog(final HttpServletResponse res, @RequestParam(value = "fileName") final String fileName)
	{
		this.perfDoctorService.doRequest(res, fileName);
	}

}
