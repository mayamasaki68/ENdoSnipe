ENS.ResourceMapView = wgp.MapView.extend({
	tagName : "div",
	initialize : function(argument) {
		_.bindAll();

		var width = $("#" + this.$el.attr("id")).width();
		var height = $("#" + this.$el.attr("id")).height();
		_.extend(argument, {width : width, height : height});

		var ajaxHandler = new wgp.AjaxHandler();
		this.ajaxHandler = ajaxHandler;

		var contextMenuId = this.cid + "_contextMenu";
		this.contextMenuId = contextMenuId;

		this.mapId = argument["mapId"];

		// 継承元の初期化メソッド実行
		this.__proto__.__proto__.initialize.apply(this, [argument]);

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
		var newView;
		if("ENS.ResourceGraphElementView" == objectName){
			newView = this._addGraphDivision(model);
			this.viewCollection[model.id] = newView;

		}else if("ENS.SignalElementView" == objectName){
			newView = this._addStateElement(model);
			this.viewCollection[model.id] = newView;

		}else if("ENS.ResourceLinkElementView" == objectName){
			newView = this._addLinkElement(model);
			this.viewCollection[model.id] = newView;

		}else{

			// 継承元の追加メソッドを実行する。
			this.__proto__.__proto__.onAdd(this, [model]);
		}

		// 編集モードの場合に各種イベントを設定する。
		var mapMode = $("#mapMode").val();
		if(mapMode == ENS.map.mode.EDIT){

			// ビューの編集イベントを設定する。
			if(newView){
				newView.setEditFunction();
			}

			// コンテキストメニューとの関連付け
			this.relateContextMenu(model);

		}else{

			// 運用時のイベントを設定する。
			newView.setOperateFunction();
		}

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
			new ENS.SignalElementView(argument);
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
	onSave : function(treeModel){
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

		// 運用モードの場合はシグナル・リンクがグラフの前に来るように調整
		if($("#mapMode").val() == ENS.map.mode.OPERATE){
			$(this.paper.canvas).css("z-index", 1);
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
							fontSize : ENS.map.fontSize,
							fill : ENS.map.fontColor,
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