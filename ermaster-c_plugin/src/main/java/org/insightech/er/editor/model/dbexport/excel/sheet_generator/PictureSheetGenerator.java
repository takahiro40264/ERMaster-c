package org.insightech.er.editor.model.dbexport.excel.sheet_generator;

import java.awt.Dimension;

import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.insightech.er.util.POIUtils;
import org.insightech.er.util.POIUtils.CellLocation;

public class PictureSheetGenerator {

	private static final String KEYWORD_ER = "$ER";

	private byte[] imageBuffer;

	private int pictureIndex;

	private int excelPictureType;

	public PictureSheetGenerator(XSSFWorkbook workbook, byte[] imageBuffer,
			int excelPictureType) {
		this.imageBuffer = imageBuffer;
		this.excelPictureType = excelPictureType;

		if (this.imageBuffer != null) {
			this.pictureIndex = workbook.addPicture(this.imageBuffer,
					this.excelPictureType);
		}
	}

	public void setImage(XSSFWorkbook workbook, XSSFSheet sheet) {
		CellLocation cellLocation = POIUtils.findMatchCell(sheet, "\\"
				+ KEYWORD_ER + ".*");
System.out.println(cellLocation);
		if (cellLocation != null) {
			int width = -1;
			int height = -1;

			String value = POIUtils.getCellValue(sheet, cellLocation);

			int startIndex = value.indexOf("(");
			if (startIndex != -1) {
				int middleIndex = value.indexOf(",", startIndex + 1);
				if (middleIndex != -1) {
					width = Integer.parseInt(value.substring(startIndex + 1,
							middleIndex).trim());
					height = Integer.parseInt(value.substring(middleIndex + 1,
							value.length() - 1).trim());
				}
			}

			this.setImage(workbook, sheet, cellLocation, width, height);
		}
	}

	private void setImage(XSSFWorkbook workbook, XSSFSheet sheet,
			CellLocation cellLocation, int width, int height) {
		POIUtils.setCellValue(sheet, cellLocation, "");
System.out.println("this.imageBuffer:" + this.imageBuffer);
		if (this.imageBuffer != null) {
			XSSFDrawing  patriarch = sheet.createDrawingPatriarch();

			XSSFPicture picture = patriarch.createPicture(
					new XSSFClientAnchor(), this.pictureIndex);

			Dimension dimension = picture.getImageDimension();
			float rate = dimension.width / dimension.height;
			float specifiedRate = width / height;

			if (width == -1 || height == -1) {
				width = dimension.width;
				height = dimension.height;
				
			} else {
				if (rate > specifiedRate) {
					if (dimension.width > width) {
						height = (int) (width / rate);

					} else {
						width = dimension.width;
						height = dimension.height;
					}

				} else {
					if (dimension.height > height) {
						width = (int) (height * rate);

					} else {
						width = dimension.width;
						height = dimension.height;
					}
				}
			}

			XSSFClientAnchor preferredSize = this.getPreferredSize(sheet,
					new XSSFClientAnchor(0, 0, 0, 0, (short) cellLocation.c,
							cellLocation.r, (short) 0, 0), width, height);
			picture.getAnchor().setDx1(preferredSize.getDx1());
			picture.getAnchor().setDx2(preferredSize.getDx2());
			picture.getAnchor().setDy1(preferredSize.getDy1());
			picture.getAnchor().setDy2(preferredSize.getDy2());
		}
	}

	public XSSFClientAnchor getPreferredSize(XSSFSheet sheet,
			XSSFClientAnchor anchor, int width, int height) {
		float w = 0.0F;
		w += getColumnWidthInPixels(sheet, anchor.getCol1())
				* (float) (1 - anchor.getDx1() / 1024);

		short col2 = (short) (anchor.getCol1() + 1);
		int dx2 = 0;
		for (; w < (float) width; w += getColumnWidthInPixels(sheet, col2++))
			;
		if (w > (float) width) {
			col2--;
			float cw = getColumnWidthInPixels(sheet, col2);
			float delta = w - (float) width;
			dx2 = (int) (((cw - delta) / cw) * 1024F);
		}
		anchor.setCol2(col2);
		anchor.setDx2(dx2);
		float h = 0.0F;
		h += (float) (1 - anchor.getDy1() / 256)
				* getRowHeightInPixels(sheet, anchor.getRow1());
		int row2 = anchor.getRow1() + 1;
		int dy2 = 0;
		for (; h < (float) height; h += getRowHeightInPixels(sheet, row2++))
			;
		if (h > (float) height) {
			row2--;
			float ch = getRowHeightInPixels(sheet, row2);
			float delta = h - (float) height;
			dy2 = (int) (((ch - delta) / ch) * 256F);
		}
		anchor.setRow2(row2);
		anchor.setDy2(dy2);
		return anchor;
	}

	private float getColumnWidthInPixels(XSSFSheet sheet, int column) {
		int cw = sheet.getColumnWidth(column);
		float px = getPixelWidth(sheet, column);
		return (float) cw / px;
	}

	private float getRowHeightInPixels(XSSFSheet sheet, int i) {
		XSSFRow row = sheet.getRow(i);
		float height;
		if (row != null) {
			height = row.getHeight();
		} else {
			height = sheet.getDefaultRowHeight();
		}

		return height / 15F;
	}

	private float getPixelWidth(XSSFSheet sheet, int column) {
		int def = sheet.getDefaultColumnWidth() * 256;
		int cw = sheet.getColumnWidth(column);
		return cw != def ? 36.56F : 32F;
	}

}
