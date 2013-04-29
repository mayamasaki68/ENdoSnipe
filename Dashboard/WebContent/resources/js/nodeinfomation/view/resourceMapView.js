ENS.ResourceMapView = wgp.MapView.extend({
	tagName : "div",
	initialize : function(argument) {
		_.bindAll();

		var width = $("#" + this.$el.attr("id")).width();
		var height = $("#" + this.$el.attr("id")).height();
		_.extend(argument, {width : width, height : height});

		var ajaxHandler = new wgp.AjaxHandler();
		this.ajaxHandler = ajaxHandler;

		var contextMenuId = "ResourceMapMenu";
		this.contextMenuId = contextMenuId;
		
		this.mapId = argument["mapId"];
		
		// 継承元の初期化メソッド実行
		this.__proto__.__proto__.initialize.apply(this, [argument]);

		// グラフリソースリンクがドロップされた際にグラフを描画する。
		var instance = this;
		$("#" + this.$el.attr("id")).droppable({
			accept: function(element){
				if(element.find("A").length > 0){
					return true;
				}else{
					return false;
				}
			},
			tolerance : "fit",
			drop : function(event, ui){

				// ドロップ時の位置を特定しておく。
				var perspectiveModel =
					perspectiveView.findPerspectiveFromId(instance.$el.attr("id"));
				var drop_area_id = perspectiveModel.get("drop_area_id");
				var dropAreaOffset = $("#" + drop_area_id).offset();

				var resourceId = ui.helper.find("A").attr("id");
				var treeModel = resourceTreeView.collection.get(resourceId);

				var resourceModel = new wgp.MapElement();
				resourceModel.set({
					objectId : resourceId,
					objectName : "ENS.ResourceGraphElementView",
					pointX : event.clientX,
					pointY : event.clientY,
					width : 300,
					height : 300,
					zIndex : 1
				});
				instance.collection.add(resourceModel);

				// TODO シグナルは別の方法で表示する。
//				var stateModel = new ENS.resourceStateElementModel();
//				stateModel.set({
//					objectId : resourceId + "_State",
//					objectName : "ENS.ResourceStateElementView",
//					pointX : event.clientX - 190,
//					pointY : event.clientY - 140,
//					width : 50,
//					height : 50,
//					zIndex : 1,
//					stateId : "normal",
//					linkId : "group1"
//				});
//				instance.collection.add(stateModel);
			}
		
		});
		
		// 本クラスのrenderメソッド実行
		this.renderExtend();
	},
	renderExtend : function(){

		// コンテキストメニュー用のタグを生成
		this.createContextMenuTag();

		// マップ情報を読み込む。
		this.onLoad();
		return this;
	},
	destroy : function (){
		this.$el.children().remove();
	},
	// 要素が追加された際に、自身の領域に要素のタイプに応じて描画要素を追加する。
	onAdd : function(model){
		var objectName = model.get("objectName");

		// リソースグラフの場合はグラフを描画する。
		if("ENS.ResourceGraphElementView" == objectName){
			var graphView = this._addGraphDivision(model);
			this.viewCollection[model.id] = graphView;

		}else if("ENS.ResourceStateElementView" == objectName){
			var stateView = this._addStateElement(model);
			this.viewCollection[model.id] = stateView;

		}else if("ENS.ResourceLinkElementView" == objectName){
			var linkView = this._addLinkElement(model);
			this.viewCollection[model.id] = linkView;

		}else{

			// 継承元の追加メソッドを実行する。
			this.__proto__.__proto__.onAdd(this, [model]);
		}

		// コンテキストメニューとの関連付けを行う。
		this.relateContextMenu(model);
	},
	_addGraphDivision : function(model) {
		var graphId = model.get("objectId");
		var width = model.get("width");
		var height = model.get("height");

		var tempId = graphId.split("/");
		var dataId = tempId[tempId.length - 1];
		var treeSettings = {
			data : dataId,
			id : graphId,
			measurementUnit : "",
			parentTreeId : "",
			treeId : ""
		};
		var viewAttribute = {
			id : graphId,
			cid : model.cid,
			rootView : this,
			graphId : graphId,
			title : dataId,
			noTermData : false,
			term : 1800 * 2,
			width : width,
			height : height,
			dateWindow : [ new Date() - 60 * 60 * 1000, new Date() ],
			attributes : {
				xlabel : "Time",
				ylabel : "value",
				labels : [ "time", "PC1", "PC2" ]
			}
		};

		var newDivAreaId = this.$el.attr("id") + "_" + model.cid;
		var newDivArea = $("<div id='" + newDivAreaId + "'></div>");
		newDivArea.css("position","absolute");

		$("#" + this.$el.attr("id")).append(newDivArea);
		newDivArea.width(model.get("width"));
		newDivArea.height(model.get("height"));

		$.extend(true, viewAttribute, {
			id : newDivAreaId
		});
		var view =
			new ENS.ResourceGraphElementView(viewAttribute, treeSettings);
		view.model = model;

		// cidを設定
		$("#" + view.$el.attr("id")).attr("cid", model.cid);

		// 後で移動する。
		newDivArea.offset({
			top : model.get("pointY"),
			left : model.get("pointX")
		});

		return view;
	},
	_addStateElement : function(model){
		var argument = {
			paper : this.paper,
			model : model
		};

		var view =
			new ENS.ResourceStateElementView(argument);
		return view;
	},
	_addLinkElement : function(model){
		var argument = {
			paper : this.paper,
			model : model
		};

		var view =
			new ENS.ResourceLinkElementView(argument);
		return view;
	},
//	onCreate : function(){
//		var instance = this;
//		console.log("click create");
//
//		var createMapDialog = $("<div title='Create new Map'></div>");
//		createMapDialog.append("<p> Please enter new Map name</p>");
//
//		var mapNameLabel = $("<label for='mapName'>Map Name</label>");
//		var mapNameText = 
//			$("<input type='text' name='mapName' id='mapName' class='text ui-widget-content ui-corner-all'>");
//		createMapDialog.append(mapNameLabel);
//		createMapDialog.append(mapNameText);
//
//		createMapDialog.dialog({
//			autoOpen: false,
//			height: 300,
//			width: 350,
//			modal: true,
//			buttons : {
//				"OK" : function(){
//
//					if(mapNameText.val().length == 0){
//						alert("Map Name is require");
//						return;
//					}
//					
//					var setting = {
//						data : {
//							name : mapNameText.val(),
//							data : "{}"
//						},
//						url : wgp.common.getContextPath() + "/map/insert"
//					}
//					instance.ajaxHandler.requestServerSync(setting);
//					createMapDialog.dialog("close");
//
//					// マップ一覧を再描画する。
//					resourceMapListView.onLoad();
//				},
//				"CANCEL" : function(){
//					createMapDialog.dialog("close");
//				}
//			},
//			close : function(event){
//				createMapDialog.remove();
//			}
//		});
//
//		createMapDialog.dialog("open");
//	},
	onSave : function(treeModel){
		console.log("click save");
		var resourceArray = [];
		_.each(this.collection.models, function(model, index){
			resourceArray.push(model.toJSON());
		});

		var resourceMap = {
			resources : resourceArray
		}

		var setting = {
			data : {
				mapId : treeModel.get("id"),
				name : treeModel.get("data"),
				data : JSON.stringify(resourceMap)
			},
			url : wgp.common.getContextPath() + "/map/update"
		}
		this.ajaxHandler.requestServerSync(setting);
	},
	onLoad : function(){

		// コレクションをリセット
		this.collection.reset();
		
		// マップ情報を画面に表示する。
		var mapId = this.mapId;

		if(mapId != undefined && mapId.length > 0){

			// サーバからマップ情報を取得し、結果をコレクションに追加する。
			var mapData = this.getMapData(mapId);
			var resources = mapData["resources"];
			var instance = this;
			_.each(resources, function(resource, index){
				var mapElement = new wgp.MapElement(resource);
				instance.collection.add(mapElement);
			});
		}

	},
	getMapData : function(mapId){
		var setting = {
			data : {
				mapId : mapId
			},
			url : wgp.common.getContextPath() + "/map/getById"
		}
		var result = this.ajaxHandler.requestServerSync(setting);
		var mapInfo = $.parseJSON(result);
		return $.parseJSON(mapInfo["mapData"]);
	},
	// raphaelPaperをクリックした際に追加するイベントを付加する。
	setClickAddEvent : function(viewClassName){
		$(this.paper.canvas).unbind("click");

		var instance = this;
		var linkName = "";
		var linkUrl = "";

		var clickEventFunction = function(event){

			var resourceModel = new wgp.MapElement();

			var createLinkDialog = $("<div title='Create new Link'></div>");
			createLinkDialog.append("<p> Please enter new Link name</p>");

			var linkNameLabel = $("<label for='linkName'>Link Name：</label>");
			var linkNameText = 
				$("<input type='text' name='linkName' id='linkName' class='text ui-widget-content ui-corner-all'>");

			var linkUrlLabel = $("<label for='linkUrl' style='margin-top: 10px;'>Target Map：</label>");
			var linkUrlSelect = $("<select name='selectMapList' class='ui-widget-content ui-corner-all' style='margin-top: 10px;'></select>")
			var mapList = resourceMapListView.collection.models;
			_.each(mapList, function(mapListModel, index){
				var linkUrlName = mapListModel.get("data");
				var linkUrlValue = mapListModel.get("id");
				linkUrlSelect.append("<option value='"+ linkUrlValue +"'>" + linkUrlName + "</option>");
			});

			createLinkDialog.append(linkNameLabel);
			createLinkDialog.append(linkNameText);
			createLinkDialog.append("</br>")
			createLinkDialog.append(linkUrlLabel);
			createLinkDialog.append(linkUrlSelect);
			createLinkDialog.dialog({
				autoOpen: false,
				height: 500,
				width: 400,
				modal: true,
				buttons : {
					"OK" : function(){

						if(linkNameText.val().length == 0){
							alert("link Name is require");
							return;
						}
						linkName = linkNameText.val();
						linkUrl = linkUrlSelect.val();
						createLinkDialog.dialog("close");

						// resourceIdの算出
						var resourceId = 1;
						var sameObjectNameArray = instance.collection.where({objectName : viewClassName});
						_.each(sameObjectNameArray, function(sameObject, index){
							if(resourceId <= sameObject.get("objectId")){
								resourceId = sameObject.get("objectId") + 1;
							}
						});

						var width = 100;
						var height = 100;
						resourceModel.set({
							objectId : resourceId,
							objectName : viewClassName,
							text : linkName,
							linkUrl : linkUrl,
							linkType : "mapLinkURL",
							pointX : event.pageX - $("#" + instance.$el.attr("id")).offset()["left"],
							pointY : event.pageY - $("#" + instance.$el.attr("id")).offset()["top"],
							width : 100,
							height : 100,
							fill : "#FFFFFF",
							zIndex : 1
						});
						instance.collection.add(resourceModel);

					},
					"CANCEL" : function(){
						createLinkDialog.dialog("close");
					}
				},
				close : function(event){
					createLinkDialog.remove();
					$(instance.paper.canvas).unbind("click", clickEventFunction);
					$(".map_menu_icon-active").removeClass("map_menu_icon-active");
				}
			});

			createLinkDialog.dialog("open");
		};

		$(this.paper.canvas).click(clickEventFunction);
	},
	createContextMenuTag : function(){

		// メニューの定義がない場合にのみメニュー用のタグを作成する。
		if($("#" + this.contextMenuId).length == 0){

			var contextMenu0 = new contextMenu("Remove", "Remove");
			var contextMenuArray = [ contextMenu0 ];
			contextMenuCreator.initializeContextMenu(this.contextMenuId, contextMenuArray);
		}
	},
	relateContextMenu : function(model){
		var instance = this;
		var option = {
			onShow: function(event, target){
			},
			onSelect : function(event, target){
				var cid = $(target).attr("cid");
				if(event.currentTarget.id == "Remove"){
					var model = instance.collection._byCid[cid];
					instance.collection.remove(model);
				}
			}
		};

		var resourceView = this.viewCollection[model.id];
		resourceView.relateContextMenu(this.contextMenuId, option);
	}
});