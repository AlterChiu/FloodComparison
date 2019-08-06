package FloodComparison.Iot.Historical;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import asciiFunction.AsciiBasicControl;
import usualTool.AtCommonMath;
import usualTool.AtFileReader;
import usualTool.AtFileWriter;
import usualTool.TimeTranslate;

public class HistoricalStatics {
	private static Map<String, String> commandList;
	private static Map<String, Double[]> iotPosition;
	private static Map<String, List<Double>> iotValues;
	private static List<AsciiBasicControl> floodResults;
	private static Map<String, List<Double>> comparedValueList;

	private static List<String> timeLine = new ArrayList<>();;
	private static String inputTimeFormate = "yyyy/MM/dd HH:mm:ss";
	private static String outputTimeFormate = "yyyy/MM/dd HH:mm";

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		/*
		 * command Line
		 * 
		 * -IoTPosition : iotPositionFile
		 * 
		 * -IoTValues : iotValues
		 * 
		 * -FloodResult : give the FloodResult folder
		 * 
		 * -OutputFolder : giver the output folder , the same as jar file default
		 * 
		 * -FloodStartTime : give the time that flood start, default 1 ( if the flood
		 * statics is from dm1d0001.asc , then give this variance "1" , SOBEK default by
		 * 1)
		 * 
		 * -DetecGrid : give the buffer grid (if you want to detect 3*3 , set the
		 * DetectGrid to 1, 5*5 to 2 , default by 1)
		 * 
		 */

		/*
		 * test
		 */
		commandList = new TreeMap<>();
		;
		commandList.put("-IoTPosition",
				"E:\\LittleProject\\IotSensorComparision\\20190702 - 桃園\\Sensor\\SensorPosition.csv");
		commandList.put("-IoTValues",
				"E:\\LittleProject\\IotSensorComparision\\20190702 - 桃園\\Sensor\\SensorValue.csv");
		commandList.put("-FloodResult", "E:\\LittleProject\\IotSensorComparision\\20190702 - 桃園\\floodResult\\");
		commandList.put("-OutputFolder", "E:\\LittleProject\\IotSensorComparision\\20190702 - 桃園\\output\\");
		commandList.put("-FloodStartTime", "1");
		commandList.put("-DetecGrid", "1");

		// get command list
//		commandList = commandLine(args);

		// get iotProperty

		iotPosition = getIotPosition(commandList.get("-IoTPosition"));
		iotValues = getIotValues(commandList.get("-IoTValues"));

		// get flood result
		floodResults = getFlooResultList(commandList.get("-FloodResult"));

		// compare the each station
		comparedValueList = getComparedValueList();

		// output the comparison
		outputResult();
	}

	/*
	 * output the comparison result
	 */
	// +++++++++++++++++++++++++++++++++++++++++++
	private static void outputResult() throws IOException {
		String saveFolder = commandList.get("-OutputFolder") + "\\";

		for (String station : iotValues.keySet()) {
			List<String[]> outList = new ArrayList<>();
			outList.add(new String[] { "time", "observation", "simulation" });

			for (int index = 0; index < timeLine.size(); index++) {
				try {
					outList.add(new String[] {
							TimeTranslate.StringGetSelected(timeLine.get(index), inputTimeFormate, outputTimeFormate),
							iotValues.get(station).get(index) + "", comparedValueList.get(station).get(index) + "" });
				} catch (ParseException e) {
					outList.add(new String[] { timeLine.get(index), iotValues.get(station).get(index) + "",
							comparedValueList.get(station).get(index) + "" });
				}
			}
			new AtFileWriter(outList.parallelStream().toArray(String[][]::new), saveFolder + station + ".csv")
					.csvWriter();
		}
	}
	// +++++++++++++++++++++++++++++++++++++++++++

	/*
	 * Comparison step
	 */
	// +++++++++++++++++++++++++++++++++++++++++++
	private static Map<String, List<Double>> getComparedValueList() {
		Map<String, List<Double>> comparedValueList = new TreeMap<>();

		// initial map
		for (String station : iotPosition.keySet()) {
			comparedValueList.put(station, new ArrayList<>());
		}

		// get the comparison value , IoT sensor to simulation
		for (int floodIndex = 0; floodIndex < floodResults.size(); floodIndex++) {
			AsciiBasicControl floodResult = floodResults.get(floodIndex);

			// get station position
			for (String station : comparedValueList.keySet()) {
				Double[] coordinate = iotPosition.get(station);
				int[] position = floodResult.getPosition(coordinate[0], coordinate[1]);

				// detect buffer grid
				List<Double> temptList = new ArrayList<>();
				int buffer = Integer.parseInt(commandList.get("-DetecGrid"));
				double targetValue = iotValues.get(station).get(floodIndex);

				for (int column = -1 * buffer; column <= buffer; column++) {
					for (int row = -1 * buffer; row <= buffer; row++) {

						String temptValue = floodResult.getValue(column + position[0], row + position[1]);
						if (!temptValue.equals(floodResult.getNullValue())) {
							temptList.add(Double.parseDouble(temptValue));
						}
					}
				}

				// get closest value from detect grid
				comparedValueList.get(station).add(new AtCommonMath(temptList).getClosestValue(targetValue));
			}
		}

		return comparedValueList;
	}
	// +++++++++++++++++++++++++++++++++++++++++++

	/*
	 * necessary file
	 */
	// +++++++++++++++++++++++++++++++++++++++++++
	private static Map<String, Double[]> getIotPosition(String iotPositionFile) {
		Map<String, Double[]> iotPosition = new TreeMap<>();
		try {
			for (String temptLine[] : new AtFileReader(iotPositionFile).getCsv(1, 0)) {
				iotPosition.put(temptLine[0],
						new Double[] { Double.parseDouble(temptLine[1]), Double.parseDouble(temptLine[2]) });
			}
		} catch (IOException e) {
			System.out.println("iot position file reading fail");
			e.printStackTrace();
		}
		return iotPosition;
	}

	private static Map<String, List<Double>> getIotValues(String iotValueFile) {
		Map<String, List<Double>> iotValues = new TreeMap<>();
		try {
			String content[][] = new AtFileReader(iotValueFile).getCsv();

			// initial map
			for (int column = 1; column < content[0].length; column++) {
				iotValues.put(content[0][column].split(" +")[0], new ArrayList<Double>());
			}

			// setting value list
			for (int row = 1; row < content.length; row++) {

				// setting time line
				timeLine.add(content[row][0]);
				for (int column = 1; column < content[0].length; column++) {

					// if the value is null add 0
					try {
						iotValues.get(content[0][column].split(" +")[0]).add(Double.parseDouble(content[row][column]));
					} catch (Exception e) {
						iotValues.get(content[0][column].split(" +")[0]).add(0.);
					}
				}
			}

		} catch (IOException e) {
			System.out.println("iot values file reading fail");
			e.printStackTrace();
		}
		return iotValues;
	}

	private static List<AsciiBasicControl> getFlooResultList(String floodResultFolder) {
		List<AsciiBasicControl> floodResult = new ArrayList<>();

		try {
			for (int index = Integer.parseInt(commandList.get("-FloodStartTime")); index <= timeLine.size(); index++) {
				floodResult.add(
						new AsciiBasicControl(floodResultFolder + "\\dm1d" + String.format("%04d", index) + ".asc"));
			}
		} catch (Exception e) {
			System.out.println("flodd result read fail");
			e.printStackTrace();
		}
		return floodResult;
	}
	// ++++++++++++++++++++++++++++++++++++++++++++

	/*
	 * command line checking
	 */
	// ++++++++++++++++++++++++++++++++++++++++++++
	private static Map<String, String> commandLine(String[] args) {
		Map<String, String> commandList = new TreeMap<>();
		commandList.put("-OutputFolder", new File(".").getAbsolutePath());
		commandList.put("-FloodStartTime", "1");
		commandList.put("-DetectGrid", "1");

		for (int index = 0; index < args.length; index = index + 2) {
			commandList.put(args[index], args[index + 1]);
		}
		commandList.keySet().forEach(key -> System.out.println(key + "\t:\t" + commandList.get(key)));
		return commandList;
	}
	// ++++++++++++++++++++++++++++++++++++++++++++
}
