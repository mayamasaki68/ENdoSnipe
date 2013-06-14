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

import java.sql.Timestamp;
import java.util.List;

/**
 * レポート出力時の絞込み条件を示すオブジェクト。
 * 
 * @author M.Yoshida
 */
public class ReportSearchCondition {

	/** 対象データベースリスト */
	private List<String> databases_;

	/** 開始日時 */
	private Timestamp startDate_;

	/** 終了日時 */
	private Timestamp endDate_;

	/** 出力パス */
	private String outputFilePath_;

	/** レポート出力対象の計測項目の親の項目名 */
	private String targetItemName_;

	/** PerformanceDoctorレポート出力で、同一原因を絞り込むかどうか */
	private boolean limitSameCause_;

	/** PerformanceDoctorレポート出力で、同一ルールで絞り込むかどうか */
	private boolean limitBySameRule_;


	/**
	 * @return the databases
	 */
	public List<String> getDatabases() {
		return databases_;
	}

	/**
	 * @return the startDate
	 */
	public Timestamp getStartDate() {
		return startDate_;
	}

	/**
	 * @return the endDate
	 */
	public Timestamp getEndDate() {
		return endDate_;
	}

	/**
	 * @return the outputFilePath
	 */
	public String getOutputFilePath() {
		return outputFilePath_;
	}

	/**
	 * @param databases
	 *            the databases to set
	 */
	public void setDatabases(List<String> databases) {
		databases_ = databases;
	}

	/**
	 * @param startDate
	 *            the startDate to set
	 */
	public void setStartDate(Timestamp startDate) {
		startDate_ = startDate;
	}

	/**
	 * @param endDate
	 *            the endDate to set
	 */
	public void setEndDate(Timestamp endDate) {
		endDate_ = endDate;
	}

	/**
	 * @param outputFilePath
	 *            the outputFilePath to set
	 */
	public void setOutputFilePath(String outputFilePath) {
		outputFilePath_ = outputFilePath;
	}

	/**
	 * @return the limitSameCause
	 */
	public boolean getLimitSameCause() {
		return this.limitSameCause_;
	}

	/**
	 * @param limitSameCause
	 *            the limitSameCause to set
	 */
	public void setLimitSameCause(boolean limitSameCause) {
		this.limitSameCause_ = limitSameCause;
	}

	/**
	 * @return the limitBySameRule
	 */
	public boolean getLimitBySameRule() {
		return this.limitBySameRule_;
	}

	/**
	 * @param limitBySameRule
	 *            the limitBySameRule to set
	 */
	public void setLimitBySameRule(boolean limitBySameRule) {
		this.limitBySameRule_ = limitBySameRule;
	}

	/**
	 * レポート出力対象の計測項目の親の項目名を取得する。
	 * 
	 * @return レポート出力対象の計測項目の親の項目名
	 */
	public String getTargetItemName() {
		return targetItemName_;
	}

	/**
	 * レポート出力対象の計測項目の親の項目名を設定する。
	 * 
	 * @param targetItemName
	 *            レポート出力対象の計測項目の親の項目名
	 */
	public void setTargetItemName(String targetItemName) {
		this.targetItemName_ = targetItemName;
	}

}
