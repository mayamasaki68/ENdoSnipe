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
var ENS = {};

ENS.ID = {};
ENS.ID.MEASUREMENT_TIME = "measurementTime";
ENS.ID.MEASUREMENT_VALUE = "measurementValue";
ENS.ID.MEASUREMENT_ITEM_NAME = "measurementItemName";

ENS.DATE_FORMAT_DETAIL = 'yyyy/MM/dd HH:mm:ss.fff';
ENS.DATE_FORMAT_DAY = 'yyyy/MM/dd';
ENS.DATE_FORMAT_HOUR = 'yyyy/MM/dd HH:mm';

ENS.URL = {};
ENS.URL.TERM_PERFDOCTOR_DATA_URL = "/performanceDoctor/getPerfDoctor";
ENS.URL.PERFDOCTOR_POSTFIX_ID = "/performanceDoctor";

ENS.common = {};
ENS.common.dualslider = {};
ENS.common.dualslider.scaleUnitStrings = 'hours';// for many hours
ENS.common.dualslider.scaleUnitString = 'hour';// for one hour
ENS.common.dualslider.scaleUnit = 60 * 60 * 1000; // millisecond
ENS.common.dualslider.groupString = 'day';// for one day
ENS.common.dualslider.groupStrings = 'days';// for many days
ENS.common.dualslider.groupUnitNum = 24;
ENS.common.dualslider.groupMaxNum = 10;
ENS.common.dualslider.groupDefaultNum = 3;
ENS.common.dualslider.idFrom = 'dualSliderFromValue';
ENS.common.dualslider.idTo = 'dualSliderToValue';

ENS.singleslider = {};
ENS.singleslider.scaleUnitString = 'hour';// for one hour
ENS.singleslider.scaleUnitStrings = 'hours';// for many hours
ENS.singleslider.scaleUnit = 60 * 60 * 1000; // millisecond
ENS.singleslider.groupString = 'day';// for one day
ENS.singleslider.groupStrings = 'days';// for many day
ENS.singleslider.groupUnitNum = 24;
ENS.singleslider.groupMaxNum = 10;
ENS.singleslider.groupDefaultNum = 3;
ENS.singleslider.idTime = 'singlesliderTimeValue';

ENS.nodeinfo = {};
ENS.nodeinfo.parent = {};
ENS.nodeinfo.parent.css = {};
ENS.nodeinfo.viewList = [];

ENS.nodeinfo.parent.css.informationArea = {
	fontSize : "14px",
	float : "right",
	width : "180px",
	height : "350px",
	border : "1px #dcdcdc solid",
	margin : "190px 20px 0px 0px"
};
ENS.nodeinfo.parent.css.legendArea = {
	height : "40px",
	margin : "5px 5px 5px 5px"
};
ENS.nodeinfo.parent.css.annotationLegendArea = {
	margin : "0px 0px 0px 0px",
	padding : "5px 5px 5px 5px"
};
ENS.nodeinfo.parent.css.dualSliderArea = {
	width : "800px",
	margin : "0px 0px 20px 60px"
};
ENS.nodeinfo.parent.css.graphArea = {
	float : "left",
	width : "650px",
	margin : "30px 0px 0px 10px"
};
ENS.nodeinfo.ONEDAY_MILLISEC = 86400000;

ENS.ResourceGraphAttribute = [ "colors", "labels", "valueRange", "xlabel",
		"ylabel", "strokeWidth", "legend", "labelsDiv", "width", "height" ];
ENS.nodeinfo.GRAPH_HEIGHT_MARGIN = 2;
ENS.nodeinfo.GRAPH_TITLE_LENGTH = 30;

ENS.tree = {};
ENS.tree.CLICK_LEFT = 1;
ENS.tree.CLICK_RIGHT = 3;
ENS.tree.ADD_SIGNAL_TYPE = "addSignal";
ENS.tree.EDIT_SIGNAL_TYPE = "editSignal";
ENS.tree.DELETE_SIGNAL_TYPE = "deleteSignal";
ENS.tree.OUTPUT_REPORT_TYPE = "outputReport";
ENS.tree.DELETE_REPORT_TYPE = "deleteReport";
ENS.tree.SIGNAL_PREFIX_ID = "/singalNode-";
ENS.tree.SIGNAL_ADD_URL = wgp.common.getContextPath() + "/signal/add";
ENS.tree.SIGNAL_GET_URL = wgp.common.getContextPath() + "/signal/getDefinition";
ENS.tree.SIGNAL_EDIT_URL = wgp.common.getContextPath() + "/signal/edit";
ENS.tree.SIGNAL_DELETE_URL = wgp.common.getContextPath() + "/signal/delete";
ENS.tree.SIGNAL_SELECT_ALL_URL = wgp.common.getContextPath()
		+ "/signal/getAllDefinition";
ENS.tree.REPORT_ADD_URL = wgp.common.getContextPath() + "/report/add";
ENS.tree.SIGNAL_NAME = "signalName";
ENS.tree.SIGNAL_VALUE = "signalValue";
ENS.tree.SIGNAL_DIALOG = "signalDialog";
ENS.tree.REPORT_DIALOG = "reportDialog";
ENS.tree.REPORT_PREFIX_ID = "/reportNode-";
ENS.tree.REPORT_SELECT_ALL_URL = wgp.common.getContextPath()
		+ "/report/getAllDefinition";
ENS.tree.REPORT_SELECT_BY_REPORT_NAME_URL = wgp.common.getContextPath()
		+ "/report/getDefinitionByReportName";
ENS.tree.REPORT_DELETE_BY_NAME_URL = wgp.common.getContextPath() + "/report/deleteByName";
ENS.tree.REPORT_DELETE_BY_ID_URL = wgp.common.getContextPath() + "/report/deleteById";
ENS.tree.GET_DIRECT_CHILDLEN_NODE = wgp.common.getContextPath() + "/tree/getDirectlChildNodes";
ENS.tree.GET_TOP_NODES = wgp.common.getContextPath() + "/tree/getTopNodes";
ENS.tree.GET_CHILD_TARGET_NODES = wgp.common.getContextPath() + "/tree/getChildTargetNodes";
ENS.tree.GET_ALL_CHILD_NODES = wgp.common.getContextPath() + "/tree/getAllChildNodes";

ENS.tree.type = {};
ENS.tree.type.GROUP = "group";
ENS.tree.type.TARGET = "target";
ENS.tree.type.SIGNAL = "signal";
ENS.tree.type.REPORT = "report";

ENS.tree.contextOption = [ {
	menu_id : ENS.tree.ADD_SIGNAL_TYPE,
	menu_name : "Add Signal",
	executeClass : "ENS.SignalDefinitionDialogView",
	showTreeTypes : [ ENS.tree.type.TARGET ],
	executeOption : {
		dialogId : ENS.tree.SIGNAL_DIALOG,
		signalType : ENS.tree.ADD_SIGNAL_TYPE
	},
	children : []
}, {
	menu_id : ENS.tree.EDIT_SIGNAL_TYPE,
	menu_name : "Edit Signal",
	executeClass : "ENS.SignalDefinitionDialogView",
	showTreeTypes : [ ENS.tree.type.SIGNAL ],
	executeOption : {
		dialogId : ENS.tree.SIGNAL_DIALOG,
		signalType : ENS.tree.EDIT_SIGNAL_TYPE
	},
	children : []
}, {
	menu_id : ENS.tree.DELETE_SIGNAL_TYPE,
	menu_name : "Delete Signal",
	executeClass : "",
	showTreeTypes : [ ENS.tree.type.SIGNAL ],
	executeOption : {
		dialogId : ENS.tree.SIGNAL_DIALOG,
		signalType : ENS.tree.DELETE_SIGNAL_TYPE
	},
	children : []
}, {
	menu_id : ENS.tree.OUTPUT_REPORT_TYPE,
	menu_name : "Output Report",
	executeClass : "ENS.ReportDialogView",
	showTreeTypes : [ ENS.tree.type.GROUP, ENS.tree.type.TARGET ],
	executeOption : {
		dialogId : ENS.tree.REPORT_DIALOG,
		signalType : ENS.tree.OUTPUT_REPORT_TYPE
	},
	children : []
}, {
	menu_id : ENS.tree.DELETE_REPORT_TYPE,
	menu_name : "Delete Report",
	executeClass : "",
	showTreeTypes : [ ENS.tree.type.REPORT ],
	executeOption : {
		dialogId : ENS.tree.REPORT_DIALOG,
		signalType : ENS.tree.DELETE_REPORT_TYPE
	},
	children : []
} ];
ENS.tree.SIGNAL_ICON_STOP = "signal_-1";
ENS.tree.SIGNAL_ICON_0 = "signal_0";
ENS.tree.SIGNAL_ICON_1 = "signal_1";
ENS.tree.SIGNAL_ICON_2 = "signal_2";
ENS.tree.SIGNAL_ICON_3 = "signal_3";
ENS.tree.SIGNAL_ICON_4 = "signal_4";
ENS.tree.SIGNAL_ICON_5 = "signal_5";
ENS.tree.REPORT_ICON = "report";

ENS.tree.DEFAULT_SIGNAL_NAME = "new Signal";
ENS.tree.DEFAULT_SIGNAL_PATTERN = "3";
ENS.tree.DEFAULT_ESCALATION_PERIOD = "15";

ENS.map = {};
ENS.map.mode = {};
ENS.map.mode.OPERATE = "OPERATE";
ENS.map.mode.EDIT = "EDIT";
ENS.map.fontSize = 16;
ENS.map.fontColor = "#F58400";

ENS.report = {};
ENS.report.DOWNLOAD_URL = wgp.common.getContextPath() + "/report/download";

ENS.perfDoctor = {};