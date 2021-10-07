package burdenSharing2;

import java.awt.Color;

import sweep.GUIStateSweep;
import sweep.SimStateSweep;

public class GraphicUI extends GUIStateSweep {

	public GraphicUI(SimStateSweep state, int gridWidth, int gridHeight, Color backdrop, Color agentDefaultColor,
			boolean agentPortrayal) {
		super(state, gridWidth, gridHeight, backdrop, agentDefaultColor, agentPortrayal);
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		GraphicUI.initialize(SimEnvironment.class, Experimenter.class, GraphicUI.class, 600, 600, Color.white, Color.blue, false, spaces.SPARSE);

	}

}
