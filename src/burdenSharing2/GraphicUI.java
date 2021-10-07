package burdenSharing2;

import java.awt.Color;
import java.net.URL;


import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.engine.SimState;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Bag;
import sim.util.media.chart.HistogramGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;
import spaces.Spaces;
import sweep.GUIStateSweep;
import sweep.SimStateSweep;

import javax.swing.*;

public class GraphicUI extends GUIStateSweep {

	public GraphicUI(SimStateSweep state, int gridWidth, int gridHeight, Color backdrop, Color agentDefaultColor,
			boolean agentPortrayal) {
		super(state, gridWidth, gridHeight, backdrop, agentDefaultColor, agentPortrayal);
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws InterruptedException {
		/*
		Set all arguments from java inputs
		 */
		int year = 1816;
		String inputDataDirectory = "/Users/kaiyinlin/Desktop/dataByYear/";
		String fileAddress = "/Users/kaiyinlin/Desktop/";
		boolean autoMode = false;
		try {
			year = Integer.valueOf(args[0]);
			inputDataDirectory = args[1];
			fileAddress = args[2];
		}catch (ArrayIndexOutOfBoundsException e){
			System.out.println("ArrayIndexOutOfBoundsException caught");
		}
		SimEnvironment simEnvironment = new SimEnvironment(200, Experimenter.class,
				fileAddress, inputDataDirectory, year);
		System.out.println("RUNNING YEAR " + year);

		GraphicUI.initialize(simEnvironment, Experimenter.class, GraphicUI.class, 600, 600,
				Color.white, Color.blue, false, spaces.SPARSE, autoMode);

	}



}
