package pl.edux.pjwstk.it.platform.test.jdbc.pool;

public interface SimulationMBean {

	void leakConnection();

	long getNumSuccess();

	long getNumFailed();

	long getNumQueued();

	long getNumDropped();

	long getNumLeaked();

	long getNumActive();

}
