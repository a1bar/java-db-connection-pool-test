package pl.edu.pja.master.s12621;

/**
 * Created by bartosz.drabik
 */
public interface SimulationMBean {

	long getNumSuccess();

	long getNumFailed();

	long getNumQueued();

	long getNumDropped();

	long getNumActive();

}
