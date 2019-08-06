package FloodComparison.Polygon.statics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gdal.ogr.Geometry;

import asciiFunction.AsciiBasicControl;
import geo.gdal.GdalGlobal;
import geo.gdal.SpatialReader;
import usualTool.AtCommonMath;
import usualTool.AtFileWriter;

public class PolygonStatics {
	public static Map<String, String> userSettingMap = null;
	public static AsciiBasicControl floodResult = null;
	public static List<AsciiBasicControl> levelingAscii = new ArrayList<>();
	public static SpatialReader shapeFile = null;

	private static double levelBondarys[][] = new double[][] { { 0.05, 0.3 }, { 0.3, 0.5 }, { 0.5, 99 } };

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		/*
		 * 
		 */
		
		/*
		 * -FloodResult : give the path of the flood result asciiFile (Maxd0)
		 * 
		 * -ShpFile : give the path of the shapeFile
		 * 
		 * -Save : give the folder path where the statics file save
		 * 
		 * -NameField : which index
		 * 
		 * -DepthFilter : give the filter in formation 0.05-0.3,0.3-0.5,0.5-99
		 */
//		userSettingMap = new TreeMap<>();
//		userSettingMap.put("-FloodResult", "C:\\Users\\alter\\Desktop\\FloodComparision\\dm1maxd0.asc");
//		userSettingMap.put("-ShpFile", "C:\\Users\\alter\\Desktop\\FloodComparision\\ShapeFile.shp");
//		userSettingMap.put("-Save", "C:\\Users\\alter\\Desktop\\FloodComparision\\");
//		userSettingMap.put("-NameField", "Town_ID");
//		userSettingMap.put("-DepthFilter", "0.05-0.3,0.3-0.5,0.5-99");

		// user setting
		System.getProperties().put("java.library.path", new File(".") + "\\FloodResultStaics_lib\\GDAL\\");
		System.out.println(System.getProperty("java.library.path"));
		
		List<String> userSetting = new ArrayList<>();
		int index = 0;
		try {
			while (args[index] != null) {
				userSetting.add(args[index]);
				index++;
			}
		} catch (Exception e) {
		}
//
//		// user setting map
		userSettingMap = userSettingMap(userSetting);

		// output ListSetting
		List<String[]> outList = new ArrayList<>();
		List<String> title = new ArrayList<>();
		title.add("polygonName");
		title.add("maxFloodDepth");

		// get the filter
		List<Double[]> filters = new ArrayList<>();
		String[] filterList = userSettingMap.get("-DepthFilter").split(",");
		for (String filter : filterList) {
			title.add(filter);
			String[] boundary = filter.split("-");
			filters.add(new Double[] { Double.parseDouble(boundary[0]), Double.parseDouble(boundary[1]) });
		}

		// setting output title
		outList.add(title.parallelStream().toArray(String[]::new));

		/*
		 * notice
		 * 
		 * 1. coordination of the shapeFile and asciiFile should be the same
		 * 
		 * 2. now it only set up three level of alerting (5-30,30-50,50up)
		 * 
		 * 
		 * 
		 * Compare steps
		 * 
		 * 1. read the asciiFile
		 * 
		 * 2. create the others level asciiFile(5-30,30-50,50up)
		 * 
		 * 3. read the ShapeFile
		 * 
		 * 4. Split the ShpaeFile to several polygon
		 * 
		 * 5. statics the flood result in each polygon
		 * 
		 * 6. output the statics file
		 * 
		 */

		// read the asciiFile
		try {
			floodResult = new AsciiBasicControl(userSettingMap.get("-FloodResult"));
		} catch (IOException e) {
			System.out.println("necessary command -FloodResult is missing");
			e.getStackTrace();
		}

		// create other level
		for (double[] levelBondary : levelBondarys) {
			levelingFloodResult(levelBondary[0], levelBondary[1]);
		}

		// read the shapeFile
		try {
			shapeFile = new SpatialReader(userSettingMap.get("-ShpFile"));
		} catch (Exception e) {
			System.out.println("necessary command -ShpFile is missing");
			e.getStackTrace();
		}

		// split shpFile to several polygon
		List<Geometry> polygons = shapeFile.getGeometryList();

		for (int featureIndex = 0; featureIndex < polygons.size(); featureIndex++) {
			List<String> polygonStatics = new ArrayList<>();
			Geometry polygon = polygons.get(featureIndex);
			Map<String, String> polygonFeature = shapeFile.getAttributeTable().get(featureIndex);

			// add polygon name
			polygonStatics.add(polygonFeature.get(userSettingMap.get("-NameField")));
			polygonStatics.add(new AtCommonMath(floodResult.getPolygonValueList(polygon)).getMax() + "");

			for (AsciiBasicControl temptAscii : levelingAscii) {
				// to statics how many cell are there in polygon
				polygonStatics.add(temptAscii.getCount(polygon) + "");
			}
			outList.add(polygonStatics.parallelStream().toArray(String[]::new));
		}

		// output the static
		new AtFileWriter(outList.parallelStream().toArray(String[][]::new), userSettingMap.get("-Save") + "staics.csv")
				.csvWriter();
	}

	/*
	 * 
	 */

	/*
	 * to leveling the asciiFile to different depth
	 */
	private static void levelingFloodResult(double lowerLevel, double upperLevel) throws IOException {
		AsciiBasicControl temptAscii = new AsciiBasicControl(userSettingMap.get("-FloodResult"));

		for (int row = 0; row < Integer.parseInt(temptAscii.getProperty().get("row")); row++) {
			for (int column = 0; column < Integer.parseInt(temptAscii.getProperty().get("column")); column++) {
				String temptValue = temptAscii.getValue(column, row);
				if (!temptValue.endsWith(temptAscii.getNullValue())) {
					if (Double.parseDouble(temptValue) < lowerLevel || Double.parseDouble(temptValue) > upperLevel) {
						temptAscii.setValue(column, row, temptAscii.getNullValue());
					}
				}
			}
		}

		new AtFileWriter(temptAscii.getAsciiFile(),
				userSettingMap.get("-Save") + "//FloodRersult(" + lowerLevel + "-" + upperLevel + ").asc")
						.textWriter(" ");
		levelingAscii.add(temptAscii);
	}

	/*
	 * to get the userSetting
	 */
	private static Map<String, String> userSettingMap(List<String> userSetting) {
		Map<String, String> userSettingMap = new TreeMap<>();
		for (int index = 0; index < userSetting.size(); index++) {
			System.out.println(userSetting.get(index) + ":" + userSetting.get(index + 1));
			userSettingMap.put(userSetting.get(index), userSetting.get(index + 1));
			index++;
		}
		return userSettingMap;
	}

}
