package burdenSharing2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

public class Input {
    String fileDirectory;
    Path path;
    List<String[]> data;
    Map<Integer, InfoIdentifier> info = new HashMap<Integer, InfoIdentifier>();

    public Input(String fileDirectory) {
        // TODO Auto-generated constructor stub
        this.fileDirectory = fileDirectory;
    }

    public Map<Integer, InfoIdentifier> getDataInformation() {
        // read in files"
        List<String> lines;
//		path = Paths.get("/Users/kaiyinlin/Desktop/dataByYear/1818.csv");
        path = Paths.get(fileDirectory);
        try {
            lines = Files.readAllLines(path);
            data = lines.stream().skip(1).map(line -> line.split(",")).collect(Collectors.toList());
//			for(String[] d: data) {
//				System.out.println(Arrays.toString(d));
//			}
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // parse file information
        for (String[] lst : data) {
            Integer alliance;
            Integer i = Integer.valueOf(lst[1]); //key
            Integer j = Integer.valueOf(lst[2]); 
            double capI = Double.valueOf(lst[3]);
            Integer cultureIndex = Integer.valueOf(lst[5]);
            Integer democI = Integer.valueOf(lst[6]);
            Integer neighb = Integer.valueOf(lst[8]);
            Integer enemy = Integer.valueOf(lst[9]);
            if (lst.length == 11) {
                alliance = Integer.valueOf(lst[10]);
            } else {
                alliance = 0;
            }

            InfoIdentifier agentInfo = info.getOrDefault(i, new InfoIdentifier(i, capI, democI, new HashSet<Integer>(),
                    new HashSet<Integer>(), new HashMap<Integer, Integer>(), new HashSet<Integer>()));
            agentInfo.updateCulture(i, cultureIndex);
            
            if (enemy == 1 && !i.equals(j)) {
                agentInfo.updateEnemy(j);
            }
//            if (neighb == 1 && !i.equals(j)) {
//                agentInfo.updateNeighbor(i);
//            }
            if (neighb == 1) {
                agentInfo.updateNeighbor(j);
            }
            if (alliance == 1 && !i.equals(j)) {
                agentInfo.updateAlliance(j);
            }
            info.put(i, agentInfo);

        }
        return this.info;
    }

}
