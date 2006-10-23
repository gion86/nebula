package org.eclipse.swt.nebula.widgets.ctabletree.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.nebula.widgets.ctabletree.CTableTree;
import org.eclipse.swt.nebula.widgets.ctabletree.CTableTreeCell;
import org.eclipse.swt.nebula.widgets.ctabletree.CTableTreeItem;
import org.eclipse.swt.nebula.widgets.ctabletree.ccontainer.CContainerColumn;
import org.eclipse.swt.widgets.Control;

/**
 * A CTableTreeEditor is a manager for a Control that appears above a cell in a CTableTree and tracks with the
 * moving and resizing of that cell.  It can be used to display a text widget above a cell
 * in a CTableTree so that the user can edit the contents of that cell.  It can also be used to display
 * a button that can launch a dialog for modifying the contents of the associated cell.
 */
public class CTableTreeEditor extends ControlEditor {
	CTableTree tree;
	CTableTreeItem item;
	int column = 0;
	ControlListener columnListener;
	TreeListener treeListener;

	/**
	 * Creates a CTableTreeEditor for the specified CTableTree.
	 *
	 * @param tree the CTableTree Control above which this editor will be displayed
	 *
	 */
	public CTableTreeEditor (CTableTree tree) {
		super(tree);
		this.tree = tree;

		columnListener = new ControlListener() {
			public void controlMoved(ControlEvent e){
				_resize();
			}
			public void controlResized(ControlEvent e){
				_resize();
			}
		};
		treeListener = new TreeListener () {
			final Runnable runnable = new Runnable() {
				public void run() {
					Control editor = getEditor();
					if (editor == null || editor.isDisposed()) return;
					if (CTableTreeEditor.this.tree.isDisposed()) return;
					_resize();
					editor.setVisible(true);
				}
			};
			public void treeCollapsed(TreeEvent e) {
				Control editor = getEditor();
				if (editor == null || editor.isDisposed ()) return;
				editor.setVisible(false);
				e.display.asyncExec(runnable);
			}
			public void treeExpanded(TreeEvent e) {
				Control editor = getEditor();
				if (editor == null || editor.isDisposed ()) return;
				editor.setVisible(false);
				e.display.asyncExec(runnable);
			}
		};
		tree.addTreeListener(treeListener);

		// To be consistent with older versions of SWT, grabVertical defaults to true
		grabVertical = true;
	}

	Rectangle computeBounds () {
		if (item == null || column == -1 || item.isDisposed()) return new Rectangle(0, 0, 0, 0);
		Rectangle cell = item.getCell(column).getBounds();
		cell.x += item.getCell(column).getTitleClientArea().x;
		cell.x -= getEditor().getBorderWidth();
		Image img = ((CTableTreeCell) item.getCell(column)).getImage();
		Rectangle rect = img == null ? new Rectangle(cell.x,cell.y,0,0) : img.getBounds();
		cell.x = rect.x + rect.width;
		cell.width -= rect.width;
		Rectangle area = tree.getClientArea();
		if (cell.x < area.x + area.width) {
			if (cell.x + cell.width > area.x + area.width) {
				cell.width = area.x + area.width - cell.x;
			}
		}
		Rectangle editorRect = new Rectangle(cell.x, cell.y, minimumWidth, minimumHeight);

		if (grabHorizontal) {
			if (tree.getColumnCount() == 0) {
				// Bounds of tree item only include the text area - stretch out to include 
				// entire client area
				cell.width = area.x + area.width - cell.x;
			}
			editorRect.width = Math.max(cell.width, minimumWidth);
		}

		if (grabVertical) {
			editorRect.height = Math.max(cell.height, minimumHeight);
		}

		if (horizontalAlignment == SWT.RIGHT) {
			editorRect.x += cell.width - editorRect.width;
		} else if (horizontalAlignment == SWT.LEFT) {
			// do nothing - cell.x is the right answer
		} else { // default is CENTER
			editorRect.x += (cell.width - editorRect.width)/2;
		}
		// don't let the editor overlap with the +/- of the tree
		editorRect.x = Math.max(cell.x, editorRect.x);

		if (verticalAlignment == SWT.BOTTOM) {
			editorRect.y += cell.height - editorRect.height;
		} else if (verticalAlignment == SWT.TOP) {
			// do nothing - cell.y is the right answer
		} else { // default is CENTER
			editorRect.y += (cell.height - editorRect.height)/2;
		}
		return editorRect;
	}

	/**
	 * Removes all associations between the CTableTreeEditor and the row in the tree.  The
	 * tree and the editor Control are <b>not</b> disposed.
	 */
	public void dispose () {
		if (this.column > -1 && this.column < tree.getColumnCount()){
			CContainerColumn treeColumn = tree.getColumn(this.column);
			treeColumn.removeControlListener(columnListener);
		}
		columnListener = null;
		if (treeListener != null) 
			tree.removeTreeListener(treeListener);
		treeListener = null;
		tree = null;
		item = null;
		column = 0;
		super.dispose();
	}

	/**
	 * Returns the zero based index of the column of the cell being tracked by this editor.
	 *
	 * @return the zero based index of the column of the cell being tracked by this editor
	 *
	 * @since 3.1
	 */
	public int getColumn () {
		return column;
	}

	/**
	 * Returns the CTableTreeItem for the row of the cell being tracked by this editor.
	 *
	 * @return the CTableTreeItem for the row of the cell being tracked by this editor
	 */
	public CTableTreeItem getItem () {
		return item;
	}

	/**
	 * Sets the zero based index of the column of the cell being tracked by this editor.
	 * 
	 * @param column the zero based index of the column of the cell being tracked by this editor
	 *
	 * @since 3.1
	 */
	public void setColumn(int column) {
		int columnCount = tree.getColumnCount();
		// Separately handle the case where the tree has no CTableTreeColumns.
		// In this situation, there is a single default column.
		if (columnCount == 0) {
			this.column = (column == 0) ? 0 : -1;
			_resize();
			return;
		}
		if (this.column > -1 && this.column < columnCount){
			CContainerColumn treeColumn = tree.getColumn(this.column);
			treeColumn.removeControlListener(columnListener);
			this.column = -1;
		}

		if (column < 0  || column >= tree.getColumnCount()) return;	

		this.column = column;
		CContainerColumn treeColumn = tree.getColumn(this.column);
		treeColumn.addControlListener(columnListener);
		_resize();
	}

	public void setItem (CTableTreeItem item) {
		this.item = item;
		_resize();
	}

	/**
	 * Specify the Control that is to be displayed and the cell in the tree that it is to be positioned above.
	 *
	 * <p>Note: The Control provided as the editor <b>must</b> be created with its parent being the CTableTree control
	 * specified in the CTableTreeEditor constructor.
	 * 
	 * @param editor the Control that is displayed above the cell being edited
	 * @param item the CTableTreeItem for the row of the cell being tracked by this editor
	 * @param column the zero based index of the column of the cell being tracked by this editor
	 *
	 * @since 3.1
	 */
	public void setEditor (Control editor, CTableTreeItem item, int column) {
		setItem(item);
		setColumn(column);
		setEditor(editor);
	}
	/**
	 * Specify the Control that is to be displayed and the cell in the tree that it is to be positioned above.
	 *
	 * <p>Note: The Control provided as the editor <b>must</b> be created with its parent being the CTableTree control
	 * specified in the CTableTreeEditor constructor.
	 * 
	 * @param editor the Control that is displayed above the cell being edited
	 * @param item the CTableTreeItem for the row of the cell being tracked by this editor
	 */
	public void setEditor (Control editor, CTableTreeItem item) {
		setItem(item);
		setEditor(editor);
	}

	void _resize () {
		if (tree.isDisposed()) return;
		if (item == null || item.isDisposed()) return;	
		int columnCount = tree.getColumnCount();
		if (columnCount == 0 && column != 0) return;
		if (columnCount > 0 && (column < 0 || column >= columnCount)) return;
		super._resize();
		Control editor = getEditor();
		if(editor != null && !editor.isDisposed()) editor.moveAbove(null);
	}
}
