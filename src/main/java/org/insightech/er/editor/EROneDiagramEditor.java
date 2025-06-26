package org.insightech.er.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.insightech.er.editor.controller.editpart.element.ERDiagramEditPartFactory;
import org.insightech.er.editor.controller.editpart.element.node.ERModelEditPart;
import org.insightech.er.editor.controller.editpart.element.node.ERTableEditPart;
import org.insightech.er.editor.controller.editpart.element.node.ERVirtualTableEditPart;
import org.insightech.er.editor.controller.editpart.element.node.VGroupEditPart;
import org.insightech.er.editor.model.ERDiagram;
import org.insightech.er.editor.model.diagram_contents.element.node.ermodel.ERModel;
import org.insightech.er.editor.model.diagram_contents.element.node.table.ERTable;
import org.insightech.er.editor.model.diagram_contents.element.node.table.ERVirtualTable;
import org.insightech.er.editor.view.ERDiagramGotoMarker;
import org.insightech.er.editor.view.ERDiagramOnePopupMenuManager;
import org.insightech.er.editor.view.action.ermodel.PlaceTableAction;
import org.insightech.er.editor.view.action.ermodel.VGroupManageAction;
import org.insightech.er.editor.view.outline.ERDiagramOutlinePage;
import org.insightech.er.editor.view.outline.ERDiagramOutlinePopupMenuManager;

/**
 * TODO ON UPDATE�AON DELETE �̃v���_�E����ݒ�ł�����̂����ɐ�������<br>
 * TODO �f�t�H���g�l�Ɍ^�̐�����K�p����<br>
 *
 */
public class EROneDiagramEditor extends ERDiagramEditor {

	private ERModel model;

	public EROneDiagramEditor(ERDiagram diagram, ERModel model,
			ERDiagramEditPartFactory editPartFactory,
			ZoomComboContributionItem zoomComboContributionItem,
			ERDiagramOutlinePage outlinePage) {
		super(diagram, editPartFactory, zoomComboContributionItem, outlinePage);
		this.model = model;
	}

	public DefaultEditDomain getDefaultEditDomain() {
		return getEditDomain();
	}

	public ActionRegistry getDefaultActionRegistry() {
		return getActionRegistry();
	}

	@Override
	protected void createActions() {
		super.createActions();

		ActionRegistry registry = this.getActionRegistry();
//		List<String> selectionActionList = this.getSelectionActions();

		List<IAction> actionList = new ArrayList<IAction>(Arrays
				.asList(new IAction[] {
						new PlaceTableAction(this),
						new VGroupManageAction(this),
				}));

		for (IAction action : actionList) {
			registry.registerAction(action);
		}
	}

	@Override
	protected void initializeGraphicalViewer() {
		GraphicalViewer viewer = this.getGraphicalViewer();
		viewer.setEditPartFactory(editPartFactory);

		this.initViewerAction(viewer);
		this.initDragAndDrop(viewer);

		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1),
				MouseWheelZoomHandler.SINGLETON);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, true);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, true);
		viewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, true);

		MenuManager menuMgr = new ERDiagramOnePopupMenuManager(this.getActionRegistry(), this.model);

		this.extensionLoader.addERDiagramPopupMenu(menuMgr, this.getActionRegistry());

		viewer.setContextMenu(menuMgr);

		viewer.setContents(model);
//		viewer.getRootEditPart().setContents(editPartFactory.);

		this.outlineMenuMgr = new ERDiagramOutlinePopupMenuManager(
				this.diagram, this.getActionRegistry(),
				this.outlinePage.getOutlineActionRegistory(), this.outlinePage.getViewer());

		this.gotoMaker = new ERDiagramGotoMarker(this);
	}

	/**
	 * model���擾���܂��B
	 * @return model
	 */
	public ERModel getModel() {
	    return model;
	}

	public void setContents(ERModel newModel) {
		model = newModel;
		getGraphicalViewer().setContents(newModel);
		newModel.changeAll();
	}

	public void refresh() {
		model.changeAll();
	}

	public void reveal(ERTable table) {
		ERModelEditPart editPart = (ERModelEditPart) getGraphicalViewer().getContents();
		List tableParts = editPart.getChildren();

		for (Object tableEditPart : tableParts) {
			if (tableEditPart instanceof ERVirtualTableEditPart) {
				ERVirtualTableEditPart vtableEditPart = (ERVirtualTableEditPart) tableEditPart;
				if (((ERVirtualTable) vtableEditPart.getModel()).getRawTable().equals(table)) {
					getGraphicalViewer().reveal(vtableEditPart);
					return;
				}
			}
			if (tableEditPart instanceof VGroupEditPart) {
				// do nothing
//				VGroupEditPart groupEditPart = (VGroupEditPart) tableEditPart;
//				List children = groupEditPart.getChildren();
//				System.out.println(children);
			}
		}
	}


}
