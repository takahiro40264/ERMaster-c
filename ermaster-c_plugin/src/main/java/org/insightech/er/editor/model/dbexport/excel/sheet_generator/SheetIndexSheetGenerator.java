package org.insightech.er.editor.model.dbexport.excel.sheet_generator;

import java.util.Map;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.core.runtime.IProgressMonitor;
import org.insightech.er.ResourceString;
import org.insightech.er.editor.model.ERDiagram;
import org.insightech.er.editor.model.ObjectModel;
import org.insightech.er.editor.model.dbexport.excel.ExportToExcelManager.LoopDefinition;
import org.insightech.er.util.POIUtils;
import org.insightech.er.util.POIUtils.CellLocation;

public class SheetIndexSheetGenerator extends AbstractSheetGenerator {

	// シートタイプ
	private static final String KEYWORD_SHEET_TYPE = "$SHTT";

	// 名前
	private static final String KEYWORD_NAME = "$NAM";

	// 説明
	private static final String KEYWORD_DESCRIPTION = "$DSC";

	// シート名
	private static final String KEYWORD_SHEET_NAME = "$SHTN";

	private static final String[] FIND_KEYWORDS_LIST = { KEYWORD_SHEET_TYPE,
			KEYWORD_NAME, KEYWORD_DESCRIPTION };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void generate(IProgressMonitor monitor, XSSFWorkbook workbook,
			int sheetNo, boolean useLogicalNameAsSheetName,
			Map<String, Integer> sheetNameMap,
			Map<String, ObjectModel> sheetObjectMap, ERDiagram diagram,
			Map<String, LoopDefinition> loopDefinitionMap) {

		XSSFSheet sheet = workbook.getSheetAt(sheetNo);

		this.setSheetListData(workbook, sheet, sheetObjectMap, diagram);
		monitor.worked(1);
	}

	public void setSheetListData(XSSFWorkbook workbook, XSSFSheet sheet,
			Map<String, ObjectModel> sheetObjectMap, ERDiagram diagram) {
		CellLocation cellLocation = POIUtils
				.findCell(sheet, FIND_KEYWORDS_LIST);

		if (cellLocation != null) {
			int rowNum = cellLocation.r;
			XSSFRow templateRow = sheet.getRow(rowNum);

			ColumnTemplate columnTemplate = this.loadColumnTemplate(workbook,
					sheet, cellLocation);
			int order = 1;

			XSSFFont linkCellFont = null;
			int linkCol = -1;

			for (Map.Entry<String, ObjectModel> entry : sheetObjectMap
					.entrySet()) {
				String sheetName = entry.getKey();
				ObjectModel objectModel = entry.getValue();

				XSSFRow row = POIUtils.insertRow(sheet, rowNum++);

				for (int columnNum : columnTemplate.columnTemplateMap.keySet()) {
					if (row == null)
						continue;

					XSSFCell cell = row.createCell(columnNum);
					String template = columnTemplate.columnTemplateMap
							.get(columnNum);

					String value = null;
					if (KEYWORD_ORDER.equals(template)) {
						value = String.valueOf(order);

					} else {
						if (KEYWORD_SHEET_TYPE.equals(template)) {
							value = ResourceString
									.getResourceString("label.object.type."
											+ objectModel.getObjectType());

						} else if (KEYWORD_NAME.equals(template)) {
							value = sheetName;
							CreationHelper createHelper = workbook.getCreationHelper();
							Hyperlink documentLink = createHelper.createHyperlink(HyperlinkType.DOCUMENT);
							documentLink.setAddress("'" + sheetName + "'!A1");
							cell.setHyperlink(documentLink);

							if (linkCellFont == null) {
								linkCol = columnNum;

								linkCellFont = POIUtils.copyFont(workbook, cell
										.getCellStyle().getFont());

								linkCellFont.setColor(IndexedColors.BLUE.index);
								linkCellFont.setUnderline(XSSFFont.U_SINGLE);
							}

						} else if (KEYWORD_DESCRIPTION.equals(template)) {
							value = objectModel.getDescription();
						}

						XSSFRichTextString text = new XSSFRichTextString(value);
						cell.setCellValue(text);
					}

					order++;
				}
			}

			this.setCellStyle(columnTemplate, sheet, cellLocation.r, rowNum
					- cellLocation.r, templateRow.getFirstCellNum());

			if (linkCol != -1) {
				for (int row = cellLocation.r; row < rowNum; row++) {
					XSSFRow xssfRow = sheet.getRow(row);
					if (xssfRow == null)
						continue;
					XSSFCell cell = xssfRow.getCell(linkCol);
					if (cell == null)
						continue;
					cell.getCellStyle().setFont(linkCellFont);
				}
			}
		}
	}

	public String getSheetName() {
		String name = this.keywordsValueMap.get(KEYWORD_SHEET_NAME);

		if (name == null) {
			name = "List of sheets";
		}

		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTemplateSheetName() {
		return "sheet_index_template";
	}

	@Override
	public String[] getKeywords() {
		return new String[] { KEYWORD_SHEET_TYPE, KEYWORD_NAME,
				KEYWORD_DESCRIPTION, KEYWORD_ORDER, KEYWORD_SHEET_NAME };
	}

	@Override
	public int getKeywordsColumnNo() {
		return 24;
	}

	@Override
	public int count(ERDiagram diagram) {
		return 1;
	}
}
