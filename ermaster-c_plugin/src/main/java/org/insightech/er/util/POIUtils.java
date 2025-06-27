package org.insightech.er.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class POIUtils {

	public static class CellLocation {
		public int r;

		public int c;

		private CellLocation(int r, short c) {
			this.r = r;
			this.c = c;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			String str = "(" + this.r + ", " + this.c + ")";

			return str;
		}
	}

	public static CellLocation findCell(XSSFSheet sheet, String str) {
		return findCell(sheet, new String[] { str });
	}

	public static CellLocation findCell(XSSFSheet sheet, String[] strs) {
		for (int rowNum = sheet.getFirstRowNum(); rowNum < sheet
				.getLastRowNum() + 1; rowNum++) {
			XSSFRow row = sheet.getRow(rowNum);
			if (row == null) {
				continue;
			}

			for (int i = 0; i < strs.length; i++) {
				Integer colNum = findColumn(row, strs[i]);

				if (colNum != null) {
					return new CellLocation(rowNum, colNum.shortValue());
				}
			}
		}

		return null;
	}

	public static Integer findColumn(XSSFRow row, String str) {
		for (int colNum = row.getFirstCellNum(); colNum <= row.getLastCellNum(); colNum++) {
			if (colNum >= 0) {
				XSSFCell cell = row.getCell(colNum);

				if (cell == null) {
					continue;
				}

				if (cell.getCellType() == CellType.STRING) {
					XSSFRichTextString cellValue = cell.getRichStringCellValue();

					if (str.equals(cellValue.getString())) {
						return Integer.valueOf(colNum);
					}
				}
			} else {
				//				throw new RuntimeException("内部エラー colNum=" + colNum + " row=" + row);
			}
		}

		return null;
	}

	public static CellLocation findMatchCell(XSSFSheet sheet, String regexp) {
		for (int rowNum = sheet.getFirstRowNum(); rowNum < sheet
				.getLastRowNum() + 1; rowNum++) {
			XSSFRow row = sheet.getRow(rowNum);
			if (row == null) {
				continue;
			}

			Integer colNum = findMatchColumn(row, regexp);

			if (colNum != null) {
				return new CellLocation(rowNum, colNum.shortValue());
			}
		}

		return null;
	}

	public static Integer findMatchColumn(XSSFRow row, String str) {
		for (int colNum = row.getFirstCellNum(); colNum <= row.getLastCellNum(); colNum++) {
			if (colNum < 0)
				continue;

			XSSFCell cell = row.getCell(colNum);

			if (cell == null) {
				continue;
			}

			if (cell.getCellType() != CellType.STRING) {
				continue;
			}

			XSSFRichTextString cellValue = cell.getRichStringCellValue();

			if (cellValue.getString().matches(str)) {
				return Integer.valueOf(colNum);
			}
		}

		return null;
	}

	public static CellLocation findCell(XSSFSheet sheet, String str, int colNum) {
		for (int rowNum = sheet.getFirstRowNum(); rowNum < sheet
				.getLastRowNum() + 1; rowNum++) {
			XSSFRow row = sheet.getRow(rowNum);
			if (row == null) {
				continue;
			}

			XSSFCell cell = row.getCell(colNum);

			if (cell == null) {
				continue;
			}
			XSSFRichTextString cellValue = cell.getRichStringCellValue();

			if (!Check.isEmpty(cellValue.getString())) {
				if (cellValue.getString().equals(str)) {
					return new CellLocation(rowNum, (short) colNum);
				}
			}
		}

		return null;
	}

	public static void replace(XSSFSheet sheet, String keyword, String str) {
		CellLocation location = findCell(sheet, keyword);

		if (location == null) {
			return;
		}

		setCellValue(sheet, location, str);
	}

	public static String getCellValue(XSSFSheet sheet, CellLocation location) {
		XSSFRow row = sheet.getRow(location.r);
		XSSFCell cell = row.getCell(location.c);

		XSSFRichTextString cellValue = cell.getRichStringCellValue();

		return cellValue.toString();
	}

	public static String getCellValue(XSSFSheet sheet, int r, int c) {
		XSSFRow row = sheet.getRow(r);

		if (row == null) {
			return null;
		}

		XSSFCell cell = row.getCell(c);

		if (cell == null) {
			return null;
		}

		XSSFRichTextString cellValue = cell.getRichStringCellValue();

		return cellValue.toString();
	}

	public static int getIntCellValue(XSSFSheet sheet, int r, int c) {
		XSSFRow row = sheet.getRow(r);
		if (row == null) {
			return 0;
		}
		XSSFCell cell = row.getCell(c);

		if (cell.getCellType() != CellType.NUMERIC) {
			return 0;
		}

		return (int) cell.getNumericCellValue();
	}

	public static boolean getBooleanCellValue(XSSFSheet sheet, int r, int c) {
		XSSFRow row = sheet.getRow(r);

		if (row == null) {
			return false;
		}

		XSSFCell cell = row.getCell(c);

		if (cell == null) {
			return false;
		}

		return cell.getBooleanCellValue();
	}

	public static short getCellColor(XSSFSheet sheet, int r, int c) {
		XSSFRow row = sheet.getRow(r);
		if (row == null) {
			return -1;
		}
		XSSFCell cell = row.getCell(c);

		return cell.getCellStyle().getFillForegroundColor();
	}

	public static XSSFColor getForegroundColor(XSSFSheet sheet, int r, int c) {
		XSSFRow row = sheet.getRow(r);
		if (row == null) {
			return null;
		}
		XSSFCell cell = row.getCell(c);

		return cell.getCellStyle().getFillForegroundColorColor();
	}

	public static void setCellValue(XSSFSheet sheet, CellLocation location,
			String value) {
		XSSFRow row = sheet.getRow(location.r);
		XSSFCell cell = row.getCell(location.c);

		XSSFRichTextString text = new XSSFRichTextString(value);
		cell.setCellValue(text);
	}

	/**
	 * �G�N�Z���t�@�C���̓ǂݍ��݂��s���܂��B
	 * 
	 * @param excelFile
	 * @return
	 * @throws IOException
	 */
	public static XSSFWorkbook readExcelBook(File excelFile) throws IOException {
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(excelFile);

			return readExcelBook(fis);

		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	/**
	 * �G�N�Z���t�@�C���̓ǂݍ��݂��s���܂��B
	 * 
	 * @param excelFile
	 * @return
	 * @throws IOException
	 */
	public static XSSFWorkbook readExcelBook(InputStream stream)
			throws IOException {
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(stream);
			return new XSSFWorkbook(bis);

		} finally {
			if (bis != null) {
				bis.close();
			}
		}
	}

	/**
	 * �G�N�Z���t�@�C���ɏ����o�����s���܂��B
	 * 
	 * @param excelFile
	 * @param workbook
	 * @return
	 * @throws IOException
	 */
	public static void writeExcelFile(File excelFile, XSSFWorkbook workbook)
			throws IOException {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;

		try {
			fos = new FileOutputStream(excelFile);
			bos = new BufferedOutputStream(fos);
			workbook.write(bos);

		} finally {
			if (bos != null) {
				bos.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
	}

	/**
	 * location�Ŏw�肵���s�́A�w�肵���񂩂�n�܂錋�����ꂽ�̈���擾���܂�
	 * 
	 * @param sheet
	 * @param location
	 * @return
	 */
	public static CellRangeAddress getMergedRegion(XSSFSheet sheet,
			CellLocation location) {
		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			CellRangeAddress region = sheet.getMergedRegion(i);

			int rowFrom = region.getFirstRow();
			int rowTo = region.getLastRow();

			if (rowFrom == location.r && rowTo == location.r) {
				int colFrom = region.getFirstColumn();

				if (colFrom == location.c) {
					return region;
				}
			}
		}

		return null;
	}

	/**
	 * location�Ŏw�肵���s�́A�������ꂽ�̈�̈ꗗ���擾���܂�
	 * 
	 * @param sheet
	 * @param location
	 * @return
	 */
	public static List<CellRangeAddress> getMergedRegionList(XSSFSheet sheet,
			int rowNum) {
		List<CellRangeAddress> regionList = new ArrayList<CellRangeAddress>();

		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			CellRangeAddress region = sheet.getMergedRegion(i);

			int rowFrom = region.getFirstRow();
			int rowTo = region.getLastRow();

			if (rowFrom == rowNum && rowTo == rowNum) {
				regionList.add(region);
			}
		}

		return regionList;
	}

	public static void copyRow(XSSFSheet oldSheet, XSSFSheet newSheet,
			int oldStartRowNum, int oldEndRowNum, int newStartRowNum) {
		XSSFRow oldAboveRow = oldSheet.getRow(oldStartRowNum - 1);

		int newRowNum = newStartRowNum;

		for (int oldRowNum = oldStartRowNum; oldRowNum <= oldEndRowNum; oldRowNum++) {
			POIUtils.copyRow(oldSheet, newSheet, oldRowNum, newRowNum++);
		}

		XSSFRow newTopRow = newSheet.getRow(newStartRowNum);

		if (oldAboveRow != null) {
			for (int colNum = newTopRow.getFirstCellNum(); colNum <= newTopRow
					.getLastCellNum(); colNum++) {
				XSSFCell oldAboveCell = oldAboveRow.getCell(colNum);
				if (oldAboveCell != null) {
					XSSFCell newTopCell = newTopRow.getCell(colNum);
					newTopCell.getCellStyle().setBorderTop(
							oldAboveCell.getCellStyle().getBorderBottom());
				}
			}
		}
	}

	public static void copyRow(XSSFSheet oldSheet, XSSFSheet newSheet,
			int oldRowNum, int newRowNum) {
		XSSFRow oldRow = oldSheet.getRow(oldRowNum);

		XSSFRow newRow = newSheet.createRow(newRowNum);

		if (oldRow == null) {
			return;
		}

		newRow.setHeight(oldRow.getHeight());

		if (oldRow.getFirstCellNum() == -1) {
			return;
		}

		for (int colNum = oldRow.getFirstCellNum(); colNum <= oldRow
				.getLastCellNum(); colNum++) {
			XSSFCell oldCell = oldRow.getCell(colNum);
			XSSFCell newCell = newRow.createCell(colNum);

			if (oldCell != null) {
				XSSFCellStyle style = oldCell.getCellStyle();
				newCell.setCellStyle(style);

				CellType cellType = oldCell.getCellType();
				newCell.setCellType(cellType);

				switch (cellType) {
				case BOOLEAN:
					newCell.setCellValue(oldCell.getBooleanCellValue());
				case FORMULA:
					newCell.setCellFormula(oldCell.getCellFormula());
					break;
				case NUMERIC:
					newCell.setCellValue(oldCell.getNumericCellValue());
					break;
				case STRING:
					newCell.setCellValue(oldCell.getRichStringCellValue());
					break;
				default:
					break;
				}
			}
		}

		POIUtils.copyMergedRegion(newSheet, getMergedRegionList(oldSheet,
				oldRowNum), newRowNum);
	}

	public static void copyMergedRegion(XSSFSheet sheet,
			List<CellRangeAddress> regionList, int rowNum) {
		for (CellRangeAddress region : regionList) {
			CellRangeAddress address = new CellRangeAddress(rowNum, rowNum,
					region.getFirstColumn(), region.getLastColumn());
			sheet.addMergedRegion(address);
		}
	}

	public static List<XSSFCellStyle> copyCellStyle(XSSFWorkbook workbook,
			XSSFRow row) {
		List<XSSFCellStyle> cellStyleList = new ArrayList<XSSFCellStyle>();

		for (int colNum = row.getFirstCellNum(); colNum <= row.getLastCellNum(); colNum++) {

			XSSFCell cell = row.getCell(colNum);
			if (cell != null) {
				XSSFCellStyle style = cell.getCellStyle();
				XSSFCellStyle newCellStyle = copyCellStyle(workbook, style);
				cellStyleList.add(newCellStyle);
			} else {
				cellStyleList.add(null);
			}
		}

		return cellStyleList;
	}

	public static XSSFCellStyle copyCellStyle(XSSFWorkbook workbook,
			XSSFCellStyle style) {

		XSSFCellStyle newCellStyle = workbook.createCellStyle();

		newCellStyle.setAlignment(style.getAlignment());
		newCellStyle.setBorderBottom(style.getBorderBottom());
		newCellStyle.setBorderLeft(style.getBorderLeft());
		newCellStyle.setBorderRight(style.getBorderRight());
		newCellStyle.setBorderTop(style.getBorderTop());
		newCellStyle.setBottomBorderColor(style.getBottomBorderColor());
		newCellStyle.setDataFormat(style.getDataFormat());
		newCellStyle.setFillBackgroundColor(style.getFillBackgroundColor());
		newCellStyle.setFillForegroundColor(style.getFillForegroundColor());
		newCellStyle.setFillPattern(style.getFillPattern());
		newCellStyle.setHidden(style.getHidden());
		newCellStyle.setIndention(style.getIndention());
		newCellStyle.setLeftBorderColor(style.getLeftBorderColor());
		newCellStyle.setLocked(style.getLocked());
		newCellStyle.setRightBorderColor(style.getRightBorderColor());
		newCellStyle.setRotation(style.getRotation());
		newCellStyle.setTopBorderColor(style.getTopBorderColor());
		newCellStyle.setVerticalAlignment(style.getVerticalAlignment());
		newCellStyle.setWrapText(style.getWrapText());

		XSSFFont font = workbook.getFontAt(style.getFontIndex());
		newCellStyle.setFont(font);

		return newCellStyle;
	}

	public static XSSFFont copyFont(XSSFWorkbook workbook, XSSFFont font) {

		XSSFFont newFont = workbook.createFont();

		// newFont.setBoldweight(font.getBoldweight());
		// newFont.setCharSet(font.getCharSet());
		// newFont.setColor(font.getColor());
		// newFont.setFontHeight(font.getFontHeight());
		// newFont.setFontHeightInPoints(font.getFontHeightInPoints());
		// newFont.setFontName(font.getFontName());
		// newFont.setItalic(font.getItalic());
		// newFont.setStrikeout(font.getStrikeout());
		// newFont.setTypeOffset(font.getTypeOffset());
		// newFont.setUnderline(font.getUnderline());

		return newFont;
	}

	public static XSSFRow insertRow(XSSFSheet sheet, int rowNum) {
		sheet.shiftRows(rowNum + 1, sheet.getLastRowNum(), 1);

		return sheet.getRow(rowNum);
	}
}
