package burdenSharing2;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class InfoIdentifier {
	private final Integer id;
	private final double capability;
	private final Set<Integer> enemy;
	private final Integer democracy;
	private final Set<Integer> neighbor;
	private final Map<Integer, Integer> culture;
	private final Set<Integer> alliance;

	public InfoIdentifier(Integer id, double capability, Integer democracy, Set<Integer> enemy, 
			Set<Integer> neighbor, Map<Integer, Integer> culture, Set<Integer> alliance) {
		this.id = id;
		this.capability = capability;
		this.enemy = enemy;
		this.democracy = democracy;
		this.neighbor = neighbor;
		this.culture = culture;
		this.alliance = alliance;
	}

	public Integer getId() {
		return id;
	}
	
	public double getCapability() {
		return capability;
	}
	
	public Integer getDemocracy() {
		return democracy;
	}
	
	public Set<Integer> getEnemy(){
		return enemy;
	}
	
	public Set<Integer> getNeighbor(){
		return neighbor;
	}

	public Set<Integer> getAlliance(){ return alliance; }

	public Map<Integer, Integer> getCulture(){
		return culture;
	}
	
	public void updateEnemy(Integer agentId) {
		if(!this.enemy.contains(agentId)) {
			this.enemy.add(agentId);
		}
	}
	
	public void updateNeighbor(Integer agentId) {
		if(!this.neighbor.contains(agentId)) {
			this.neighbor.add(agentId);
		}
	}

	public void updateAlliance(Integer agentId) {
		if(!this.alliance.contains(agentId)) {
			this.alliance.add(agentId);
		}
	}
	
	public void updateCulture(Integer agentId, Integer sameCulture) { //agentID indicates j's id and see if i and j has same culture
		if (this.culture.containsKey(agentId)) {
			this.culture.replace(agentId, sameCulture);
		}else {
			this.culture.put(agentId, sameCulture);
		}
	}

}
