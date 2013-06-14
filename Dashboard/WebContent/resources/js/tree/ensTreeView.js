// シグナルダイアログの設定項目を格納しておくためのリスト
ENS.tree.signalDefinitionList = [];
ENS.tree.doRender = true;

ENS.treeView = wgp.TreeView
		.extend({
			/**
			 * ツリー要素をクリックした際の、別ペインとの 連携処理を行う。 targetId {String} 別ペインのID
			 */
			setClickEvent : function(targetId) {
				var instance = this;
				this.treeCollection = {};
				this.maxId = 0;
				this.targetId = targetId;
				$("#" + this.$el.attr("id")).mousedown(function(event) {
					var whichValue = event.which;
					if (whichValue != 1) {
						return;
					}
					var target = event.target;
					if ("A" == target.tagName) {
						var treeId = $(target).attr("id");
						var treeModel = instance.collection.get(treeId);
						instance.clickModel(treeModel);
					}
				});
			},
			/**
			 * クリックされたツリー要素のモデルに応じて処理を行う。 treeModel {Backbone.Model}
			 */
			clickModel : function(treeModel) {

				// モデルタイプが「シグナル」の場合は何もしない。
				if (ENS.tree.type.SIGNAL == treeModel.get("type")) {
					return;
				}

				if (this.childView) {
					var tmpAppView = new wgp.AppView();
					tmpAppView.removeView(this.childView);
					this.childView = null;
				}
				$("#" + this.targetId).children().remove();
				var targetContentId = this.targetId + "_content";
				$("#" + this.targetId).append(
						"<div id ='" + targetContentId + "'></div>");

				var dataId = treeModel.get("id");

				var viewSettings = null;
				$.each(wgp.constants.VIEW_SETTINGS, function(index, value) {
					if (dataId.match(index)) {
						viewSettings = value;
						return false;
					}
				});
				if (viewSettings == null) {
					viewSettings = wgp.constants.VIEW_SETTINGS["default"];
					if (viewSettings == null) {
						return;
					}
				}
				var viewClassName = viewSettings.viewClassName;
				$.extend(true, viewSettings, {
					id : targetContentId
				});
				var treeSettings = treeModel.attributes;
				// 動的に生成するオブジェクトを切り替える必要があるため、やむを得ずeval()を使う
				this.childView = eval("new " + viewClassName
						+ "(viewSettings, treeSettings)");
			},
			render : function(renderType, treeModel) {
				// renderを行わない場合はreturnする
				if (ENS.tree.doRender === false) {
					return;
				}
				if (renderType == wgp.constants.RENDER_TYPE.ADD) {
					var parentTreeId = treeModel.get("parentTreeId");
					var idAttribute = treeModel.idAttribute;
					var targetTag;
					if (parentTreeId !== null && parentTreeId !== undefined) {
						targetTag = this.getTreeNode(parentTreeId, idAttribute);
					}

					var instance = this;
					var treeData = this.createTreeData(treeModel);

					$("#" + this.$el.attr("id")).jstree("create_node",
							$(targetTag), "last", treeData).bind(
							"create_node.jstree",
							function(event, data) {
								var childModel = data.args[2].data[0];
								var icon = childModel.icon;

								var id = childModel.attr.Id;
								var elem = document.getElementById(id);

								if (icon == "center") {
									var parentLiTag = $(elem).parent("li");
									if (parentLiTag) {
										parentLiTag.attr("class",
												"jstree-last jstree-closed");
									}
								}

								if (icon == "center" || icon == "leaf") {
									// シグナルやレポートアイコンなど、その他ノードをコレクションから検索し追加する
									instance.addOtherNodes(id);
								}
							});
				} else {
					wgp.TreeView.prototype.render.call(this, renderType,
							treeModel);
				}
			},
			renderAll : function() {
				var instance = this;
				// View jsTree
				var settings = this.treeOption;
				settings = $.extend(true, settings, {
					json_data : {
						data : this.createJSONData()
					}
				});

				$("#" + this.$el.attr("id")).jstree(settings).bind(
						"loaded.jstree", function(event, data) {
							instance.setOpenCloseIcon();
						});

				this.getAllReport_();
				this.getAllSignal_();
			},
			/**
			 * ツリーを開け閉めするためのアイコンを設定する。
			 */
			setOpenCloseIcon : function() {
				_.each(this.collection.models, function(model, index) {
					var type = model.attributes.type;
					if (type == ENS.tree.type.GROUP) {
						var id = model.attributes.id;
						var elem = document.getElementById(id);
						var parentLiTag = $(elem).parent("li");
						if (parentLiTag) {
							parentLiTag.attr("class",
									"jstree-last jstree-closed");
						}
					}
				});
			},
			createTreeData : function(treeModel) {
				var returnData = wgp.TreeView.prototype.createTreeData.call(
						this, treeModel);
				var titleData = returnData.data;
				if (titleData.title.indexOf("&#47;") >= 0) {
					titleData.title = titleData.title.split("&#47;").join("/");
				}

				var type = treeModel.get("type");
				if (treeModel.get("icon")) {
					returnData.data.icon = treeModel.get("icon");
				} else if (type == ENS.tree.type.GROUP) {
					returnData.data.icon = "center";
				} else if (type == ENS.tree.type.TARGET) {
					returnData.data.icon = "leaf";
				}
				return returnData;
			},
			addOtherNodes : function(childNodeId) {
				var otherNodes = this.collection.where({
					parentTreeId : childNodeId
				});

				var idAttribute = "id";
				var targetTag = this.getTreeNode(childNodeId, idAttribute);

				var instance = this;
				_.each(otherNodes, function(otherNode, index) {
					var treeData = instance.createTreeData(otherNode);
					$("#" + instance.$el.attr("id")).jstree("create_node",
							$(targetTag), "last", treeData);
				});

			},
			/**
			 * ツリーにコンテキストメニューの表示設定を行う。
			 */
			addContextMenu : function(contextOption) {
				// contextOptionの中身
				// "menu_id" : 表示するコンテキストメニュータグのID名,
				// "menu_name" : 表示するメニュー名
				// "showParam" : コンテキストメニュー表示制限,
				// "executeClass" : メニュー選択時の実行クラス。
				// "children" : 子要素のコンテキストオプション
				var instance = this;
				this.contextCollection = new Backbone.Collection();
				this.addContextCollection_(contextOption);

				// コンテキストメニュー用のタグを生成する。
				var menuId = this.$el.attr("id") + "_contextManu";
				var tmpClickTarget = null;

				var settingOptions = {
					onShow : function(event, target) {
						var clickTarget = event.target;
						var tagName = clickTarget.tagName;
						if (tagName != "A") {
							return false;
						}
						var treeId = $(clickTarget).attr("id");
						var treeModel = instance.collection.get(treeId);

						// 各コンテキストメニューの表示可否設定を行う。
						instance.checkShowContext_(treeModel);
						tmpClickTarget = clickTarget;

					},
					onSelect : function(event, target) {
						var clickTarget = event.target;
						var id = $(clickTarget).attr("id");
						var targetOption = instance.getContextOption_(id);
						if (targetOption == null) {
							return;
						}
						var dialogId = targetOption.get("executeOption").dialogId;

						if (dialogId == ENS.tree.SIGNAL_DIALOG) {
							instance.signalOnSelect_(event, target, menuId,
									tmpClickTarget);
						} else if (dialogId == ENS.tree.REPORT_DIALOG) {
							instance.reportOnSelect_(event, target, menuId,
									tmpClickTarget);
						}
					}
				};
				contextMenuCreator.initializeContextMenu(menuId, contextOption);
				contextMenuCreator.createContextMenu(this.$el.attr("id"),
						menuId, settingOptions);
			},
			/**
			 * シグナルに関連するコンテキストメニュー選択時の処理を行う。
			 */
			signalOnSelect_ : function(event, target, menuId, tmpClickTarget) {
				var clickTarget = event.target;
				var id = $(clickTarget).attr("id");
				var targetOption = this.getContextOption_(id);

				var executeOption = targetOption.get("executeOption");
				executeOption.treeId = $(tmpClickTarget).attr("id");
				executeOption.displayName = $(tmpClickTarget).text();

				// 削除が選択された場合はそのノードを削除する
				if (id == ENS.tree.DELETE_SIGNAL_TYPE) {
					if (confirm("Are you sure you want to delete it?")) {
						this.deleteSignal_(executeOption);
					}
					return;
				}

				// 開くViewのクラス
				var executeClass = targetOption.get("executeClass");
				if (executeClass) {
					if (id == ENS.tree.ADD_SIGNAL_TYPE) {
						// Matching Patternにデフォルトのツリー階層を入力する
						$("#matchingPattern").val(executeOption.treeId);

						// 編集の場合は事前に値を設定する。
					} else if (id == ENS.tree.EDIT_SIGNAL_TYPE) {
						var treeModel = this.collection
								.get(executeOption.treeId);
						this.inputSignalDialog_(treeModel);

						// 追加時と同様に扱うため、親ツリーのIDを設定する。
						executeOption.treeId = this
								.getSignalParentTreeId_(executeOption.treeId);
					}

					// set execute class and function, if push ok
					// button.
					executeOption.okObject = this;
					executeOption.okFunctionName = "signalPushOkFunction";
					executeOption.cancelObject = this;
					executeOption.cancelFunctionName = "signalPushCancelFunction";
					eval("new " + executeClass + "(executeOption)");
				}
			},
			/**
			 * レポートに関するコンテキストメニュー選択時の処理を行う。
			 */
			reportOnSelect_ : function(event, target, menuId, tmpClickTarget) {
				var clickTarget = event.target;
				var id = $(clickTarget).attr("id");
				var targetOption = this.getContextOption_(id);
				if (targetOption == null) {
					return;
				}

				var executeOption = targetOption.get("executeOption");
				executeOption.treeId = $(tmpClickTarget).attr("id");
				executeOption.displayName = $(tmpClickTarget).text();

				// 削除が選択された場合はそのノードを削除する
				if (id == ENS.tree.DELETE_REPORT_TYPE) {
					if (confirm("Are you sure you want to delete it?")) {
						this.deleteReport_(executeOption);
					}
					return;
				}

				// 開くViewのクラス
				var executeClass = targetOption.get("executeClass");
				if (executeClass) {
					if (id == ENS.tree.OUTPUT_REPORT_TYPE) {
						// Target Measurement Nameにツリー階層を入力する
						$("#targetName").val(executeOption.treeId);
					}

					// set execute class and function, if push ok
					// button.
					executeOption.okObject = this;
					executeOption.okFunctionName = "reportPushOkFunction";
					executeOption.cancelObject = this;
					executeOption.cancelFunctionName = "reportPushCancelFunction";
					eval("new " + executeClass + "(executeOption)");
				}
			},
			/** 閾値判定の定義を入力した後に実行するメソッド。 */
			signalPushOkFunction : function(event, option) {

				// add tree data for signal
				var treeId = option.treeId;
				var signalDispalyName = $("#signalName").val();
				var signalName = treeId + "/" + signalDispalyName;

				// Ajax通信のコールバック関数名
				var callbackFunction = "";
				// Ajax通信の送信先URL
				var url = "";
				// サーバに送信するデータ
				var sendData;
				// Ajax通信のコールバック関数と送信先URLを追加か編集かによって決める
				// シグナル追加時
				if (option.signalType == ENS.tree.ADD_SIGNAL_TYPE) {
					sendData = this.createSendAddSignalData_(signalName);
					callbackFunction = "callbackAddSignal_";
					url = ENS.tree.SIGNAL_ADD_URL;

					// シグナル編集時
				} else if (option.signalType == ENS.tree.EDIT_SIGNAL_TYPE) {
					sendData = this.createSendEditSignalData_(signalName);
					callbackFunction = "callbackEditSignal_";
					url = ENS.tree.SIGNAL_EDIT_URL;
				}

				// Ajax通信用の設定
				var settings = {
					data : sendData,
					url : url
				};

				// 非同期通信でデータを送信する
				var ajaxHandler = new wgp.AjaxHandler();
				settings[wgp.ConnectionConstants.SUCCESS_CALL_OBJECT_KEY] = this;
				settings[wgp.ConnectionConstants.SUCCESS_CALL_FUNCTION_KEY] = callbackFunction;
				ajaxHandler.requestServerAsync(settings);

				// 閾値判定定義の入力内容をクリアする。
				this.clearSignalDialog_();
			},
			/** 閾値判定の定義を入力をキャンセルした場合に実行するメソッド */
			signalPushCancelFunction : function(event, option){

				// 閾値判定定義の入力内容をクリアする。
				this.clearSignalDialog_();
			},
			/**
			 * シグナル新規追加時の送信データを生成する。
			 */
			createSendAddSignalData_ : function(signalName) {
				// 入力された秒をミリ秒に変換する
				var escalationPeriod = $("#escalationPeriod").val() * 1000;
				// シグナル定義を作成する
				var signalDefinition = {
					signalId : $("#signalId").val(),
					signalName : signalName,
					matchingPattern : $("#matchingPattern").val(),
					level : $("#signalPatternValue select").val(),
					patternValue : this.createPatternValue_(),
					escalationPeriod : escalationPeriod
				};

				var sendData = {
					signalDefinition : JSON.stringify(signalDefinition)
				};

				return sendData;
			},
			/**
			 * シグナル編集時の送信データを生成する。
			 */
			createSendEditSignalData_ : function(signalName) {
				// 入力された秒をミリ秒に変換する
				var escalationPeriod = $("#escalationPeriod").val() * 1000;
				// シグナル定義を作成する
				var signalDefinition = {
					signalId : $("#signalId").val(),
					signalName : signalName,
					matchingPattern : $("#matchingPattern").val(),
					level : $("#signalPatternValue select").val(),
					patternValue : this.createPatternValue_(),
					escalationPeriod : escalationPeriod
				};

				var sendData = {
					signalDefinition : JSON.stringify(signalDefinition)
				};

				return sendData;
			},
			createPatternValue_ : function() {
				var level = $("#signalPatternValue select").val();

				// 各レベルの閾値をカンマ区切りの文字列としてサーバに送る
				var patternValue = "";
				if (level == 3) {
					patternValue += $("#patternValue_2").val();
					patternValue += ",";
					patternValue += $("#patternValue_4").val();
				} else if (level == 5) {
					patternValue += $("#patternValue_1").val();
					patternValue += ",";
					patternValue += $("#patternValue_2").val();
					patternValue += ",";
					patternValue += $("#patternValue_3").val();
					patternValue += ",";
					patternValue += $("#patternValue_4").val();
				}

				return patternValue;
			},
			/** レポート出力の定義を入力した後に実行するメソッド。 */
			reportPushOkFunction : function(event, option) {
				// Ajax通信のコールバック関数名
				var callbackFunction = "";
				// Ajax通信の送信先URL
				var url = "";
				// レポート出力定義
				var reportDefinition;
				// サーバに送信するデータ
				var sendData;
				// Ajax通信のコールバック関数と送信先URLをシグナルタイプによって決める
				if (option.signalType == ENS.tree.OUTPUT_REPORT_TYPE) {
					reportDefinition = this.createReportDefinition_(option);
					sendData = {
						reportDefinition : JSON.stringify(reportDefinition)
					};
					callbackFunction = "callbackAddReport_";
					url = ENS.tree.REPORT_ADD_URL;
				}

				// Ajax通信用の設定
				var settings = {
					data : sendData,
					url : url
				};

				// 非同期通信でデータを送信する
				var ajaxHandler = new wgp.AjaxHandler();
				settings[wgp.ConnectionConstants.SUCCESS_CALL_OBJECT_KEY] = this;
				settings[wgp.ConnectionConstants.SUCCESS_CALL_FUNCTION_KEY] = callbackFunction;
				ajaxHandler.requestServerAsync(settings);

				// レポート出力ダイアログの入力内容をクリアする。
				this.clearReportDialog_();

				// ツリーノードを追加する
				var treeOption = this.createReportTreeOption_(reportDefinition);
				this.collection.add([ treeOption ]);
			},
			/** レポート出力をキャンセルした場合に実行するメソッド */
			reportPushCancelFunction : function(event, option){

				// レポート出力ダイアログの入力内容をクリアする。
				this.clearReportDialog_();
			},
			createReportDefinition_ : function(option) {
				var treeId = option.treeId;
				var reportName = $("#reportName").val();
				var reportFullName = treeId + "/" + reportName;

				// レポート出力定義を作成する
				var reportDefinition = {
					reportId : $("#reportId").val(),
					reportName : reportFullName,
					targetMeasurementName : $("#targetName").val(),
					reportTermFrom : $("#jquery-ui-datepicker-from").val(),
					reportTermTo : $("#jquery-ui-datepicker-to").val()
				};

				return reportDefinition;
			},
			checkShowContext_ : function(treeModel) {
				$.each(this.contextCollection.models, function(index, value) {
					var menuId = value.get("menu_id");
					$("#" + menuId).show();
					var showTreeTypes = value.get("showTreeTypes");
					if (!showTreeTypes) {
						return true;
					}

					if (_.contains(showTreeTypes, treeModel.get("type"))) {
						return true;
					} else {
						$("#" + menuId).hide();
					}
				});
			},
			addContextCollection_ : function(contextOption) {
				var instance = this;
				jQuery.each(contextOption, function(index, target) {
					instance.contextCollection.push(target);
					var children = target.children;
					if (children != null && children.length !== 0) {
						instance.addContextCollection_(children);
					}
				});
			},
			getContextOption_ : function(id) {
				var optionList = this.contextCollection.where({
					menu_id : id
				});
				if (optionList == null) {
					return;
				}
				if (optionList.length === 0) {
					return;
				}
				return optionList[0];
			},
			deleteSignal_ : function(executeOption) {
				// リアルタイム通信を止める
				appView.stopSyncData([ executeOption.treeId ]);

				// Ajax通信の送信先URL
				var callbackFunction = "callbackDeleteSignal_";
				var url = ENS.tree.SIGNAL_DELETE_URL;

				// Ajax通信用の設定
				var settings = {
					data : {
						signalName : executeOption.treeId
					},
					url : url
				};

				// 非同期通信でシグナル削除依頼電文を送信する。
				var ajaxHandler = new wgp.AjaxHandler();
				settings[wgp.ConnectionConstants.SUCCESS_CALL_OBJECT_KEY] = this;
				settings[wgp.ConnectionConstants.SUCCESS_CALL_FUNCTION_KEY] = callbackFunction;
				ajaxHandler.requestServerAsync(settings);
			},
			deleteReport_ : function(executeOption) {
				// Ajax通信の送信先URL
				var callbackFunction = "callbackDeleteReport_";
				var url = ENS.tree.REPORT_DELETE_BY_NAME_URL;

				// ツリー
				var treeId = executeOption.treeId;
				var treeIdSplitList = treeId.split("/");
				var splitLength = treeIdSplitList.length;

				var reportName = "/";
				for ( var index = 1; index < splitLength - 1; index++) {
					reportName += treeIdSplitList[index];
					reportName += "/";
				}
				var treeModel = this.collection.get(treeId);
				reportName += treeModel.get("data");

				// Ajax通信用の設定
				var settings = {
					data : {
						reportName : reportName
					},
					url : url
				};

				// 非同期通信でシグナル削除依頼電文を送信する。
				var ajaxHandler = new wgp.AjaxHandler();
				settings[wgp.ConnectionConstants.SUCCESS_CALL_OBJECT_KEY] = this;
				settings[wgp.ConnectionConstants.SUCCESS_CALL_FUNCTION_KEY] = callbackFunction;
				ajaxHandler.requestServerAsync(settings);
			},
			getAllSignal_ : function() {
				// シグナル定義を取得する
				// Ajax通信用の設定
				var settings = {
					url : ENS.tree.SIGNAL_SELECT_ALL_URL
				};

				// 非同期通信でデータを送信する
				var ajaxHandler = new wgp.AjaxHandler();
				settings[wgp.ConnectionConstants.SUCCESS_CALL_OBJECT_KEY] = this;
				settings[wgp.ConnectionConstants.SUCCESS_CALL_FUNCTION_KEY] = "callbackGetAllSignal_";
				ajaxHandler.requestServerAsync(settings);
			},
			getAllReport_ : function() {
				// レポート出力定義を取得する
				// Ajax通信用の設定
				var settings = {
					url : ENS.tree.REPORT_SELECT_ALL_URL
				};

				// 非同期通信でデータを送信する
				var ajaxHandler = new wgp.AjaxHandler();
				settings[wgp.ConnectionConstants.SUCCESS_CALL_OBJECT_KEY] = this;
				settings[wgp.ConnectionConstants.SUCCESS_CALL_FUNCTION_KEY] = "callbackGetAllReport_";
				ajaxHandler.requestServerAsync(settings);
			},
			callbackGetAllSignal_ : function(signalDefinitionList) {
				var instance = this;
				var addOptionList = [];
				_.each(signalDefinitionList, function(signalDefinition, index) {
					var treeOption = instance
							.createSignalTreeOption_(signalDefinition);
					addOptionList.push(treeOption);
				});

				// renderのADDを実行する権限を無くす
				ENS.tree.doRender = false;
				// ツリーノードに追加する
				this.collection.add(addOptionList);
				// renderのADDを実行する権限を与える
				ENS.tree.doRender = true;
			},
			callbackGetAllReport_ : function(reportDefinitionList) {
				var instance = this;
				var addOptionList = [];

				// 追加するツリーノードのオプションを作成する
				_.each(reportDefinitionList, function(reportDefinition,
						reportIndex) {
					var treeOption = instance
							.createReportTreeOption_(reportDefinition);
					addOptionList.push(treeOption);
				});

				// renderのADDを実行する権限を無くす
				ENS.tree.doRender = false;
				// ツリーノードに追加する
				this.collection.add(addOptionList);
				// renderのADDを実行する権限を与える
				ENS.tree.doRender = true;
			},
			/**
			 * シグナル追加操作の結果を表示する。
			 */
			callbackAddSignal_ : function(responseDto) {
				var result = responseDto.result;

				// 追加操作に失敗した場合はメッセージを表示する。
				if (result === "fail") {
					var message = responseDto.message;
					alert(message);
					return;
				}
				var signalDefinition = responseDto.data;
				var signalId = signalDefinition.signalId;
				if (signalId === 0) {
					alert("This signalName already exists."
							+ "\nPlease change signal name and try again.");
					return;
				}
			},
			/**
			 * シグナル編集操作の結果を表示する。
			 */
			callbackEditSignal_ : function(responseDto) {
				var result = responseDto.result;

				// 編集操作に失敗した場合はメッセージを表示する。
				if (result === "fail") {
					var message = responseDto.message;
					alert(message);
					return;
				}
			},
			// シグナル削除処理応答受信後にツリーからシグナルを削除する。
			callbackDeleteSignal_ : function(responseDto) {
				var result = responseDto.result;

				// 削除操作に失敗した場合はメッセージを表示する。
				if (result === "fail") {
					var message = responseDto.message;
					alert(message);
					return;
				}
			},
			// レポート削除処理応答受信後にツリーからシグナルを削除する。
			callbackDeleteReport_ : function(data) {
				var reportName = data.reportName;
				if (reportName === undefined || reportName === null) {
					alert("Failed to delete report.");
					return;
				}

				var reportNameSplitList = reportName.split("/");
				var splitLength = reportNameSplitList.length;

				var treeId = "";
				// レポート名からツリーIDを作成する
				for ( var index = 1; index < splitLength; index++) {
					if (index == splitLength - 1) {
						treeId += ENS.tree.REPORT_PREFIX_ID;
					} else {
						treeId += "/";
					}
					treeId += reportNameSplitList[index];
				}

				this.collection.remove({
					id : treeId
				});
			},
			callbackAddReport_ : function(reportDefinition) {
				// TODO レポート一覧テーブルが表示されているときはリロードする
			},
			createSignalTreeOption_ : function(signalDefinition) {
				var signalName = signalDefinition.signalName;

				var nameSplitList = signalName.split("/");
				var nameSplitListLength = nameSplitList.length;

				var showName = nameSplitList[nameSplitListLength - 1];
				// 親ノードのパス
				var targetTreeId = "";
				// 新規ノードのパス
				var reportTreeId = "";
				// 親ノードへのパスと、新規ノードのパスを作成する
				for ( var index = 1; index < nameSplitListLength; index++) {
					var nameSplit = nameSplitList[index];

					if (index == nameSplitListLength - 1) {
						reportTreeId += ENS.tree.REPORT_PREFIX_ID;
					} else {
						targetTreeId += "/";
						targetTreeId += nameSplit;

						reportTreeId += "/";
					}
					reportTreeId += nameSplit;
				}

				var treeOption = {
					id : signalName,
					data : showName,
					parentTreeId : targetTreeId,
					icon : ENS.tree.SIGNAL_ICON_0,
					type : ENS.tree.type.SIGNAL
				};

				return treeOption;
			},
			createReportTreeOption_ : function(reportDefinition) {
				var reportId = reportDefinition.reportId;

				var reportName = reportDefinition.reportName;

				var nameSplitList = reportName.split("/");
				var nameSplitListLength = nameSplitList.length;

				var showName = nameSplitList[nameSplitListLength - 1];
				// 親ノードのパス
				var targetTreeId = "";
				// 新規ノードのパス
				var reportTreeId = "";
				// 親ノードへのパスと、新規ノードのパスを作成する
				for ( var index = 1; index < nameSplitListLength; index++) {
					var nameSplit = nameSplitList[index];

					if (index == nameSplitListLength - 1) {
						reportTreeId += ENS.tree.REPORT_PREFIX_ID;
					} else {
						targetTreeId += "/";
						targetTreeId += nameSplit;

						reportTreeId += "/";
					}
					reportTreeId += nameSplit;
				}

				var treeOption = {
					id : reportTreeId,
					data : showName,
					parentTreeId : targetTreeId,
					icon : ENS.tree.REPORT_ICON,
					type : ENS.tree.type.REPORT
				};

				return treeOption;
			},
			/**
			 * シグナルダイアログをクリアする。
			 */
			clearSignalDialog_ : function() {
				$("#signalId").val("");
				$("#signalName").val(ENS.tree.DEFAULT_SIGNAL_NAME);
				$("#matchingPattern").val("");
				$("#signalPatternValue select").val(
						ENS.tree.DEFAULT_SIGNAL_PATTERN);
				$("#patternValue_1").val("");
				$("#patternValue_2").val("");
				$("#patternValue_3").val("");
				$("#patternValue_4").val("");
				$("#patternValue_5").val("");
				$("#escalationPeriod").val(ENS.tree.DEFAULT_ESCALATION_PERIOD);
			},
			/**
			 * レポートダイアログをクリアする。
			 */
			clearReportDialog_ : function() {
				$("#reportId").val("");
				$("#reportName").val("new report");
				$("#targetName").val("");
				$("#jquery-ui-datepicker-from").val("");
				$("#jquery-ui-datepicker-to").val("");

				$("#jquery-ui-datepicker-from, #jquery-ui-datepicker-to")
						.datetimepicker('destroy');
			},
			inputSignalDialog_ : function(treeModel) {
				// Ajax通信用の送信先URL
				var settings = {
					url : ENS.tree.SIGNAL_GET_URL,
					data : {
						signalName : treeModel.get("id")
					}
				}

				var ajaxHandler = new wgp.AjaxHandler();
				var result = ajaxHandler.requestServerSync(settings);
				var signalDefinition = JSON.parse(result);
				var patternValueSplit = signalDefinition.patternValue
						.split(",");

				var level = signalDefinition.level;

				// レベル別の閾値を入力する
				if (level == 3) {
					$("#patternValue_2").val(patternValueSplit[0]);
					$("#patternValue_4").val(patternValueSplit[1]);
				} else if (level == 5) {
					_.each(patternValueSplit, function(data, index) {
						var patternValue = patternValueSplit[index];
						$("#patternValue_" + (index + 1)).val(patternValue);
					});
				}

				// 各入力項目を入力する
				$("#signalId").val(signalDefinition.signalId);

				// シグナル名の表示名称は自身より親のツリー構造を除外した値を指定する。
				$("#signalName")
						.val(
								this
										.getSignalDisplayName_(signalDefinition.signalName));
				$("#beforeSignalName").val(signalDefinition.signalName);

				$("#matchingPattern").val(signalDefinition.matchingPattern);
				$("#signalPatternValue select").val(level);

				var escalationPeriod = $("#escalationPeriod").val(
						(signalDefinition.escalationPeriod - 0) / 1000);
			},
			getIcon : function(signalDefinition) {
				var icon = "";
				var level = signalDefinition.level;
				var signalValue = signalDefinition.signalValue;

				if (signalValue === undefined) {
					return ENS.tree.SIGNAL_ICON_0;
				}
				var signalLevel = 0;
				if (level == 3) {
					signalLevel = parseInt(signalValue, 10);
					if (0 <= signalLevel && signalLevel < 3) {
						icon = "signal_" + 2 * signalLevel;
					} else {
						icon = ENS.tree.SIGNAL_ICON_STOP;
					}
				} else if (level == 5) {
					signalLevel = parseInt(signalValue, 10);
					if (0 <= signalLevel && signalLevel < 5) {
						icon = "signal_" + signalLevel;
					} else {
						icon = ENS.tree.SIGNAL_ICON_STOP;
					}
				} else {
					icon = ENS.tree.SIGNAL_ICON_STOP;
				}

				return icon;
			},
			/**
			 * ツリーの表示状態を復元する。 selectTreeId {String} 選択しているツリーのID openNodes
			 * {String[]} 展開しているツリーのID配列
			 */
			restoreDisplayState : function(selectTreeId, openNodes) {

				// ツリーの展開状態を復元する。
				if (openNodes && openNodes.length > 0) {
					this.openNodes(openNodes, "id");
				}

				// ツリーの選択状態を復元する。
				if (selectTreeId) {
					this.selectNode(selectTreeId, "id");
				}
			},
			/**
			 * ツリー要素の基となるコレクションに要素が追加された場合に、変更内容をツリーに反映する。
			 */
			onAdd : function(treeModel) {

				// 継承元の追加処理を実行して画面に反映する。
				wgp.TreeView.prototype.onAdd.call(this, treeModel);

				var treeType = treeModel.get("type");

				// シグナルの場合はリアルタイム更新開始処理を行う。
				if(ENS.tree.type.SIGNAL == treeType){
					appView.syncData([ treeModel.get("id") ]);
				}

			},
			/**
			 * ツリー要素の基となるコレクションが変更された場合に、 変更内容をツリーに反映する。
			 */
			onChange : function(treeModel) {

				// 継承元の変更処理を実行して画面に反映する。
				wgp.TreeView.prototype.onChange.call(this, treeModel);

				var treeId = treeModel.get("id");
				var treeType = treeModel.get("type");

				var previousTreeId = treeModel.previousAttributes()["id"];
				var treeTag = this.getTreeNode(previousTreeId, "id");
				treeTag.attr("id", treeId);

				// シグナルの場合は状態を変更する。
				if (ENS.tree.type.SIGNAL == treeType) {
					var treeIcon = treeModel.get("icon");
					var iconTag = treeTag.find("ins");

					// 状態を一度クリアする。
					iconTag.removeClass(ENS.tree.SIGNAL_ICON_0);
					iconTag.removeClass(ENS.tree.SIGNAL_ICON_1);
					iconTag.removeClass(ENS.tree.SIGNAL_ICON_2);
					iconTag.removeClass(ENS.tree.SIGNAL_ICON_3);
					iconTag.removeClass(ENS.tree.SIGNAL_ICON_4);
					iconTag.removeClass(ENS.tree.SIGNAL_ICON_5);

					// 状態を再度設定する。
					iconTag.addClass(treeIcon);

					// 前回のシグナルIDによるリアルタイム更新を停止
					// 新しいシグナルIDによるリアルタイム更新を開始
					appView.stopSyncData([ previousTreeId ]);
					appView.syncData([ treeId ]);
				}

			},
			/**
			 * ツリー要素の基となるコレクションの要素が削除された場合に、削除内容をツリーに反映する。
			 */
			onRemove : function(treeModel) {

				// 継承元の削除処理を実行して画面に反映する。
				wgp.TreeView.prototype.onRemove.call(this, treeModel);

				var treeType = treeModel.get("type");

				// シグナルの場合はリアルタイム更新開始処理を行う。
				if(ENS.tree.type.SIGNAL == treeType){
					appView.stopSyncData([ treeModel.get("id") ]);
				}

			},
			// シグナル名を基にシグナルの親階層のツリーIDを取得する。
			getSignalParentTreeId_ : function(signalName) {
				var lastIndexOf = signalName.lastIndexOf("/");
				return signalName.substr(0, lastIndexOf);
			},
			// シグナル名を基にシグナルの表示名称を取得する。
			getSignalDisplayName_ : function(signalName) {
				var lastIndexOf = signalName.lastIndexOf("/");
				return signalName.substr(lastIndexOf + 1);
			},
			onComplete : function(type) {
				if (type == wgp.constants.syncType.SEARCH) {
					this.renderAll();
				}
			}
		});