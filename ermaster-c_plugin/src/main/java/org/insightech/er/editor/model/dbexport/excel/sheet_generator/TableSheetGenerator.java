package org.insightech.er.editor.model.dbexport.excel.sheet_generator;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.core.runtime.IProgressMonitor;
import org.insightech.er.editor.model.ERDiagram;
import org.insightech.er.editor.model.ObjectModel;
import org.insightech.er.editor.model.dbexport.excel.ExportToExcelManager.LoopDefinition;
import org.insightech.er.editor.model.diagram_contents.element.node.table.ERTable;
import org.insightech.er.editor.model.diagram_contents.element.node.table.column.NormalColumn;
import org.insightech.er.editor.model.diagram_contents.element.node.table.index.Index;
import org.insightech.er.editor.model.diagram_contents.element.node.table.unique_key.ComplexUniqueKey;
import org.insightech.er.util.Format;
import org.insightech.er.util.POIUtils;
import org.insightech.er.util.POIUtils.CellLocation;

public class TableSheetGenerator extends AbstractSheetGenerator {

	private static final String KEYWORD_LOGICAL_INDEX_MATRIX = "$LIDX";

	private static final String KEYWORD_PHYSICAL_INDEX_MATRIX = "$PIDX";

	private static final String KEYWORD_LOGICAL_COMPLEX_UNIQUE_KEY_MATRIX = "$LCUK";

	private static final String KEYWORD_PHYSICAL_COMPLEX_UNIQUE_KEY_MATRIX = "$PCUK";

	private static final String KEYWORD_TABLE_CONSTRAINT = "$TCON";

	private static final String[] FIND_KEYWORDS_OF_FK_COLUMN = {
			KEYWORD_LOGICAL_FOREIGN_KEY_NAME, KEYWORD_PHYSICAL_FOREIGN_KEY_NAME };

	private ColumnTemplate columnTemplate;

	private ColumnTemplate fkColumnTemplate;

	private MatrixCellStyle physicalIndexMatrixCellStyle;

	private MatrixCellStyle logicalIndexMatrixCellStyle;

	private MatrixCellStyle physicalComplexUniqueKeyMatrixCellStyle;

	private MatrixCellStyle logicalComplexUniqueKeyMatrixCellStyle;

	protected void clear() {
		this.columnTemplate = null;
		this.fkColumnTemplate = null;
		this.physicalIndexMatrixCellStyle = null;
		this.logicalIndexMatrixCellStyle = null;
		this.physicalComplexUniqueKeyMatrixCellStyle = null;
		this.logicalComplexUniqueKeyMatrixCellStyle = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void generate(IProgressMonitor monitor, XSSFWorkbook workbook,
			int sheetNo, boolean useLogicalNameAsSheetName,
			Map<String, Integer> sheetNameMap,
			Map<String, ObjectModel> sheetObjectMap, ERDiagram diagram,
			Map<String, LoopDefinition> loopDefinitionMap) {
		this.clear();

		List<ERTable> nodeSet = null;

		if (diagram.getCurrentCategory() != null) {
			nodeSet = diagram.getCurrentCategory().getTableContents();
		} else {
			nodeSet = diagram.getDiagramContents().getContents().getTableSet()
					.getList();
		}

		for (ERTable table : nodeSet) {
			String name = null;
			if (useLogicalNameAsSheetName) {
				name = table.getLogicalName();
			} else {
				name = table.getPhysicalName();
			}

			XSSFSheet newSheet = createNewSheet(workbook, sheetNo, name,
					sheetNameMap);

			sheetObjectMap.put(workbook.getSheetName(workbook
					.getSheetIndex(newSheet)), table);

			this.setTableData(workbook, newSheet, table);

			monitor.worked(1);
		}
	}

	public void setTableData(XSSFWorkbook workbook, XSSFSheet sheet,
			ERTable table) {
		POIUtils.replace(sheet, KEYWORD_LOGICAL_TABLE_NAME, this.getValue(
				this.keywordsValueMap, KEYWORD_LOGICAL_TABLE_NAME, table
						.getLogicalName()));

		POIUtils.replace(sheet, KEYWORD_PHYSICAL_TABLE_NAME, this.getValue(
				this.keywordsValueMap, KEYWORD_PHYSICAL_TABLE_NAME, table
						.getPhysicalName()));

		POIUtils.replace(sheet, KEYWORD_TABLE_DESCRIPTION, this.getValue(
				this.keywordsValueMap, KEYWORD_TABLE_DESCRIPTION, table
						.getDescription()));

		POIUtils.replace(sheet, KEYWORD_TABLE_CONSTRAINT, this.getValue(
				this.keywordsValueMap, KEYWORD_TABLE_CONSTRAINT, table
						.getConstraint()));

		CellLocation cellLocation = POIUtils.findCell(sheet,
				FIND_KEYWORDS_OF_COLUMN);

		if (cellLocation != null) {
			int rowNum = cellLocation.r;
			XSSFRow templateRow = sheet.getRow(rowNum);

			if (this.columnTemplate == null) {
				this.columnTemplate = this.loadColumnTemplate(workbook, sheet,
						cellLocation);
			}

			int order = 1;

			for (NormalColumn normalColumn : table.getExpandedColumns()) {
				XSSFRow row = POIUtils.insertRow(sheet, rowNum++);
				this.setColumnData(this.keywordsValueMap, columnTemplate, row,
						normalColumn, table, order);
				order++;
			}

			this.setCellStyle(columnTemplate, sheet, cellLocation.r, rowNum
					- cellLocation.r, templateRow.getFirstCellNum());
		}

		CellLocation fkCellLocation = POIUtils.findCell(sheet,
				FIND_KEYWORDS_OF_FK_COLUMN);

		if (fkCellLocation != null) {
			int rowNum = fkCellLocation.r;
			XSSFRow templateRow = sheet.getRow(rowNum);

			if (this.fkColumnTemplate == null) {
				this.fkColumnTemplate = this.loadColumnTemplate(workbook,
						sheet, fkCellLocation);
			}

			int order = 1;

			for (NormalColumn normalColumn : table.getExpandedColumns()) {
				if (normalColumn.isForeignKey()) {
					XSSFRow row = POIUtils.insertRow(sheet, rowNum++);
					this.setColumnData(this.keywordsValueMap,
							this.fkColumnTemplate, row, normalColumn, table,
							order);
					order++;
				}
			}

			this.setCellStyle(this.fkColumnTemplate, sheet, fkCellLocation.r,
					rowNum - fkCellLocation.r, templateRow.getFirstCellNum());
		}

		this.setIndexMatrix(workbook, sheet, table);
		this.setComplexUniqueKeyMatrix(workbook, sheet, table);
	}

	private void setIndexMatrix(XSSFWorkbook workbook, XSSFSheet sheet,
			ERTable table) {
		CellLocation logicalIndexCellLocation = POIUtils.findCell(sheet,
				KEYWORD_LOGICAL_INDEX_MATRIX);

		if (logicalIndexCellLocation != null) {
			if (this.logicalIndexMatrixCellStyle == null) {
				this.logicalIndexMatrixCellStyle = this.createMatrixCellStyle(
						workbook, sheet, logicalIndexCellLocation);
			}
			setIndexMatrix(workbook, sheet, table, logicalIndexCellLocation,
					this.logicalIndexMatrixCellStyle, true);
		}

		CellLocation physicalIndexCellLocation = POIUtils.findCell(sheet,
				KEYWORD_PHYSICAL_INDEX_MATRIX);

		if (physicalIndexCellLocation != null) {
			if (this.physicalIndexMatrixCellStyle == null) {
				this.physicalIndexMatrixCellStyle = this.createMatrixCellStyle(
						workbook, sheet, physicalIndexCellLocation);
			}
			setIndexMatrix(workbook, sheet, table, physicalIndexCellLocation,
					this.physicalIndexMatrixCellStyle, false);
		}
	}

	private void setComplexUniqueKeyMatrix(XSSFWorkbook workbook,
			XSSFSheet sheet, ERTable table) {
		CellLocation logicalCellLocation = POIUtils.findCell(sheet,
				KEYWORD_LOGICAL_COMPLEX_UNIQUE_KEY_MATRIX);

		if (logicalCellLocation != null) {
			if (this.logicalComplexUniqueKeyMatrixCellStyle == null) {
				this.logicalComplexUniqueKeyMatrixCellStyle = this
						.createMatrixCellStyle(workbook, sheet,
								logicalCellLocation);
			}
			setComplexUniqueKeyMatrix(workbook, sheet, table,
					logicalCellLocation,
					this.logicalComplexUniqueKeyMatrixCellStyle, true);
		}

		CellLocation physicalCellLocation = POIUtils.findCell(sheet,
				KEYWORD_PHYSICAL_COMPLEX_UNIQUE_KEY_MATRIX);

		if (physicalCellLocation != null) {
			if (this.physicalComplexUniqueKeyMatrixCellStyle == null) {
				this.physicalComplexUniqueKeyMatrixCellStyle = this
						.createMatrixCellStyle(workbook, sheet,
								physicalCellLocation);
			}

			this.setComplexUniqueKeyMatrix(workbook, sheet, table,
					physicalCellLocation,
					this.physicalComplexUniqueKeyMatrixCellStyle, false);
		}
	}

	private void setIndexMatrixColor(XSSFWorkbook workbook,
			XSSFCellStyle indexStyle) {
		indexStyle.setFillForegroundColor(IndexedColors.WHITE.index);
		XSSFFont font = workbook.getFontAt(indexStyle.getFontIndex());
		font.setColor(IndexedColors.BLACK.index);
	}

	private MatrixCellStyle createMatrixCellStyle(XSSFWorkbook workbook,
			XSSFSheet sheet, CellLocation matrixCellLocation) {

		int matrixRowNum = matrixCellLocation.r;
		int matrixColumnNum = matrixCellLocation.c;

		XSSFRow matrixHeaderTemplateRow = sheet.getRow(matrixRowNum);
		XSSFCell matrixHeaderTemplateCell = matrixHeaderTemplateRow
				.getCell(matrixColumnNum);

		MatrixCellStyle matrixCellStyle = new MatrixCellStyle();

		matrixCellStyle.headerTemplateCellStyle = matrixHeaderTemplateCell
				.getCellStyle();

		matrixCellStyle.style11 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, false, true, true,
				false);

		matrixCellStyle.style12 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, false, true, true,
				true);

		matrixCellStyle.style13 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, false, false, true,
				true);

		matrixCellStyle.style21 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, true, true, true,
				false);

		matrixCellStyle.style22 = this
				.createMatrixCellStyle(workbook,
						matrixCellStyle.headerTemplateCellStyle, true, true,
						true, true);
		this.setIndexMatrixColor(workbook, matrixCellStyle.style22);

		matrixCellStyle.style23 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, true, false, true,
				true);
		this.setIndexMatrixColor(workbook, matrixCellStyle.style23);

		matrixCellStyle.style31 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, true, true, false,
				false);

		matrixCellStyle.style32 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, true, true, false,
				true);
		this.setIndexMatrixColor(workbook, matrixCellStyle.style32);

		matrixCellStyle.style33 = this.createMatrixCellStyle(workbook,
				matrixCellStyle.headerTemplateCellStyle, true, false, false,
				true);
		this.setIndexMatrixColor(workbook, matrixCellStyle.style33);

		return matrixCellStyle;
	}

	private XSSFCellStyle createMatrixCellStyle(XSSFWorkbook workbook,
			XSSFCellStyle matrixHeaderTemplateCellStyle, boolean top,
			boolean right, boolean bottom, boolean left) {
		XSSFCellStyle cellStyle = POIUtils.copyCellStyle(workbook,
				matrixHeaderTemplateCellStyle);

		if (top) {
			cellStyle.setBorderTop(BorderStyle.THIN);
		}
		if (right) {
			cellStyle.setBorderRight(BorderStyle.THIN);
		}
		if (bottom) {
			cellStyle.setBorderBottom(BorderStyle.THIN);
		}
		if (left) {
			cellStyle.setBorderLeft(BorderStyle.THIN);
		}

		return cellStyle;
	}

	private void setIndexMatrix(XSSFWorkbook workbook, XSSFSheet sheet,
			ERTable table, CellLocation cellLocation,
			MatrixCellStyle matrixCellStyle, boolean isLogical) {

		int rowNum = cellLocation.r;
		int columnNum = cellLocation.c;

		XSSFRow headerTemplateRow = sheet.getRow(rowNum);
		XSSFCell headerTemplateCell = headerTemplateRow.getCell(columnNum);

		int num = table.getIndexes().size();

		if (num == 0) {
			XSSFCellStyle cellStyle = headerTemplateCell.getCellStyle();
			headerTemplateRow.removeCell(headerTemplateCell);

			XSSFRow row = sheet.getRow(rowNum - 1);
			if (row != null) {
				XSSFCell cell = row.getCell(columnNum);
				if (cell != null && cell instanceof XSSFCell) {
					cell.getCellStyle().setBorderBottom(cellStyle.getBorderBottom());
				}
			}
			return;
		}

		XSSFRow headerRow = sheet.createRow(rowNum++);

		for (int i = 0; i < num + 1; i++) {
			XSSFCell cell = headerRow.createCell(columnNum + i);

			if (i == 0) {
				cell.setCellStyle(matrixCellStyle.style11);

			} else {
				Index index = table.getIndexes().get(i - 1);
				XSSFRichTextString text = new XSSFRichTextString(index
						.getName());
				cell.setCellValue(text);

				if (i != num) {
					cell.setCellStyle(matrixCellStyle.style12);
				} else {
					cell.setCellStyle(matrixCellStyle.style13);
				}
			}
		}

		int columnSize = table.getExpandedColumns().size();
		for (int j = 0; j < columnSize; j++) {
			NormalColumn normalColumn = table.getExpandedColumns().get(j);

			XSSFRow row = POIUtils.insertRow(sheet, rowNum++);

			for (int i = 0; i < num + 1; i++) {
				XSSFCell cell = row.createCell(columnNum + i);

				if (i == 0) {
					String columnName = null;
					if (isLogical) {
						columnName = normalColumn.getLogicalName();
					} else {
						columnName = normalColumn.getPhysicalName();
					}

					XSSFRichTextString text = new XSSFRichTextString(columnName);
					cell.setCellValue(text);
					cell.setCellStyle(headerTemplateCell.getCellStyle());

					if (j != columnSize - 1) {
						cell.setCellStyle(matrixCellStyle.style21);
					} else {
						cell.setCellStyle(matrixCellStyle.style31);
					}

				} else {
					Index index = table.getIndexes().get(i - 1);
					List<NormalColumn> indexColumnList = index.getColumns();

					int indexNo = indexColumnList.indexOf(normalColumn);
					if (indexNo != -1) {
						cell.setCellValue(indexNo + 1);
					}

					if (i != num) {
						if (j != columnSize - 1) {
							cell.setCellStyle(matrixCellStyle.style22);
						} else {
							cell.setCellStyle(matrixCellStyle.style32);
						}

					} else {
						if (j != columnSize - 1) {
							cell.setCellStyle(matrixCellStyle.style23);
						} else {
							cell.setCellStyle(matrixCellStyle.style33);
						}
					}
				}
			}
		}
	}

	private void setComplexUniqueKeyMatrix(XSSFWorkbook workbook,
			XSSFSheet sheet, ERTable table, CellLocation cellLocation,
			MatrixCellStyle matrixCellStyle, boolean isLogical) {

		int rowNum = cellLocation.r;
		int columnNum = cellLocation.c;

		XSSFRow headerTemplateRow = sheet.getRow(rowNum);
		XSSFCell headerTemplateCell = headerTemplateRow.getCell(columnNum);

		int num = table.getComplexUniqueKeyList().size();

		if (num == 0) {
			XSSFCellStyle cellStyle = headerTemplateCell.getCellStyle();
			headerTemplateRow.removeCell(headerTemplateCell);

			XSSFRow row = sheet.getRow(rowNum - 1);
			if (row != null) {
				XSSFCell cell = row.getCell(columnNum);
				if (cell != null) {
					cell.getCellStyle()
							.setBorderBottom(cellStyle.getBorderBottom());
				}
			}
			return;
		}

		XSSFRow headerRow = sheet.createRow(rowNum++);

		for (int i = 0; i < num + 1; i++) {
			XSSFCell cell = headerRow.createCell(columnNum + i);

			if (i == 0) {
				cell.setCellStyle(matrixCellStyle.style11);

			} else {
				ComplexUniqueKey complexUniqueKey = table
						.getComplexUniqueKeyList().get(i - 1);
				XSSFRichTextString text = new XSSFRichTextString(Format
						.null2blank(complexUniqueKey.getUniqueKeyName()));
				cell.setCellValue(text);

				if (i != num) {
					cell.setCellStyle(matrixCellStyle.style12);
				} else {
					cell.setCellStyle(matrixCellStyle.style13);
				}
			}
		}

		int columnSize = table.getExpandedColumns().size();
		for (int j = 0; j < columnSize; j++) {
			NormalColumn normalColumn = table.getExpandedColumns().get(j);

			XSSFRow row = POIUtils.insertRow(sheet, rowNum++);

			for (int i = 0; i < num + 1; i++) {
				XSSFCell cell = row.createCell(columnNum + i);

				if (i == 0) {
					String columnName = null;
					if (isLogical) {
						columnName = normalColumn.getLogicalName();
					} else {
						columnName = normalColumn.getPhysicalName();
					}

					XSSFRichTextString text = new XSSFRichTextString(columnName);
					cell.setCellValue(text);
					cell.setCellStyle(headerTemplateCell.getCellStyle());

					if (j != columnSize - 1) {
						cell.setCellStyle(matrixCellStyle.style21);
					} else {
						cell.setCellStyle(matrixCellStyle.style31);
					}

				} else {
					ComplexUniqueKey complexUniqueKey = table
							.getComplexUniqueKeyList().get(i - 1);
					List<NormalColumn> targetColumnList = complexUniqueKey
							.getColumnList();

					int indexNo = targetColumnList.indexOf(normalColumn);
					if (indexNo != -1) {
						cell.setCellValue(indexNo + 1);
					}

					if (i != num) {
						if (j != columnSize - 1) {
							cell.setCellStyle(matrixCellStyle.style22);
						} else {
							cell.setCellStyle(matrixCellStyle.style32);
						}

					} else {
						if (j != columnSize - 1) {
							cell.setCellStyle(matrixCellStyle.style23);
						} else {
							cell.setCellStyle(matrixCellStyle.style33);
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTemplateSheetName() {
		return "table_template";
	}

	@Override
	public String[] getKeywords() {
		return new String[] { KEYWORD_LOGICAL_TABLE_NAME,
				KEYWORD_PHYSICAL_TABLE_NAME, KEYWORD_TABLE_DESCRIPTION,
				KEYWORD_TABLE_CONSTRAINT, KEYWORD_ORDER,
				KEYWORD_LOGICAL_COLUMN_NAME, KEYWORD_PHYSICAL_COLUMN_NAME,
				KEYWORD_TYPE, KEYWORD_LENGTH, KEYWORD_DECIMAL,
				KEYWORD_PRIMARY_KEY, KEYWORD_NOT_NULL, KEYWORD_UNIQUE_KEY,
				KEYWORD_FOREIGN_KEY, KEYWORD_LOGICAL_REFERENCE_TABLE_KEY,
				KEYWORD_PHYSICAL_REFERENCE_TABLE_KEY,
				KEYWORD_LOGICAL_REFERENCE_TABLE,
				KEYWORD_PHYSICAL_REFERENCE_TABLE,
				KEYWORD_LOGICAL_REFERENCE_KEY, KEYWORD_PHYSICAL_REFERENCE_KEY,
				KEYWORD_AUTO_INCREMENT, KEYWORD_DEFAULT_VALUE,
				KEYWORD_DESCRIPTION, KEYWORD_LOGICAL_INDEX_MATRIX,
				KEYWORD_PHYSICAL_INDEX_MATRIX,
				KEYWORD_LOGICAL_FOREIGN_KEY_NAME,
				KEYWORD_PHYSICAL_FOREIGN_KEY_NAME };
	}

	@Override
	public int getKeywordsColumnNo() {
		return 0;
	}

	@Override
	public int count(ERDiagram diagram) {
		return diagram.getDiagramContents().getContents().getTableSet()
				.getList().size();
	}
}
