package org.insightech.er.editor.model;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Locale;

import org.eclipse.draw2d.geometry.Point;
import org.insightech.er.editor.ERDiagramMultiPageEditor;
import org.insightech.er.editor.EROneDiagramEditor;
import org.insightech.er.editor.model.diagram_contents.DiagramContents;
import org.insightech.er.editor.model.diagram_contents.element.node.Location;
import org.insightech.er.editor.model.diagram_contents.element.node.NodeElement;
import org.insightech.er.editor.model.diagram_contents.element.node.NodeSet;
import org.insightech.er.editor.model.diagram_contents.element.node.category.Category;
import org.insightech.er.editor.model.diagram_contents.element.node.ermodel.ERModel;
import org.insightech.er.editor.model.diagram_contents.element.node.ermodel.ERModelSet;
import org.insightech.er.editor.model.diagram_contents.element.node.ermodel.VGroup;
import org.insightech.er.editor.model.diagram_contents.element.node.note.Note;
import org.insightech.er.editor.model.diagram_contents.element.node.table.ERTable;
import org.insightech.er.editor.model.diagram_contents.element.node.table.ERVirtualTable;
import org.insightech.er.editor.model.diagram_contents.element.node.table.TableView;
import org.insightech.er.editor.model.diagram_contents.element.node.table.column.NormalColumn;
import org.insightech.er.editor.model.diagram_contents.not_element.group.GlobalGroupSet;
import org.insightech.er.editor.model.settings.DBSetting;
import org.insightech.er.editor.model.settings.PageSetting;
import org.insightech.er.editor.model.settings.Settings;
import org.insightech.er.editor.model.tracking.ChangeTrackingList;

public class ERDiagram extends ViewableModel {

	private static final long serialVersionUID = 8729319470770699498L;

	public static final String PROPERTY_CHANGE_ALL = "all";

	public static final String PROPERTY_CHANGE_DATABASE = "database";

	public static final String PROPERTY_CHANGE_SETTINGS = "settings";

	public static final String PROPERTY_CHANGE_ADD = "add";

	public static final String PROPERTY_CHANGE_ERMODEL = "ermodel";

	public static final String PROPERTY_CHANGE_TABLE = "table";

	private ChangeTrackingList changeTrackingList;

	private DiagramContents diagramContents;

	private ERDiagramMultiPageEditor editor;

	private int[] defaultColor;

	private boolean tooltip;
	private boolean showMainColumn;

	private boolean disableSelectColumn;

	private Category currentCategory;
	private int currentCategoryIndex;

	private ERModel currentErmodel;

	private double zoom = 1.0d;

	private int x;

	private int y;

	private DBSetting dbSetting;

	private PageSetting pageSetting;

	public Point mousePoint = new Point();

	private String defaultModelName;

	public ERDiagram(String database) {
		this.diagramContents = new DiagramContents();
		this.diagramContents.getSettings().setDatabase(database);
		this.pageSetting = new PageSetting();

		this.setDefaultColor(128, 128, 192);
		this.setColor(255, 255, 255);
	}

	public void init() {
		this.diagramContents.setColumnGroups(GlobalGroupSet.load());

		Settings settings = this.getDiagramContents().getSettings();

		if (Locale.JAPANESE.getLanguage().equals(
				Locale.getDefault().getLanguage())) {
			settings.getTranslationSetting().setUse(true);
			settings.getTranslationSetting().selectDefault();
		}

		settings.getModelProperties().init();
	}

	public void addNewContent(NodeElement element) {
		element.setColor(this.defaultColor[0], this.defaultColor[1],
				this.defaultColor[2]);
		element.setFontName(this.getFontName());
		element.setFontSize(this.getFontSize());

		this.addContent(element);
	}

	public void addContent(NodeElement element) {
		element.setDiagram(this);

		this.diagramContents.getContents().addNodeElement(element);

		if (this.editor != null) {
			Category category = this.editor.getCurrentPageCategory();
			if (category != null) {
				category.getContents().add(element);
			}
		}

		if (element instanceof TableView) {
			for (NormalColumn normalColumn : ((TableView) element)
					.getNormalColumns()) {
				this.getDiagramContents().getDictionary().add(normalColumn);
			}

		}

		if (element instanceof ERTable) {
			ERTable table = (ERTable) element;
			if (getCurrentErmodel() != null) {
				// �r���[��ɉ��z�e�[�u����ǉ�����

				ERModel model = getCurrentErmodel();
				ERVirtualTable virtualTable = new ERVirtualTable(model, table);
				virtualTable.setPoint(element.getX(), element.getY());

				// TODO ���C���r���[��ł͍���ɔz�u
				element.setLocation(new Location(0, 0, element.getWidth(), element.getHeight()));

				model.addTable(virtualTable);
			}
		}

		if (element instanceof ERVirtualTable) {
			ERVirtualTable virtualTable = (ERVirtualTable) element;
			if (getCurrentErmodel() != null) {
				// �r���[��ɉ��z�e�[�u����ǉ�����

				ERModel model = getCurrentErmodel();
//				ERVirtualTable virtualTable = new ERVirtualTable(model, table);
				virtualTable.setPoint(element.getX(), element.getY());

				// TODO ���C���r���[��ł͍���ɔz�u
				element.setLocation(new Location(0, 0, element.getWidth(), element.getHeight()));

				model.addTable(virtualTable);
			}
		}

		this.firePropertyChange(NodeSet.PROPERTY_CHANGE_CONTENTS, null, null);
	}

	public void removeContent(NodeElement element) {
		if (element instanceof ERVirtualTable) {
			// ���C���r���[�̃m�[�h�͎c���ĉ��z�e�[�u�������폜
			currentErmodel.remove((ERVirtualTable)element);
		} else if (element instanceof VGroup) {
			currentErmodel.remove((VGroup)element);
		} else if (element instanceof Note) {
			currentErmodel.remove((Note)element);
		} else {
			this.diagramContents.getContents().remove(element);
			if (element instanceof ERTable) {
				// ���C���r���[�̃e�[�u�����폜�����Ƃ��́A�r���[�̃m�[�h���폜�iTODO �����������Ɏc���Ă��܂��j
				for (ERModel model : getDiagramContents().getModelSet()) {
					ERVirtualTable vtable = model.findVirtualTable((TableView) element);
					model.remove(vtable);
				}
			}
		}

		if (element instanceof TableView) {
			this.diagramContents.getDictionary().remove((TableView) element);
		}

		for (Category category : this.diagramContents.getSettings()
				.getCategorySetting().getAllCategories()) {
			category.getContents().remove(element);
		}

		this.firePropertyChange(NodeSet.PROPERTY_CHANGE_CONTENTS, null, null);
	}

	public void replaceContents(DiagramContents newDiagramContents) {
		this.diagramContents = newDiagramContents;
		this.firePropertyChange(NodeSet.PROPERTY_CHANGE_CONTENTS, null, null);
	}

	public void addErmodel(ERModel ermodel) {
		diagramContents.getModelSet().add(ermodel);
		firePropertyChange(PROPERTY_CHANGE_ADD, null, ermodel);
	}

	public void changeAll() {
		this.firePropertyChange(PROPERTY_CHANGE_ALL, null, null);
	}

	public void changeAll(List<NodeElement> nodeElementList) {
		this.firePropertyChange(PROPERTY_CHANGE_ALL, null, nodeElementList);
	}

	public void changeTable(TableView tableView) {
		this.firePropertyChange(PROPERTY_CHANGE_TABLE, null, tableView);
	}

	public void setDatabase(String str) {
		String oldDatabase = getDatabase();

		this.getDiagramContents().getSettings().setDatabase(str);

		if (str != null && !str.equals(oldDatabase)) {
			this.firePropertyChange(PROPERTY_CHANGE_DATABASE, oldDatabase,
					getDatabase());
			this.changeAll();
		}
	}

	public String getDatabase() {
		return this.getDiagramContents().getSettings().getDatabase();
	}

	public void restoreDatabase(String str) {
		this.getDiagramContents().getSettings().setDatabase(str);
	}

	public void setSettings(Settings settings) {
		this.getDiagramContents().setSettings(settings);
		this.editor.initCategoryPages();

		this.firePropertyChange(PROPERTY_CHANGE_SETTINGS, null, null);
		this.firePropertyChange(NodeSet.PROPERTY_CHANGE_CONTENTS, null, null);
	}

	public void setCurrentCategoryPageName() {
		this.editor.setCurrentCategoryPageName();
	}

	public void addCategory(Category category) {
		category.setColor(this.defaultColor[0], this.defaultColor[1],
				this.defaultColor[2]);
		this.getDiagramContents().getSettings().getCategorySetting()
				.addCategoryAsSelected(category);
		this.editor.initCategoryPages();
		this.firePropertyChange(NodeSet.PROPERTY_CHANGE_CONTENTS, null, null);
	}

	public void removeCategory(Category category) {
		this.getDiagramContents().getSettings().getCategorySetting()
				.removeCategory(category);
		this.editor.initCategoryPages();
		this.firePropertyChange(NodeSet.PROPERTY_CHANGE_CONTENTS, null, null);
	}

	public void restoreCategories() {
		this.editor.initCategoryPages();
		this.firePropertyChange(NodeSet.PROPERTY_CHANGE_CONTENTS, null, null);
	}

	public void change() {
		this.firePropertyChange(PROPERTY_CHANGE_SETTINGS, null, null);
	}

	public ChangeTrackingList getChangeTrackingList() {
		if (this.changeTrackingList == null) {
			this.changeTrackingList = new ChangeTrackingList();
		}
		return changeTrackingList;
	}

	public DiagramContents getDiagramContents() {
		return this.diagramContents;
	}

	public void setEditor(ERDiagramMultiPageEditor editor) {
		this.editor = editor;
	}

	public int[] getDefaultColor() {
		return defaultColor;
	}

	public void setDefaultColor(int red, int green, int blue) {
		this.defaultColor = new int[3];
		this.defaultColor[0] = red;
		this.defaultColor[1] = green;
		this.defaultColor[2] = blue;
	}

	public void setCurrentCategory(Category currentCategory,
			int currentCategoryIndex) {
		this.currentCategory = currentCategory;
		this.currentCategoryIndex = currentCategoryIndex;
		this.changeAll();
	}

	public Category getCurrentCategory() {
		return currentCategory;
	}

	public int getCurrentCategoryIndex() {
		return currentCategoryIndex;
	}

	public boolean isTooltip() {
		return tooltip;
	}

	public void setTooltip(boolean tooltip) {
		this.tooltip = tooltip;
	}

	/**
	 * currentErmodel���擾���܂��B
	 * @return currentErmodel
	 */
	public ERModel getCurrentErmodel() {
	    return currentErmodel;
	}

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	/**
	 * dbSetting ���擾���܂�.
	 *
	 * @return dbSetting
	 */
	public DBSetting getDbSetting() {
		return dbSetting;
	}

	/**
	 * dbSetting ��ݒ肵�܂�.
	 *
	 * @param dbSetting
	 *            dbSetting
	 */
	public void setDbSetting(DBSetting dbSetting) {
		this.dbSetting = dbSetting;
	}

	/**
	 * pageSetting ���擾���܂�.
	 *
	 * @return pageSetting
	 */
	public PageSetting getPageSetting() {
		return pageSetting;
	}

	/**
	 * pageSetting ��ݒ肵�܂�.
	 *
	 * @param pageSetting
	 *            pageSetting
	 */
	public void setPageSetting(PageSetting pageSetting) {
		this.pageSetting = pageSetting;
	}

	/**
	 * editor ���擾���܂�.
	 *
	 * @return editor
	 */
	public ERDiagramMultiPageEditor getEditor() {
		return editor;
	}

	public String filter(String str) {
		if (str == null) {
			return str;
		}

		Settings settings = this.getDiagramContents().getSettings();

		if (settings.isCapital()) {
			return str.toUpperCase();
		}

		return str;
	}

	/**
	 * showMainColumn��ݒ肵�܂��B
	 * @param showMainColumn showMainColumn
	 */
	public void setShowMainColumn(boolean showMainColumn) {
	    this.showMainColumn = showMainColumn;
	}

	/**
	 * showMainColumn���擾���܂��B
	 * @return showMainColumn
	 */
	public boolean isShowMainColumn() {
	    return showMainColumn;
	}

	/**
	 * disableSelectColumn ���擾���܂�.
	 *
	 * @return disableSelectColumn
	 */
	public boolean isDisableSelectColumn() {
		return disableSelectColumn;
	}

	/**
	 * disableSelectColumn ��ݒ肵�܂�.
	 *
	 * @param disableSelectColumn
	 *            disableSelectColumn
	 */
	public void setDisableSelectColumn(boolean disableSelectColumn) {
		this.disableSelectColumn = disableSelectColumn;
	}

	public void setCurrentErmodel(ERModel model, String defaultModelName) {
		this.currentErmodel = model;
		this.defaultModelName = defaultModelName;
		if (model != null) {
			model.changeAll();
		}
	}

	/**
	 * defaultModelName���擾���܂��B
	 * @return defaultModelName
	 */
	public String getDefaultModelName() {
	    return defaultModelName;
	}

	/**
	 * ���C���r���[�Ńe�[�u�����X�V�����Ƃ��ɌĂ΂�܂��B
	 * �T�u�r���[�̃e�[�u�����X�V���܂��B
	 * @param newCopyTableView
	 */
	public void doChangeTable(TableView table) {
		for (ERModel model : getDiagramContents().getModelSet()) {
			ERVirtualTable vtable = model.findVirtualTable(table);
			if (vtable != null) {
				vtable.doChangeTable();
			}
		}
	}

	public ERModel findModelByTable(ERTable table) {
		for (ERModel model : diagramContents.getModelSet()) {
			for (ERVirtualTable vtable : model.getTables()) {
				if (vtable.getRawTable().equals(table)) {
					return model;
				}
			}
		}
		return null;
	}

//	/**
//	 * �S�̃r���[�������͒ʏ�r���[�ōX�V���ꂽ���e���A�S�Ẵr���[�ɓW�J���܂��B
//	 * @param event ���������C�x���g
//	 * @param nodeElement �X�V�������f��
//	 */
//	public void refreshAllModel(PropertyChangeEvent event, NodeElement nodeElement) {
//		if (nodeElement instanceof ERVirtualTable) {
//			ERTable table = ((ERVirtualTable)nodeElement).getRawTable();
//			// ���C���r���[�ɓW�J
//			table.getDiagram().doChangeTable(table);
//			// �S�r���[�ɓW�J
//			for (ERModel model : getDiagramContents().getModelSet()) {
//				ERVirtualTable vtable = model.findVirtualTable(table);
//				if (vtable != null) {
//					vtable.doChangeTable();
////					vtable.firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
//				}
//			}
//		} else if (nodeElement instanceof ERTable) {
//			ERTable table = (ERTable)nodeElement;
//			// ���C���r���[�ɓW�J
//			table.getDiagram().doChangeTable(table);
//			// �S�r���[�ɓW�J
//			for (ERModel model : getDiagramContents().getModelSet()) {
//				ERVirtualTable vtable = model.findVirtualTable(table);
//				if (vtable != null) {
//					vtable.doChangeTable();
////					vtable.firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
//				}
//			}
//		}
//
//	}


}
