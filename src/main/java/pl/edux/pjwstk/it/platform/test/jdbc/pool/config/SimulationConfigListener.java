package pl.edux.pjwstk.it.platform.test.jdbc.pool.config;

/**
 * Contract to be implemented by classes that want to be notified when an application config property changes.
 * Note that the initial values of loaded configuration properties will not be notified through this interface.
 * @author bdrabik
 */
public interface SimulationConfigListener {

	/**
	 * Invoked when there is a modification of the configuration of a resource. Use this callback to reconfigure the resource in your app
	 */
	void onAppConfigUpdated();

}
