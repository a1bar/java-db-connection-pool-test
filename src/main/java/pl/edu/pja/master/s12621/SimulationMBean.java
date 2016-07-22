package pl.edu.pja.master.s12621;

/**
 * Created by bartosz.drabik
 */
public interface SimulationMBean {

	void leakConnection();

	long getNumSuccess();

	long getNumFailed();

	long getNumQueued();

	long getNumDropped();

	long getNumLeaked();

	long getNumActive();

}
